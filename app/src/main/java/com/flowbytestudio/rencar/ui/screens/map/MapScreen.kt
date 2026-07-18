package com.flowbytestudio.rencar.ui.screens.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import android.view.Gravity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.geocoding.GeocodingResult
import com.flowbytestudio.rencar.data.vehicles.VehicleDto
import com.flowbytestudio.rencar.data.vehicles.VehicleStatus
import com.flowbytestudio.rencar.data.vehicles.vehicleStatus
import com.flowbytestudio.rencar.ui.common.MapStyles
import com.flowbytestudio.rencar.ui.common.formatTl
import com.flowbytestudio.rencar.ui.theme.Background
import com.flowbytestudio.rencar.ui.theme.Danger
import com.flowbytestudio.rencar.ui.theme.DarkRencarColors
import com.flowbytestudio.rencar.ui.theme.Dimens
import com.flowbytestudio.rencar.ui.theme.LocalRencarColors
import com.flowbytestudio.rencar.ui.theme.Primary
import com.flowbytestudio.rencar.ui.theme.PrimaryVariant
import com.flowbytestudio.rencar.ui.theme.Success
import com.flowbytestudio.rencar.ui.theme.Surface
import com.flowbytestudio.rencar.ui.theme.TextPrimary
import com.flowbytestudio.rencar.ui.theme.TextSecondary
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.OnSymbolClickListener
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import kotlin.math.pow

