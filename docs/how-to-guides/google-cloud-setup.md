# How-to Setup Google Cloud Console

To use the Google Drive integration, you must enable the Drive API and register your Android application in the Google Cloud Console.

## 1. Enable Google Drive API
1. Visit the [Google Cloud Console](https://console.developers.google.com/).
2. Select your project (ID: `...`).
3. Navigate to **APIs & Services > Library**.
4. Search for **"Google Drive API"**.
5. Click **Enable**.

## 2. Configure OAuth Consent Screen
1. Navigate to **APIs & Services > OAuth consent screen**.
2. Set the type to **External** (unless you are part of a Workspace).
3. Add the scope: `https://www.googleapis.com/auth/drive.readonly`.

## 3. Register Android Client
1. Navigate to **APIs & Services > Credentials**.
2. Click **+ CREATE CREDENTIALS > OAuth client ID**.
3. Set Application type to **Android**.
4. Package name: `com.hitsuji.pdfdrivereader`.
5. **SHA-1 certificate fingerprint**:
   - Run this in your terminal to get your debug fingerprint:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
   - Copy the SHA-1 value and paste it into the console.

## 4. Troubleshooting
If you see a **403 Forbidden** error in the logs after sign-in, double-check that Step 1 (Enabling the API) has been completed. It may take 5-10 minutes for the activation to propagate globally.
