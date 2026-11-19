# CHI MAIL — Implementation Memory and Handoff

> Last updated: 2026-11-19  
> Repository: https://github.com/Nadirinfograph23/Chi-Mail  
> Production webmail: https://webmail.chilya-catering.com/  
> Android application ID: `com.chilyacatering.chimail`

## 1. Product requirement (authoritative)

CHI MAIL must be a **real Android email client experience**, not a demo, mock inbox, or static email UI.

The existing webmail platform at `https://webmail.chilya-catering.com/` is the source of truth for mail operations. The Android app should provide a reliable mobile app container around that existing service, so the service itself performs authentication, mailbox access, sending, receiving, folders, message actions, and attachment handling.

The credentials previously shared by the user were supplied **only to inspect the webmail layout, folder structure, and navigation URLs**. They must never be:
- embedded in source code, APK, build files, or environment defaults;
- committed to GitHub, logs, screenshots, test fixtures, or documentation;
- used to auto-login or pre-fill a user's account;
- reused as a shared application account.

Every user must sign in with their own email account through the webmail's own login page.

## 2. Current verified repository state

The repository currently has:
- Next.js static export and Capacitor Android build pipeline.
- CHI MAIL branding and CHILYA logo at `public/chilya-logo.jpg`.
- Black/yellow styling and a responsive mock mailbox interface in `src/app/page.tsx`.
- A workflow at `.github/workflows/android-apk.yml` that builds a debug APK and uploads the artifact `CHI-MAIL-Android-APK`.

**Important:** the current `src/app/page.tsx` is still a demonstration UI. It contains hard-coded sample messages, does not connect to a mailbox, and does not perform real send/receive or attachment operations. Do not describe this build as a working email client.

The latest known APK workflow run before this handoff was:
- Run: https://github.com/Nadirinfograph23/Chi-Mail/actions/runs/44511061172
- It completed successfully, but that only confirms the demo app compiled. It does not prove real mail functionality.

## 3. Recommended architecture

### Primary approach: native Android WebView wrapper around the existing webmail

Do not reimplement IMAP, SMTP, mail storage, mailbox synchronization, or message parsing in the app unless the webmail provider explicitly exposes a supported API and the user approves that architecture.

Build a native Android WebView container that loads:

`https://webmail.chilya-catering.com/`

The website remains responsible for:
- login, logout, password reset, and account security;
- inbox and all other folders;
- fetching and reading messages;
- composing and sending messages;
- replying, forwarding, deleting, archiving, starring, and marking spam;
- drafts and sent mail;
- attachment upload, download, and preview;
- search, pagination, and server-side mailbox state.

The app is responsible for:
- CHI MAIL launcher identity and black/yellow splash/branding;
- mobile-safe WebView display and navigation;
- persistent, secure web session behavior using the platform's normal cookie store;
- Android file chooser support for attachments;
- download handling and opening supported downloaded files;
- Android back navigation;
- safe external-link handling;
- optional native push notifications, but only after a real push source/backend is established.

This approach lets any employee sign into their own account and use the same real mail service as the browser.

### Before finalizing the wrapper

Inspect the live webmail manually in a normal browser and record:
1. Login page URL and post-login landing URL.
2. Whether login redirects to a separate hostname or subdomain.
3. Actual folder names and folder navigation behavior.
4. Whether the site uses a supported mobile layout.
5. Whether attachment inputs use standard HTML file inputs and whether downloads use direct links/blob URLs.
6. Whether the site blocks embedded WebViews using CSP `frame-ancestors`, X-Frame-Options, user-agent checks, or other restrictions.
7. Whether the service already has PWA/service-worker push support.

Do not guess these values from the old demo UI. The previous account credentials are not required for ordinary anonymous inspection of the login page; if authenticated inspection is necessary, use them only interactively and never save them.

## 4. Android WebView requirements

Implement and verify all of the following:

