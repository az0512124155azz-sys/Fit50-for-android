const PROJECT_ID = 'fit50-plus';
const DATABASE = '(default)';
const SENDER_NAME = 'Fit50+';

function installTrigger() {
  const exists = ScriptApp.getProjectTriggers().some(t => t.getHandlerFunction() === 'processPasswordResetQueue');
  if (!exists) {
    ScriptApp.newTrigger('processPasswordResetQueue')
      .timeBased()
      .everyMinutes(1)
      .create();
  }
  processPasswordResetQueue();
}

function processPasswordResetQueue() {
  const docs = listRecentResetRequests_();

  docs.forEach(doc => {
    const fields = doc.fields || {};
    const status = value_(fields.status);
    const gmailStatus = value_(fields.gmailStatus);

    if (!['firebase_accepted', 'admin_accepted'].includes(status)) return;
    if (gmailStatus === 'sent' || gmailStatus === 'sending') return;

    const email = String(value_(fields.email) || '').trim().toLowerCase();
    if (!email || !email.includes('@')) return;

    const docName = doc.name;
    markRequest_(docName, {
      gmailStatus: { stringValue: 'sending' },
      gmailLastAttemptAt: { timestampValue: new Date().toISOString() }
    });

    try {
      const link = generateResetLink_(email);
      sendResetMail_(email, link);

      markRequest_(docName, {
        gmailStatus: { stringValue: 'sent' },
        gmailSentAt: { timestampValue: new Date().toISOString() },
        gmailError: { stringValue: '' }
      });
    } catch (err) {
      markRequest_(docName, {
        gmailStatus: { stringValue: 'error' },
        gmailError: { stringValue: String(err && err.message ? err.message : err).slice(0, 1000) },
        gmailLastAttemptAt: { timestampValue: new Date().toISOString() }
      });
    }
  });
}

function listRecentResetRequests_() {
  const url =
    'https://firestore.googleapis.com/v1/projects/' +
    encodeURIComponent(PROJECT_ID) +
    '/databases/' +
    encodeURIComponent(DATABASE) +
    '/documents/passwordResetRequests?pageSize=100&orderBy=createdAt%20desc';

  const res = UrlFetchApp.fetch(url, {
    method: 'get',
    headers: { Authorization: 'Bearer ' + ScriptApp.getOAuthToken() },
    muteHttpExceptions: true
  });

  if (res.getResponseCode() >= 300) {
    throw new Error('Firestore read failed: ' + res.getContentText());
  }

  const body = JSON.parse(res.getContentText() || '{}');
  return body.documents || [];
}

function generateResetLink_(email) {
  const url =
    'https://identitytoolkit.googleapis.com/v1/projects/' +
    encodeURIComponent(PROJECT_ID) +
    '/accounts:sendOobCode';

  const payload = {
    requestType: 'PASSWORD_RESET',
    email: email,
    returnOobLink: true,
    linkDomain: PROJECT_ID + '.firebaseapp.com'
  };

  const res = UrlFetchApp.fetch(url, {
    method: 'post',
    contentType: 'application/json',
    payload: JSON.stringify(payload),
    headers: { Authorization: 'Bearer ' + ScriptApp.getOAuthToken() },
    muteHttpExceptions: true
  });

  const text = res.getContentText();
  if (res.getResponseCode() >= 300) {
    throw new Error('Reset link generation failed: ' + text);
  }

  const body = JSON.parse(text || '{}');
  const link = body.oobLink || body.oob_link;
  if (!link) throw new Error('Firebase returned no reset link');
  return link;
}

function sendResetMail_(email, link) {
  const subject = 'איפוס הסיסמה שלך ב-Fit50+';
  const plain =
    'קיבלנו בקשה לאיפוס הסיסמה שלך ב-Fit50+.\n\n' +
    'פתח את הקישור הבא כדי לבחור סיסמה חדשה:\n' + link + '\n\n' +
    'אם לא ביקשת איפוס, אפשר להתעלם מההודעה.';

  const html =
    '<div dir="rtl" style="font-family:Arial,sans-serif;max-width:560px;margin:auto;background:#FAF7F2;padding:32px;border-radius:18px;color:#1A1A1A">' +
      '<div dir="ltr" style="font-size:34px;font-weight:900;margin-bottom:24px"><span>Fit</span><span style="color:#E85A2C">50</span><span>+</span></div>' +
      '<h2 style="margin:0 0 12px">איפוס סיסמה</h2>' +
      '<p style="line-height:1.7">קיבלנו בקשה לאיפוס הסיסמה שלך. לחץ על הכפתור כדי לבחור סיסמה חדשה.</p>' +
      '<p style="margin:28px 0"><a href="' + escapeHtml_(link) + '" style="display:inline-block;background:#1F2B24;color:white;text-decoration:none;padding:14px 22px;border-radius:12px;font-weight:700">בחר סיסמה חדשה</a></p>' +
      '<p style="font-size:13px;color:#666;line-height:1.6">אם לא ביקשת איפוס סיסמה, אפשר להתעלם מההודעה הזאת.</p>' +
    '</div>';

  GmailApp.sendEmail(email, subject, plain, {
    htmlBody: html,
    name: SENDER_NAME
  });
}

function markRequest_(documentName, fields) {
  const encodedName = documentName.split('/documents/')[1];
  const mask = Object.keys(fields)
    .map(k => 'updateMask.fieldPaths=' + encodeURIComponent(k))
    .join('&');

  const url =
    'https://firestore.googleapis.com/v1/projects/' +
    encodeURIComponent(PROJECT_ID) +
    '/databases/' +
    encodeURIComponent(DATABASE) +
    '/documents/' +
    encodedName +
    '?' + mask;

  const res = UrlFetchApp.fetch(url, {
    method: 'patch',
    contentType: 'application/json',
    payload: JSON.stringify({ fields: fields }),
    headers: { Authorization: 'Bearer ' + ScriptApp.getOAuthToken() },
    muteHttpExceptions: true
  });

  if (res.getResponseCode() >= 300) {
    throw new Error('Firestore update failed: ' + res.getContentText());
  }
}

function value_(field) {
  if (!field) return null;
  if ('stringValue' in field) return field.stringValue;
  if ('booleanValue' in field) return field.booleanValue;
  if ('integerValue' in field) return Number(field.integerValue);
  if ('doubleValue' in field) return Number(field.doubleValue);
  if ('timestampValue' in field) return field.timestampValue;
  return null;
}

function escapeHtml_(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
