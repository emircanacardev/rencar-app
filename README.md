# Rencar — Kullanım Bazlı Araç Kiralama

*Türkçe | [English](README.en.md)*

Rencar, dakikalık/saatlik/günlük plan seçenekleriyle çalışan, harita üzerinden anlık araç bulup kiralamayı sağlayan bir Android uygulamasıdır. Kullanıcı haritadan uygun aracı bulur, rezerve eder, teslim alma fotoğraflarını çekip yolculuğu başlatır; yolculuk süresince anlık ücret, mesafe ve canlı konum takip edilir, bitişte otomatik fatura kesilir ve cüzdan/kart/İyzico ile ödeme yapılır.

Bu depo, uygulamanın **Android (Kotlin + Jetpack Compose) istemci** tarafını içerir. Backend, [docs/api-openapi_v2.json](docs/api-openapi_v2.json) içinde tanımlı OpenAPI 3.0 REST API'si üzerinden (`rencarv2` sürümü) tüketilir.

---

## İçindekiler

- [Kullanıcı Akışı](#kullanıcı-akışı)
- [Özellikler](#özellikler)
  - [1. Kimlik Doğrulama, Onboarding & Oturum Kalıcılığı](#1-kimlik-doğrulama-onboarding--oturum-kalıcılığı)
  - [2. Ehliyet Doğrulama](#2-ehliyet-doğrulama)
  - [3. Araç Keşfi & Rezervasyon](#3-araç-keşfi--rezervasyon)
  - [4. Teslim Alma & Aktif Yolculuk](#4-teslim-alma--aktif-yolculuk)
  - [5. Ödeme & Cüzdan](#5-ödeme--cüzdan)
  - [6. Profil, Geçmiş, Referans & Ayarlar](#6-profil-geçmiş-referans--ayarlar)
- [Ekran Görüntüleri](#ekran-görüntüleri)
- [Mimari ve Teknoloji Yığını](#mimari-ve-teknoloji-yığını)
- [Proje Yapısı (Tam Dosya Haritası)](#proje-yapısı-tam-dosya-haritası)
- [Navigasyon Grafiği](#navigasyon-grafiği)
- [İzinler](#i̇zinler)
- [API](#api)
- [Kurulum](#kurulum)
- [Yapılandırma](#yapılandırma)
- [Ekip](#ekip)

---

## Kullanıcı Akışı

```
Splash (kayıtlı oturum doğrulanana kadar bekler)
        → Onboarding (3 sayfa, yalnız ilk açılışta) → Login (OTP) / Register (+ referans kodu)
        → Ehliyet Yükleme (ön + arka + selfie) [kayıt sonrası zorunlu ilk durak]
        → [Bottom Nav: Harita · Geçmiş · Cüzdan · Profil]
        → Araç Seç (harita/arama) → Rezervasyon (15 dk ücretsiz tutma)
        → Teslim Alma (4 yön fotoğraf) → Aktif Yolculuk (canlı süre/ücret/mesafe/konum)
        → Yolculuk Özeti → Ödeme (Cüzdan / Kayıtlı Kart / İyzico)
```

Kayıtlı bir oturum varsa splash ekranı arka planda token'ı doğrulayıp kullanıcıyı doğrudan Harita ekranına yönlendirir; onboarding ve login tekrar gösterilmez.

## Özellikler

İnceleme ve test kolaylığı için özellikler, uygulamadaki gerçek akış sırasına göre 6 bölüme ayrılmıştır.

### 1. Kimlik Doğrulama, Onboarding & Oturum Kalıcılığı
- **Onboarding** — 3 sayfalık kaydırmalı tanıtım, sayfa göstergesi, geri tuşuyla önceki sayfaya dönüş; yalnız ilk açılışta gösterilir (`OnboardingPreferences`, DataStore ile kalıcı).
- **Şifresiz giriş (OTP)** — telefon numarasıyla giriş (`+90` ön eki otomatik eklenir), 6 haneli SMS doğrulama kodu, 60 saniyelik "tekrar gönder" sayacı (`AuthConstants`); kod tamamlanır tamamlanmaz otomatik doğrulama tetiklenir.
- **Kayıt** — ad soyad, e-posta, parola (min. 6 karakter), telefon ve isteğe bağlı **referans kodu**; kayıt olan kullanıcı `PENDING` rolüyle anında oturum açar.
- **Kalıcı oturum (Splash + SessionManager)** — `SessionManager` uygulama açılışında kayıtlı access/refresh token çiftini `TokenStorage`'dan (DataStore) geri yükler ve 5 saniyelik zaman aşımıyla arka planda doğrular; bu süre boyunca `core-splashscreen` ekranı ekranda tutulur (`setKeepOnScreenCondition`), böylece oturumu açık bir kullanıcı hiçbir zaman login ekranını görmez. Eşzamanlı 401'lerde token yenileme `Mutex` ile serileştirilir — aksi halde backend'in refresh-token rotasyon kuralı, aynı anda gelen ikinci yenileme isteğini "çalınmış token" sayıp tüm oturum zincirini iptal eder. `AuthSession`, access/refresh çiftini tek parça (`TokenPair`) olarak tutup diske yazımı ayrı, tek iş parçacıklı bir kuyrukta sıraya koyar; böylece paralel yazımlar birbirinin üzerine yazmaz.
- **Token rotasyonu** — refresh token her kullanımda yenilenir; eski token tekrar kullanılırsa oturum zinciri güvenlik gereği iptal edilir. Ehliyet onayı sonrası yeniden giriş yapmadan `refreshSession()` ile `CUSTOMER` token'ı alınabilir (bkz. Bölüm 6 — Profil).
- Kayıt sonrası kullanıcı doğrudan **Ehliyet Yükleme** ekranına yönlendirilir (`AuthSession.justRegistered`).
- İlgili dosyalar: `ui/screens/onboarding/`, `ui/screens/login/`, `ui/screens/register/`, `data/auth/`

### 2. Ehliyet Doğrulama
- 3 adımlı akış: **ehliyet ön/arka fotoğraf → selfie → onay ekranı**, adımlar arası geri gidilebilir.
- Selfie adımı, kullanıcının profil fotoğrafı zaten varsa (`AuthSession.currentUser.avatarUrl`) onu otomatik önerir; kullanıcı isterse değiştirebilir.
- Fotoğraflar galeriden seçilebilir veya kamerayla çekilebilir (`CameraCapture.kt`); yüklemeden önce otomatik **küçültme + EXIF döndürme + JPEG sıkıştırma** yapılır (`ImageFiles.kt`, sunucu 5MB sınırı için).
- Durum takibi: `NOT_SUBMITTED` → `UNDER_REVIEW` → `APPROVED` / `REJECTED`; reddedilirse red gerekçesiyle birlikte yeniden yükleme yapılabilir.
- Onaylanınca kullanıcı `PENDING` → `CUSTOMER` rolüne yükseltilir.
- İlgili dosyalar: `ui/screens/license/`, `data/license/`

### 3. Araç Keşfi & Rezervasyon
- **MapLibre** tabanlı harita: müsait araçlar renkli, meşgul (RENTED/RESERVED) araçlar gri marker ile gösterilir (`includeBusy=true`); bakımdaki araçlar hiçbir zaman gösterilmez. Araç/rezervasyon/kiralama durumları backend sözleşmesine göre merkezi enum'larla (`VehicleStatus`, `RentalStatus`, `PaymentStatus`, `ReservationStatus`) ayrıştırılır — ekranlar birbirinden bağımsız ham string karşılaştırması yapmaz, backend yeni bir durum eklerse `UNKNOWN`'a düşüp uygulama kırılmaz.
- **Marker kümeleme (clustering)** ve özel canvas-çizim marker/balon grafikleri (`MarkerBitmapFactory.kt`).
- Tip (Sedan/SUV/Hatchback/Station/Minivan) ve fiyat segmenti (Ekonomik/Konfor/SUV) filtreleri; segment sunucu tarafında, tip istemci tarafında filtrelenir.
- **Yer arama** — üst arama çubuğunda OpenStreetMap **Nominatim** API'si ile adres/konum arama (debounce'lı; sonuçlar `countrycodes=tr` ile Türkiye'yle sınırlandırılır, ayrı bir `User-Agent` interceptor'lı Retrofit istemcisi kullanılır).
- **En yakın aracı bul** — kullanıcı konumuna göre haversine formülüyle en yakın müsait aracı bulup odaklar.
- Araç detay bottom sheet'i: yakıt yüzdesi (progress bar), menzil, vites, koltuk sayısı, dakikalık/saatlik fiyat.
- Durum banner'ı (öncelik sırası): **aktif yolculuk** > **hazırlıktaki (PREPARING) yolculuk** > **aktif rezervasyon** — kullanıcı uygulamayı kapatıp açsa bile devam eden akışına haritadan geri dönebilir.
- **Rezervasyon** — aracı 15 dakika ücretsiz tutma (`RESERVATION_TTL_MIN`), canlı geri sayım; süre dolunca otomatik serbest bırakma. Plan seçimi (dakikalık/saatlik/günlük) ve seçilen süreye göre **fiyat önizleme (quote)** — kayıt oluşturmadan tahmini ücret hesabı. Başka araçta aktif rezervasyon varsa engelleyici bildirim gösterilir, oradan iptal edilebilir.
- İlgili dosyalar: `ui/screens/map/`, `ui/screens/reservation/`, `data/geocoding/`, `data/vehicles/`, `data/reservations/`

### 4. Teslim Alma & Aktif Yolculuk
- **Handover** — dakikalık/saatlik planlarda yolculuk `PREPARING` durumunda açılır; aracın 4 yönünden (ön/arka/sol/sağ) fotoğraf çekimi zorunludur, aynı yöne ikinci çekim öncekinin yerine geçer. Uygulama yeniden açılırsa yarım kalan foto akışı sunucudan devralınır. Tüm yönler tamamlanınca "Başlat" aktif olur ve süre o an başlar (foto çekerken geçen süre faturalanmaz). `PREPARING` aşamasındaki yolculuk iptal edilebilir (araç anında `AVAILABLE` olur).
- **Günlük (DAILY) plan** foto akışı olmadan doğrudan `ACTIVE` başlar (geriye dönük uyumluluk).
- **Aktif yolculuk paneli** — geçen süre (sunucu senkronu + yerel 1sn tikleyici ile akıcı sayaç), anlık tahmini ücret ve biriken mesafe; 5 saniyede bir sunucudan yoklanır (polling), ardışık hatalarda hafif uyarı gösterilir.
- **Canlı konum** — Socket.IO ile (`/ws/locations` namespace, `my-vehicle` event'i) aktif kiralamadaki aracın anlık konumu haritada gösterilir; token süresi dolarsa bir kez tazelenip yeniden bağlanılır, soket hatası sessizce yutulur (harita son REST konumunda kalır).
- Yolculuk bitirme (`finish`) tüm planlarda geçerlidir; eski `return` ucu yalnız DAILY planı destekler.
- İlgili dosyalar: `ui/screens/handover/`, `ui/screens/activerental/`, `data/rentals/`

### 5. Ödeme & Cüzdan
- Yolculuk bitişinde kalem kalem **ücret dökümü**: kullanım ücreti (dakika/saat bazlı) + açılış ücreti + servis ücreti; DAILY planda toplam baştan kilitlenir.
- **Yolculuk özeti** ekranından 3 ödeme yöntemi:
  - **Cüzdan** — bakiye yetersizse otomatik olarak kart yöntemine geçilir.
  - **Kayıtlı kart** — simülasyon (gerçek tahsilat yapılmaz).
  - **İyzico** — gerçek tahsilat, backend'in kendi REST uçları üzerinden (istemci tarafında native İyzico SDK'sı kullanılmaz), 3 alt yöntem: **hazır ödeme sayfası** (WebView içinde İyzico'nun barındırdığı checkout form), **3-D Secure kart formu** (kart bilgisi uygulamada toplanır, banka onayı WebView'de tamamlanır) veya **doğrudan kart tahsilatı** (3DS'siz, tek istekte senkron sonuç). Her ödeme, backend'in eşleştirebilmesi için `rental-<id>` biçiminde bir basketId ile başlatılır. İyzico ile ödemede indirim kodu kullanılamaz.
- **İndirim kodu** desteği (yüzde veya sabit tutar; yalnız cüzdan/kart yönteminde).
- **Cüzdan** — bakiye görüntüleme, 10–5000 TL arası yükleme (simülasyon), son 20 işlem listesi (yükleme / yolculuk ödemesi / **referans bonusu**); ekrana her dönüşte (`ON_RESUME`) bakiye ve işlemler sessizce tazelenir.
- **Kayıtlı kart yönetimi** — kart ekleme (marka, son 4 hane, son kullanma tarihi; PCI kapsamı gereği tam kart no/CVV saklanmaz), varsayılan kart yapma, kart silme.
- İlgili dosyalar: `ui/screens/tripsummary/`, `ui/screens/wallet/`, `data/iyzico/`, `data/cards/`, `data/wallet/`

### 6. Profil, Geçmiş, Referans & Ayarlar
- **Yolculuk geçmişi** — tüm kiralamalar (yeniden eskiye), durum rozetleri (Hazırlanıyor/Devam ediyor/Tamamlandı/İptal edildi), tamamlanmış ama ödenmemiş yolculuklar için "ödenmedi" rozeti ve doğrudan ödeme ekranına yönlendirme; ekrana her dönüşte (`ON_RESUME`) otomatik yenilenir.
- **Aylık istatistikler** — yolculuk sayısı, toplam harcama, süre ve mesafe özeti (`rentals/stats`); hem Geçmiş hem Profil ekranında kullanılır.
- **Profil** — ad, telefon, avatar, rol rozeti, ehliyet durum kartı (onaylanana kadar gizli kalır, yanlış "doğrula" uyarısı önlenir), ehliyet onaylandıysa "oturumu yenile" aksiyonu (CUSTOMER token'ı almak için); Cüzdan sekmesinin geri yığınını paylaşarak "Ödeme yöntemleri"nden gidilip Profil'e dönüldüğünde ekranın Cüzdan'da takılı kalmaması sağlanır.
- **Referans kodu** — kullanıcının kendi davet kodu (`/auth/me`'de üretilir), ayrı bir ekranda paylaşım aksiyonu; davet edilen kişi referans koduyla kayıt olup ilk yolculuğunu tamamladığında referans sahibinin cüzdanına bonus yansır.
- **Ayarlar** — açık/koyu/sistem teması seçimi (DataStore ile kalıcı).
- İlgili dosyalar: `ui/screens/history/`, `ui/screens/profile/`, `ui/screens/referral/`, `ui/screens/settings/`

## Ekran Görüntüleri

### Açılış

| Splash |
|---|
| <img src="docs/screenshots/splash.png" width="220"> |

### Bölüm 1 — Kimlik Doğrulama, Onboarding & Oturum Kalıcılığı

| Ekran | Koyu Tema | Açık Tema |
|---|---|---|
| Onboarding | <img src="docs/screenshots/onboarding.png" width="220"> | <img src="docs/screenshots/onboarding_light.png" width="220"> |
| Giriş / OTP | <img src="docs/screenshots/login.png" width="220"> | <img src="docs/screenshots/login_light.png" width="220"> |
| Kayıt | <img src="docs/screenshots/register.png" width="220"> | <img src="docs/screenshots/register_light.png" width="220"> |

### Bölüm 2 — Ehliyet Doğrulama

| Ekran | Koyu Tema | Açık Tema |
|---|---|---|
| Ehliyet Yükleme | <img src="docs/screenshots/license.png" width="220"> | <img src="docs/screenshots/license_light.png" width="220"> |

### Bölüm 3 — Araç Keşfi & Rezervasyon

| Ekran | Koyu Tema | Açık Tema |
|---|---|---|
| Harita (Ana Sayfa) | <img src="docs/screenshots/homepage.png" width="220"> | <img src="docs/screenshots/homepage_light.png" width="220"> |
| Araç Detay | <img src="docs/screenshots/vehicle_detail.png" width="220"> | <img src="docs/screenshots/vehicle_detail_light.png" width="220"> |
| Rezervasyon | <img src="docs/screenshots/reservation.png" width="220"> | <img src="docs/screenshots/reservation_light.png" width="220"> |

### Bölüm 4 — Teslim Alma & Aktif Yolculuk

| Ekran | Koyu Tema | Açık Tema |
|---|---|---|
| Teslim Alma (4 Yön Foto) | <img src="docs/screenshots/handover.png" width="220"> | <img src="docs/screenshots/handover_light.png" width="220"> |
| Aktif Yolculuk | <img src="docs/screenshots/active_rental.png" width="220"> | <img src="docs/screenshots/active_rental_light.png" width="220"> |

### Bölüm 5 — Ödeme & Cüzdan

| Ekran | Koyu Tema | Açık Tema |
|---|---|---|
| Yolculuk Özeti / Ödeme | <img src="docs/screenshots/trip_summary.png" width="220"> | <img src="docs/screenshots/trip_summary_light.png" width="220"> |
| Cüzdan | <img src="docs/screenshots/wallet.png" width="220"> | <img src="docs/screenshots/wallet_light.png" width="220"> |

### Bölüm 6 — Profil, Geçmiş, Referans & Ayarlar

| Ekran | Koyu Tema | Açık Tema |
|---|---|---|
| Geçmiş | <img src="docs/screenshots/history.png" width="220"> | <img src="docs/screenshots/history_light.png" width="220"> |
| Profil | <img src="docs/screenshots/profile.png" width="220"> | <img src="docs/screenshots/profile_light.png" width="220"> |
| Referans | <img src="docs/screenshots/referral.png" width="220"> | <img src="docs/screenshots/referral_light.png" width="220"> |
| Ayarlar | <img src="docs/screenshots/settings.png" width="220"> | <img src="docs/screenshots/settings_light.png" width="220"> |

## Mimari ve Teknoloji Yığını

- **Dil:** Kotlin (2.2.10), Java 11 uyumluluğu
- **UI:** Jetpack Compose (BOM 2026.02.01), Material 3 (açık/koyu/sistem tema desteği, `CompositionLocal` tabanlı semantik renk paleti), `androidx.core:core-splashscreen`
- **Tasarım sistemi:** `ui/theme/Dimens.kt` — boşluk (4–32dp), köşe yarıçapı ve kontrol yüksekliği için tek kaynaklı token seti; `ui/common/ErrorMapping.kt` — `Throwable.toErrorRes()` ile ekranlar arası ortak HTTP hata → string kaynağı eşlemesi; tüm UI metinleri `res/values/strings.xml`'de (375 satır) toplanmıştır.
- **Navigasyon:** Navigation Compose (type-safe `@Serializable` routes), ayrı auth graph (Onboarding/Login/Register) ve ana app graph, `SessionState`'e göre seçilir (bkz. [Navigasyon Grafiği](#navigasyon-grafiği))
- **Ağ:** Retrofit 2.11 + OkHttp 4.12 (logging interceptor, Bearer token authenticator ile otomatik refresh), kotlinx.serialization (JSON)
- **Gerçek zamanlı iletişim:** Socket.IO client 2.1.0 (canlı araç konumu)
- **Harita:** MapLibre Android SDK 13.3.1 + Annotation Plugin (kümeleme, özel canvas marker'lar)
- **Konum:** Google Play Services Location 21.3.0
- **Adres arama:** OpenStreetMap Nominatim REST API (ayrı Retrofit istemcisi, `countrycodes=tr`, zorunlu `User-Agent` interceptor'ı)
- **Görsel yükleme:** Coil 2.7.0
- **Yerel depolama:** Jetpack DataStore (Preferences) — oturum token'ları (`TokenStorage`), tema ve onboarding tercihleri; `ReplaceFileCorruptionHandler` ile bozuk dosyada crash-loop yerine sıfırlama
- **Asenkron:** Kotlin Coroutines (StateFlow tabanlı UI state, polling, ticker'lar, `Mutex` ile token yenileme serileştirme)
- **Ödeme:** İyzico (backend REST uçları üzerinden checkout form + 3-D Secure + doğrudan kart; istemci tarafı native SDK kullanılmaz), WebView tabanlı geri dönüş akışı
- **Min SDK / Target SDK / Compile SDK:** 24 / 36 / 36

Mimari katmanlama:

```
UI (Compose Screens) → ViewModel (StateFlow) → Repository → Retrofit/Socket.IO API (NetworkModule) → Backend (rencarv2, OpenAPI v2)
```

Singleton nesneler (`AuthSession`, `SessionManager`, `ThemeController`, `OnboardingPreferences`) DI framework'ü yerine basit `object` + `StateFlow` deseniyle uygulanmıştır.

## Proje Yapısı (Tam Dosya Haritası)

```
app/src/main/java/com/flowbytestudio/rencar/
├── MainActivity.kt          # installSplashScreen(), SessionManager.init, tema/onboarding init, SessionState'e göre nav graph seçimi
├── data/
│   ├── auth/
│   │   ├── AuthApi.kt             # register/login/verify-otp/refresh/logout/me
│   │   ├── AuthConstants.kt       # Telefon/OTP/parola alan kısıtları (login+register ortak)
│   │   ├── AuthDtos.kt            # Request/Response modelleri (referralCode, avatarUrl dahil)
│   │   ├── AuthRepository.kt      # AuthSession'ı güncelleyen sarmalayıcı
│   │   ├── AuthSession.kt         # TokenPair/currentUser/SessionState singleton'ı, diske sıralı yazım kuyruğu
│   │   ├── SessionManager.kt      # Açılışta oturum geri yükleme + zaman aşımlı doğrulama, Mutex'li refresh rotasyonu
│   │   ├── TokenStorage.kt        # DataStore tabanlı token/kullanıcı kalıcılığı
│   │   └── UserRole.kt            # PENDING/CUSTOMER/ADMIN
│   ├── cards/
│   │   ├── CardApi.kt             # GET/POST/PATCH(default)/DELETE cards
│   │   ├── CardDtos.kt            # PCI kapsamı: yalnız brand/last4/exp saklanır
│   │   └── CardRepository.kt
│   ├── geocoding/
│   │   ├── GeocodingApi.kt        # Nominatim (OSM) arama ucu (countrycodes=tr)
│   │   └── GeocodingRepository.kt
│   ├── iyzico/
│   │   ├── IyzicoApi.kt           # checkout-form init/result, doğrudan kart, 3DS init
│   │   └── IyzicoRepository.kt    # basketId="rental-<id>" sözleşmesi
│   ├── license/
│   │   ├── LicenseApi.kt          # multipart upload (front/back/selfie), status
│   │   ├── LicenseDtos.kt
│   │   ├── LicenseRepository.kt   # Uri → geçici dosya → multipart parça
│   │   └── LicenseStatus.kt       # NOT_SUBMITTED/UNDER_REVIEW/APPROVED/REJECTED/UNKNOWN
│   ├── network/
│   │   ├── ApiError.kt            # HttpException → backend hata mesajı çıkarımı
│   │   └── NetworkModule.kt       # Retrofit/OkHttp kurulumu (ana + geocoding istemcisi), token authenticator, WS URL
│   ├── rentals/
│   │   ├── RentalApi.kt           # create/list/stats/active/photos/start/cancel/finish/return/pay
│   │   ├── RentalDto.kt           # Ortak DTO (PREPARING/ACTIVE alanları tek modelde)
│   │   ├── RentalPlan.kt          # DAKIKALIK/SAATLIK/GUNLUK ↔ backend apiValue eşlemesi
│   │   ├── RentalRepository.kt
│   │   ├── RentalStatus.kt        # RentalStatus + PaymentStatus enum'ları (backend sözleşmesi)
│   │   └── RideLocationClient.kt  # Socket.IO canlı konum akışı (/ws/locations)
│   ├── reservations/
│   │   ├── ReservationApi.kt
│   │   ├── ReservationDtos.kt
│   │   ├── ReservationRepository.kt
│   │   └── ReservationStatus.kt
│   ├── settings/
│   │   ├── OnboardingPreferences.kt  # DataStore: onboarding görüldü mü
│   │   ├── ThemeController.kt        # DataStore: tema tercihi
│   │   └── ThemeMode.kt              # LIGHT/DARK/SYSTEM
│   ├── vehicles/
│   │   ├── VehicleApi.kt          # list (filtre+sayfalama), detail, quote
│   │   ├── VehicleDto.kt
│   │   ├── VehicleRepository.kt
│   │   └── VehicleStatus.kt       # AVAILABLE/RESERVED/RENTED/MAINTENANCE/UNKNOWN
│   └── wallet/
│       ├── WalletApi.kt           # get, topup
│       ├── WalletDtos.kt
│       ├── WalletLimits.kt        # Min/maks yükleme tutarı, hızlı tutar chip'leri
│       └── WalletRepository.kt
├── navigation/
│   ├── AppRoute.kt           # Tüm @Serializable route tanımları
│   ├── AppNavGraph.kt        # Ana app NavHost (Map/LicenseUpload başlangıç noktası, tüm geçişler)
│   ├── BottomNavItem.kt      # 4 sekme: Harita/Geçmiş/Cüzdan/Profil
│   └── RencarNavBar.kt       # Alt navigasyon çubuğu Composable'ı
├── ui/
│   ├── common/
│   │   ├── CameraCapture.kt  # Sistem kamerası + runtime izin akışı
│   │   ├── ErrorMapping.kt   # Throwable.toErrorRes() — ortak HTTP hata → string kaynağı eşlemesi
│   │   ├── ImageFiles.kt     # Yükleme öncesi küçültme/EXIF/JPEG sıkıştırma
│   │   ├── MapStyles.kt      # MapLibre açık/koyu raster stil JSON'ları
│   │   └── Money.kt          # TL biçimlendirme (₺ ile)
│   ├── screens/
│   │   ├── activerental/     # ActiveRentalScreen/UiState/ViewModel — Bölüm 4
│   │   ├── handover/         # HandoverScreen/UiState/ViewModel — Bölüm 4
│   │   ├── history/          # HistoryScreen/UiState/ViewModel/RentalUiModel — Bölüm 6
│   │   ├── license/          # LicenseUploadScreen/UiState/ViewModel (3 adım + selfie) — Bölüm 2
│   │   ├── login/            # LoginScreen/UiState/ViewModel — Bölüm 1
│   │   ├── map/               # MapScreen/UiState/ViewModel + MarkerBitmapFactory,
│   │   │                      # VehicleDetailSheet, VehicleStatusVisuals, VehicleType — Bölüm 3
│   │   ├── onboarding/        # OnboardingScreen/UiState/ViewModel (3 sayfa) — Bölüm 1
│   │   ├── profile/           # ProfileScreen/UiState/ViewModel — Bölüm 6
│   │   ├── referral/          # ReferralScreen/UiState/ViewModel — Bölüm 6
│   │   ├── register/          # RegisterScreen/UiState/ViewModel (+referans kodu) — Bölüm 1
│   │   ├── reservation/       # ReservationScreen/UiState/ViewModel — Bölüm 3
│   │   ├── settings/          # SettingsScreen/UiState/ViewModel — Bölüm 6
│   │   ├── tripsummary/       # TripSummaryScreen/UiState/ViewModel (+İyzico) — Bölüm 5
│   │   └── wallet/            # WalletScreen/UiState/ViewModel (+kart yönetimi) — Bölüm 5
│   └── theme/
│       ├── Color.kt          # Açık/koyu semantik renk paleti (CompositionLocal)
│       ├── Dimens.kt         # Boşluk/köşe yarıçapı/kontrol yüksekliği tasarım token'ları
│       ├── Theme.kt          # RencarTheme — ThemeController'a bağlı MaterialTheme
│       └── Type.kt           # Tipografi
docs/
├── api-openapi.json       # Backend OpenAPI şeması (v1)
└── api-openapi_v2.json    # Backend OpenAPI 3.0 şeması (v2, güncel — bu istemcinin tükettiği sözleşme)
```

## Navigasyon Grafiği

`MainActivity.kt`, `AuthSession.sessionState` durumuna göre üç farklı üst seviye görünüm arasında geçiş yapar:

- **`UNKNOWN`** — hiçbir şey çizilmez; `core-splashscreen` ekranda kalır (`SessionManager` kayıtlı oturumu doğrularken).
- **`LOGGED_OUT`** — ayrı bir auth `NavHost`'u: `OnboardingRoute` (yalnız ilk açılışta veya debug bayrağıyla) → `LoginRoute` ↔ `RegisterRoute`.
- **`LOGGED_IN`** — `RencarNavBar` alt çubuklu ana `Scaffold`; çubuk şu route'larda gizlenir: `ReservationRoute`, `SettingsRoute`, `LicenseUploadRoute`, `HandoverRoute`, `ActiveRentalRoute`, `TripSummaryRoute`.

Ana graf (`AppNavGraph.kt`) başlangıç noktası, kullanıcı az önce kayıt olduysa (`AuthSession.justRegistered`) `LicenseUploadRoute`, aksi halde `MapRoute`'tur.

```
MapRoute ──▶ ReservationRoute(vehicleId)
                 ├─ dakikalık/saatlik plan ─▶ HandoverRoute(rentalId) ─▶ ActiveRentalRoute(rentalId)
                 └─ günlük (DAILY) plan ────────────────────────────▶ ActiveRentalRoute(rentalId)
ActiveRentalRoute(rentalId) ──▶ TripSummaryRoute(rentalId)

Bottom Nav: MapRoute · HistoryRoute · WalletRoute · ProfileRoute
ProfileRoute ──▶ SettingsRoute / LicenseUploadRoute / ReferralRoute / WalletRoute (ödeme yöntemleri)
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

Backend, [docs/api-openapi_v2.json](docs/api-openapi_v2.json) dosyasında tanımlı OpenAPI 3.0 şemasını uygular (`docs/api-openapi.json` eski v1 sürümüdür, referans amaçlı depoda tutulur). Başlıca uç nokta grupları:

| Grup | Açıklama | İlgili Bölüm |
|---|---|---|
| `Auth` | Kayıt, OTP ile giriş, token yenileme, çıkış, `/auth/me` | 1 |
| `License` | Ehliyet yükleme ve durum sorgulama | 2 |
| `Vehicles` | Araç listeleme, detay, fiyat önizleme (quote) | 3 |
| `Reservations` | Araç rezervasyonu (15 dk ücretsiz tutma) | 3 |
| `Rentals` | Yolculuk açma, foto akışı, başlatma, bitirme, ödeme | 4, 5 |
| `Wallet` | Cüzdan bakiyesi ve bakiye yükleme | 5 |
| `Admin` | Ehliyet/araç/kiralama yönetimi (admin paneli — bu istemcide kullanılmaz) | — |

Ayrıca OpenAPI şemasında yer almayan, istemcinin doğrudan tükettiği harici/entegrasyon uçları:

| Grup | Açıklama | İlgili Bölüm |
|---|---|---|
| `Cards` | Kayıtlı kart CRUD + varsayılan kart | 5 |
| `İyzico` | Checkout form, 3-D Secure init, doğrudan kart tahsilatı (backend REST uçları) | 5 |
| `Nominatim (OSM)` | Adres/yer arama (harita üstü arama çubuğu, `countrycodes=tr`) | 3 |
| `Socket.IO /ws/locations` | Aktif kiralamadaki aracın canlı konumu | 4 |

Kimlik doğrulama **JWT Bearer token** ile yapılır (`Authorization: Bearer <access_token>`), OkHttp `Authenticator` ile 401 alındığında otomatik yenilenir. Roller: `PENDING`, `CUSTOMER`, `ADMIN`.

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
- `BASE_URL` — ana backend adresi (`https://rencarv2.halitkalayci.com/`)
- `WS_LOCATIONS_URL` — Socket.IO canlı konum namespace'i
- Nominatim istemcisi ayrı bir `Retrofit`/`OkHttpClient` ile (`https://nominatim.openstreetmap.org/`) tanımlıdır, kendi `User-Agent` interceptor'ına sahiptir.

Kendi backend adresinizi kullanmak için bu dosyadaki değerleri güncelleyin.

## Ekip

Turkcell Geleceği Yazan Gençler 5.0 — Kotlin Bootcamp kapsamında geliştirilmiştir.

- Emircan Açar
- Batuhan Berk Ertekin
- Emirhan Erdoğan