- Load only HTTPS webmail origins by default; reject invalid certificates and do not bypass TLS errors.
- Enable JavaScript and DOM storage only because the webmail application may require them.
- Preserve the user's own session cookies across app restarts, and clear them only on an explicit logout/clear-session action.
- Support HTML file inputs through Android's file picker (`WebChromeClient.onShowFileChooser`).
- Support downloads through `DownloadListener` and Android DownloadManager, including appropriate MIME type and notification behavior.
- Handle Android back button: navigate WebView history first; exit only when there is no page history.
- Open external links outside the approved webmail host in the system browser after confirmation or according to a strict allowlist.
- Do not inject credentials, passwords, cookies, or authentication tokens through JavaScript.
- Do not add a JavaScript bridge unless a specific feature requires it and the bridge surface is tightly restricted.
- Keep the interface usable on narrow screens and support keyboard resizing when composing mail.
- Verify file upload from local storage and download to the device's Downloads folder.
- Verify camera/photo selection only if the site offers those input types and permissions are justified.
- Add accessibility labels and a clear app name/icon.

If the webmail rejects WebView or critical features cannot work in WebView, stop and document the exact failure. Then evaluate a supported provider API or a secure external-browser/PWA route. Do not silently ship a broken wrapper.

## 5. Push notifications

Do not show a notification toggle that merely asks Android for permission and then claims mail notifications work.

First determine whether the existing webmail/server offers:
- Web Push with a service worker and VAPID keys; or
- a supported IMAP IDLE / server event / webhook integration.

Android WebView does not automatically provide a complete native push-notification pipeline. If reliable native notifications are required, implement FCM with a backend that securely monitors mailbox events or receives server-side events. Never store mail passwords in the APK or a public client. Use least-privilege server credentials, encrypted secrets, per-user authorization, and explicit consent. If the mail server cannot provide a secure event source, clearly mark native push as unavailable rather than faking it.

## 6. Remove the demo implementation

The following demo behavior must not remain in the production app:
- hard-coded `demo` message array;
- fake inbox counts and sample senders/messages;
- compose action that only shows a demo notice;
- folder links saved as arbitrary per-folder URLs as a substitute for real mailbox folders;
- demo-mode banners and placeholder empty states presented as real mailbox data.

The actual folder navigation should come from the webmail platform after the user signs in. Do not hard-code per-user folder URLs unless the live service demonstrably requires a stable route and the route is verified.

## 7. Implementation sequence for the next agent

### Phase A — inspect and document the real service
- Open the webmail URL in a browser.
- Identify the webmail product/version if possible (for example, Roundcube, SnappyMail, or another product) from public page assets and markup; do not assume which one it is.
- Map login, mailbox, compose, folder, attachment, logout, and mobile flows.
- Test whether the service can run inside Android WebView.
- Record verified URLs and any WebView limitations here, without recording credentials or session tokens.

### Phase B — build the Android wrapper
- Keep the current GitHub Actions APK workflow, but ensure the generated Android project receives the native WebView implementation before Gradle builds.
- Use the existing application ID `com.chilyacatering.chimail`.
- Load the real webmail HTTPS URL as the initial screen.
- Add secure WebView settings, cookies, file chooser, downloads, back navigation, and external-link policy.
- Retain the CHILYA launcher icon and black/yellow splash branding.
- Remove or bypass the mock Next.js mailbox screen in the production Android APK. The APK must open the actual webmail, not the demo page.
- Avoid setting a production `server.url` in Capacitor unless its behavior and security implications are deliberately validated; a dedicated native WebView activity is preferred for a production wrapper.

### Phase C — functional verification
Use a test mailbox/account authorized by the service owner. Never use the user's inspection credentials as a hard-coded test identity.

