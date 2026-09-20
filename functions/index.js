const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const crypto = require("node:crypto");

initializeApp();

const db = getFirestore();
const auth = getAuth();

function normalizeEmail(value) {
  return String(value || "").trim().toLowerCase();
}

function requireAdmin(request) {
  const expected = String(process.env.FIT50_ADMIN_EMAIL || "").toLowerCase();
  const actual = String(request.auth?.token?.email || "").toLowerCase();
  if (!expected || !request.auth || actual !== expected) {
    throw new HttpsError("permission-denied", "Admin access required");
  }
}

function emailHash(email) {
  return crypto.createHash("sha256").update(email).digest("hex");
}

async function logReset(email, source, status, extra = {}) {
  await db.collection("passwordResetRequests").add({
    email,
    source,
    status,
    createdAt: FieldValue.serverTimestamp(),
    ...extra
  });
}

async function sendFirebaseResetEmail(email) {
  const key = process.env.FIT50_WEB_API_KEY;
  if (!key) throw new Error("FIT50_WEB_API_KEY is not configured");

  const response = await fetch(
    "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" +
      encodeURIComponent(key),
    {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ requestType: "PASSWORD_RESET", email })
    }
  );

  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(body?.error?.message || "PASSWORD_RESET_SEND_FAILED");
  }
}

exports.requestPasswordReset = onCall(async (request) => {
  const email = normalizeEmail(request.data?.email);
  if (!email || !email.includes("@")) {
    throw new HttpsError("invalid-argument", "כתובת אימייל אינה תקינה");
  }

  const rateRef = db.collection("passwordResetRate").doc(emailHash(email));
  const rateSnap = await rateRef.get();
  const last = rateSnap.get("lastRequestedAt");
  const lastMs = last?.toMillis ? last.toMillis() : 0;

  if (Date.now() - lastMs < 60000) {
    return {
      ok: true,
      message: "אם קיים חשבון עם האימייל הזה, קישור האיפוס כבר נשלח."
    };
  }

  await rateRef.set({
    email,
    lastRequestedAt: FieldValue.serverTimestamp()
  }, { merge: true });

  let user = null;
  try {
    user = await auth.getUserByEmail(email);
  } catch (error) {
    if (error.code !== "auth/user-not-found") {
      await logReset(email, "app", "lookup_error", {
        error: String(error.message || error)
      });
      throw new HttpsError("internal", "לא הצלחנו לבדוק את החשבון");
    }
  }

  if (!user) {
    await logReset(email, "app", "not_found");
    return {
      ok: true,
      message: "אם קיים חשבון עם האימייל הזה, קישור האיפוס נשלח."
    };
  }

  try {
    await sendFirebaseResetEmail(email);
    await logReset(email, "app", "sent", {
      uid: user.uid,
      providers: user.providerData.map((p) => p.providerId)
    });
    return {
      ok: true,
      message: "קישור איפוס נשלח. בדוק גם ספאם וקידומי מכירות."
    };
  } catch (error) {
    await logReset(email, "app", "send_error", {
      uid: user.uid,
      error: String(error.message || error)
    });
    throw new HttpsError("internal", "Firebase לא הצליח לשלוח את מייל האיפוס");
  }
});

exports.lookupUser = onCall(async (request) => {
  requireAdmin(request);

  const email = normalizeEmail(request.data?.email);
  if (!email || !email.includes("@")) {
    throw new HttpsError("invalid-argument", "Invalid email");
  }

  try {
    const user = await auth.getUserByEmail(email);
    return {
      exists: true,
      uid: user.uid,
      email: user.email || "",
      emailVerified: user.emailVerified,
      disabled: user.disabled,
      displayName: user.displayName || "",
      providers: user.providerData.map((p) => p.providerId),
      createdAt: user.metadata.creationTime || "",
      lastSignInAt: user.metadata.lastSignInTime || ""
    };
  } catch (error) {
    if (error.code === "auth/user-not-found") {
      return { exists: false, email };
    }
    throw new HttpsError("internal", error.message || "Lookup failed");
  }
});

exports.adminSendPasswordReset = onCall(async (request) => {
  requireAdmin(request);

  const email = normalizeEmail(request.data?.email);
  if (!email || !email.includes("@")) {
    throw new HttpsError("invalid-argument", "Invalid email");
  }

  let user;
  try {
    user = await auth.getUserByEmail(email);
  } catch (error) {
    if (error.code === "auth/user-not-found") {
      throw new HttpsError("not-found", "No Firebase user with this email");
    }
    throw error;
  }

  await sendFirebaseResetEmail(email);
  await logReset(email, "admin", "sent", {
    uid: user.uid,
    providers: user.providerData.map((p) => p.providerId)
  });

  return { ok: true, email };
});

exports.listResetRequests = onCall(async (request) => {
  requireAdmin(request);

  const snapshot = await db.collection("passwordResetRequests")
    .orderBy("createdAt", "desc")
    .limit(100)
    .get();

  return {
    items: snapshot.docs.map((doc) => {
      const d = doc.data();
      return {
        id: doc.id,
        email: d.email || "",
        source: d.source || "",
        status: d.status || "",
        createdAt: d.createdAt?.toDate?.()?.toISOString?.() || "",
        providers: d.providers || [],
        error: d.error || ""
      };
    })
  };
});