private const val ISTANBUL_LAT = 41.0082
private const val ISTANBUL_LON = 28.9784
private const val VEHICLE_REFRESH_INTERVAL_MS = 10_000L
private const val ME_SOURCE_ID = "me"
private val ME_MARKER_COLOR = android.graphics.Color.parseColor("#4285F4")
private const val CLUSTER_MAX_ZOOM = 15.0
private const val CLUSTER_RADIUS_DEGREES_AT_ZOOM0 = 40.0


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToReservation: (String) -> Unit = {},
    onNavigateToHandover: (String) -> Unit = {},
    onNavigateToActiveRental: (String) -> Unit = {},
    viewModel: MapViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = context.resources.displayMetrics.density
    val clusterColor = Primary.toArgb()
    val isDarkTheme = LocalRencarColors.current == DarkRencarColors
    val scope = rememberCoroutineScope()
    val locationNotFoundMessage = stringResource(R.string.map_snackbar_location_not_found)
    val locationNotFoundCheckPermissionMessage = stringResource(R.string.map_snackbar_location_not_found_check_permission)
    val noVehicleFoundMessage = stringResource(R.string.map_snackbar_no_vehicle_found)
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState()
    var selectedVehicle by remember { mutableStateOf<VehicleDto?>(null) }
    // Filtre kartının gerçek yüksekliği: segment/tip satırlarının sayısına göre
    // değişebildiği için konum butonu sabit bir değer yerine buna göre konumlanır.
    val localDensity = LocalDensity.current
    var bottomCardHeight by remember { mutableStateOf(0.dp) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        hasLocationPermission = grants.values.any { it }
    }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                // Ekrana her dönüşte (ör. kiralama bitirilip geri gelindiğinde) araçlar ve
                // aktif kiralama banner'ı hemen tazelensin diye yükleme başta yapılır.
                viewModel.loadVehicles()
                delay(VEHICLE_REFRESH_INTERVAL_MS)
            }
        }
    }

    var myLocation by remember { mutableStateOf<LatLng?>(null) }
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@DisposableEffect onDispose {}

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc -> myLocation = LatLng(loc.latitude, loc.longitude) }
            }
        }
        startLocationUpdates(fusedClient, callback)
        onDispose { fusedClient.removeLocationUpdates(callback) }
    }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapStyle by remember { mutableStateOf<Style?>(null) }
    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }
    var hasCenteredOnVehicles by remember { mutableStateOf(false) }
    var hasZoomedToUser by remember { mutableStateOf(false) }
    var cameraZoom by remember { mutableStateOf(12.0) }

    // Kullanıcı konumu gelene (ya da izin reddi/timeout sonucu İstanbul'a düşülene) kadar
    // haritanın ilk karesi hiç gösterilmez; böylece kullanıcı önce İstanbul'u görüp sonra
    // kendi konumuna kayma hissi yaşamaz.
    var isInitialCameraReady by remember { mutableStateOf(false) }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            delay(4_000L)
            if (!isInitialCameraReady) isInitialCameraReady = true
        }
    }
    LaunchedEffect(myLocation) {
        if (myLocation == null) {
            delay(4_000L)
        }
        if (!isInitialCameraReady) isInitialCameraReady = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapLibre.getInstance(ctx)
                MapView(ctx).apply {
                    onCreate(null)
                    getMapAsync { map ->
                        mapLibreMap = map
                        map.uiSettings.setLogoGravity(Gravity.TOP or Gravity.END)
                        map.uiSettings.setAttributionGravity(Gravity.TOP or Gravity.END)
                        map.addOnCameraIdleListener {
                            cameraZoom = map.cameraPosition.zoom
                        }
                    }
                    mapView = this
                }
            },
        )

        LaunchedEffect(mapLibreMap, mapView, isDarkTheme) {
            val map = mapLibreMap ?: return@LaunchedEffect
            val view = mapView ?: return@LaunchedEffect
            val isFirstLoad = mapStyle == null
            symbolManager?.onDestroy()
            loadMapStyle(map, isDarkTheme) { style ->
                mapStyle = style
                symbolManager = SymbolManager(view, map, style).apply {
                    iconAllowOverlap = true
                    iconIgnorePlacement = true
                }
                if (!isFirstLoad) hasCenteredOnVehicles = false
            }
        }

        DisposableEffect(lifecycleOwner, mapView) {
            val currentMapView = mapView
            if (currentMapView == null) {
                onDispose {}
            } else {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> currentMapView.onStart()
                        Lifecycle.Event.ON_RESUME -> currentMapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> currentMapView.onPause()
                        Lifecycle.Event.ON_STOP -> currentMapView.onStop()
                        Lifecycle.Event.ON_DESTROY -> currentMapView.onDestroy()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
        }

        LaunchedEffect(mapStyle, myLocation) {
            val style = mapStyle ?: return@LaunchedEffect
            updateMeMarker(style, myLocation)
        }

        LaunchedEffect(mapLibreMap, myLocation, isInitialCameraReady) {
            if (hasZoomedToUser) return@LaunchedEffect
            val map = mapLibreMap ?: return@LaunchedEffect
            if (!isInitialCameraReady) return@LaunchedEffect
            val target = myLocation ?: LatLng(ISTANBUL_LAT, ISTANBUL_LON)
            hasZoomedToUser = true
            map.cameraPosition = CameraPosition.Builder()
                .target(target)
                .zoom(if (myLocation != null) 15.0 else 12.0)
                .build()
        }

        LaunchedEffect(uiState.filteredVehicles, mapStyle, symbolManager, cameraZoom, clusterColor) {
            val style = mapStyle ?: return@LaunchedEffect
            val manager = symbolManager ?: return@LaunchedEffect
            manager.deleteAll()

            // Mesafeye göre kümeleme (remote): yakın araçlar tek balonda toplanır.
            val clusters = clusterVehicles(uiState.filteredVehicles, cameraZoom)

            clusters.forEach { cluster ->
                if (cluster.vehicles.size == 1) {
                    val vehicle = cluster.vehicles.first()
                    val available = vehicle.vehicleStatus == VehicleStatus.AVAILABLE
                    // Marker balonu: varsa dakikalık fiyat, yoksa günlük.
                    val priceText = vehicle.pricePerMinute
                        ?.let { context.getString(R.string.common_price_per_minute, formatTl(it)) }
                        ?: context.getString(R.string.common_amount_tl, formatTl(vehicle.pricePerDay))
                    // status'ü id'ye katarak müsait->meşgul geçişinde marker görselini tazeleriz.
                    val imageId = "vehicle-marker-${vehicle.id}-${if (available) "a" else "b"}"
                    style.addImage(
                        imageId,
                        MarkerBitmapFactory.create(
                            text = priceText,
                            backgroundColor = VehicleType.colorFor(vehicle.type).toArgb(),
                            density = density,
                            // Meşgul (RENTED/RESERVED) araç gri gösterilir.
                            disabled = !available,
                        ),
                    )
                    manager.create(
                        SymbolOptions()
                            .withLatLng(LatLng(vehicle.latitude, vehicle.longitude))
                            .withIconImage(imageId)
                            .withIconAnchor("bottom")
                            .withData(JsonPrimitive(vehicle.id)),
                    )
                } else {
                    val imageId = "vehicle-cluster-${cluster.vehicles.size}"
                    style.addImage(
                        imageId,
                        MarkerBitmapFactory.createClusterBubble(
                            count = cluster.vehicles.size,
                            backgroundColor = clusterColor,
                            density = density,
                        ),
                    )
                    manager.create(
                        SymbolOptions()
                            .withLatLng(LatLng(cluster.latitude, cluster.longitude))
                            .withIconImage(imageId)
                            .withIconAnchor("bottom")
                            .withData(
                                JsonPrimitive(cluster.vehicles.joinToString(",") { it.id }),
                            ),
                    )
                }
            }

            if (!hasCenteredOnVehicles && uiState.filteredVehicles.isNotEmpty()) {
                hasCenteredOnVehicles = true
                val avgLat = uiState.filteredVehicles.map { it.latitude }.average()
                val avgLon = uiState.filteredVehicles.map { it.longitude }.average()
                mapLibreMap?.easeCamera(CameraUpdateFactory.newLatLngZoom(LatLng(avgLat, avgLon), 12.5), 600)
            }
        }

        DisposableEffect(symbolManager) {
            val manager = symbolManager
            if (manager == null) {
                onDispose {}
            } else {
                val listener = OnSymbolClickListener { symbol: Symbol ->
                    val data = symbol.data?.asString
                    val ids = data?.split(",").orEmpty()
                    if (ids.size == 1) {
                        val vehicle = viewModel.uiState.value.vehicles.find { it.id == ids.first() }
                        if (vehicle != null) {
                            selectedVehicle = vehicle
                        }
                    } else if (ids.size > 1) {
                        val clusterVehicles = viewModel.uiState.value.vehicles.filter { it.id in ids }
                        focusCameraOnVehicles(mapLibreMap, clusterVehicles)
                    }
                    true
                }
                manager.addClickListener(listener)
                onDispose { manager.removeClickListener(listener) }
            }
        }

        if (!isInitialCameraReady || (uiState.isLoading && uiState.vehicles.isEmpty())) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp),
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                results = uiState.searchResults,
                isSearching = uiState.isSearching,
                onResultClick = { result ->
                    viewModel.onSearchResultSelected(result)
                    mapLibreMap?.easeCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(result.latitude, result.longitude), 13.0),
                        800,
                    )
                },
            )

            when (val banner = uiState.banner) {
                is MapBanner.ActiveRental -> {
                    Spacer(modifier = Modifier.height(Dimens.SpaceS))
                    StateBanner(
                        title = stringResource(R.string.common_rental_active) +
                            (banner.currentCost?.let {
                                stringResource(R.string.map_banner_rental_active_cost_suffix, formatTl(it))
                            } ?: ""),
                        subtitle = banner.vehicleName,
                        dotColor = Success,
                        actionLabel = stringResource(R.string.map_banner_rental_active_action),
                        actionColor = Success,
                        onClick = { onNavigateToActiveRental(banner.rentalId) },
                    )
                }

                is MapBanner.PreparingRental -> {
                    Spacer(modifier = Modifier.height(Dimens.SpaceS))
                    StateBanner(
                        title = stringResource(R.string.map_banner_preparing_title),
                        subtitle = banner.vehicleName,
                        dotColor = PrimaryVariant,
                        actionLabel = stringResource(R.string.map_banner_preparing_action),
                        actionColor = PrimaryVariant,
                        onClick = { onNavigateToHandover(banner.rentalId) },
                    )
                }

                is MapBanner.ActiveReservation -> {
                    Spacer(modifier = Modifier.height(Dimens.SpaceS))
                    StateBanner(
                        title = stringResource(R.string.map_banner_reservation_title, formatMmSs(banner.remainingSeconds)),
                        subtitle = banner.vehicleName,
                        dotColor = PrimaryVariant,
                        actionLabel = stringResource(R.string.map_banner_reservation_action),
                        actionColor = PrimaryVariant,
                        onClick = { onNavigateToReservation(banner.vehicleId) },
                    )
                }

                null -> Unit
            }

            val errorRes = uiState.error
            if (errorRes != null) {
                Spacer(modifier = Modifier.height(Dimens.SpaceS))
                ErrorBanner(message = stringResource(errorRes), onRetry = viewModel::loadVehicles)
            }
        }

        IconButton(
            onClick = {
                val map = mapLibreMap
                val location = myLocation
                if (map != null && location != null) {
                    map.easeCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0), 600)
                } else {
                    scope.launch { snackbarHostState.showSnackbar(locationNotFoundMessage) }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = bottomCardHeight + 16.dp, end = 20.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(Surface),
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.map_my_location_content_description), tint = Primary)
        }

        BottomVehiclesCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { coordinates ->
                    bottomCardHeight = with(localDensity) { coordinates.size.height.toDp() }
                },
            uiState = uiState,
            onSegmentSelected = { segment -> viewModel.onSegmentSelected(segment) },
            onTypeSelected = { type ->
                viewModel.onTypeSelected(type)
                focusCameraOnVehicles(mapLibreMap, viewModel.uiState.value.filteredVehicles)
            },
            onFindNearestClick = {
                val location = myLocation
                if (location == null) {
                    scope.launch { snackbarHostState.showSnackbar(locationNotFoundCheckPermissionMessage) }
                } else {
                    val nearest = viewModel.findNearestVehicle(location.latitude, location.longitude)
                    if (nearest == null) {
                        scope.launch { snackbarHostState.showSnackbar(noVehicleFoundMessage) }
                    } else {
                        mapLibreMap?.easeCamera(
                            CameraUpdateFactory.newLatLngZoom(LatLng(nearest.latitude, nearest.longitude), 15.0),
                            700,
                        )
                    }
                }
            },
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomCardHeight + 40.dp),
        ) { data ->
            Snackbar(snackbarData = data)
        }

        val vehicle = selectedVehicle
        if (vehicle != null) {
            val distanceLabel = myLocation?.let {
                formatDistanceMeters(haversineMeters(it.latitude, it.longitude, vehicle.latitude, vehicle.longitude))
            }
            
            val hasActiveRental = uiState.activeRental != null
            val isVehicleAvailable = vehicle.vehicleStatus == VehicleStatus.AVAILABLE

            VehicleDetailSheet(
                vehicle = vehicle,
                distanceLabel = distanceLabel,
                canReserve = isVehicleAvailable && !hasActiveRental,
                sheetState = sheetState,
                onDismiss = { selectedVehicle = null },
                onReserve = {
                    selectedVehicle = null
                    onNavigateToReservation(vehicle.id)
                },
            )
        }
    }
}

