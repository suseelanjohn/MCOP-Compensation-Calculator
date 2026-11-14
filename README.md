
# ⚖️ MCOP Compensation Calculator

An Android application built with **100% pure Kotlin and Jetpack Compose** for legal practitioners, insurance panel advocates, and Motor Accidents Claims Tribunal (MACT / MCOP) professionals. It provides real-time actuarial compensation calculations, *Sarla Verma* & *Pranay Sethi* compliance, Lok Adalat lump-sum compromise analysis, local case archiving, and instant multi-format exports (PDF, WhatsApp, and CSV).

---

## 📱 App Screenshots

| **Fatal Claim & *Pranay Sethi* Step** | **Permanent Disability & Auto-Days** | **Saved MCOP Cases Directory** |
| :---: | :---: | :---: |
| <img src="screenshots/1000683339.png" width="260" alt="Fatal Claim Mode & Head-Wise Bifurcation"/> | <img src="screenshots/1000683338.png" width="260" alt="Permanent Disability & Interest Projection"/> | <img src="screenshots/1000683340.png" width="260" alt="Saved MCOP Cases Directory"/> |
| *Fatal mode with +21% Pranay Sethi escalation (`₹48.4k` consortium, `₹18.15k` estate/funeral) & 2-column live bifurcation.* | *Permanent Disability mode with automatic Petition Date-to-Today day counter (`554 days`) and export toggles.* | *Dedicated full-screen Case Vault to search, reload, or delete saved MCOP records on device.* |

> **Note for GitHub Display:** Upload your three screenshots (`1000683339.png`, `1000683338.png`, and `1000683340.png`) into a `screenshots/` folder in the root of your repository (or drag-and-drop them directly into the GitHub web editor to generate `https://github.com/user-attachments/assets/...` URLs).

---

## ✨ Key Features

### 🧮 Comprehensive Claim Categories
* **Fatal Accident Claims:**
  * Automatic *Sarla Verma* age-based multiplier selection (`18` down to `5`).
  * Automatic personal expense deduction based on dependent count (`1/2`, `1/3`, `1/4`, `1/5`).
  * **Supreme Court *Pranay Sethi* 10% Escalation Selector:** One-tap compounding (`Base ₹40k`, `+10% ₹44k`, `+21% ₹48.4k`, `+33% ₹53.2k`) applied to Loss of Consortium, Loss of Estate, and Funeral Expenses.
  * Pecuniary heads for Transportation / Ambulance, Damage to Clothing & Articles, and Pre-Death Medical Expenses.
* **Permanent Disability (Multiplier Method):**
  * Functional Loss of Earning Capacity with Future Prospects percentage.
  * Full conventional and pecuniary heads: Pain & Suffering, Medical Bills, Future Medical Expenses, Loss of Income During Treatment (Months), Transportation, Extra Nourishment, Attendant Charges, Clothing Damage, and Loss of Amenities / Marriage Prospects.
* **Partial Permanent Disability (Percentage Point Method):**
  * Configurable Rate per Percentage Point (`₹/pct`) alongside treatment income loss and all 8 conventional injury heads.
* **Simple / Temporary Injury:**
  * Dedicated primary **Injury Compensation** head for injuries sustained, plus Loss of Income During Treatment and conventional heads.

### ⚡ Single-Viewport Zero-Scroll Workbench
* **No Vertical Scrolling:** Designed to fit completely within a single screen viewport using a 3-tab input workbench (**1. Case & Primary**, **2. Conventional Heads**, **3. Interest & Lok Adalat**) and a 2-column live Head-Wise Bifurcation grid.
* **Auto-Calculated Interest Days:** Typing a Petition Date (`DD-MM-YYYY`, `DD/MM/YYYY`, or `YYYY-MM-DD`) or selecting a date from the built-in Calendar Picker automatically computes the exact number of elapsed days up to today.
* **Auto-Advancing Keyboard Focus:** Pressing **Enter / Next / OK** on the soft keyboard automatically moves focus to the next input box.

### 📁 Local Case Vault & Multi-Format Exports
* **On-Device Case Directory:** Save unlimited MCOP calculation files locally and access them from a dedicated full-screen **Saved MCOP Cases Directory** with instant search by MCOP Number, Claimant Name, or Claim Mode.
* **Court-Ready PDF Memo:** Generates a formatted A4 Calculation & Head-Wise Bifurcation Memo using Android's native `PdfDocument` API.
* **WhatsApp & Text Share:** Formatted summary with bold highlights and itemized heads ready for client or insurer sharing.
* **Google Sheets CSV Export:** Single-line structured CSV export containing all parameters and head-wise breakdowns.
* **Granular Export Controls:** Independent checkboxes to include or exclude the **Interest Projection** and the **Lok Adalat Consolidated Compromise Section** (lump-sum settlement without separate interest).

---

## 🛠️ Tech Stack & Requirements

* **Language:** Kotlin `1.9.0`
* **UI Framework:** Jetpack Compose (Material 3 `1.2.0+` via Compose BOM `2024.02.00`)
* **Build System:** Gradle `8.5` / Android Gradle Plugin (AGP) `8.2.0`
* **Java / JVM Target:** JDK `17`
* **Android SDK:** `minSdk 26` (Android 8.0) | `targetSdk 34` (Android 14)

---

## 🚀 Building the Project

### 1. Local Build via Android Studio / CLI
Clone the repository and run the debug build using Gradle:

```bash
git clone https://github.com/suseelanjohn/MCOP-Compensation-Calculator.git
cd MCOP-Compensation-Calculator
gradle assembleDebug
```

The compiled APK will be generated at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Automated Build via GitHub Actions CI/CD
Every push or pull request to `main` / `master` (or manual trigger via **Actions → Build MCOP Android APK → Run workflow**) automatically:
1. Provisions Ubuntu with **Temurin JDK 17**, **Android SDK 34**, and **Gradle 8.5**.
2. Compiles the project and runs resource linking.
3. Uploads `mcop-calculator-debug-apk` as a downloadable build artifact.

---

## 📂 Project Structure

```text
MCOP-Compensation-Calculator/
├── .github/workflows/
│   └── main.yml                  # GitHub Actions CI/CD pipeline
├── screenshots/
│   ├── 1000683339.png            # Fatal Claim & Pranay Sethi screenshot
│   ├── 1000683338.png            # Permanent Disability & Auto-Days screenshot
│   └── 1000683340.png            # Saved MCOP Cases Directory screenshot
├── app/
│   ├── build.gradle              # Module-level Compose & Material 3 config
│   └── src/main/
│       ├── AndroidManifest.xml   # App metadata, theme & FileProvider setup
│       ├── java/com/kamarajlawoffice/mcopcalculator/
│       │   └── MainActivity.kt   # Complete UI, calculation engine, PDF/CSV & Vault
│       └── res/
│           ├── drawable/         # Custom vector Scales of Justice & Calculator icon
│           ├── mipmap-anydpi-v26/# Adaptive launcher icon XMLs
│           ├── values/           # Royal Navy & Gold colors and themes
│           └── xml/file_paths.xml# FileProvider cache path for PDF/CSV sharing
├── build.gradle                  # Root plugin declarations (AGP 8.2.0 / Kotlin 1.9.0)
├── gradle.properties             # AndroidX & Jetifier flags
└── settings.gradle               # Centralized repository management
```

---

## 📄 License

This project is open-source and available for legal and actuarial use.
