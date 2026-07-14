# ⚡ IR Blaster — Universal Remote Control App

A professional Android IR remote control app with a sleek dark neon UI, supporting 8+ device brands across 4 categories with auto brute-force scanning.

## Features

### 📺 Device Categories
- **TV** — Samsung, LG, Sony, Philips, TCL, Haier, Hisense, Sharp
- **❄️ Air Conditioner** — Daikin, Midea, LG AC, Samsung AC, Gree
- **📽️ Projector** — Epson, BenQ, ViewSonic
- **💿 DVD/Blu-ray** — Samsung DVD, Sony DVD

### ⚡ Auto Brute-Force Scanner
- Automatically blasts all possible IR signal combinations
- Adjustable speed (100ms–1000ms per signal)
- Mark working signals with one tap
- Real-time signal name display with animated progress bar

### 🎨 UI Design
- Dark neon theme (Cyan + Purple glow)
- Animated signal wave during scan
- Glow card effects with blur shadows
- Brand chip selector with smooth transitions
- Per-button glow color by signal type (power = orange, mute = red, etc.)

## Tech Stack
- **Kotlin** + **Jetpack Compose** (Material3)
- `ConsumerIrManager` Android API for IR transmission
- Real IR protocols: NEC, Samsung, Sony SIRC
- `kotlinx.coroutines` for async scanning loop
- ViewModel + StateFlow architecture

## How It Works

```
Phone IR Blaster → NEC/Samsung/Sony protocol frames → Device
```

The app uses `android.hardware.ConsumerIrManager` to transmit raw pulse patterns at 38kHz (standard IR carrier). Each signal is a precise on/off pattern in microseconds encoding device commands.

### Brute Force Mode
Iterates through 230+ pre-computed signal combinations covering the most common manufacturer addresses and power command codes, with a configurable delay between each transmission.

## Requirements
- Android 5.0+ (API 21)
- Device with **IR blaster hardware** (e.g., Xiaomi, Samsung flagship, Huawei, older HTC/LG)
- `TRANSMIT_IR` permission

## Portfolio Notes
This project demonstrates:
- Hardware API integration (ConsumerIrManager)
- Real-world IR protocol encoding (NEC, Samsung, Sony)
- Modern Compose UI with custom animations
- Async state management with coroutines + StateFlow
- Clean MVVM architecture
