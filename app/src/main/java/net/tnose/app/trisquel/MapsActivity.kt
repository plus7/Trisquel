package net.tnose.app.trisquel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.Normalizer

class MapsActivity : ComponentActivity() {
    internal val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    
    private var locationPermissionGranted = mutableStateOf(false)

    private val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            locationPermissionGranted.value = true
        } else {
            Toast.makeText(this, getString(R.string.error_permission_location), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || 
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted.value = true
        } else {
            requestLocationPermissionLauncher.launch(PERMISSIONS)
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapScreen(
                        intent = intent,
                        locationPermissionGranted = locationPermissionGranted.value,
                        onResult = { resultCode, data ->
                            setResult(resultCode, data)
                            finish()
                        }
                    )
                }
            }
        }
    }

    companion object {
        internal const val RESULT_DELETE = 100
    }
}

@Composable
fun MapScreen(
    intent: Intent,
    locationPermissionGranted: Boolean,
    onResult: (Int, Intent?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var markerPosition by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(35.6895, 139.6917), 10f)
    }
    
    var mapProperties by remember {
        mutableStateOf(MapProperties(isMyLocationEnabled = locationPermissionGranted))
    }
    
    LaunchedEffect(locationPermissionGranted) {
        mapProperties = mapProperties.copy(isMyLocationEnabled = locationPermissionGranted)
    }

    LaunchedEffect(locationPermissionGranted) {
        val lat = intent.getDoubleExtra("latitude", 999.0)
        val lng = intent.getDoubleExtra("longitude", 999.0)
        
        if (lat != 999.0 && lng != 999.0) {
            val latlng = LatLng(lat, lng)
            markerPosition = latlng
            cameraPositionState.position = CameraPosition.fromLatLngZoom(latlng, 17f)
        } else if (locationPermissionGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latlng = LatLng(location.latitude, location.longitude)
                        markerPosition = latlng
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latlng, 17f)
                    }
                }
            } catch (_: SecurityException) {
                // Ignore
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                onMapClick = { latLng ->
                    markerPosition = latLng
                }
            ) {
                markerPosition?.let {
                    Marker(
                        state = MarkerState(position = it)
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onResult(MapsActivity.RESULT_DELETE, Intent()) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.delete))
            }
            Button(
                onClick = { onResult(Activity.RESULT_CANCELED, Intent()) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(android.R.string.cancel))
            }
            Button(
                onClick = {
                    coroutineScope.launch {
                        val resultData = Intent()
                        markerPosition?.let { pos ->
                            resultData.putExtra("latitude", pos.latitude)
                            resultData.putExtra("longitude", pos.longitude)
                            
                            val locationString = withContext(Dispatchers.IO) {
                                getAddressString(context, pos.latitude, pos.longitude)
                            }
                            if (locationString != null) {
                                resultData.putExtra("location", locationString)
                            }
                        }
                        onResult(Activity.RESULT_OK, resultData)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }
}

fun getAddressString(context: Context, lat: Double, lng: Double): String? {
    if (!Geocoder.isPresent()) return null
    val coder = Geocoder(context)
    var addresses: List<Address> = emptyList()
    try {
        @Suppress("DEPRECATION")
        addresses = coder.getFromLocation(lat, lng, 1) ?: emptyList()
    } catch (_: IOException) {
        // ignore
    }

    if (addresses.isNotEmpty()) {
        val a = addresses[0]
        val featureName = Normalizer.normalize(a.featureName ?: "", Normalizer.Form.NFKC)
        val address = Normalizer.normalize(a.getAddressLine(0) ?: "", Normalizer.Form.NFKC)
        Log.d("getAddressLine", address)
        if (a.featureName != null) Log.d("getFeatureName", a.featureName)
        if (a.adminArea != null) Log.d("getAdminArea", a.adminArea)
        if (a.locality != null) Log.d("getLocality", a.locality)
        if (a.subAdminArea != null) Log.d("getSubAdminArea", a.subAdminArea)
        if (a.subLocality != null) Log.d("getSubLocality", a.subLocality)
        if (a.thoroughfare != null) Log.d("getThoroughfare", a.thoroughfare)
        Log.d("getMaxAddressLineIndex", a.maxAddressLineIndex.toString())
        // U+002DとU+2212とU+2015。
        // このAPI＋Normalizerの組み合わせだと2212が返るみたいだが
        // 警戒してほかにも来そうな奴らをregexに入れてある
        return if (featureName.matches("^[\\d\\-−－]++$".toRegex()) || featureName.isEmpty()) {
            if (address.matches("^(日本、).+".toRegex())) { //他国にもこういう余計なものがつくんだろうか？
                address.replaceFirst("日本、".toRegex(), "")
            } else {
                address
            }
        } else {
            featureName
        }
    }
    return null
}
