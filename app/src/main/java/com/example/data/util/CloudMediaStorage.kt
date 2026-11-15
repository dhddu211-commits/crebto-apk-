package com.example.data.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class UploadedCloudMedia(
  val downloadUrl: String,
  val isVideo: Boolean,
)

object CloudMediaStorage {
  private const val MAX_FILE_BYTES = 180L * 1024L * 1024L

  suspend fun uploadPickedMedia(
    context: Context,
    sourceUri: Uri,
    folder: String,
    onProgress: (Float) -> Unit = {},
  ): UploadedCloudMedia =
    withContext(Dispatchers.IO) {
      val uid =
        Firebase.auth.currentUser?.uid
          ?: throw IllegalStateException("Please sign in with Google first.")

      val resolver = context.contentResolver
      val mimeType =
        resolver.getType(sourceUri)
          ?: if (folder in listOf("reels", "videos")) "video/mp4" else "image/jpeg"
      val isVideo = mimeType.startsWith("video/")
      val ext =
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
          ?: if (isVideo) "mp4" else "jpg"

      resolver.openAssetFileDescriptor(sourceUri, "r")?.use { afd ->
        if (afd.length > MAX_FILE_BYTES) {
          throw IllegalArgumentException("File exceeds 180 MB cloud upload limit.")
        }
      }

      val fileName = "media_${UUID.randomUUID().toString().replace("-", "").take(16)}.$ext"
      val storageRef = Firebase.storage.reference.child("crypto_media/$uid/$folder/$fileName")
      val metadata = StorageMetadata.Builder().setContentType(mimeType).build()

      try {
        val uploadTask = storageRef.putFile(sourceUri, metadata)
        uploadTask.addOnProgressListener { snapshot ->
          val total = snapshot.totalByteCount
          if (total > 0L) {
            onProgress((snapshot.bytesTransferred.toFloat() / total.toFloat()).coerceIn(0f, 1f))
          }
        }
        uploadTask.await()
        onProgress(1f)
        val httpsUrl = storageRef.downloadUrl.await().toString()
        UploadedCloudMedia(downloadUrl = httpsUrl, isVideo = isVideo)
      } catch (e: StorageException) {
        val msg =
          when (e.errorCode) {
            StorageException.ERROR_OBJECT_NOT_FOUND,
            StorageException.ERROR_BUCKET_NOT_FOUND ->
              "Firebase Storage is not enabled yet in Firebase Console. Enable Storage or paste a direct HTTPS URL."
            StorageException.ERROR_NOT_AUTHORIZED,
            StorageException.ERROR_NOT_AUTHENTICATED ->
              "Cloud Storage permission denied. Check Firebase Storage Rules or paste a direct HTTPS URL."
            else -> e.localizedMessage ?: "Cloud upload failed."
          }
        throw IllegalStateException(msg, e)
      }
    }

  fun isHttpUrl(url: String): Boolean {
    val normalized = MediaUrlResolver.normalizeInputUrl(url).lowercase()
    return normalized.startsWith("https://") || normalized.startsWith("http://")
  }

  fun isVideoUri(uriString: String): Boolean {
    val clean = uriString.trim().substringBefore("?").substringBefore("#").lowercase()
    val lower = uriString.trim().lowercase()
    return clean.endsWith(".mp4") ||
      clean.endsWith(".webm") ||
      clean.endsWith(".mkv") ||
      clean.endsWith(".mov") ||
      clean.endsWith(".m4v") ||
      clean.endsWith(".3gp") ||
      clean.endsWith(".m3u8") ||
      clean.endsWith(".mpd") ||
      lower.contains("mime=video") ||
      lower.contains("%2fvideos%2f") ||
      lower.contains("%2freels%2f") ||
      MediaUrlResolver.extractYouTubeVideoId(uriString) != null ||
      MediaUrlResolver.extractVimeoVideoId(uriString) != null
  }
}