private fun loadMapStyle(map: MapLibreMap, isDarkTheme: Boolean, onLoaded: (Style) -> Unit) {
    val styleJson = if (isDarkTheme) MapStyles.CARTO_DARK else MapStyles.OSM_LIGHT
    map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
        style.addSource(GeoJsonSource(ME_SOURCE_ID))
        style.addLayer(
            CircleLayer("me-halo-layer", ME_SOURCE_ID).withProperties(
                PropertyFactory.circleColor(ME_MARKER_COLOR),
                PropertyFactory.circleRadius(20f),
                PropertyFactory.circleOpacity(0.2f),
                PropertyFactory.circleBlur(0.4f),
            ),
        )
        style.addLayer(
            CircleLayer("me-layer", ME_SOURCE_ID).withProperties(
                PropertyFactory.circleColor(ME_MARKER_COLOR),
                PropertyFactory.circleRadius(9f),
                PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                PropertyFactory.circleStrokeWidth(3f),
            ),
        )
        onLoaded(style)
    }
}

private data class VehicleCluster(val latitude: Double, val longitude: Double, val vehicles: List<VehicleDto>)

/**
 * Greedy mesafe tabanlı gruplama: bir aracı, kendisine en yakın ve halihazırda büyümekte olan
 * bir cluster'ın merkezine belirli bir eşik (zoom'a göre küçülen derece yarıçapı) içindeyse o
 * cluster'a ekler; değilse yeni bir cluster başlatır. Sabit ızgara sınırlarının aksine yakın
 * araçları rastgele bölmez.
 */
