package com.flowbytestudio.rencar.ui.screens.activerental

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.ui.common.MapStyles
import com.flowbytestudio.rencar.ui.common.formatTl
import com.flowbytestudio.rencar.ui.theme.Background
import com.flowbytestudio.rencar.ui.theme.BgLight
import com.flowbytestudio.rencar.ui.theme.Danger
import com.flowbytestudio.rencar.ui.theme.Dimens
import com.flowbytestudio.rencar.ui.theme.Primary
import com.flowbytestudio.rencar.ui.theme.Success
import com.flowbytestudio.rencar.ui.theme.Surface
import com.flowbytestudio.rencar.ui.theme.TextPrimary
import com.flowbytestudio.rencar.ui.theme.TextSecondary
import java.util.Locale
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import com.flowbytestudio.rencar.ui.screens.map.MarkerBitmapFactory
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point

private const val VEHICLE_SOURCE_ID = "active-rental-vehicle"
private const val VEHICLE_ICON_ID = "active-rental-vehicle-icon"

@Composable
fun ActiveRentalScreen(
    rentalId: String,
    onEnded: () -> Unit,
) {
    val viewModel: ActiveRentalViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ActiveRentalViewModel(rentalId) }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // finish başarılı / 404 (başka yerden bitmiş) / baştan COMPLETED → TripSummary'ye devret.
    LaunchedEffect(uiState.ended) {
        if (uiState.ended) onEnded()
    }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapStyle by remember { mutableStateOf<Style?>(null) }
    val density = LocalDensity.current.density

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapLibre.getInstance(ctx)
                MapView(ctx).apply {
                    onCreate(null)
                    getMapAsync { map ->
                        mapLibreMap = map
                        map.setStyle(Style.Builder().fromJson(MapStyles.OSM_LIGHT)) { style ->
                            style.addSource(GeoJsonSource(VEHICLE_SOURCE_ID))
                            style.addLayer(
                                CircleLayer("vehicle-halo-layer", VEHICLE_SOURCE_ID).withProperties(
                                    PropertyFactory.circleColor(android.graphics.Color.parseColor("#0B6BCB")),
                                    PropertyFactory.circleRadius(22f),
                                    PropertyFactory.circleOpacity(0.2f),
                                    PropertyFactory.circleBlur(0.4f),
                                ),
                            )
                            style.addImage(
                                VEHICLE_ICON_ID,
                                MarkerBitmapFactory.createVehicleIcon(
                                    backgroundColor = android.graphics.Color.parseColor("#0B6BCB"),
                                    density = density,
                                ),
                            )
                            style.addLayer(
                                SymbolLayer("vehicle-layer", VEHICLE_SOURCE_ID).withProperties(
                                    PropertyFactory.iconImage(VEHICLE_ICON_ID),
                                    PropertyFactory.iconAllowOverlap(true),
                                    PropertyFactory.iconIgnorePlacement(true),
                                ),
                            )
                            mapStyle = style
                        }
                    }
                    mapView = this
                }
            },
        )

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

        // İlk konumda 15x yakınlaşılır; sonraki canlı güncellemelerde yalnız marker
        // taşınıp kamera yumuşakça takip eder (her karede yeniden zoom yapılmaz).
        var hasInitialFix by remember { mutableStateOf(false) }
        // Canlı soket konumu varsa onu, yoksa REST'ten gelen araç konumunu kullan.
        val markerLat = uiState.livePosition?.latitude ?: uiState.vehicle?.latitude
        val markerLng = uiState.livePosition?.longitude ?: uiState.vehicle?.longitude
        LaunchedEffect(mapLibreMap, mapStyle, markerLat, markerLng) {
            val map = mapLibreMap ?: return@LaunchedEffect
            val style = mapStyle ?: return@LaunchedEffect
            val lat = markerLat ?: return@LaunchedEffect
            val lng = markerLng ?: return@LaunchedEffect
            style.getSourceAs<GeoJsonSource>(VEHICLE_SOURCE_ID)
                ?.setGeoJson(Point.fromLngLat(lng, lat))
            if (!hasInitialFix) {
                hasInitialFix = true
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15.0),
                )
            } else {
                map.easeCamera(CameraUpdateFactory.newLatLng(LatLng(lat, lng)))
            }
        }

        ActiveRentalPill(
            // Araç bilgisi rental.vehicle özetinden gelir (ayrı VehicleDto yalnız harita içindir).
            vehicleName = uiState.rental?.vehicle?.let { "${it.brand} ${it.model}" }
                ?: uiState.vehicle?.let { "${it.brand} ${it.model}" },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = Dimens.SpaceS),
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }

        ActiveRentalCard(
            uiState = uiState,
            onEnd = viewModel::endRental,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ActiveRentalPill(
    vehicleName: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(TextPrimary)
            .padding(horizontal = Dimens.SpaceS, vertical = Dimens.SpaceXs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Success),
            )
            Spacer(modifier = Modifier.width(Dimens.SpaceXs))
            Text(
                text = if (vehicleName != null) {
                    stringResource(R.string.active_rental_pill_active_with_vehicle, vehicleName)
                } else {
                    stringResource(R.string.common_rental_active)
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Background,
            )
        }
    }
}

@Composable
private fun ActiveRentalCard(
    uiState: ActiveRentalUiState,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.SpaceS),
        shape = RoundedCornerShape(Dimens.SpaceXl),
        color = Surface,
        shadowElevation = 16.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.SpaceL, vertical = Dimens.SpaceM),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BgLight),
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceS))

            if (uiState.isPreparing) {
                // PREPARING: süre işlemiyor; foto akışı tamamlanıp start çağrılmalı.
                Text(
                    text = stringResource(R.string.active_rental_preparing_notice),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(Dimens.SpaceS))
            }

            Text(text = stringResource(R.string.active_rental_elapsed_time_label), fontSize = 13.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(Dimens.SpaceXxs))

            Text(
                text = formatElapsed(uiState.elapsedSeconds),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceS))

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                StatTile(
                    label = stringResource(R.string.active_rental_current_cost_label),
                    value = uiState.currentCost?.let { "₺${formatTl(it)}" } ?: "—",
                    valueColor = Primary,
                    modifier = Modifier.weight(1f),
                )
                // GET /rentals/active — sunucunun biriktirdiği mesafe (km).
                StatTile(
                    label = stringResource(R.string.common_distance_label),
                    value = uiState.distanceKm?.let { formatKm(it) } ?: "—",
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }

            val fatalError = uiState.endError ?: uiState.loadError
            if (fatalError != null) {
                Spacer(modifier = Modifier.height(Dimens.SpaceXs))
                Text(text = stringResource(fatalError), color = Danger, fontSize = 13.sp)
            }

            // Yoklama sırasında oluşan geçici (ölümcül olmayan) bağlantı hatası; ince göster.
            if (uiState.pollError != null) {
                Spacer(modifier = Modifier.height(Dimens.SpaceXxs))
                Text(text = stringResource(uiState.pollError), color = TextSecondary, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(Dimens.SpaceS))

            Button(
                onClick = onEnd,
                enabled = !uiState.isEnding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ControlHeight),
                shape = RoundedCornerShape(Dimens.CornerL),
                colors = ButtonDefaults.buttonColors(containerColor = Danger),
            ) {
                if (uiState.isEnding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = stringResource(R.string.active_rental_end_button), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BgLight)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

private fun formatElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun formatKm(km: Double): String =
    String.format(Locale("tr", "TR"), "%,.1f km", km)