Required tests:
- [ ] Fresh install opens the real webmail login page.
- [ ] User can log in with their own account.
- [ ] Session remains after closing/reopening the app.
- [ ] User can log out and another user can log in without seeing the previous account.
- [ ] Inbox displays real server messages.
- [ ] Open/read a message and navigate back.
- [ ] Search real messages.
- [ ] Compose and send a message to an authorized test mailbox.
- [ ] Receive a reply and verify it appears in the inbox.
- [ ] Reply and forward.
- [ ] Save and reopen a draft.
- [ ] Move/archive/delete/mark spam and verify server-side state.
- [ ] Upload an attachment from Android storage and send it.
- [ ] Download and open an attachment.
- [ ] Verify folder navigation and unread counts reflect the server.
- [ ] Verify links, keyboard, orientation, and small-screen layout.
- [ ] Verify behavior on a slow or interrupted connection.
- [ ] Verify TLS errors are not bypassed.
- [ ] Verify app logs and repository contain no credentials, cookies, or tokens.

### Phase D — build and publish
- Run `npm ci` (or the lockfile-appropriate install command), `npm run build`, Capacitor sync, and Android debug/release build.
- Run the GitHub Actions workflow.
- Download and install the generated APK on a real Android phone.
- Repeat the critical login/send/receive/attachment tests on the installed APK, not only in a desktop browser.
- Publish a release APK only after all critical tests pass.
- In the release notes, state exactly which features were verified and any known limitations.

## 8. Security and privacy rules

- Never commit passwords, session cookies, bearer tokens, OAuth secrets, VAPID private keys, or FCM service-account JSON.
- Do not place secrets in `.env.example`, client-side Next.js variables, build logs, or APK resources.
- Do not create a shared logged-in mailbox or prefilled credentials.
- Keep HTTPS certificate validation enabled.
- Use a strict origin allowlist for navigation and downloads.
- Avoid collecting, logging, or analytics-tracking message contents, recipients, attachments, or credentials.
- Treat downloaded attachments as untrusted files.
- Ask for explicit authorization before adding any backend that accesses employee mailboxes.

## 9. Current status and next action

- [x] CHI MAIL repository and Android APK workflow exist.
- [x] CHILYA logo is committed at `public/chilya-logo.jpg`.
- [x] Black/yellow responsive demo UI exists as a temporary scaffold.
- [x] User clarified that the previously supplied mailbox credentials are for inspecting platform structure only.
- [ ] Live webmail page and real folder routes inspected and documented.
- [ ] WebView compatibility verified against the live service (not yet tested from a physical Android device).
- [x] Native Android WebView wrapper implemented in `native/android/MainActivity.java` and installed by the APK workflow.
- [ ] Demo UI removed from the production APK path.
- [x] Android file chooser and authenticated HTTPS download handling implemented in the native wrapper (physical-device testing still required).
- [ ] Real login/send/receive/attachment flows tested on Android.
- [ ] Native push source/backend evaluated and implemented only if supported.
- [ ] Final APK installed and verified on a physical Android device.

**Next agent instruction:** Start with Phase A. Do not claim the app is a real email client until Phase C's login, send, receive, and attachment tests pass. If the live site cannot be inspected from the current environment, state that blocker and ask for the minimum required access or screenshots; do not invent the platform type or its folder URLs.


## 10. Build verification — 2026-11-19

- GitHub Actions run #19: https://github.com/Nadirinfograph23/Chi-Mail/actions/runs/44512725750
- Commit: `f4e819338a642173b7bd8730b6a9dca8be167de4`
- Result: **success**. The steps `npm run build`, Capacitor Android setup, secure native WebView installation, **Apply CHILYA launcher icon**, Capacitor sync, `assembleDebug`, and artifact upload all completed successfully.
- Artifact: `CHI-MAIL-Android-APK` (3,742,344 bytes; retained until 2026-12-19).
- This verifies that the build generated the launcher icon files from `public/chilya-logo.jpg` and packaged the APK. It does not prove the icon's visual appearance on a device or verify live mailbox functions.
- Physical Android testing of sign-in, sending, receiving, attachments, and push notifications remains outstanding.