private fun clusterVehicles(vehicles: List<VehicleDto>, zoom: Double): List<VehicleCluster> {
    if (zoom >= CLUSTER_MAX_ZOOM || vehicles.isEmpty()) {
        return vehicles.map { VehicleCluster(it.latitude, it.longitude, listOf(it)) }
    }

    val thresholdDegrees = CLUSTER_RADIUS_DEGREES_AT_ZOOM0 / 2.0.pow(zoom)

    data class MutableCluster(var sumLat: Double, var sumLon: Double, val members: MutableList<VehicleDto>) {
        val centerLat get() = sumLat / members.size
        val centerLon get() = sumLon / members.size
    }

    val clusters = mutableListOf<MutableCluster>()
    vehicles.forEach { vehicle ->
        val nearest = clusters.minByOrNull { cluster ->
            val dLat = cluster.centerLat - vehicle.latitude
            val dLon = cluster.centerLon - vehicle.longitude
            dLat * dLat + dLon * dLon
        }
        val isWithinThreshold = nearest != null &&
            run {
                val dLat = nearest.centerLat - vehicle.latitude
                val dLon = nearest.centerLon - vehicle.longitude
                (dLat * dLat + dLon * dLon) <= thresholdDegrees * thresholdDegrees
            }
        if (nearest != null && isWithinThreshold) {
            nearest.sumLat += vehicle.latitude
            nearest.sumLon += vehicle.longitude
            nearest.members.add(vehicle)
        } else {
            clusters.add(MutableCluster(vehicle.latitude, vehicle.longitude, mutableListOf(vehicle)))
        }
    }

    // İkinci geçiş: sıralamaya bağlı olarak ayrı kalmış ama birbirine çok yakın cluster'ları birleştir.
    var merged = true
    while (merged) {
        merged = false
        outer@ for (i in clusters.indices) {
            for (j in clusters.indices) {
                if (i == j) continue
                val a = clusters[i]
                val b = clusters[j]
                val dLat = a.centerLat - b.centerLat
                val dLon = a.centerLon - b.centerLon
                if (dLat * dLat + dLon * dLon <= thresholdDegrees * thresholdDegrees) {
                    a.sumLat += b.sumLat
                    a.sumLon += b.sumLon
                    a.members.addAll(b.members)
                    clusters.removeAt(j)
                    merged = true
                    break@outer
                }
            }
        }
    }

    return clusters.map { VehicleCluster(it.centerLat, it.centerLon, it.members) }
}

