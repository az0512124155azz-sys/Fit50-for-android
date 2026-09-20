const PROJECT_ID = 'fit50-plus';
const DATABASE = '(default)';
const SENDER_NAME = 'Fit50+';

/**
 * Run once.
 * Creates a time trigger that processes password reset requests every minute.
 */
function installTrigger() {
  testConnection();

  const triggers = ScriptApp.getProjectTriggers();

  triggers.forEach(trigger => {
    if (trigger.getHandlerFunction() === 'processPasswordResetQueue') {
      ScriptApp.deleteTrigger(trigger);
    }
  });

  ScriptApp.newTrigger('processPasswordResetQueue')
    .timeBased()
    .everyMinutes(1)
    .create();

  Logger.log('Trigger installed successfully.');

  // Process requests that are already waiting.
  processPasswordResetQueue();
}

/**
 * Verifies Firestore access and confirms that the script
 * has an available mail quota.
 */
function testConnection() {
  Logger.log('Testing Firestore access...');

  const url =
    'https://firestore.googleapis.com/v1/projects/' +
    encodeURIComponent(PROJECT_ID) +
    '/databases/' +
    encodeURIComponent(DATABASE) +
    '/documents/passwordResetRequests?pageSize=1';

  const response = UrlFetchApp.fetch(url, {
    method: 'get',
    headers: {
      Authorization: 'Bearer ' + ScriptApp.getOAuthToken()
    },
    muteHttpExceptions: true
  });

  const code = response.getResponseCode();

  Logger.log('Firestore HTTP status: ' + code);

  if (code >= 300) {
    throw new Error(
      'Firestore connection failed (' +
      code +
      '): ' +
      response.getContentText()
    );
  }

  Logger.log('Firestore connection OK.');

  const quota = MailApp.getRemainingDailyQuota();

  Logger.log('Remaining daily email quota: ' + quota);

  if (quota <= 0) {
    throw new Error('No email sending quota remains for today.');
  }

  Logger.log('Mail service OK.');

  return true;
}

/**
 * Trigger worker.
 */
function processPasswordResetQueue() {
  Logger.log('Starting password reset worker...');

  const docs = listResetRequests_();

  Logger.log('Found ' + docs.length + ' recent reset requests.');

  let processed = 0;

  docs.forEach(doc => {
    const fields = doc.fields || {};

    const status = value_(fields.status);
    const gmailStatus = value_(fields.gmailStatus);
    const email = String(value_(fields.email) || '')
      .trim()
      .toLowerCase();

    Logger.log(
      'Checking ' +
      email +
      ' | status=' +
      status +
      ' | mail=' +
      gmailStatus
    );

    if (
      status !== 'queued' &&
      status !== 'firebase_accepted' &&
      status !== 'admin_accepted'
    ) {
      return;
    }

    if (gmailStatus === 'sent' || gmailStatus === 'sending') {
      return;
    }

    if (!email || !email.includes('@')) {
      Logger.log('Skipping invalid email.');
      return;
    }

    const docName = doc.name;

    try {
      updateResetRequest_(docName, {
        gmailStatus: {
          stringValue: 'sending'
        },
        gmailLastAttemptAt: {
          timestampValue: new Date().toISOString()
        },
        gmailError: {
          stringValue: ''
        }
      });

      Logger.log('Generating reset link for ' + email + '...');

      const resetLink = generatePasswordResetLink_(email);

      Logger.log('Reset link generated.');

      sendPasswordResetEmail_(email, resetLink);

      Logger.log('Email sent.');

      updateResetRequest_(docName, {
        status: {
          stringValue: 'completed'
        },
        gmailStatus: {
          stringValue: 'sent'
        },
        gmailSentAt: {
          timestampValue: new Date().toISOString()
        },
        gmailError: {
          stringValue: ''
        }
      });

      processed++;

      Logger.log('Completed reset for ' + email + '.');

    } catch (error) {
      const message = String(
        error && error.message ? error.message : error
      ).slice(0, 1500);

      Logger.log(
        'Reset failed for ' +
        email +
        ': ' +
        message
      );

      try {
        updateResetRequest_(docName, {
          gmailStatus: {
            stringValue: 'error'
          },
          gmailError: {
            stringValue: message
          },
          gmailLastAttemptAt: {
            timestampValue: new Date().toISOString()
          }
        });
      } catch (updateError) {
        Logger.log(
          'Could not save error status: ' +
          String(updateError)
        );
      }
    }
  });

  Logger.log(
    'Worker finished. Processed: ' +
    processed
  );
}

