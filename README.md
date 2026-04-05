# BudgetApp

A fully local Android budget tracking app. All your financial data stays on your device — no cloud accounts, no telemetry, no sign-ups. The only network calls are optional Plaid API requests to sync bank transactions, and those go directly from your phone to Plaid.

## Features

- **Local-first** — Room SQLite database on-device; nothing leaves your phone
- **Password protection** — App-level password with bcrypt hashing
- **AES-256-GCM encryption** — Sensitive data (Plaid tokens) encrypted via Android Keystore
- **Manual transactions** — Add income/expenses by hand
- **Bank sync via Plaid** — Optionally connect a bank account to auto-import transactions
- **Notification parsing** — Optionally read bank push notifications to auto-detect spending
- **Budgets & categories** — Track spending against budgets with default and custom categories
- **Multiple accounts** — Manage checking, savings, credit cards, etc.

## Tech Stack

- Kotlin, Jetpack Compose, Material 3
- MVVM + Repository pattern
- Hilt dependency injection
- Room (SQLite) for persistence
- Retrofit + OkHttp for Plaid API
- Android Keystore + `EncryptedSharedPreferences` for secrets
- Min SDK 26, Target SDK 34

## Getting Started

1. Clone the repo and open it in **Android Studio** (Hedgehog or later recommended)
2. Let Gradle sync finish
3. Run the `app` configuration on an emulator or physical device (API 26+)
4. On first launch, create a password and choose a tracking method

## Tracking Methods

When you first set up the app you pick one of three methods (changeable later in Settings):

| Method | How it works | Permissions needed |
|---|---|---|
| **Manual** | You add every transaction yourself | None |
| **Plaid Bank Sync** | Pulls transactions from your bank via Plaid API | Internet |
| **Notification Parsing** | Reads your bank's push notifications to auto-detect charges | Notification Access |

---

## Linking Plaid (Bank Sync)

Plaid lets the app pull real transaction data from your bank. Here's how to set it up:

### 1. Create a Plaid account

Go to [https://dashboard.plaid.com/signup](https://dashboard.plaid.com/signup) and create a free developer account.

### 2. Get your API credentials

After signing up, go to **Keys** in the Plaid dashboard. You'll see:

- **Client ID** — a hex string like `60f1a2b3c4d5e6f7a8b9c0d1`
- **Sandbox Secret** — another hex string

The **Sandbox** environment is free and unlimited — it uses fake bank data so you can test without connecting a real bank. You do not need to pay anything or apply for production access to try the app.

### 3. Get a Sandbox access token

In Sandbox mode, Plaid provides test credentials you can use directly. Run this in a terminal to get an access token:

```bash
curl -X POST https://sandbox.plaid.com/sandbox/public_token/create \
  -H "Content-Type: application/json" \
  -d '{
    "client_id": "YOUR_CLIENT_ID",
    "secret": "YOUR_SANDBOX_SECRET",
    "institution_id": "ins_109508",
    "initial_products": ["transactions"]
  }'
```

This returns a `public_token`. Exchange it for an `access_token`:

```bash
curl -X POST https://sandbox.plaid.com/item/public_token/exchange \
  -H "Content-Type: application/json" \
  -d '{
    "client_id": "YOUR_CLIENT_ID",
    "secret": "YOUR_SANDBOX_SECRET",
    "public_token": "THE_PUBLIC_TOKEN_FROM_ABOVE"
  }'
```

The response contains an `access_token` — this is what the app needs per-account.

### 4. Enter credentials in the app

1. Open the app and go to **Settings**
2. In the **Plaid Bank Sync** section, enter your **Client ID** and **Sandbox Secret**, then tap **Save**
3. Go to **Accounts → Add Account**
4. Enter an account name, set balance, and paste the **Plaid Access Token** from step 3
5. The app will now sync transactions on every app open or manual refresh

### 5. Sync

Transactions sync automatically when the app resumes. You can also tap **Refresh Now** in Settings to sync manually. The app fetches the last 90 days of transactions and deduplicates them so nothing gets double-counted.

### Plaid pricing

- **Sandbox** (fake data): Free, unlimited
- **Development** (real banks): Free for up to 100 connected bank accounts
- **Production**: Paid per-connection; see [plaid.com/pricing](https://plaid.com/pricing)

For personal use and testing, Sandbox and Development are completely free.

---

## Notification Parsing

If you choose the Notification Parsing tracking method:

1. The app prompts you to open **Notification Access** settings
2. Enable **Budget Notification Tracker** in the list
3. The service runs in the background and watches for notifications from known banking apps (Chase, Bank of America, Wells Fargo, Venmo, Cash App, etc.)
4. When a bank notification arrives (e.g. "You spent $24.99 at Amazon"), the service extracts the amount and merchant and creates a transaction automatically

No data is sent anywhere — the notification text is parsed locally and stored in the on-device database.

## Project Structure

```
app/src/main/java/com/budgetapp/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # Plaid API service (Retrofit)
│   └── repository/     # Auth, Account, Transaction, Budget, Plaid repos
├── di/                 # Hilt modules
├── security/           # CryptoManager (AES-256-GCM), PasswordHasher (bcrypt)
├── service/            # NotificationParserService
├── ui/
│   ├── auth/           # Login / password setup
│   ├── onboarding/     # Tracking method selection
│   ├── dashboard/      # Home screen with balance overview
│   ├── transactions/   # Transaction list + add screen
│   ├── budgets/        # Budget management
│   ├── accounts/       # Account management
│   ├── settings/       # Settings + Plaid config
│   ├── navigation/     # NavGraph + bottom nav
│   └── theme/          # Material 3 theme
└── util/               # Currency + date formatting
```

## License

This project is provided as-is for personal use.
# BudgetApp-VIBECODED
