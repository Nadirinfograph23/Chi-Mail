# CHI MAIL — CHILYA CATERING

Android email application project for CHILYA CATERING.

## Project status

The current UI is a temporary scaffold/demo and is **not yet connected to a real mailbox**. Do not distribute it as a functional email client.

## Agent handoff — read first

Before changing the project or building a production APK, read **[mail.md](./mail.md)**. It contains the authoritative product requirements, architecture decision, security rules, implementation phases, Android WebView requirements, and acceptance checklist.

The target live mail service is:

- https://webmail.chilya-catering.com/

The Android app must use the real service for login, mail folders, send/receive, and attachments. Each user signs in with their own account. Credentials previously supplied for inspection must never be embedded or committed.

## Branding and Android build

- App name: CHI MAIL
- Application ID: `com.chilyacatering.chimail`
- Brand: CHILYA CATERING (black/yellow)
- Logo: `public/chilya-logo.jpg`
- GitHub Actions workflow: `.github/workflows/android-apk.yml`

A successful APK compilation is not proof of mail functionality. Publish a production APK only after the real login, send, receive, and attachment tests in `mail.md` pass.
