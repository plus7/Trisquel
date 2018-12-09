package net.tnose.app.trisquel

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.text.Normalizer
import java.util.*

class MapsActivity : FragmentActivity(), OnMapReadyCallback {
    internal val RETCODE_LOC_PERM = 100
    internal val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    private var mMap: GoogleMap? = null
    private var mMarker: Marker? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mSettingsClient: SettingsClient? = null
    private val locationSettingsRequest: LocationSettingsRequest? = null
    private val locationCallback: LocationCallback? = null
    private val locationRequest: LocationRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)

        val b = findViewById<Button>(R.id.btnOk)
        b.setOnClickListener {
            val resultData = Intent()
            if (mMap != null && mMarker != null) {
                resultData.putExtra("latitude", mMarker!!.position.latitude)
                resultData.putExtra("longitude", mMarker!!.position.longitude)

                if (Geocoder.isPresent()) {
                    val coder = Geocoder(this@MapsActivity) //ブロックするっぽいので本当は良くない
                    var addresses: List<Address>
                    try {
                        addresses = coder.getFromLocation(
                                mMarker!!.position.latitude,
                                mMarker!!.position.longitude,
                                1)
                    } catch (e: IOException) {
                        addresses = ArrayList()
                    }

                    if (addresses.size == 1) {
                        val a = addresses[0]
                        val featureName = Normalizer.normalize(a.featureName?: "", Normalizer.Form.NFKC)
                        val address = Normalizer.normalize(a.getAddressLine(0)?: "", Normalizer.Form.NFKC)
                        Log.d("getAddressLine", address)
                        if (a.featureName != null) Log.d("getFeatureName", a.featureName)
                        if (a.adminArea != null) Log.d("getAdminArea", a.adminArea)
                        if (a.locality != null) Log.d("getLocality", a.locality)
                        if (a.subAdminArea != null) Log.d("getSubAdminArea", a.subAdminArea)
                        if (a.subLocality != null) Log.d("getSubLocality", a.subLocality)
                        if (a.thoroughfare != null) Log.d("getThoroughfare", a.thoroughfare)
                        Log.d("getMaxAddressLineIndex", Integer.toString(a.maxAddressLineIndex))
                        if (featureName.matches("^[\\d\\-−－]++$".toRegex()) || featureName.isEmpty()) {
                            // U+002DとU+2212とU+2015。
                            // このAPI＋Normalizerの組み合わせだと2212が返るみたいだが
                            // 警戒してほかにも来そうな奴らをregexに入れてある
                            if (address.matches("^(日本、).+".toRegex())) { //他国にもこういう余計なものがつくんだろうか？
                                resultData.putExtra("location", address.replaceFirst("日本、".toRegex(), ""))
                            } else {
                                resultData.putExtra("location", address)
                            }
                        } else {
                            resultData.putExtra("location", featureName)
                        }
                    }
                }
            }
            setResult(Activity.RESULT_OK, resultData)
            finish()
        }
        val cb = findViewById<Button>(R.id.btnCancel)
        cb.setOnClickListener {
            setResult(Activity.RESULT_CANCELED, Intent())
            finish()
        }
        val del = findViewById<Button>(R.id.btnDelete)
        del.setOnClickListener {
            setResult(RESULT_DELETE, Intent())
            finish()
        }
    }

    private fun setMyLocationEnabledAndGetLastLocation() {
        if (mMap == null) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, RETCODE_LOC_PERM)
            return
        }
        mMap!!.isMyLocationEnabled = true
        val data = intent
        val lat: Double
        val lng: Double
        lat = data.getDoubleExtra("latitude", 999.0)
        lng = data.getDoubleExtra("longitude", 999.0)
        if (lat != 999.0 && lng != 999.0) {
            val latlng = LatLng(lat, lng)
            mMap!!.moveCamera(CameraUpdateFactory.zoomTo(17f))
            mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latlng))
            if (mMarker != null) mMarker!!.remove()
            mMarker = mMap!!.addMarker(MarkerOptions().position(latlng))
        } else {
            mFusedLocationClient!!.lastLocation
                    .addOnSuccessListener(this) { location ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            val latlng = LatLng(location.latitude, location.longitude)
                            mMap!!.moveCamera(CameraUpdateFactory.zoomTo(17f))
                            mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latlng))
                            if (mMarker != null) mMarker!!.remove()
                            mMarker = mMap!!.addMarker(MarkerOptions().position(latlng))
                        }
                    }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setMyLocationEnabledAndGetLastLocation()
        mMap!!.setOnMapClickListener { latLng ->
            if (mMarker != null) mMarker!!.remove()
            mMarker = mMap!!.addMarker(MarkerOptions().position(latLng))
        }
        //mMap.setOn
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RETCODE_LOC_PERM) {
            onRequestLocationPermissionsResult(permissions, grantResults)
        }
    }

    internal fun onRequestLocationPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        val granted2 = intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED)
        if (Arrays.equals(permissions, PERMISSIONS) && Arrays.equals(grantResults, granted2)) {
            setMyLocationEnabledAndGetLastLocation()
        } else {
            Toast.makeText(this, getString(R.string.error_permission_location), Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        internal val RESULT_DELETE = 100
    }


}