/**
 * Reads recent password reset requests from Firestore.
 */
function listResetRequests_() {
  const url =
    'https://firestore.googleapis.com/v1/projects/' +
    encodeURIComponent(PROJECT_ID) +
    '/databases/' +
    encodeURIComponent(DATABASE) +
    '/documents/passwordResetRequests' +
    '?pageSize=100' +
    '&orderBy=createdAt%20desc';

  const response = googleRequest_(
    url,
    'get'
  );

  const body = JSON.parse(
    response.getContentText() || '{}'
  );

  return body.documents || [];
}

/**
 * Creates a real Firebase password-reset OOB link.
 */
function generatePasswordResetLink_(email) {
  const url =
    'https://identitytoolkit.googleapis.com/v1/projects/' +
    encodeURIComponent(PROJECT_ID) +
    '/accounts:sendOobCode';

  const payload = {
    requestType: 'PASSWORD_RESET',
    email: email,
    returnOobLink: true
  };

  const response = UrlFetchApp.fetch(
    url,
    {
      method: 'post',
      contentType: 'application/json',
      headers: {
        Authorization:
          'Bearer ' +
          ScriptApp.getOAuthToken()
      },
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    }
  );

  const code = response.getResponseCode();
  const text = response.getContentText();

  if (code >= 300) {
    throw new Error(
      'Firebase reset-link error (' +
      code +
      '): ' +
      text
    );
  }

  const body = JSON.parse(
    text || '{}'
  );

  const link =
    body.oobLink ||
    body.oob_link;

  if (!link) {
    throw new Error(
      'Firebase did not return a reset link. Response: ' +
      text
    );
  }

  return link;
}

/**
 * Sends the reset email through the Google account
 * that owns/runs this Apps Script project.
 */
function sendPasswordResetEmail_(email, resetLink) {
  const subject =
    'איפוס הסיסמה שלך ב-Fit50+';

  const plainText =
    'שלום,\n\n' +
    'קיבלנו בקשה לאיפוס הסיסמה שלך ב-Fit50+.\n\n' +
    'כדי לבחור סיסמה חדשה, פתח את הקישור הבא:\n\n' +
    resetLink +
    '\n\n' +
    'אם לא ביקשת לאפס את הסיסמה, אין צורך לעשות דבר.\n\n' +
    'Fit50+';

  const safeLink = escapeHtml_(resetLink);

  const htmlBody =
    '<div dir="rtl" style="' +
      'font-family:Arial,sans-serif;' +
      'max-width:560px;' +
      'margin:0 auto;' +
      'background:#FAF7F2;' +
      'padding:36px;' +
      'border-radius:20px;' +
      'color:#1A1A1A;' +
    '">' +

      '<div dir="ltr" style="' +
        'font-size:36px;' +
        'font-weight:900;' +
        'margin-bottom:26px;' +
      '">' +
        '<span>Fit</span>' +
        '<span style="color:#E85A2C;">50</span>' +
        '<span>+</span>' +
      '</div>' +

      '<h1 style="' +
        'font-size:27px;' +
        'margin:0 0 14px;' +
      '">' +
        'איפוס סיסמה' +
      '</h1>' +

      '<p style="' +
        'font-size:16px;' +
        'line-height:1.7;' +
      '">' +
        'קיבלנו בקשה לאיפוס הסיסמה שלך ב-Fit50+.' +
      '</p>' +

      '<p style="' +
        'font-size:16px;' +
        'line-height:1.7;' +
      '">' +
        'לחץ על הכפתור הבא כדי לבחור סיסמה חדשה.' +
      '</p>' +

      '<div style="' +
        'text-align:center;' +
        'margin:32px 0;' +
      '">' +
        '<a href="' +
          safeLink +
        '" style="' +
          'display:inline-block;' +
          'background:#1F2B24;' +
          'color:#FFFFFF;' +
          'text-decoration:none;' +
          'padding:16px 28px;' +
          'border-radius:12px;' +
          'font-size:16px;' +
          'font-weight:700;' +
        '">' +
          'בחר סיסמה חדשה' +
        '</a>' +
      '</div>' +

      '<p style="' +
        'font-size:13px;' +
        'color:#6F6F6F;' +
        'line-height:1.6;' +
      '">' +
        'אם לא ביקשת לאפס את הסיסמה, אפשר להתעלם מהודעה זו.' +
      '</p>' +

      '<hr style="' +
        'border:0;' +
        'border-top:1px solid #DED8CE;' +
        'margin:28px 0 18px;' +
      '">' +

      '<div style="' +
        'font-size:12px;' +
        'color:#8A8A8A;' +
      '">' +
        'Fit50+ · כושר, תנועה ואיזון' +
      '</div>' +

    '</div>';

  MailApp.sendEmail({
    to: email,
    subject: subject,
    body: plainText,
    htmlBody: htmlBody,
    name: SENDER_NAME
  });
}

