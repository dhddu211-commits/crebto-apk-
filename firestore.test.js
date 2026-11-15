const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_dhddu";
const ADMIN_EMAIL = "dhddu211@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function publicUserPayload(uid, extra = {}) {
  return {
    userId: uid,
    displayName: "Creator " + uid,
    username: "user_" + uid,
    bio: "Crypto creator",
    avatarUrl: "",
    avatarMediaType: "image",
    bannerUrl: "",
    bannerMediaType: "image",
    followersCount: 0,
    followingCount: 0,
    likesCount: 0,
    isBanned: false,
    isVerified: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...extra,
  };
}

function videoPayload(authorId, extra = {}) {
  return {
    authorId,
    authorName: "Alice",
    authorAvatarUrl: "",
    authorAvatarType: "image",
    videoType: "reel",
    title: "My Viral Reel",
    description: "Testing viral feed",
    tags: ["crypto", "viral"],
    category: "Tech",
    mediaUrl: "https://example.com/video.mp4",
    thumbnailUrl: "",
    durationSeconds: 30,
    likesCount: 0,
    commentsCount: 0,
    repostsCount: 0,
    favoritesCount: 0,
    viewsCount: 0,
    viralBoostScore: 1000,
    isBanned: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...extra,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read videos or public users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("videos").where("isBanned", "==", false).get());
  await assertFails(unauthDb.collection("users_public").where("isBanned", "==", false).get());
});

test("Public user: owner can create profile, shadow update isVerified fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    aliceDb.collection("users_public").doc(ALICE_UID).set(publicUserPayload(ALICE_UID))
  );

  // Shadow update attempting to set isVerified = true must fail
  await assertFails(
    aliceDb.collection("users_public").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: serverTimestamp(),
    })
  );

  // Updating animated video avatar & animated video banner succeeds
  await assertSucceeds(
    aliceDb.collection("users_public").doc(ALICE_UID).update({
      avatarUrl: "content://media/avatar.mp4",
      avatarMediaType: "video",
      bannerUrl: "content://media/banner.mp4",
      bannerMediaType: "video",
      updatedAt: serverTimestamp(),
    })
  );
});

test("PII Isolation: Bob cannot read Alice private PII, Admin can", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    aliceDb.collection("users_private").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phoneNumber: "+966500000000",
      preferredLanguage: "ar",
      isBanned: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  await assertFails(bobDb.collection("users_private").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users_private").get());

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("users_private").doc(ALICE_UID).get());
  await assertSucceeds(adminDb.collection("users_private").get());
});

test("Email Spoofing Test: unverified dhddu211@gmail.com cannot access admin operations", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("videos").doc("vid_1").set(videoPayload(ALICE_UID));
  });

  const spoofAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofAdminDb.collection("videos").doc("vid_1").update({
      isBanned: true,
      banReason: "Spoof",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(spoofAdminDb.collection("users_private").get());
});

test("Videos & Stories: non-admin cannot ban, verified Admin can ban video and story", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(aliceDb.collection("videos").doc("vid_1").set(videoPayload(ALICE_UID)));

  // Client aligned query passes
  await assertSucceeds(bobDb.collection("videos").where("isBanned", "==", false).get());
  // Unfiltered query for non-admin fails
  await assertFails(bobDb.collection("videos").get());

  // Non-admin cannot ban
  await assertFails(
    bobDb.collection("videos").doc("vid_1").update({
      isBanned: true,
      banReason: "Unauthorized",
      updatedAt: serverTimestamp(),
    })
  );

  // Admin can list all and ban
  await assertSucceeds(adminDb.collection("videos").get());
  await assertSucceeds(
    adminDb.collection("videos").doc("vid_1").update({
      isBanned: true,
      banReason: "Policy violation",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Chats & Moderation: participants can message, Admin can view & ban chat locking new messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const chatId = `${ALICE_UID}_${BOB_UID}`;
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      participantIds: [ALICE_UID, BOB_UID],
      participant1Name: "Alice",
      participant2Name: "Bob",
      lastMessage: "Hello",
      lastSenderId: ALICE_UID,
      isBanned: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      participantIds: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Hi Bob",
      createdAt: serverTimestamp(),
    })
  );

  // Admin can inspect chats & messages
  await assertSucceeds(adminDb.collection("chats").get());
  await assertSucceeds(adminDb.collection("chats").doc(chatId).collection("messages").get());

  // Admin bans the chat
  await assertSucceeds(
    adminDb.collection("chats").doc(chatId).update({
      isBanned: true,
      banReason: "Illegal conversation",
      updatedAt: serverTimestamp(),
    })
  );

  // Participants can no longer send messages in banned chat
  await assertFails(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_2").set({
      chatId,
      participantIds: [ALICE_UID, BOB_UID],
      senderId: BOB_UID,
      senderName: "Bob",
      text: "Blocked message",
      createdAt: serverTimestamp(),
    })
  );
});

test("Admin Alerts: only Admin can create alerts, target user can read and mark read", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  // Alice cannot create admin alerts
  await assertFails(
    aliceDb.collection("admin_alerts").doc("alert_1").set({
      targetUserId: BOB_UID,
      title: "Fake Alert",
      message: "Test",
      severity: "warning",
      sentByEmail: "alice@example.com",
      isRead: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Admin creates direct alert for Alice
  await assertSucceeds(
    adminDb.collection("admin_alerts").doc("alert_1").set({
      targetUserId: ALICE_UID,
      title: "تنبيه إداري",
      message: "يرجى الالتزام بسياسات منصة كريبتو",
      severity: "warning",
      sentByEmail: ADMIN_EMAIL,
      isRead: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob cannot read Alice's alert
  await assertFails(bobDb.collection("admin_alerts").doc("alert_1").get());

  // Alice can read her own alerts and mark isRead = true
  await assertSucceeds(
    aliceDb.collection("admin_alerts").where("targetUserId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("admin_alerts").doc("alert_1").update({
      isRead: true,
      updatedAt: serverTimestamp(),
    })
  );
});
