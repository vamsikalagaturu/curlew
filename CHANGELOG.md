# Changelog

Each release's notes are its section below; `platform/release.sh` publishes the section whose heading matches
`appVersion` and refuses to release a version without one.

## 0.2.3

### Call screen

- The number of the person you're talking to is back under their name.
- With more than one SIM, the call screen and the incoming call card name the SIM in use without showing your own
  number.

## 0.2.2

### Fixes

- Curlew no longer crashes when a call comes in. The crash handed the call to Android's own phone app, which is
  why its call notification sometimes showed up, and left a Curlew notification behind that closed straight away
  when tapped.
- The glows around the in-call buttons are no longer cut off while the screen fades out after a call.

### Call screen

- The hang-up button no longer pulses: a steady glow with one soft light slowly circling it.
- The call timer counts without animating.
- A saved contact's number is no longer shown under their name.

## 0.2.1

### Call screen

- A new look on Android 13 and later: a slowly flowing aurora in the call's colours (purple while calling, green
  once connected, red when it ends) that glows up around the caller and the answer handle.
- Drifting light strands and sparkles around the caller, and ripples with orbiting lights around the answer
  handle. When you swipe up to answer, the handle's glow flies up into the caller's and the call screen rises in.
- The in-call buttons float in the aurora instead of sitting on solid discs. Mute glows red with rings drawing
  inward, the speaker sends out sound waves, and the hang-up button is a red orb.
- "Calling" has bouncing dots, the call timer rolls, and long numbers are formatted and shrink to fit.
- Android 10–12 shows a simpler soft glow instead.

### Fixes

- The tab bar no longer stutters: the highlight follows your finger exactly while swiping and slides smoothly
  when you tap a tab.

## 0.2.0

### Incoming calls

- Swipe the handle up to answer or down to decline, with a buzz when you pass the line. Tapping no longer answers
  or declines by accident, and the handle ignores touches while the phone is in a pocket.
- Silence the ringtone and keep the call ringing quietly, from the call screen or the incoming call card.
- An incoming call card over other apps while the phone is unlocked, with silence, decline and answer, instead
  of Android's heads-up notification. Tap it for the full call screen, or drag it down. Turn it on under
  Settings → Phone → Incoming call card, which asks for permission to display over other apps.

### Call screen

- New motion throughout: a glow that follows the call's state, pulsing rings and a breathing avatar while ringing
  or dialing, a colour wash when you answer or decline, a rolling call timer, springy buttons, and a short
  "Call ended" before the screen fades away.
- A buzz when an outgoing call is answered.

### SIM cards

- Your SIM's phone number shows next to its name wherever you pick a SIM and on the call screen, when the carrier
  stored it on the SIM. Settings → Phone lists your numbers with a copy button. This asks for the Phone numbers
  permission.

### Fixes

- Tapping a tab that isn't next to the current one fades straight to it instead of sliding through the tab in
  between.
- Recents no longer shows a call count after each name.

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
