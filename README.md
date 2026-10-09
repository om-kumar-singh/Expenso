# Expenso

### Less Writing. Easy Life.

**Expenso** is an offline-first Android expense manager that transforms simple, Notes-style entries into organized financial records. Write expenses in your usual format, and Expenso automatically parses transactions, calculates totals, and organizes your finances by month and account.

No complicated forms. No login. No cloud dependency. Just write, calculate, and track.

<p align="center">
  <strong>Private by design · Offline first · Built for simplicity</strong>
</p>

---

## ✨ Features

- **Notes-Style Entry** — Record expenses and income using simple text instead of filling out forms.
- **Automatic Parsing** — Convert your notes into structured transactions with dates, descriptions, expenses, and income.
- **Monthly Dashboard** — View monthly expenses, money received, net balance, and transaction counts.
- **Transaction Tables** — Review organized financial records with aligned columns and readable amounts.
- **Smart Error Handling** — Identify invalid entries, understand why they failed, and jump directly to the affected line.
- **Multiple Accounts** — Create and manage separate accounts and view combined financial summaries.
- **Custom Date Ranges** — Calculate spending and income for a particular day or any selected period.
- **Local Persistence** — Save notes and transactions on your device using Room.
- **Offline Operation** — No account registration, cloud service, or internet connection required for core functionality.

## 📱 How It Works

**1. Write your notes**

Use the same format you already use in your phone's Notes app.

**2. Let Expenso organize them**

The app parses your entries into dated transactions and separates expenses from money received.

**3. Review your finances**

View monthly summaries, account-wise totals, combined records, or custom date-range reports.

## 📝 Notes Format

Select a month and enter transactions like this:

```text
5- wifi bill-589
Milk packet-11
Juice-120

11-
milk-46
Salary =+25000
Poster +1500
```

### Supported syntax

| Format | Meaning |
|---|---|
| `11-` | Set the current entry date to day 11 |
| `11- milk-46` | Record an expense on day 11 |
| `milk-46` | Record a ₹46 expense |
| `Salary =+25000` | Record ₹25,000 received |
| `Poster +1500` | Record ₹1,500 received |

Subsequent entries inherit the most recently specified day. Blank lines are ignored. Invalid entries are reported without preventing valid transactions from being processed.

## 📊 Financial Tracking

Expenso calculates three essential figures:

- **Total Expenses** — The sum of outgoing transactions.
- **Total Received** — The sum of incoming transactions.
- **Net Balance** — Total received minus total expenses.

You can view these figures for an individual month, a selected account, multiple accounts, or a custom date range.

## 🧭 App Sections

| Section | Purpose |
|---|---|
| **Months** | Browse monthly summaries and open saved records |
| **Accounts** | Create, rename, and manage expense accounts |
| **Period** | Calculate totals for custom date ranges |
| **Month Detail** | Review a month's transaction table |
| **Notes Editor** | Enter, edit, and recalculate transactions |

## 🛠️ Technology Stack

- **Language:** Kotlin
- **UI:** Android XML Views
- **UI Components:** Material Components and View Binding
- **Local Database:** Room
- **Build System:** Gradle
- **Architecture:** Native Android, offline-first

No Firebase, REST APIs, or cloud database is required for the core application.

## 🚀 Getting Started

### Requirements

- Android 8.0 (API 26) or later
- Android Studio
- Compatible JDK for the project's Gradle and Android Gradle Plugin versions
- Gradle Wrapper included in the repository

### Clone the repository

```bash
git clone https://github.com/om-kumar-singh/Expenso.git
cd Expenso
```

Open the project in Android Studio and allow Gradle synchronization to finish.

### Build and test

**Windows — PowerShell**

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

**Linux / macOS**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install the APK on a compatible Android device to use the application.

## 🔒 Privacy

Expenso is designed to keep financial records on your device. Its core functionality does not require an online account or remote database.

Keep in mind that locally stored data may be lost if the app's data is cleared or the app is uninstalled. Consider maintaining a secure backup of important records.

## 📄 License

**Proprietary License — All Rights Reserved**

Copyright © 2026 Om Kumar Singh.

This project is proprietary software. No permission is granted to copy, modify, redistribute, sublicense, publish, or commercially exploit this software without the copyright holder's prior written authorization, except where applicable law or the hosting platform's terms provide otherwise.

For permission to use or distribute this software, contact the copyright holder.

See the [`LICENSE`](LICENSE) file for the complete terms.

---

<p align="center">
  <strong>Expenso</strong><br>
  Less Writing. Easy Life.
</p>