/**
 * Updates selected fields on the Firestore reset-request document.
 */
function updateResetRequest_(documentName, fields) {
  const parts =
    documentName.split('/documents/');

  if (parts.length !== 2) {
    throw new Error(
      'Invalid Firestore document name: ' +
      documentName
    );
  }

  const documentPath =
    parts[1];

  const updateMask =
    Object.keys(fields)
      .map(
        key =>
          'updateMask.fieldPaths=' +
          encodeURIComponent(key)
      )
      .join('&');

  const url =
    'https://firestore.googleapis.com/v1/projects/' +
    encodeURIComponent(PROJECT_ID) +
    '/databases/' +
    encodeURIComponent(DATABASE) +
    '/documents/' +
    documentPath +
    '?' +
    updateMask;

  googleRequest_(
    url,
    'patch',
    {
      fields: fields
    }
  );
}

/**
 * Authenticated request to a Google REST API.
 */
function googleRequest_(url, method, body) {
  const options = {
    method: method,
    headers: {
      Authorization:
        'Bearer ' +
        ScriptApp.getOAuthToken()
    },
    muteHttpExceptions: true
  };

  if (body !== undefined) {
    options.contentType =
      'application/json';

    options.payload =
      JSON.stringify(body);
  }

  const response =
    UrlFetchApp.fetch(
      url,
      options
    );

  const code =
    response.getResponseCode();

  if (code >= 300) {
    throw new Error(
      'Google API request failed (' +
      code +
      '): ' +
      response.getContentText()
    );
  }

  return response;
}

/**
 * Converts a Firestore REST field value to JavaScript.
 */
function value_(field) {
  if (!field) {
    return null;
  }

  if (
    Object.prototype.hasOwnProperty.call(
      field,
      'stringValue'
    )
  ) {
    return field.stringValue;
  }

  if (
    Object.prototype.hasOwnProperty.call(
      field,
      'booleanValue'
    )
  ) {
    return field.booleanValue;
  }

  if (
    Object.prototype.hasOwnProperty.call(
      field,
      'integerValue'
    )
  ) {
    return Number(
      field.integerValue
    );
  }

  if (
    Object.prototype.hasOwnProperty.call(
      field,
      'doubleValue'
    )
  ) {
    return Number(
      field.doubleValue
    );
  }

  if (
    Object.prototype.hasOwnProperty.call(
      field,
      'timestampValue'
    )
  ) {
    return field.timestampValue;
  }

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
