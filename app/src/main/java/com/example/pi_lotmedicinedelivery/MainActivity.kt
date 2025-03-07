package com.example.pi_lotmedicinedelivery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings







class MainActivity : ComponentActivity(), OnMapReadyCallback {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MedicineDeliveryApp() // Set your main composable function here
        }

        // Check location permissions
        checkLocationPermissions()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        getCurrentLocation()
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
            composable("medicineSelection") {
                MedicineSelectionScreen { medicine ->
                    navController.navigate("gpsSelection/$medicine")
                }
            }
            composable("gpsSelection/{medicine}") { backStackEntry ->
                val medicine = backStackEntry.arguments?.getString("medicine")
                medicine?.let {
                    GpsSelectionScreen(it) { latLng ->
                        navController.navigate("receipt/$medicine/${latLng.latitude}/${latLng.longitude}")
                    }
                }
            }
            composable("receipt/{medicine}/{lat}/{lng}") { backStackEntry ->
                val medicine = backStackEntry.arguments?.getString("medicine")
                val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
                val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()
                if (medicine != null && lat != null && lng != null) {
                    ReceiptScreen(medicine, LatLng(lat, lng))
                }
            }
        }
    }
}

@Composable
fun MedicineSelectionScreen(onMedicineSelected: (String) -> Unit) {
    val medicines = listOf("Paracetamol", "Ibuprofen", "Amoxicillin", "Cetirizine")

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Select a Medicine", modifier = Modifier.padding(bottom = 16.dp))
        LazyColumn {
            items(medicines) { medicine ->
                Button(onClick = { onMedicineSelected(medicine) }, modifier = Modifier.padding(8.dp)) {
                    Text(text = medicine)
                }
            }
        }
    }
}


@Composable
fun GpsSelectionScreen(medicine: String, onLocationConfirmed: (LatLng) -> Unit) {
    val context = LocalContext.current
    var selectedLocation by remember { mutableStateOf(LatLng(51.5074, -0.1278)) } // Default to London



    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(selectedLocation, 12f)
    }

    // Get last known location
    LaunchedEffect(Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    selectedLocation = latLng

                    // Move and zoom to user location
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.weight(1f),
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(zoomControlsEnabled = true),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                selectedLocation = latLng
            }
        ) {
            Marker(
                state = MarkerState(position = selectedLocation),
                title = "Selected Location"
            )

        }

        Button(
            onClick = { onLocationConfirmed(selectedLocation) },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(text = "Confirm Location")
        }
    }
}


@Composable
fun ReceiptScreen(medicine: String, location: LatLng) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Medicine: $medicine", modifier = Modifier.padding(bottom = 8.dp))
        Text(text = "Location: ${location.latitude}, ${location.longitude}", modifier = Modifier.padding(bottom = 16.dp))
        Button(onClick = { /* TODO: Handle order confirmation */ }) {
            Text(text = "Confirm Order")
        }
    }
}

