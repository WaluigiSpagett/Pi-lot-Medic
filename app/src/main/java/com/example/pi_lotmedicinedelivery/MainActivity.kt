package com.example.pi_lotmedicinedelivery
import org.mapsforge.core.model.LatLong
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.InternalRenderTheme
import org.mapsforge.map.model.Model
import org.mapsforge.map.model.common.PreferencesFacade




class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setContent {
            MedicineDeliveryApp()
        }
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getLastKnownLocation()
        }
    }

    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val geoPoint = LatLong(it.latitude, it.longitude)
                    // Use this geoPoint as the initial location in your GpsSelectionScreen
                }
            }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}

@Composable
fun MedicineDeliveryApp() {
    val navController = rememberNavController()
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "medicineSelection",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Route: Medicine Selection Screen
            composable("medicineSelection") {
                MedicineSelectionScreen { medicine ->
                    navController.navigate("gpsSelection/$medicine")
                }
            }

            // Route: GPS Selection Screen
            composable("gpsSelection/{medicine}") { backStackEntry ->
                val medicine = backStackEntry.arguments?.getString("medicine")
                if (medicine != null) {
                    GpsSelectionScreen(medicine, onLocationConfirmed = { latLong ->
                        navController.navigate("receipt/$medicine/${latLong.latitude}/${latLong.longitude}")
                    })
                }
            }

            // Route: Receipt Screen
            composable("receipt/{medicine}/{lat}/{lng}") { backStackEntry ->
                val medicine = backStackEntry.arguments?.getString("medicine")
                val lat = backStackEntry.arguments?.getString("lat")?.toDouble()
                val lng = backStackEntry.arguments?.getString("lng")?.toDouble()
                if (medicine != null && lat != null && lng != null) {
                    ReceiptScreen(medicine, LatLong(lat, lng))
                }
            }
        }
    }
}

@Composable
fun MedicineSelectionScreen(onMedicineSelected: (String) -> Unit) {
    val medicineList = listOf("Paracetamol", "Ibuprofen", "Amoxicillin", "Cetirizine")

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Select a Medicine", modifier = Modifier.padding(bottom = 16.dp))
        LazyColumn {
            items(medicineList) { medicine ->
                Button(
                    onClick = { onMedicineSelected(medicine) },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(text = medicine)
                }
            }
        }
    }
}
@Composable
fun GpsSelectionScreen(medicine: String, onLocationConfirmed: (LatLong) -> Unit) {
    var selectedLocation by remember { mutableStateOf(LatLong(0.0, 0.0)) }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                MapView(context).apply {
                    // Initialize the map
                    AndroidGraphicFactory.createInstance(context.applicationContext)
                    setClickable(true)
                    setBuiltInZoomControls(true)

                    // Load the map file from assets
                    val mapFile = MapFile.readFrom(context.assets.open("mapfile.map")) // Use readFrom()
                    val tileCache = TileCache() // Initialize TileCache
                    val tileRendererLayer = TileRendererLayer(
                        tileCache, // Tile cache
                        mapFile, // Map file
                        this.model.mapViewPosition, // Map view position
                        false, // Transparent
                        true, // Render labels
                        AndroidGraphicFactory.INSTANCE // Graphics factory
                    )
                    tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.DEFAULT) // Use the default theme
                    this.layerManager.layers.add(tileRendererLayer)

                    // Set the initial map position
                    this.model.mapViewPosition.setCenter(LatLong(0.0, 0.0))
                    this.model.mapViewPosition.setZoomLevel(12.toByte())

                    // Add a marker for the selected location
                    val marker = Marker(
                        selectedLocation,
                        AndroidGraphicFactory.convertResourceToBitmap(context, R.drawable.ic_marker),
                        0,
                        0
                    )
                    this.layerManager.layers.add(marker)

                    // Handle map clicks
                    this.setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            val tapLatLong = this.mapViewPosition.pointToLatLong(event.x, event.y)
                            selectedLocation = tapLatLong
                            marker.latLong = tapLatLong
                            this.mapViewPosition.setCenter(tapLatLong)
                            this.layerManager.redrawLayers()
                            true
                        } else {
                            false
                        }
                    }
                }
            },
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = { onLocationConfirmed(selectedLocation) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "Confirm Location")
        }
    }
}
@Composable
fun ReceiptScreen(medicine: String, location: LatLong) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Medicine: $medicine", modifier = Modifier.padding(bottom = 8.dp))
        Text(text = "Location: ${location.latitude}, ${location.longitude}", modifier = Modifier.padding(bottom = 16.dp))
        Button(onClick = { /* TODO: Handle order confirmation */ }) {
            Text(text = "Confirm Order")
        }
    }
}