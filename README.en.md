# Rencar — Usage-Based Car Rental

*[Türkçe](README.md) | English*

Rencar is an Android app that lets users find and rent a car on a per-minute/hourly/daily basis via a live map. The user finds a suitable car on the map, reserves it, takes handover photos and starts the trip; during the trip the live cost, distance, and location are tracked, an invoice is generated automatically at the end, and payment is made via wallet/card/İyzico.

This repository contains the **Android (Kotlin + Jetpack Compose) client** side of the app. The backend is consumed through the OpenAPI 3.0 REST API defined in [docs/api-openapi_v2.json](docs/api-openapi_v2.json) (the `rencarv2` release).

---

## Table of Contents

- [User Flow](#user-flow)
- [Features](#features)
  - [1. Authentication, Onboarding & Session Persistence](#1-authentication-onboarding--session-persistence)
  - [2. License Verification](#2-license-verification)
  - [3. Vehicle Discovery & Reservation](#3-vehicle-discovery--reservation)
  - [4. Handover & Active Trip](#4-handover--active-trip)
  - [5. Payment & Wallet](#5-payment--wallet)
  - [6. Profile, History, Referral & Settings](#6-profile-history-referral--settings)
- [Screenshots](#screenshots)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Project Structure (Full File Map)](#project-structure-full-file-map)
- [Navigation Graph](#navigation-graph)
- [Permissions](#permissions)
- [API](#api)
- [Setup](#setup)
- [Configuration](#configuration)
- [Team](#team)

---

## User Flow

```
Splash (waits until the saved session is verified)
        → Onboarding (3 pages, first launch only) → Login (OTP) / Register (+ referral code)
        → License Upload (front + back + selfie) [mandatory first stop after registration]
        → [Bottom Nav: Map · History · Wallet · Profile]
        → Pick a Vehicle (map/search) → Reservation (15 min free hold)
        → Handover (4-direction photos) → Active Trip (live time/cost/distance/location)
        → Trip Summary → Payment (Wallet / Saved Card / İyzico)
```

If a session is already saved, the splash screen validates the token in the background and takes the user straight to the Map screen; onboarding and login are not shown again.

## Features

For ease of review and testing, features are split into 6 sections that follow the app's actual flow order.

### 1. Authentication, Onboarding & Session Persistence
- **Onboarding** — a 3-page swipeable intro with a page indicator and back-button support to return to the previous page; shown only on first launch (`OnboardingPreferences`, persisted via DataStore).
- **Passwordless login (OTP)** — sign in with a phone number (the `+90` prefix is added automatically), a 6-digit SMS verification code, a 60-second "resend" counter (`AuthConstants`); verification is triggered automatically as soon as the code is complete.
- **Registration** — full name, email, password (min. 6 characters), phone number, and an optional **referral code**; a newly registered user is signed in immediately with the `PENDING` role.
- **Persistent session (Splash + SessionManager)** — on app start, `SessionManager` restores the saved access/refresh token pair from `TokenStorage` (DataStore) and validates it in the background with a 5-second timeout; the `core-splashscreen` screen is kept on screen for that duration (`setKeepOnScreenCondition`), so a logged-in user never sees the login screen flash by. Concurrent 401s are serialized behind a `Mutex` for token refresh — otherwise the backend's refresh-token rotation rule would treat a second, simultaneous refresh request as a "stolen token" and kill the whole session chain. `AuthSession` keeps the access/refresh pair as a single atomic unit (`TokenPair`) and queues disk writes on a separate, single-threaded queue so parallel writes can't overwrite each other.
- **Token rotation** — the refresh token is renewed on every use; if an old token is reused, the session chain is revoked for security. After license approval, a `CUSTOMER` token can be obtained via `refreshSession()` without logging in again (see Section 6 — Profile).
- After registration, the user is routed directly to the **License Upload** screen (`AuthSession.justRegistered`).
- Related files: `ui/screens/onboarding/`, `ui/screens/login/`, `ui/screens/register/`, `data/auth/`

### 2. License Verification
- A 3-step flow: **license front/back photo → selfie → confirmation screen**, with the ability to go back between steps.
- The selfie step automatically suggests the user's existing profile photo if one is available (`AuthSession.currentUser.avatarUrl`); the user can replace it if they wish.
- Photos can be picked from the gallery or taken with the camera (`CameraCapture.kt`); before upload they are automatically **downscaled + EXIF-rotated + JPEG-compressed** (`ImageFiles.kt`, to stay under the server's 5MB limit).
- Status tracking: `NOT_SUBMITTED` → `UNDER_REVIEW` → `APPROVED` / `REJECTED`; if rejected, the rejection reason is shown and the documents can be re-uploaded.
- Upon approval, the user is upgraded from `PENDING` to the `CUSTOMER` role.
- Related files: `ui/screens/license/`, `data/license/`

### 3. Vehicle Discovery & Reservation
- A **MapLibre**-based map: available vehicles are shown with colored markers, busy (RENTED/RESERVED) vehicles with gray markers (`includeBusy=true`); vehicles under maintenance are never shown. Vehicle/reservation/rental statuses are parsed through centralized enums that mirror the backend contract (`VehicleStatus`, `RentalStatus`, `PaymentStatus`, `ReservationStatus`) — screens never do ad-hoc raw string comparisons, and if the backend adds a new status the app falls back to `UNKNOWN` instead of breaking.
- **Marker clustering** and custom canvas-drawn marker/bubble graphics (`MarkerBitmapFactory.kt`).
- Type (Sedan/SUV/Hatchback/Station/Minivan) and price segment (Economy/Comfort/SUV) filters; the segment filter runs server-side, the type filter client-side.
- **Place search** — a search bar at the top backed by the OpenStreetMap **Nominatim** API for address/location search (debounced; results are restricted to Turkey via `countrycodes=tr`, using a separate Retrofit client with its own `User-Agent` interceptor).
- **Find nearest vehicle** — finds and focuses on the nearest available vehicle to the user's location using the haversine formula.
- Vehicle detail bottom sheet: fuel percentage (progress bar), range, transmission, seat count, per-minute/per-hour price.
- Status banner (priority order): **active trip** > **trip in handover (PREPARING)** > **active reservation** — even if the user closes and reopens the app, they can return to their in-progress flow from the map.
- **Reservation** — holds the vehicle free for 15 minutes (`RESERVATION_TTL_MIN`) with a live countdown; the hold is released automatically when it expires. Plan selection (per-minute/hourly/daily) with a **price preview (quote)** based on the selected duration — an estimated cost without creating a booking. If the user already has an active reservation on another vehicle, a blocking notice is shown, from which it can be cancelled.
- Related files: `ui/screens/map/`, `ui/screens/reservation/`, `data/geocoding/`, `data/vehicles/`, `data/reservations/`

### 4. Handover & Active Trip
- **Handover** — for per-minute/hourly plans, opens while the trip is in the `PREPARING` state; taking a photo of all 4 sides of the vehicle (front/back/left/right) is mandatory, and a second shot of the same side replaces the previous one. If the app is reopened, an unfinished photo flow is resumed from the server. Once all sides are complete, "Start" becomes active and the timer starts at that moment (time spent taking photos is not billed). A trip still in the `PREPARING` stage can be cancelled (the vehicle immediately becomes `AVAILABLE` again).
- A **Daily (DAILY) plan** starts directly as `ACTIVE` without the photo flow (for backward compatibility).
- **Active trip panel** — elapsed time (kept smooth via server sync + a local 1-second ticker), current estimated cost, and accumulated distance; polled from the server every 5 seconds, with a subtle warning shown on consecutive failures.
- **Live location** — via Socket.IO (the `/ws/locations` namespace, the `my-vehicle` event), the live location of the vehicle in the active rental is shown on the map; if the token expires it is refreshed once and reconnected, and socket errors are swallowed silently (the map stays at the last REST location).
- Ending a trip (`finish`) works for all plans; the older `return` endpoint only supports the DAILY plan.
- Related files: `ui/screens/handover/`, `ui/screens/activerental/`, `data/rentals/`

### 5. Payment & Wallet
- At the end of a trip, an itemized **cost breakdown**: usage fee (per-minute/hour) + opening fee + service fee; for the DAILY plan the total is locked in upfront.
- 3 payment methods available from the **Trip Summary** screen:
  - **Wallet** — automatically switches to the card method if the balance is insufficient.
  - **Saved card** — simulated (no real charge is made).
  - **İyzico** — a real charge, made through the backend's own REST endpoints (no native İyzico SDK is used on the client), with 3 sub-methods: a **hosted checkout page** (İyzico's own payment page loaded in a WebView), a **3-D Secure card form** (card details are collected in the app, bank confirmation is completed in a WebView), or **direct card charging** (no 3DS, synchronous result in a single request). Every payment is started with a basketId in the form `rental-<id>` so the backend can match it. A discount code cannot be used with İyzico payments.
- **Discount code** support (percentage or fixed amount; wallet/card methods only).
- **Wallet** — balance display, topping up between 10–5000 TL (simulated), a list of the last 20 transactions (top-up / trip payment / **referral bonus**); the balance and transactions are silently refreshed every time the screen resumes (`ON_RESUME`).
- **Saved card management** — adding a card (brand, last 4 digits, expiry date; the full card number/CVV is never stored, per PCI scope), setting a default card, deleting a card.
- Related files: `ui/screens/tripsummary/`, `ui/screens/wallet/`, `data/iyzico/`, `data/cards/`, `data/wallet/`

### 6. Profile, History, Referral & Settings
- **Trip history** — all rentals (newest first), status badges (Preparing/In Progress/Completed/Cancelled), an "unpaid" badge with a direct link to the payment screen for completed-but-unpaid trips; refreshed automatically every time the screen resumes.
- **Monthly statistics** — trip count, total spend, duration and distance summary (`rentals/stats`); used on both the History and Profile screens.
- **Profile** — name, phone, avatar, role badge, a license status card (hidden until the status is known, so a misleading "verify" prompt isn't shown too early), and a "refresh session" action once the license is approved (to obtain a `CUSTOMER` token); the Wallet tab's back stack is shared so navigating to it via "Payment methods" and then back to Profile doesn't leave the user stuck on the Wallet tab.
- **Referral code** — the user's own invite code (generated at `/auth/me`), with a dedicated sharing screen; when the invited person registers with the code and completes their first trip, a bonus is credited to the referrer's wallet.
- **Settings** — light/dark/system theme selection (persisted via DataStore).
- Related files: `ui/screens/history/`, `ui/screens/profile/`, `ui/screens/referral/`, `ui/screens/settings/`

## Screenshots

### Launch

| Splash |
|---|
| <img src="docs/screenshots/splash.png" width="220"> |

### Section 1 — Authentication, Onboarding & Session Persistence

| Screen | Dark Theme | Light Theme |
|---|---|---|
| Onboarding | <img src="docs/screenshots/onboarding.png" width="220"> | <img src="docs/screenshots/onboarding_light.png" width="220"> |
| Login / OTP | <img src="docs/screenshots/login.png" width="220"> | <img src="docs/screenshots/login_light.png" width="220"> |
| Register | <img src="docs/screenshots/register.png" width="220"> | <img src="docs/screenshots/register_light.png" width="220"> |

### Section 2 — License Verification

| Screen | Dark Theme | Light Theme |
|---|---|---|
| License Upload | <img src="docs/screenshots/license.png" width="220"> | <img src="docs/screenshots/license_light.png" width="220"> |

### Section 3 — Vehicle Discovery & Reservation

| Screen | Dark Theme | Light Theme |
|---|---|---|
| Map (Home) | <img src="docs/screenshots/homepage.png" width="220"> | <img src="docs/screenshots/homepage_light.png" width="220"> |
| Vehicle Detail | <img src="docs/screenshots/vehicle_detail.png" width="220"> | <img src="docs/screenshots/vehicle_detail_light.png" width="220"> |
| Reservation | <img src="docs/screenshots/reservation.png" width="220"> | <img src="docs/screenshots/reservation_light.png" width="220"> |

### Section 4 — Handover & Active Trip

| Screen | Dark Theme | Light Theme |
|---|---|---|
| Handover (4-Direction Photos) | <img src="docs/screenshots/handover.png" width="220"> | <img src="docs/screenshots/handover_light.png" width="220"> |
| Active Trip | <img src="docs/screenshots/active_rental.png" width="220"> | <img src="docs/screenshots/active_rental_light.png" width="220"> |

### Section 5 — Payment & Wallet

| Screen | Dark Theme | Light Theme |
|---|---|---|
| Trip Summary / Payment | <img src="docs/screenshots/trip_summary.png" width="220"> | <img src="docs/screenshots/trip_summary_light.png" width="220"> |
| Wallet | <img src="docs/screenshots/wallet.png" width="220"> | <img src="docs/screenshots/wallet_light.png" width="220"> |

### Section 6 — Profile, History, Referral & Settings

| Screen | Dark Theme | Light Theme |
|---|---|---|
| History | <img src="docs/screenshots/history.png" width="220"> | <img src="docs/screenshots/history_light.png" width="220"> |
| Profile | <img src="docs/screenshots/profile.png" width="220"> | <img src="docs/screenshots/profile_light.png" width="220"> |
| Referral | <img src="docs/screenshots/referral.png" width="220"> | <img src="docs/screenshots/referral_light.png" width="220"> |
| Settings | <img src="docs/screenshots/settings.png" width="220"> | <img src="docs/screenshots/settings_light.png" width="220"> |

## Architecture & Tech Stack

- **Language:** Kotlin (2.2.10), Java 11 compatibility
- **UI:** Jetpack Compose (BOM 2026.02.01), Material 3 (light/dark/system theme support, a `CompositionLocal`-based semantic color palette), `androidx.core:core-splashscreen`
- **Design system:** `ui/theme/Dimens.kt` — a single-source-of-truth token set for spacing (4–32dp), corner radii, and control height; `ui/common/ErrorMapping.kt` — `Throwable.toErrorRes()` provides a shared HTTP-error-to-string-resource mapping across screens; all UI text lives in `res/values/strings.xml` (375 lines).
- **Navigation:** Navigation Compose (type-safe `@Serializable` routes), a separate auth graph (Onboarding/Login/Register) and main app graph, selected based on `SessionState` (see [Navigation Graph](#navigation-graph))
- **Networking:** Retrofit 2.11 + OkHttp 4.12 (logging interceptor, Bearer token authenticator with automatic refresh), kotlinx.serialization (JSON)
- **Real-time communication:** Socket.IO client 2.1.0 (live vehicle location)
- **Maps:** MapLibre Android SDK 13.3.1 + Annotation Plugin (clustering, custom canvas markers)
- **Location:** Google Play Services Location 21.3.0
- **Address search:** OpenStreetMap Nominatim REST API (a separate Retrofit client, `countrycodes=tr`, a mandatory `User-Agent` interceptor)
- **Image loading:** Coil 2.7.0
- **Local storage:** Jetpack DataStore (Preferences) — session tokens (`TokenStorage`), theme and onboarding preferences; a `ReplaceFileCorruptionHandler` resets a corrupted file instead of crash-looping
- **Async:** Kotlin Coroutines (StateFlow-based UI state, polling, tickers, `Mutex` for serializing token refresh)
- **Payment:** İyzico (checkout form + 3-D Secure + direct card, all through backend REST endpoints; no native client-side SDK is used), a WebView-based return flow
- **Min SDK / Target SDK / Compile SDK:** 24 / 36 / 36

Architecture layering:

```
UI (Compose Screens) → ViewModel (StateFlow) → Repository → Retrofit/Socket.IO API (NetworkModule) → Backend (rencarv2, OpenAPI v2)
```

Singletons (`AuthSession`, `SessionManager`, `ThemeController`, `OnboardingPreferences`) are implemented with a simple `object` + `StateFlow` pattern instead of a DI framework.

## Project Structure (Full File Map)

```
app/src/main/java/com/flowbytestudio/rencar/
├── MainActivity.kt          # installSplashScreen(), SessionManager.init, theme/onboarding init, nav graph selection based on SessionState
├── data/
│   ├── auth/
│   │   ├── AuthApi.kt             # register/login/verify-otp/refresh/logout/me
│   │   ├── AuthConstants.kt       # Phone/OTP/password field constraints (shared by login+register)
│   │   ├── AuthDtos.kt            # Request/response models (including referralCode, avatarUrl)
│   │   ├── AuthRepository.kt      # Wrapper that updates AuthSession
│   │   ├── AuthSession.kt         # TokenPair/currentUser/SessionState singleton, sequential disk-write queue
│   │   ├── SessionManager.kt      # Session restore on launch + timed validation, Mutex'd refresh rotation
│   │   ├── TokenStorage.kt        # DataStore-backed token/user persistence
│   │   └── UserRole.kt            # PENDING/CUSTOMER/ADMIN
│   ├── cards/
│   │   ├── CardApi.kt             # GET/POST/PATCH(default)/DELETE cards
│   │   ├── CardDtos.kt            # PCI scope: only brand/last4/exp are stored
│   │   └── CardRepository.kt
│   ├── geocoding/
│   │   ├── GeocodingApi.kt        # Nominatim (OSM) search endpoint (countrycodes=tr)
│   │   └── GeocodingRepository.kt
│   ├── iyzico/
│   │   ├── IyzicoApi.kt           # checkout-form init/result, direct card, 3DS init
│   │   └── IyzicoRepository.kt    # basketId="rental-<id>" contract
│   ├── license/
│   │   ├── LicenseApi.kt          # multipart upload (front/back/selfie), status
│   │   ├── LicenseDtos.kt
│   │   ├── LicenseRepository.kt   # Uri → temp file → multipart part
│   │   └── LicenseStatus.kt       # NOT_SUBMITTED/UNDER_REVIEW/APPROVED/REJECTED/UNKNOWN
│   ├── network/
│   │   ├── ApiError.kt            # HttpException → backend error message extraction
│   │   └── NetworkModule.kt       # Retrofit/OkHttp setup (main + geocoding clients), token authenticator, WS URL
│   ├── rentals/
│   │   ├── RentalApi.kt           # create/list/stats/active/photos/start/cancel/finish/return/pay
│   │   ├── RentalDto.kt           # Shared DTO (PREPARING/ACTIVE fields in one model)
│   │   ├── RentalPlan.kt          # DAKIKALIK/SAATLIK/GUNLUK ↔ backend apiValue mapping
│   │   ├── RentalRepository.kt
│   │   ├── RentalStatus.kt        # RentalStatus + PaymentStatus enums (backend contract)
│   │   └── RideLocationClient.kt  # Socket.IO live location stream (/ws/locations)
│   ├── reservations/
│   │   ├── ReservationApi.kt
│   │   ├── ReservationDtos.kt
│   │   ├── ReservationRepository.kt
│   │   └── ReservationStatus.kt
│   ├── settings/
│   │   ├── OnboardingPreferences.kt  # DataStore: has onboarding been seen
│   │   ├── ThemeController.kt        # DataStore: theme preference
│   │   └── ThemeMode.kt              # LIGHT/DARK/SYSTEM
│   ├── vehicles/
│   │   ├── VehicleApi.kt          # list (filter+pagination), detail, quote
│   │   ├── VehicleDto.kt
│   │   ├── VehicleRepository.kt
│   │   └── VehicleStatus.kt       # AVAILABLE/RESERVED/RENTED/MAINTENANCE/UNKNOWN
│   └── wallet/
│       ├── WalletApi.kt           # get, topup
│       ├── WalletDtos.kt
│       ├── WalletLimits.kt        # Min/max top-up amount, quick-amount chips
│       └── WalletRepository.kt
├── navigation/
│   ├── AppRoute.kt           # All @Serializable route definitions
│   ├── AppNavGraph.kt        # Main app NavHost (Map/LicenseUpload start destination, all transitions)
│   ├── BottomNavItem.kt      # 4 tabs: Map/History/Wallet/Profile
│   └── RencarNavBar.kt       # Bottom navigation bar Composable
├── ui/
│   ├── common/
│   │   ├── CameraCapture.kt  # System camera + runtime permission flow
│   │   ├── ErrorMapping.kt   # Throwable.toErrorRes() — shared HTTP error → string resource mapping
│   │   ├── ImageFiles.kt     # Pre-upload downscale/EXIF/JPEG compression
│   │   ├── MapStyles.kt      # MapLibre light/dark raster style JSON
│   │   └── Money.kt          # TL formatting (with ₺)
│   ├── screens/
│   │   ├── activerental/     # ActiveRentalScreen/UiState/ViewModel — Section 4
│   │   ├── handover/         # HandoverScreen/UiState/ViewModel — Section 4
│   │   ├── history/          # HistoryScreen/UiState/ViewModel/RentalUiModel — Section 6
│   │   ├── license/          # LicenseUploadScreen/UiState/ViewModel (3 steps + selfie) — Section 2
│   │   ├── login/            # LoginScreen/UiState/ViewModel — Section 1
│   │   ├── map/               # MapScreen/UiState/ViewModel + MarkerBitmapFactory,
│   │   │                      # VehicleDetailSheet, VehicleStatusVisuals, VehicleType — Section 3
│   │   ├── onboarding/        # OnboardingScreen/UiState/ViewModel (3 pages) — Section 1
│   │   ├── profile/           # ProfileScreen/UiState/ViewModel — Section 6
│   │   ├── referral/          # ReferralScreen/UiState/ViewModel — Section 6
│   │   ├── register/          # RegisterScreen/UiState/ViewModel (+referral code) — Section 1
│   │   ├── reservation/       # ReservationScreen/UiState/ViewModel — Section 3
│   │   ├── settings/          # SettingsScreen/UiState/ViewModel — Section 6
│   │   ├── tripsummary/       # TripSummaryScreen/UiState/ViewModel (+İyzico) — Section 5
│   │   └── wallet/            # WalletScreen/UiState/ViewModel (+card management) — Section 5
│   └── theme/
│       ├── Color.kt          # Light/dark semantic color palette (CompositionLocal)
│       ├── Dimens.kt         # Spacing/corner-radius/control-height design tokens
│       ├── Theme.kt          # RencarTheme — MaterialTheme wired to ThemeController
│       └── Type.kt           # Typography
docs/
├── api-openapi.json       # Backend OpenAPI schema (v1)
└── api-openapi_v2.json    # Backend OpenAPI 3.0 schema (v2, current — the contract this client consumes)
```

## Navigation Graph

`MainActivity.kt` switches between three top-level views based on `AuthSession.sessionState`:

- **`UNKNOWN`** — nothing is drawn; the `core-splashscreen` stays on screen while `SessionManager` validates the saved session.
- **`LOGGED_OUT`** — a separate auth `NavHost`: `OnboardingRoute` (first launch only, or via the debug flag) → `LoginRoute` ↔ `RegisterRoute`.
- **`LOGGED_IN`** — the main `Scaffold` with the `RencarNavBar` bottom bar; the bar is hidden on the following routes: `ReservationRoute`, `SettingsRoute`, `LicenseUploadRoute`, `HandoverRoute`, `ActiveRentalRoute`, `TripSummaryRoute`.

The main graph's (`AppNavGraph.kt`) start destination is `LicenseUploadRoute` if the user just registered (`AuthSession.justRegistered`), otherwise `MapRoute`.

```
MapRoute ──▶ ReservationRoute(vehicleId)
                 ├─ per-minute/hourly plan ─▶ HandoverRoute(rentalId) ─▶ ActiveRentalRoute(rentalId)
                 └─ daily (DAILY) plan ─────────────────────────────▶ ActiveRentalRoute(rentalId)
ActiveRentalRoute(rentalId) ──▶ TripSummaryRoute(rentalId)

Bottom Nav: MapRoute · HistoryRoute · WalletRoute · ProfileRoute
ProfileRoute ──▶ SettingsRoute / LicenseUploadRoute / ReferralRoute / WalletRoute (payment methods)
```

## Permissions

Permissions declared in `AndroidManifest.xml` and their justification:

| Permission | Reason |
|---|---|
| `INTERNET` | All API/Socket.IO/map traffic |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | User location on the map and the "find nearest vehicle" feature |
| `CAMERA` | License/selfie and handover (4-direction) photo capture |

Camera captures are written to temporary cache files through an app-specific `FileProvider` (`${applicationId}.fileprovider`).

## API

The backend implements the OpenAPI 3.0 schema defined in [docs/api-openapi_v2.json](docs/api-openapi_v2.json) (`docs/api-openapi.json` is the older v1 version, kept in the repo for reference). Main endpoint groups:

| Group | Description | Related Section |
|---|---|---|
| `Auth` | Registration, OTP login, token refresh, logout, `/auth/me` | 1 |
| `License` | License upload and status lookup | 2 |
| `Vehicles` | Vehicle listing, detail, price preview (quote) | 3 |
| `Reservations` | Vehicle reservation (15 min free hold) | 3 |
| `Rentals` | Starting a trip, photo flow, start, finish, payment | 4, 5 |
| `Wallet` | Wallet balance and top-up | 5 |
| `Admin` | License/vehicle/rental management (admin panel — not used by this client) | — |

Additionally, external/integration endpoints not present in the OpenAPI schema that the client consumes directly:

| Group | Description | Related Section |
|---|---|---|
| `Cards` | Saved card CRUD + default card | 5 |
| `İyzico` | Checkout form, 3-D Secure init, direct card charging (backend REST endpoints) | 5 |
| `Nominatim (OSM)` | Address/place search (the map's search bar, `countrycodes=tr`) | 3 |
| `Socket.IO /ws/locations` | Live location of the vehicle in the active rental | 4 |

Authentication is done via a **JWT Bearer token** (`Authorization: Bearer <access_token>`), refreshed automatically on a 401 via an OkHttp `Authenticator`. Roles: `PENDING`, `CUSTOMER`, `ADMIN`.

## Setup

### Requirements

- Android Studio (Koala or newer recommended)
- JDK 11+
- Android SDK 36

### Steps

```bash
git clone <this-repo>
cd rencar-app
```

Open the project in Android Studio, wait for the Gradle sync to finish, and run it on an emulator/device.

```bash
./gradlew assembleDebug
```

## Configuration

The API base address and other network settings are defined in [NetworkModule.kt](app/src/main/java/com/flowbytestudio/rencar/data/network/NetworkModule.kt):
- `BASE_URL` — the main backend address (`https://rencarv2.halitkalayci.com/`)
- `WS_LOCATIONS_URL` — the Socket.IO live location namespace
- The Nominatim client is defined with its own separate `Retrofit`/`OkHttpClient` (`https://nominatim.openstreetmap.org/`) and has its own `User-Agent` interceptor.

To point the app at your own backend, update the values in this file.

## Team

Developed as part of Turkcell Geleceği Yazan Gençler 5.0 — Kotlin Bootcamp.

- Emircan Açar
- Batuhan Berk Ertekin
- Emirhan Erdoğan
