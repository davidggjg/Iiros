# Iiros Scanner

An on-device Android security scanner: it goes app by app over everything installed
on your phone, statically inspects each one (permissions, signing certificate, and
optionally its APK code) for signals commonly seen in malware/spyware, and gives
you a plain-language risk verdict. It also has a standalone URL checker — paste a
link and it's scored against phishing/malware heuristics before you tap it.

Everything runs locally. The app requests no `INTERNET` permission and makes no
network calls — scanning, hashing, and scoring all happen on-device.

## What it actually checks

**Installed apps** (`Apps` tab):
- Dangerous/privacy-sensitive permissions (SMS, contacts, location, camera, mic, …)
- Device-control permission combos associated with real attack patterns: overlay +
  accessibility service (screen takeover), SMS + overlay (OTP interception/banking
  fraud), device-admin + accessibility + install-packages (full device takeover)
- Install source (Play Store / sideloaded / unknown)
- Signing certificate SHA-256, checked against a small bundled list of known-bad
  indicators, plus a brand-impersonation check (e.g. an app labeled "WhatsApp"
  that isn't `com.whatsapp`)
- **Optional deep scan**: opens the installed APK as a zip and greps its dex files
  for suspicious API strings — dynamic code loading (`DexClassLoader`), shell/root
  execution, SMS APIs, Accessibility-service automation, device-admin persistence,
  hardcoded `.onion`/raw-IP endpoints, anti-emulator/anti-debugger checks, and
  known APK packer signatures. This is a lightweight static-analysis pass (string
  matching, not a full disassembler), run on demand per-app since scanning every
  installed APK's bytes on every app-list refresh would be slow.

**URLs** (`URL Scan` tab): raw-IP hosts, punycode/homograph domains, the userinfo
`@`-sign phishing trick, brand-impersonation on the wrong domain, URL shorteners,
disposable/abused TLDs, deep subdomain chains, phishing-kit path keywords, and
non-standard ports/length. Pure string heuristics — no live page fetch, no
reputation API call, by design (nothing leaves the device).

All of this feeds a shared, explainable risk engine: every verdict comes with the
specific list of findings that produced it, not just a bare score.

Everything is bilingual (English default, Hebrew via `values-iw`) and follows
system light/dark theme.

## Architecture

```
core/   pure-Kotlin/JVM module — the actual detection logic, zero Android
        dependency. Fully unit tested (30 tests) and runnable/testable outside
        Android entirely.
          permissions/  dangerous-permission + combo analysis
          apk/          APK-as-zip static string scanner
          malware/      known-bad package/signature/brand-impersonation lookups
          url/          URL heuristic scanner
          scoring/      combines the above into one AppScanResult

app/    Android app (Kotlin + Jetpack Compose, Material 3).
          data/         PackageManager <-> core bridge, Room-backed scan history
          ui/           Compose screens: app list, app detail, URL scan, history
```

`core` has no `android.*` imports anywhere, so its logic can be exercised (and was
exercised, see below) with a plain JVM test run — no emulator, no Android Gradle
Plugin, no SDK needed.

## CI: getting a built APK without installing anything

`.github/workflows/android-build.yml` builds the app on every push, using
GitHub's own runners (which have real network access and can install the
Android SDK) — this is the recommended way to get a working APK if you don't
want to set up Android Studio locally. It runs `:core:test`, then
`:app:assembleDebug`, and uploads the resulting APK as a build artifact.
After a push, check the **Actions** tab on GitHub, open the latest run, and
download `iiros-debug-apk` from the artifacts section at the bottom of the
run summary — that's an installable debug APK you can sideload onto your
phone.

## Building locally

Requirements: JDK 17+, Android SDK (compileSdk/targetSdk 34), Android Studio
recommended.

```
./gradlew :core:test    # pure-JVM detection logic + its test suite
./gradlew :app:assembleDebug
```

**Note on `gradle/wrapper/gradle-wrapper.jar`**: this repo was assembled in a
sandboxed environment with no outbound access to `services.gradle.org`
(it 307-redirects to a GitHub release, and this sandbox's proxy blocks GitHub
downloads outside its own repo scope), so I could not fetch/verify an authentic
wrapper jar here and did not want to ship a hand-rolled one that might silently
break for you. `gradle/wrapper/gradle-wrapper.properties` is in place (pointing
at Gradle 8.7); the first time you open this project somewhere with normal
internet access, run:

```
gradle wrapper --gradle-version 8.7
```

(using any local Gradle install, or just open the project in Android Studio,
which will offer to generate/sync the wrapper for you automatically).

## What was and wasn't verified

- `:core` — fully built and tested in this sandbox: `gradle :core:test` passes
  all 30 tests (permission-combo detection, dex string-signature scanning,
  malware/brand-impersonation lookups, URL heuristics, end-to-end risk scoring).
- `:app` — **could not be compiled or run here.** This sandbox has a plain JDK
  and Gradle but no Android SDK (no `aapt2`, no `android.jar`, no emulator), so
  the Compose UI and PackageManager integration are written carefully but
  unverified by an actual build. Please build `:app` in Android Studio (or CI
  with the Android SDK installed) before relying on it, and expect to fix any
  small issues a real compile turns up (version bumps, resource typos, etc.).

## Privacy

No `INTERNET` permission is requested. `QUERY_ALL_PACKAGES` is required to list
every installed app on Android 11+ (package-visibility rules would otherwise
hide most apps from the scanner) — that's the only non-default permission this
app declares.

## Extending the malware signature list

`core/src/main/kotlin/com/iiros/scanner/core/malware/KnownThreats.kt` is a small
bundled starter list, not a live feed. `MalwareSignatureDatabase` accepts extra
package names/signing hashes at construction time, so the app layer can merge in
a user-supplied or periodically-updated local list without touching `core`.
