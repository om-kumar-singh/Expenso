# Expenso

A simple, offline-first Android expense manager that turns everyday notes into organized tables, totals income and spending, and keeps monthly finances in one place.

Write expenses the way you already jot them down. Expenso parses the text, saves it on the device, and shows clear totals. There is no login, no cloud account, and no network requirement.

Repository: [om-kumar-singh/Expenso](https://github.com/om-kumar-singh/Expenso)

## Features

- **Notes-style entry** — type a day, then list expenses and income on following lines
- **Monthly overview** — saved months with expenses, received, and net
- **Transaction table** — Date, Account, Description, Expense, Received
- **Invalid-line highlighting** — skipped lines are listed with a reason; tap to jump back in the editor
- **Multiple accounts** — default “My Expenses”, plus create, rename, delete, and combined views
- **Custom period** — pick a start and end date and see totals across that range
- **Offline Room storage** — notes and transactions stay on the phone

## Notes format

Pick a month, then write notes like this:

```text
5- wifi bill-589
Milk packet-11
Juice-120
11-
milk-46
Salary =+25000
Poster +1500
```

- A day line starts with `11-` or `11- milk-46`. Later lines keep that day until you set a new one.
- Expenses use `description-amount` (for example `milk-46`).
- Income uses `Name=+1500`, `Name +1500`, or `Name-+1500`.
- Blank lines are ignored. Invalid lines are skipped and reported instead of crashing the parse.

## App screens

| Screen | What it does |
| --- | --- |
| **Months** | List saved months, filter by account, open a month, or write a new note |
| **Accounts** | Manage expense accounts |
| **Period** | Calculate totals between two dates |
| **Month detail** | Compact transaction table and Edit Notes |
| **Notes editor** | Blank editor for a new month, or edit existing notes and Calculate |

## Stack

- Kotlin + XML views (no Jetpack Compose)
- Material components and View Binding
- Room for local persistence
- No Firebase, no REST APIs, no extra third-party SDKs beyond AndroidX / Material / KSP

Package and application id: `com.example.expensesbyom`

## Requirements

- Android 8.0 (API 26) or later
- Android Studio with JDK 17+ (the project builds with the Android Studio JBR)
- Gradle 9.3.1 (wrapper included)

## Build

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

On Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## License

This project is licensed under the [MIT License](LICENSE). Copyright (c) 2026 Om Kumar Singh.