private fun focusCameraOnVehicles(map: MapLibreMap?, vehicles: List<VehicleDto>) {
    if (map == null || vehicles.isEmpty()) return
    if (vehicles.size == 1) {
        val vehicle = vehicles.first()
        map.easeCamera(CameraUpdateFactory.newLatLngZoom(LatLng(vehicle.latitude, vehicle.longitude), 15.0), 700)
        return
    }
    val bounds = LatLngBounds.fromLatLngs(vehicles.map { LatLng(it.latitude, it.longitude) })
    map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120), 700)
}

@Composable
private fun formatDistanceMeters(meters: Double): String = if (meters < 1000) {
    stringResource(R.string.map_distance_meters, meters.toInt())
} else {
    stringResource(R.string.common_distance_km, "%.1f".format(meters / 1000))
}

private fun updateMeMarker(style: Style, myLocation: LatLng?) {
    val source = style.getSourceAs<GeoJsonSource>(ME_SOURCE_ID) ?: return
    if (myLocation == null) {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
    } else {
        source.setGeoJson(Point.fromLngLat(myLocation.longitude, myLocation.latitude))
    }
}

@SuppressLint("MissingPermission")
private fun startLocationUpdates(fusedClient: FusedLocationProviderClient, callback: LocationCallback) {
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
        .setMinUpdateIntervalMillis(2_000L)
        .build()

    fusedClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            callback.onLocationResult(LocationResult.create(listOf(location)))
        }
    }
    fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<GeocodingResult>,
    isSearching: Boolean,
    onResultClick: (GeocodingResult) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Surface,
            shadowElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(text = stringResource(R.string.map_search_placeholder), fontSize = 15.sp, color = TextSecondary)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = TextPrimary),
                        cursorBrush = SolidColor(Primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Primary)
                } else if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.map_search_clear_content_description),
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp).clickable { onQueryChange("") },
                    )
                }
            }
        }

        if (results.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Surface,
                shadowElevation = 3.dp,
            ) {
                Column {
                    results.forEachIndexed { index, result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onResultClick(result) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NearMe,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = result.displayName,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                        if (index != results.lastIndex) {
                            HorizontalDivider(color = Background, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

// Harita üstündeki durum banner'ı (aktif kiralama / hazırlıktaki kiralama / aktif rezervasyon)
// aynı görsel dilde; yalnız metin, nokta rengi ve aksiyon değişir.
@Composable
private fun StateBanner(
    title: String,
    subtitle: String?,
    dotColor: Color,
    actionLabel: String,
    actionColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TextPrimary)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Background,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Background.copy(alpha = 0.7f),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = actionLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = actionColor,
        )
    }
}

private fun formatMmSs(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = message, fontSize = 13.sp, color = Danger, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Tekrar dene",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary,
                modifier = Modifier.clickable(onClick = onRetry),
            )
        }
    }
}

