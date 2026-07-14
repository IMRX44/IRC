# ⚡ IR Blaster — Universal Remote Control App

A professional Android IR remote with a sleek dark-neon UI, an **80+ brand** power-code database, and a **Smart Scan** that automatically finds your device's power code by sweeping the protocol space.

## ✨ Key Features

### 🎯 Smart Scan (the headline feature)
Pick your device brand → the app fires **every known power on/off code** for that brand back-to-back at the hardware's maximum speed. The moment your device reacts, tap the big **"✅ کار کرد"** button and the code is locked in.

- **80+ brand profiles** across TV / AC / Projector / DVD / Sound
- Includes Iranian brands (**X.Vision, Snowa, Marshal, G-Plus, Blest, Sam, Pars**), Turkish (**Beko, Arçelik, Regal, Telefunken, Profilo**), Chinese (**TCL, Hisense, Skyworth, Konka, Changhong, Xiaomi**), and global majors
- **⚡ Universal mode** — sweeps the *entire* NEC + Samsung + Sony address space, so even an unlisted brand gets covered
- Full transport controls: **قبلی / توقف / دوباره / بعدی** to step through codes manually
- Live progress, animated signal waves, code label + protocol readout

### 📺 Manual Remotes
Full button layouts (power, volume, channel, menu, D-pad, source…) for TV, AC, projector, DVD and sound systems, per brand.

### 🎨 UI
- Dark neon theme (cyan + purple glow, blur shadows)
- Every button animated and wired
- RTL Persian interface

## ⏱️ About speed ("test everything in 1 second")
Each IR frame physically takes **~45–70 ms** to leave the LED — that's the NEC/Samsung protocol timing, a hardware limit no code can beat. The scanner removes *all* artificial delay and fires frames back-to-back, hitting the real hardware maximum (~15–20 codes/sec). A full brand sweep therefore takes a few seconds, not milliseconds — but nothing is skipped.

## 📡 Protocols implemented
NEC · NEC-extended · Samsung · Sony SIRC (12-bit, 3× repeat) · RC5 · JVC · Sharp · Panasonic · Kaseikyo — all encoded to precise microsecond pulse patterns.

## 🔧 Tech Stack
- **Kotlin** + **Jetpack Compose** (Material3)
- `ConsumerIrManager` hardware API, transmit off the main thread (`Dispatchers.IO`)
- ViewModel + StateFlow, coroutine scan loop paced by the blocking transmit call

## 📦 Install
The delivered `IRBlaster-v1.0-signed.apk` is a **signed release build** (v1+v2+v3 signature schemes).

> **"Are you sure you want to install?" / Play Protect prompt** — this is Android's standard warning for **any** app installed outside the Play Store ("unknown sources"). It cannot be fully removed without publishing to the Play Store, but because this APK is properly **release-signed** (not a debug build) it is *not* flagged as a test/unverified app, and the extra "blocked by Play Protect" hard-stop no longer appears. Just tap **Install anyway**.

Requirements:
- Android 5.0+ (API 21)
- A phone **with an IR blaster** (Xiaomi/Redmi/POCO, older Samsung Galaxy, Huawei, Honor, some LG/HTC)
- `TRANSMIT_IR` permission (auto-granted)

## 🏗️ Building from source
```bash
# 1. Generate your own signing key (kept out of git on purpose)
keytool -genkeypair -v -keystore app/release.keystore -alias irblaster \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass yourpass -keypass yourpass \
  -dname "CN=IR Blaster, O=You, C=IR"

# 2. Build
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```
If no keystore is present the release build still succeeds (unsigned). Passwords can be supplied via `KEYSTORE_PASS` / `KEY_ALIAS` / `KEY_PASS` env vars instead of the defaults.

## 🧑‍💼 Portfolio notes
Demonstrates hardware API integration, real IR protocol encoding across 9 protocol families, an 80-brand code database with a brute-force search UX, modern Compose UI with custom animations, coroutine/StateFlow architecture, and a signed release pipeline.
