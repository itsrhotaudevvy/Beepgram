<p align="center">
  <img src="branding/beepgram-icon.png" width="112" height="112" alt="Beepgram icon" />
</p>

<h1 align="center">Beepgram</h1>

<p align="center">
  A third-party <b>Telegram</b> client for Android, based on
  <a href="https://github.com/NextAlone/Nagram">Nagram</a>, NekoX and Telegram for Android.
  It is being built toward a unified, multi-network inbox.
</p>

<p align="center">
  <a href="https://github.com/itsrhotaudevvy/Beepgram/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/itsrhotaudevvy/Beepgram/actions/workflows/ci.yml/badge.svg" /></a>
  <img alt="License: GPL-3.0" src="https://img.shields.io/badge/license-GPL--3.0-0fb5a6" />
  <img alt="Android 6.0+" src="https://img.shields.io/badge/android-6.0%2B-ff6b5e" />
</p>

---

- **Application ID:** `app.beepgram`
- **License:** GPLv3 (see [`LICENSE`](LICENSE))
- **Sibling project:** [Reefgram](https://github.com/itsrhotaudevvy/Reefgram), a Rust + Tauri
  desktop inbox that syncs Discord, WhatsApp, Fluxer, SMS, Instagram, Messenger and X into one
  Telegram-style view

> [!NOTE]
> Beepgram is not affiliated with Telegram, Nagram or any other network. It ships its own name,
> launcher icon, signing key and Telegram API credentials.

## What's different from Nagram

- **Own identity.** Beepgram has its own application ID, launcher icon, wordmark, notification
  icon and splash icon. All Nagram icon artwork has been removed from the tree and the icon picker,
  as [`BRANDING.md`](BRANDING.md) requires.
- **Self-signed builds.** The JNI signature check is disabled so builds signed with your own key
  run. `KEYSTORE_FILE` in `local.properties` points the build at your keystore.
- **Bridge framework.** `TMessagesProj/src/main/kotlin/app/beepgram/bridge/` is the foundation for
  showing chats from other networks as ordinary Telegram dialogs, without a Matrix server (see
  below).
- **No upstream update channel.** In-app update checks are off until you set
  `BuildVars.GITHUB_RELEASE_URL` to your fork's releases page.

Everything else (features, settings, translations) follows upstream Nagram.

## Bridge framework

The framework is network-neutral. It contains the model and contracts, but no live connectors yet:

| File | Purpose |
|------|---------|
| `BridgeIds.kt` | Reserved id range (bit 56 marker, bits 48–55 network id, bits 0–47 local sequence), so bridged peers never collide with real Telegram ids. Also defines the `NetworkType` enum for the seven target networks. |
| `Remote.kt` | The neutral model (`RemoteUser`, `RemoteChat`, `RemoteMessage`, `RemoteAttachment`, `RemoteCommand`, …) and the `BridgeSink` event interface |
| `TextFormat.kt` | Formatting spans that map one-to-one onto Telegram message entities |
| `NetworkConnector.kt` | The interface that every connector implements |
| `BridgeStore.kt` | The contract for local id allocation and session persistence |
| `Bridge.kt` | An orchestrator skeleton that owns the connectors and the sink |

Live connectors are being developed first in [Reefgram](https://github.com/itsrhotaudevvy/Reefgram),
which shares this model, and will then be ported here.

## Building

Requirements:

- Android SDK with `build-tools;36.0.0`, `platforms;android-36`, `ndk;27.2.12479018` and
  `cmake;3.22.1`
- JDK 17

Steps:

1. `git submodule update --init --recursive`
2. Get **your own** Telegram API credentials at <https://my.telegram.org/auth>, and generate your
   own keystore. Do **not** reuse Nagram's or Telegram's credentials or keys.
3. Create `local.properties` (it is git-ignored):

   ```properties
   TELEGRAM_APP_ID=123456
   TELEGRAM_APP_HASH=0123456789abcdef0123456789abcdef
   KEYSTORE_FILE=/absolute/path/to/beepgram-release.keystore
   KEYSTORE_PASS=...
   ALIAS_NAME=beepgram
   ALIAS_PASS=...
   ```

4. Build the native libraries: `./run init libs && ./run libs native` (set `NATIVE_TARGET=arm64-v8a`
   to build one ABI only).
5. Build the app: `./gradlew --no-configuration-cache TMessagesProj:assembleRelease`

### CI

- [`ci.yml`](.github/workflows/ci.yml) builds an unsigned arm64 APK for each push and pull request,
  and uploads it as a workflow artifact.
- [`release.yml`](.github/workflows/release.yml) runs when you push a `v*` tag. It builds signed
  APKs and attaches them to a draft GitHub release. It needs these repository secrets:
  `KEYSTORE_BASE64`, `KEYSTORE_PASS`, `ALIAS_NAME`, `ALIAS_PASS`, `TELEGRAM_APP_ID` and
  `TELEGRAM_APP_HASH`.

## Relationship to Nagram

The source history and the GPL notices from Nagram, NekoX and Telegram are preserved, and accurate
statements such as "based on Nagram" are intentional. Bugs that you can also reproduce in upstream
Nagram should be reported [there](https://github.com/NextAlone/Nagram/issues). The original NekoX
readme is kept in [`README.NekoX.md`](README.NekoX.md).

## Credits

- [Nagram](https://github.com/NextAlone/Nagram) by [@NextAlone](https://github.com/NextAlone)
- [NekoX](https://github.com/NekoX-Dev/NekoX) and [Nekogram](https://gitlab.com/Nekogram/Nekogram)
- Telegram for Android, Copyright Nikolai Kudashov
- The Beepgram wordmark is set in [Nunito](https://github.com/googlefonts/nunito)
  ([SIL OFL 1.1](branding/fonts/OFL.txt))