@Composable
private fun BottomVehiclesCard(
    modifier: Modifier = Modifier,
    uiState: MapUiState,
    onSegmentSelected: (String?) -> Unit,
    onTypeSelected: (String?) -> Unit,
    onFindNearestClick: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Surface,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(Dimens.SpaceL)) {
            Text(
                text = stringResource(R.string.map_nearby_vehicle_count, uiState.availableFilteredVehicles.size),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(Dimens.SpaceXxs))
            Text(
                text = stringResource(R.string.map_nearby_vehicles_subtitle),
                fontSize = 13.sp,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceS))

            // Fiyat segmenti sekmeleri (sunucu tarafı ?segment filtresi).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            ) {
                SegmentTab(
                    label = stringResource(R.string.map_filter_all),
                    selected = uiState.selectedSegment == null,
                    onClick = { onSegmentSelected(null) },
                )
                VehicleSegment.entries.forEach { segment ->
                    SegmentTab(
                        label = stringResource(segment.label),
                        selected = uiState.selectedSegment == segment.apiValue,
                        onClick = { onSegmentSelected(segment.apiValue) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpaceXs))

            // Karoseri tipi filtresi (istemci tarafı, segmentin üstünde çalışır).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            ) {
                TypeChip(
                    label = stringResource(R.string.map_filter_all),
                    dotColor = Primary,
                    selected = uiState.selectedType == null,
                    onClick = { onTypeSelected(null) },
                )
                uiState.availableTypes.forEach { type ->
                    val typeLabelRes = VehicleType.labelFor(type)
                    TypeChip(
                        label = if (typeLabelRes != null) stringResource(typeLabelRes) else type,
                        dotColor = VehicleType.colorFor(type),
                        selected = uiState.selectedType == type,
                        onClick = { onTypeSelected(type) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpaceM))

            Button(
                onClick = onFindNearestClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(Dimens.CornerL),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Icon(Icons.Outlined.NearMe, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(Dimens.SpaceXs))
                Text(text = stringResource(R.string.map_find_nearest_vehicle_button), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SegmentTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Primary else Background,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun TypeChip(
    label: String,
    dotColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) dotColor.copy(alpha = 0.14f) else Background,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) TextPrimary else TextSecondary,
            )
        }
    }
}
