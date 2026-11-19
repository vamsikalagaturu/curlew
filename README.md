# curlew

<img src="docs/logo.svg" width="96" alt="curlew logo">

A phone app for Android 10+, written in Kotlin with Jetpack Compose. It replaces the default dialer: it places and
answers calls, and keeps your call history and contacts in one Nord-themed app.

- Dialpad with a cursor, paste, suggestions as you type, speed dial on 2–9, and `*#*#…#*#*` / carrier codes.
- In-call screen: mute, speaker and Bluetooth, hold, merge and swap, keypad tones, and the SIM in use. Decline
  with a quick text message.
- Recents grouped by contact under date headings, with search, a missed-calls filter, the SIM each call used,
  multi-select delete, copy, share and block.
- Contacts with favourites, search, an editor for name, numbers, emails and photo, vCard export, and delete.
- Missed-call notifications with call back and message, blocked numbers, and silencing or blocking callers who
  aren't in your contacts or hide their number.
- Dark, light and pure-black themes. Updates itself from GitHub releases.

## Install

Install `curlew.apk` from [Releases](https://github.com/vamsikalagaturu/curlew/releases), open it, and tap
**Set as default** to make it your phone app.

## Build

`./gradlew assembleDebug` with JDK 17+ and the Android SDK (platform 36). A release is published by
`.github/workflows/release.yml` when `appVersion` in `app/build.gradle.kts` has no release yet, with that
version's section of [CHANGELOG.md](CHANGELOG.md) as its notes.

## Licence

MIT. Lucide icons: ISC (`third_party/lucide/LICENSE`).
