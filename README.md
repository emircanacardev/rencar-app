# Rencar — Kullanım Bazlı Araç Kiralama

Rencar, dakikalık/saatlik/günlük plan seçenekleriyle çalışan, harita üzerinden anlık araç bulup kiralamayı sağlayan bir Android uygulamasıdır. Kullanıcı haritadan uygun aracı bulur, rezerve eder, teslim alma fotoğraflarını çekip yolculuğu başlatır; yolculuk süresince anlık ücret, mesafe ve canlı konum takip edilir, bitişte otomatik fatura kesilir ve cüzdan/kart/İyzico ile ödeme yapılır.

Bu depo, uygulamanın **Android (Kotlin + Jetpack Compose) istemci** tarafını içerir. Backend, [docs/api-openapi_v2.json](docs/api-openapi_v2.json) içinde tanımlı OpenAPI 3.0 REST API'si üzerinden tüketilir. Bu README, projedeki **her Kotlin dosyası tek tek incelenerek** hazırlanmıştır — hiçbir modül atlanmamıştır.

---

## İçindekiler

- [Kullanıcı Akışı](#kullanıcı-akışı)
- [Özellikler (Batch Bazlı)](#özellikler-batch-bazlı)
  - [1. Kimlik Doğrulama & Onboarding](#1-kimlik-doğrulama--onboarding)
  - [2. Ehliyet Doğrulama](#2-ehliyet-doğrulama)
  - [3. Araç Keşfi & Rezervasyon](#3-araç-keşfi--rezervasyon)
  - [4. Teslim Alma & Aktif Yolculuk](#4-teslim-alma--aktif-yolculuk)
  - [5. Ödeme & Cüzdan](#5-ödeme--cüzdan)
  - [6. Profil, Geçmiş & Ayarlar](#6-profil-geçmiş--ayarlar)
- [Ekran Görüntüleri](#ekran-görüntüleri)
- [Mimari ve Teknoloji Yığını](#mimari-ve-teknoloji-yığını)
- [Proje Yapısı (Tam Dosya Haritası)](#proje-yapısı-tam-dosya-haritası)
- [İzinler](#i̇zinler)
- [API](#api)
- [Kurulum](#kurulum)
- [Yapılandırma](#yapılandırma)
- [Ekip](#ekip)

---

## Kullanıcı Akışı

```
Onboarding (3 sayfa) → Login (OTP) / Register (+ referans kodu)
        → Ehliyet Yükleme (ön+arka+selfie) [kayıt sonrası zorunlu ilk durak]
        → [Bottom Nav: Harita · Geçmiş · Cüzdan · Profil]
        → Araç Seç (harita/liste/arama) → Rezervasyon (15 dk ücretsiz tutma)
        → Teslim Alma (4 yön fotoğraf) → Aktif Yolculuk (canlı süre/ücret/mesafe/konum)
        → Yolculuk Özeti → Ödeme (Cüzdan / Kayıtlı Kart / İyzico)
```

## Özellikler (Batch Bazlı)

İnceleme ve test kolaylığı için özellikler, uygulamadaki gerçek akış sırasına göre 6 batch'e bölünmüştür. Her batch, ilgili `data/` ve `ui/screens/` klasörlerinin satır satır incelenmesiyle çıkarılmıştır.

### 1. Kimlik Doğrulama & Onboarding
- **Onboarding** — 3 sayfalık kaydırmalı tanıtım (araç keşfi, hızlı kiralama, özgürce sürüş), sayfa göstergesi, geri tuşuyla önceki sayfaya dönüş; `debug` derlemede her açılışta tekrar gösterilir (bkz. [Yapılandırma](#yapılandırma)).
- **Şifresiz giriş (OTP)** — telefon numarasıyla giriş (`+90` ön eki otomatik eklenir), 6 haneli SMS doğrulama kodu (simülasyon, varsayılan kod `123456`), 60 saniyelik "tekrar gönder" sayacı, access + refresh token çifti.
- **Kayıt** — ad soyad, e-posta, parola (min. 6 karakter), telefon ve isteğe bağlı **referans kodu**; kayıt olan kullanıcı `PENDING` rolüyle anında oturum açar.
- **Token rotasyonu** — refresh token her kullanımda yenilenir; eski token tekrar kullanılırsa oturum zinciri güvenlik gereği iptal edilir. Ehliyet onayı sonrası yeniden giriş yapmadan `refreshSession()` ile `CUSTOMER` token'ı alınabilir (bkz. Batch 6 — Profil).
- Kayıt sonrası kullanıcı doğrudan **Ehliyet Yükleme** ekranına yönlendirilir (`AuthSession.justRegistered`).
- İlgili ekranlar: `onboarding/`, `login/`, `register/`

### 2. Ehliyet Doğrulama
- 3 adımlı akış: **ehliyet ön/arka fotoğraf → selfie → onay ekranı**, adımlar arası geri gidilebilir.
- Fotoğraflar galeriden seçilebilir veya kamerayla çekilebilir (`CameraCapture.kt`); yüklemeden önce otomatik **küçültme + EXIF döndürme + JPEG sıkıştırma** yapılır (`ImageFiles.kt`, sunucu 5MB sınırı için).
- Durum takibi: `NOT_SUBMITTED` → `UNDER_REVIEW` → `APPROVED` / `REJECTED`; reddedilirse red gerekçesiyle birlikte yeniden yükleme yapılabilir.
- Onaylanınca kullanıcı `PENDING` → `CUSTOMER` rolüne yükseltilir.
- İlgili ekran: `license/`

### 3. Araç Keşfi & Rezervasyon
- **MapLibre** tabanlı harita: müsait araçlar renkli, meşgul (RENTED/RESERVED) araçlar gri marker ile gösterilir (`includeBusy=true`); bakımdaki araçlar hiçbir zaman gösterilmez.
- **Marker kümeleme (clustering)** ve özel canvas-çizim marker/balon grafikleri (`MarkerBitmapFactory.kt`).
- Tip (Sedan/SUV/Hatchback/Station/Minivan) ve fiyat segmenti (Ekonomik/Konfor/SUV) filtreleri; segment sunucu tarafında, tip istemci tarafında filtrelenir.
- **Yer arama** — üst arama çubuğunda OpenStreetMap Nominatim API'si ile adres/konum arama (debounce'lı, 450ms).
- **En yakın aracı bul** — kullanıcı konumuna göre haversine formülüyle en yakın müsait aracı bulup odaklar.
- Araç detay bottom sheet'i: yakıt yüzdesi (progress bar), menzil, vites, koltuk sayısı, dakikalık/saatlik fiyat.
- Durum banner'ı (öncelik sırası): **aktif yolculuk** > **hazırlıktaki (PREPARING) yolculuk** > **aktif rezervasyon** — kullanıcı uygulamayı kapatıp açsa bile devam eden akışına haritadan geri dönebilir.
- **Rezervasyon** — aracı 15 dakika ücretsiz tutma (`RESERVATION_TTL_MIN`), canlı geri sayım; süre dolunca otomatik serbest bırakma. Plan seçimi (dakikalık/saatlik/günlük) ve seçilen süreye göre **fiyat önizleme (quote)** — kayıt oluşturmadan tahmini ücret hesabı. Başka araçta aktif rezervasyon varsa engelleyici bildirim gösterilir, oradan iptal edilebilir.
- İlgili ekranlar: `map/`, `reservation/`

### 4. Teslim Alma & Aktif Yolculuk
- **Handover** — dakikalık/saatlik planlarda yolculuk `PREPARING` durumunda açılır; aracın 4 yönünden (ön/arka/sol/sağ) fotoğraf çekimi zorunludur, aynı yöne ikinci çekim öncekinin yerine geçer. Uygulama yeniden açılırsa yarım kalan foto akışı sunucudan devralınır. Tüm yönler tamamlanınca "Başlat" aktif olur ve süre o an başlar (foto çekerken geçen süre faturalanmaz). `PREPARING` aşamasındaki yolculuk iptal edilebilir (araç anında `AVAILABLE` olur).
- **Günlük (DAILY) plan** foto akışı olmadan doğrudan `ACTIVE` başlar (geriye dönük uyumluluk).
- **Aktif yolculuk paneli** — geçen süre (sunucu senkronu + yerel 1sn tikleyici ile akıcı sayaç), anlık tahmini ücret ve biriken mesafe; 5 saniyede bir sunucudan yoklanır (polling), ardışık hatalarda hafif uyarı gösterilir.
- **Canlı konum** — Socket.IO ile (`/ws/locations` namespace, `my-vehicle` event'i) aktif kiralamadaki aracın anlık konumu haritada gösterilir; token süresi dolarsa bir kez tazelenip yeniden bağlanılır, soket hatası sessizce yutulur (harita son REST konumunda kalır).
- Yolculuk bitirme (`finish`) tüm planlarda geçerlidir; eski `return` ucu yalnız DAILY planı destekler.
- İlgili ekranlar: `handover/`, `activerental/`

### 5. Ödeme & Cüzdan
- Yolculuk bitişinde kalem kalem **ücret dökümü**: kullanım ücreti (dakika/saat bazlı) + açılış ücreti + servis ücreti; DAILY planda toplam baştan kilitlenir.
- **Yolculuk özeti** ekranından 3 ödeme yöntemi:
  - **Cüzdan** — bakiye yetersizse otomatik olarak kart yöntemine geçilir.
  - **Kayıtlı kart** — simülasyon (gerçek tahsilat yapılmaz).
  - **İyzico** — gerçek tahsilat, 3 alt yöntem: **hazır ödeme sayfası** (WebView içinde İyzico'nun barındırdığı checkout form), **3-D Secure kart formu** (kart bilgisi uygulamada toplanır, SMS onayı WebView'de tamamlanır) veya **doğrudan kart tahsilatı** (3DS'siz, senkron sonuç). İyzico ile ödemede indirim kodu kullanılamaz.
- **İndirim kodu** desteği (yüzde veya sabit tutar; yalnız cüzdan/kart yönteminde).
- **Cüzdan** — bakiye görüntüleme, 10–5000 TL arası yükleme (simülasyon), son 20 işlem listesi (yükleme / yolculuk ödemesi / **referans bonusu**).
- **Kayıtlı kart yönetimi** — kart ekleme (marka, son 4 hane, son kullanma tarihi; PCI kapsamı gereği tam kart no/CVV saklanmaz), varsayılan kart yapma, kart silme.
- İlgili ekranlar: `tripsummary/`, `wallet/`

### 6. Profil, Geçmiş & Ayarlar
- **Yolculuk geçmişi** — tüm kiralamalar (yeniden eskiye), durum rozetleri (Hazırlanıyor/Devam ediyor/Tamamlandı/İptal edildi), tamamlanmış ama ödenmemiş yolculuklar için "ödenmedi" rozeti ve doğrudan ödeme ekranına yönlendirme; ekrana her dönüşte otomatik yenilenir.
- **Aylık istatistikler** — yolculuk sayısı, toplam harcama, süre ve mesafe özeti (`rentals/stats`); hem Geçmiş hem Profil ekranında kullanılır.
- **Profil** — ad, telefon, avatar, rol rozeti, ehliyet durum kartı (onaylanana kadar gizli kalır, yanlış "doğrula" uyarısı önlenir), ehliyet onaylandıysa "oturumu yenile" aksiyonu (CUSTOMER token'ı almak için).
- **Referans kodu** — kullanıcının kendi davet kodu (`/auth/me`'de üretilir), paylaşım ekranı; davet edilen kişi kayıt olduğunda referans sahibine cüzdanına bonus yansır.
- **Ayarlar** — açık/koyu/sistem teması seçimi (DataStore ile kalıcı).
- İlgili ekranlar: `history/`, `profile/`, `referral/`, `settings/`

---

## Ekran Görüntüleri

> Görselleri `docs/screenshots/` klasörüne aşağıdaki dosya adlarıyla eklemen yeterli — bu tablo otomatik olarak dolacak, README'de başka değişiklik gerekmez. Tablolar yukarıdaki batch sırasına göre gruplanmıştır.

### Batch 1 — Kimlik Doğrulama & Onboarding

| Onboarding | Giriş / OTP | Kayıt |
|---|---|---|
| ![Onboarding](docs/screenshots/onboarding.png) | ![Login](docs/screenshots/login.png) | ![Register](docs/screenshots/register.png) |

### Batch 2 — Ehliyet Doğrulama

| Ehliyet Yükleme |
|---|
| ![License](docs/screenshots/license.png) |

### Batch 3 — Araç Keşfi & Rezervasyon

| Harita (Ana Sayfa) | Araç Detay | Rezervasyon |
|---|---|---|
| ![Homepage](docs/screenshots/homepage.png) | ![Vehicle Detail](docs/screenshots/vehicle_detail.png) | ![Reservation](docs/screenshots/reservation.png) |

### Batch 4 — Teslim Alma & Aktif Yolculuk

| Teslim Alma (4 Yön Foto) | Aktif Yolculuk |
|---|---|
| ![Handover](docs/screenshots/handover.png) | ![Active Rental](docs/screenshots/active_rental.png) |

### Batch 5 — Ödeme & Cüzdan

| Yolculuk Özeti / Ödeme | Cüzdan |
|---|---|
| ![Trip Summary](docs/screenshots/trip_summary.png) | ![Wallet](docs/screenshots/wallet.png) |

### Batch 6 — Profil, Geçmiş & Ayarlar

| Geçmiş | Profil | Referans | Ayarlar |
|---|---|---|---|
| ![History](docs/screenshots/history.png) | ![Profile](docs/screenshots/profile.png) | ![Referral](docs/screenshots/referral.png) | ![Settings](docs/screenshots/settings.png) |

## Mimari ve Teknoloji Yığını

- **Dil:** Kotlin
- **UI:** Jetpack Compose, Material 3 (açık/koyu/sistem tema desteği, `CompositionLocal` tabanlı semantik renk paleti)
- **Navigasyon:** Navigation Compose (type-safe `@Serializable` routes), ayrı auth graph (Onboarding/Login/Register) ve ana app graph
- **Ağ:** Retrofit + OkHttp (logging interceptor, Bearer token interceptor), kotlinx.serialization (JSON)
- **Gerçek zamanlı iletişim:** Socket.IO client (canlı araç konumu)
- **Harita:** MapLibre Android SDK + Annotation Plugin (kümeleme, özel canvas marker'lar)
- **Konum:** Google Play Services Location
- **Adres arama:** OpenStreetMap Nominatim REST API (ayrı Retrofit istemcisi, User-Agent zorunlu)
- **Görsel yükleme:** Coil
- **Yerel depolama:** Jetpack DataStore (Preferences) — tema ve onboarding tercihleri
- **Asenkron:** Kotlin Coroutines (StateFlow tabanlı UI state, polling, ticker'lar)
- **Ödeme:** İyzico (checkout form + 3-D Secure + doğrudan kart), WebView tabanlı geri dönüş akışı
- **Min SDK / Target SDK:** 24 / 36

Mimari katmanlama:

```
UI (Compose Screens) → ViewModel (StateFlow) → Repository → Retrofit/Socket.IO API (NetworkModule) → Backend (OpenAPI v2)
```

Singleton nesneler (`AuthSession`, `ThemeController`, `OnboardingPreferences`) DI framework'ü yerine basit `object` + `StateFlow` deseniyle uygulanmıştır.

## Proje Yapısı (Tam Dosya Haritası)

Aşağıdaki harita, `app/src/main/java` altındaki **her Kotlin dosyasını** kapsar.

```
app/src/main/java/com/flowbytestudio/rencar/
├── MainActivity.kt          # Splash, tema/onboarding init, auth durumuna göre nav graph seçimi
├── data/
│   ├── auth/
│   │   ├── AuthApi.kt            # register/login/verify-otp/refresh/logout/me
│   │   ├── AuthDtos.kt           # Request/Response modelleri (referralCode, avatarUrl dahil)
│   │   ├── AuthRepository.kt     # AuthSession'ı güncelleyen sarmalayıcı
│   │   └── AuthSession.kt        # accessToken/refreshToken/currentUser/isLoggedIn singleton'ı
│   ├── cards/
│   │   ├── CardApi.kt            # GET/POST/PATCH(default)/DELETE cards
│   │   ├── CardDtos.kt           # PCI kapsamı: yalnız brand/last4/exp saklanır
│   │   └── CardRepository.kt
│   ├── geocoding/
│   │   ├── GeocodingApi.kt       # Nominatim (OSM) arama ucu
│   │   └── GeocodingRepository.kt
│   ├── iyzico/
│   │   ├── IyzicoApi.kt          # checkout-form init/result, payments, 3DS init
│   │   └── IyzicoRepository.kt   # basketId="rental-<id>" sözleşmesi
│   ├── license/
│   │   ├── LicenseApi.kt         # multipart upload (front/back/selfie), status
│   │   ├── LicenseDtos.kt
│   │   └── LicenseRepository.kt  # Uri → geçici dosya → multipart parça
│   ├── network/
│   │   ├── ApiError.kt           # HttpException → backend hata mesajı çıkarımı
│   │   └── NetworkModule.kt      # Retrofit/OkHttp kurulumu, tüm API singleton'ları, WS URL
│   ├── rentals/
│   │   ├── RentalApi.kt          # create/list/stats/active/photos/start/cancel/finish/return/pay
│   │   ├── RentalDto.kt          # Ortak DTO (PREPARING/ACTIVE alanları tek modelde)
│   │   ├── RentalRepository.kt
│   │   └── RideLocationClient.kt # Socket.IO canlı konum akışı (/ws/locations)
│   ├── reservations/
│   │   ├── ReservationApi.kt
│   │   ├── ReservationDtos.kt
│   │   └── ReservationRepository.kt
│   ├── settings/
│   │   ├── OnboardingPreferences.kt  # DataStore: onboarding görüldü mü
│   │   ├── ThemeController.kt        # DataStore: tema tercihi
│   │   └── ThemeMode.kt              # LIGHT/DARK/SYSTEM
│   ├── vehicles/
│   │   ├── VehicleApi.kt         # list (filtre+sayfalama), detail, quote
│   │   ├── VehicleDto.kt
│   │   └── VehicleRepository.kt
│   └── wallet/
│       ├── WalletApi.kt          # get, topup
│       ├── WalletDtos.kt
│       └── WalletRepository.kt
├── navigation/
│   ├── AppRoute.kt           # Tüm @Serializable route tanımları
│   ├── AppNavGraph.kt        # Ana app NavHost (Map başlangıç noktası, tüm geçişler)
│   ├── BottomNavItem.kt      # 4 sekme: Harita/Geçmiş/Cüzdan/Profil
│   └── RencarNavBar.kt       # Alt navigasyon çubuğu Composable'ı
├── ui/
│   ├── common/
│   │   ├── CameraCapture.kt  # Sistem kamerası + runtime izin akışı
│   │   ├── ImageFiles.kt     # Yükleme öncesi küçültme/EXIF/JPEG sıkıştırma
│   │   └── Money.kt          # TL biçimlendirme (₺ ile)
│   ├── screens/
│   │   ├── activerental/     # ActiveRentalScreen/UiState/ViewModel — Batch 4
│   │   ├── handover/         # HandoverScreen/UiState/ViewModel — Batch 4
│   │   ├── history/          # HistoryScreen/UiState/ViewModel/RentalUiModel — Batch 6
│   │   ├── license/          # LicenseUploadScreen/UiState/ViewModel — Batch 2
│   │   ├── login/            # LoginScreen/UiState/ViewModel — Batch 1
│   │   ├── map/               # MapScreen/UiState/ViewModel + MarkerBitmapFactory,
│   │   │                      # VehicleDetailSheet, VehicleType/Segment — Batch 3
│   │   ├── onboarding/        # OnboardingScreen/UiState/ViewModel (3 sayfa) — Batch 1
│   │   ├── profile/           # ProfileScreen/UiState/ViewModel — Batch 6
│   │   ├── referral/          # ReferralScreen/UiState/ViewModel — Batch 6
│   │   ├── register/          # RegisterScreen/UiState/ViewModel — Batch 1
│   │   ├── reservation/       # ReservationScreen/UiState/ViewModel — Batch 3
│   │   ├── settings/          # SettingsScreen/UiState/ViewModel — Batch 6
│   │   ├── tripsummary/       # TripSummaryScreen/UiState/ViewModel (+İyzico) — Batch 5
│   │   └── wallet/            # WalletScreen/UiState/ViewModel (+kart yönetimi) — Batch 5
│   └── theme/
│       ├── Color.kt          # Açık/koyu semantik renk paleti (CompositionLocal)
│       ├── Theme.kt          # RencarTheme — ThemeController'a bağlı MaterialTheme
│       └── Type.kt           # Tipografi
docs/
├── api-openapi_v2.json   # Backend OpenAPI 3.0 şeması
└── screenshots/           # README ekran görüntüleri (bkz. yukarıdaki tablolar)
```

## İzinler

`AndroidManifest.xml` içinde tanımlı izinler ve gerekçeleri:

| İzin | Neden |
|---|---|
| `INTERNET` | Tüm API/Socket.IO/harita trafiği |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Haritada kullanıcı konumu ve "en yakın araç" özelliği |
| `CAMERA` | Ehliyet/selfie ve teslim alma (4 yön) fotoğraf çekimi |

Kamera çekimleri, uygulamaya özel `FileProvider` (`${applicationId}.fileprovider`) üzerinden geçici cache dosyalarına yazılır.

## API

Backend, [docs/api-openapi_v2.json](docs/api-openapi_v2.json) dosyasında tanımlı OpenAPI 3.0 şemasını uygular. Başlıca uç nokta grupları:

| Grup | Açıklama | İlgili Batch |
|---|---|---|
| `Auth` | Kayıt, OTP ile giriş, token yenileme, çıkış, `/auth/me` | 1 |
| `License` | Ehliyet yükleme ve durum sorgulama | 2 |
| `Vehicles` | Araç listeleme, detay, fiyat önizleme (quote) | 3 |
| `Reservations` | Araç rezervasyonu (15 dk ücretsiz tutma) | 3 |
| `Rentals` | Yolculuk açma, foto akışı, başlatma, bitirme, ödeme | 4, 5 |
| `Wallet` | Cüzdan bakiyesi ve bakiye yükleme | 5 |
| `Admin` | Ehliyet/araç/kiralama yönetimi (admin paneli — bu istemcide kullanılmaz) | — |

Ayrıca OpenAPI şemasında yer almayan, istemcinin doğrudan tükettiği harici/entegrasyon uçları:

| Grup | Açıklama | İlgili Batch |
|---|---|---|
| `Cards` | Kayıtlı kart CRUD + varsayılan kart | 5 |
| `İyzico` | Checkout form, 3-D Secure init, doğrudan kart tahsilatı | 5 |
| `Nominatim (OSM)` | Adres/yer arama (harita üstü arama çubuğu) | 3 |
| `Socket.IO /ws/locations` | Aktif kiralamadaki aracın canlı konumu | 4 |

Kimlik doğrulama **JWT Bearer token** ile yapılır (`Authorization: Bearer <access_token>`). Roller: `PENDING`, `CUSTOMER`, `ADMIN`.

## Kurulum

### Gereksinimler

- Android Studio (Koala veya üzeri önerilir)
- JDK 11+
- Android SDK 36

### Adımlar

```bash
git clone <bu-repo>
cd rencar-app
```

Android Studio ile projeyi açın, Gradle senkronizasyonunun tamamlanmasını bekleyin ve bir emülatör/cihazda çalıştırın.

```bash
./gradlew assembleDebug
```

## Yapılandırma

API taban adresi ve diğer ağ ayarları [NetworkModule.kt](app/src/main/java/com/flowbytestudio/rencar/data/network/NetworkModule.kt) içinde tanımlıdır:
- `BASE_URL` — ana backend adresi
- `WS_LOCATIONS_URL` — Socket.IO canlı konum namespace'i
- `NOMINATIM_BASE_URL` — adres arama servisi

Kendi backend adresinizi kullanmak için bu dosyadaki değerleri güncelleyin.

`debug` build tipinde onboarding ekranı `ALWAYS_SHOW_ONBOARDING` bayrağıyla her açılışta gösterilir; bu davranış `release` derlemesinde kapatılır ([app/build.gradle.kts](app/build.gradle.kts)).

## Ekip

Turkcell Geleceği Yazan Gençler 5.0 — Kotlin Bootcamp kapsamında geliştirilmiştir.

- [Ad Soyad](https://github.com/kullanici-adi)
- [Ad Soyad](https://github.com/kullanici-adi)
