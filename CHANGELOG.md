# Changelog

Each release's notes are its section below; `platform/release.sh` publishes the section whose heading matches
`appVersion` and refuses to release a version without one.

## 0.1.0

The first release: curlew replaces the phone app as Android's default dialer. It needs Android 10 or later.

### Calling

- Places and answers calls through Android's Telecom service, with a full-screen incoming call over the lock
  screen and a call notification with answer, decline and hang up.
- In-call controls: mute, speaker, Bluetooth and wired headsets, hold, add call, merge and swap, and a keypad for
  tones. The screen turns off at your ear.
- Decline an incoming call with a quick text message, or write your own.
- Dual SIM: choose a default SIM for calls, or long-press the call button to pick one for a single call. The
  in-call screen and Recents show which SIM a call used.

### Keypad

- A cursor you can move by tapping, with paste and copy from Android's text toolbar.
- Matching contacts and recent numbers appear as you type, with a shortcut to save a new number.
- Speed dial on keys 2–9, set up in Settings; long-press 1 for voicemail and 0 for `+`.
- `*#*#…#*#*` service codes run as you type them, and carrier codes such as `*123#` open the carrier's reply.

### Recents

- Calls grouped by contact under date headings. Expand a contact for every call with its time, duration and SIM,
  and for actions to edit the number, open or add the contact, share, and block.
- Search by name or number, and a filter for missed calls.
- Long-press to select contacts and delete their call history. Long-press a number to copy it.
- Missed-call notifications with call back and message, cleared when you open Recents.

### Contacts

- Favourites at the top; tap to call, long-press to open.
- An editor for name, phone numbers, emails, labels and photo, with favourite, share, block and delete. Contacts
  that only come from read-only apps such as WhatsApp say so instead of offering edits.
- Long-press to select contacts to share, export as a `.vcf` file, block or delete. Export all contacts from
  Settings.

### Blocking and screening

- Blocked numbers use Android's own list, managed from Recents, a contact, or Settings.
- Calls from numbers not in your contacts can be allowed, silenced or blocked, and calls that hide their number
  can be blocked.

### Settings and look

- System, light, dark and pure-black themes in Nord colours, Lucide icons, a floating tab bar, and swiping
  between tabs.
- Choose the tab curlew opens on, keypad vibration and tones, and shortcuts to Android's ringtone and carrier call
  settings.
- Checks GitHub once a day for a new release and installs it after you confirm.

### Install

Download `curlew.apk` below, open it, and tap **Set as default** in curlew. A test build installed another way
must be uninstalled first, because releases are signed with a different key.
