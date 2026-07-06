# LOAD LEDGER

LOAD LEDGER is a modern, visually stunning peer-to-peer ledger and monthly credit-union manager application built for Android using Jetpack Compose and Kotlin. It serves as a single-point ledger for social groups, friends, and family ("Sunday Groups") to seamlessly track personal peer-to-peer loans, dynamic interest rates, and automated monthly recurring collections.

---

## 🎨 Design Philosophy & Theme
LOAN LEDGER is built entirely on the modern **Material Design 3 (M3)** specification:
* **Slate Blue Minimalist Palette**: Deep premium blues, soft background slates, crisp clean whites, and semantic accent colors to reduce cognitive load.
* **Proportional Spacing**: Adheres to a strict 8dp spacing grid, with generous negative space and comfortable 48dp+ interactive touch targets for absolute ease of use.
* **Adaptive Styling**: Modern rounded card designs (`24dp` and `16dp` corners), high-contrast icons, and customized visual avatars to easily identify members at a glance.

---

## ✨ Features

### 1. Peer-to-Peer Loan Ledger (Rupees `₹` Format)
* **INR Currency Support**: Full native support for Indian Rupees (`₹`) across all financial calculations and user interfaces.
* **Flexible Interest Structures**: Supports multiple calculation methods:
  * **No Interest** / Interest-Free loans.
  * **Fixed Fee**: A single flat interest rate applied to the principal.
  * **Monthly Simple**: Period-based monthly interest calculation.
  * **Yearly Simple**: Period-based yearly interest calculation.
* **Repayments Track**: Record custom partial or full repayments with timestamps, updating the outstanding balance instantly.

### 2. Monthly Group Collections
* **Fixed Contribution**: A specialized navigation tab tracks monthly recurring group collections fixed at **₹100 per member**.
* **Visual Tick Affirmation**: A simple, high-fidelity check toggle on the right side of each member card marks them paid.
* **Live Statistics Box**: Displays the total amount collected in real-time, along with paid ratios (e.g., `5 / 12 Paid`).
* **Reset Lifecycle**: "Reset All" option to easily clear out a month's ledger and prepare for the next monthly collection cycle.

### 3. Smart Search & Navigation
* **Global Search**: Instantly filter members by name inside both the Loans directory and the Monthly Collection screen.
* **Bottom Navigation Rail**: High-touch bottom navigation panel allows quick, modern switching between active **Loans** and the **Monthly Collection** ledger.

---

## 🏗️ Architecture & Stack
LOAN LEDGER adheres to modern Android architecture principles:
* **UI Layer**: Built entirely on **Jetpack Compose** with state managed via **ViewModel** and **MutableStateFlow** observing real-time database state.
* **Data Layer**: Powered by a robust **Room Database** (v2 Schema) ensuring offline-first reliability, data integrity, and atomic transactional writes.
* **Navigation**: Type-safe declarative composable routing for fluent, reliable screen transitions.

---

## 🛠️ Build and Verify
To verify that everything compiles successfully and passes lint checks, run:

```bash
# Compile and build the debug APK
gradle assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```
