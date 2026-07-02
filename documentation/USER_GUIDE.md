# ReY Platform User Setup Guide

This guide is written for users with limited technical experience to easily set up, configure, and maintain the ReY Platform ecosystem.

---

## 1. Installing & Initializing Private OML (Android APK)

### Step A: Install the APK
1. Transfer the generated `.apk` file to your Android phone.
2. Open the file on your device. If prompted, allow installations from "Unknown Sources" or your file manager app.
3. Click **Install** and launch the app.

### Step B: Complete the Setup Wizard
On first launch, you will be greeted by a 12-step setup wizard:
1. **Welcome**: Quick intro to the platform.
2. **App Password**: Define a secure password specific to this app (different from your phone lock screen).
3. **Permissions**: Accept the prompt to allow the app to send SMS (used for recovery codes), location mapping, and storage access.
4. **Battery optimization**: Follow instructions to turn off battery limits for this app so background monitoring doesn't stop.
5. **Services**: Tap to start the background syncer.
6. **Gallery Folder**: Type or select the folder where you store pictures you want to sync.
7. **Server Link**: Input the backend Worker URL (e.g. `https://rey-backend.yoursubdomain.workers.dev/api/v1`) and press **Run Sync Test**.
8. **AI Config**: Input your Gemini API key and recovery phone number.
9. **Finish**: Complete and enter the dashboard.

---

## 2. Viewer Website Login & Recovery

### Step A: Accessing the Vault
1. Open your deployed website URL in any browser.
2. Log in with the default credentials:
   - **Username**: `Rozuly`
   - **Password**: `Roza1448404Ali`
3. We recommend changing this password immediately using the Forgot Password recovery flow.

### Step B: Changing Passwords (Forgot Password Flow)
1. On the login page, click **Forgot?**.
2. Type `Rozuly` and click **Send**.
3. A verification code will be generated and sent via SMS to your phone from your linked Android device.
4. Type the 6-digit code on the website screen, verify, and input your new secure password.

---

## 3. Operations & Controls

### Publishing Messages (Status Updates)
1. In the Android app, tap **Edit Last Message Status**.
2. Type your message. You can use standard formatting chips like **Bold**, **Italic**, or **Quote**.
3. Press the **Checkmark** in the top action bar to publish. The website updates instantly.

### Viewer AI Assistant
1. Navigate to the **AI Assistant** tab on the website.
2. Type any questions about the owner's biography, schedule, or documents.
3. The chatbot uses RAG technology to index your uploaded PDFs/Notes and answers viewer queries accurately based on those files.
