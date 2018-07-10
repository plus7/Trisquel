package net.tnose.app.trisquel;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    final int RETCODE_LOC_PERM = 100;
    final static int RESULT_DELETE = 100;
    final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private GoogleMap mMap;
    private Marker mMarker;
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mFusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        Button b = findViewById(R.id.btnOk);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultData = new Intent();
                if(mMap != null && mMarker != null) {
                    resultData.putExtra("latitude",  mMarker.getPosition().latitude);
                    resultData.putExtra("longitude", mMarker.getPosition().longitude);

                    if (Geocoder.isPresent()) {
                        Geocoder coder = new Geocoder(MapsActivity.this); //ブロックするっぽいので本当は良くない
                        List<Address> addresses;
                        try {
                            addresses = coder.getFromLocation(
                                    mMarker.getPosition().latitude,
                                    mMarker.getPosition().longitude,
                                    1);
                        } catch (IOException e) {
                            addresses = new ArrayList<Address>();
                        }
                        if (addresses.size() == 1) {
                            Address a = addresses.get(0);
                            String featureName = Normalizer.normalize(a.getFeatureName(), Normalizer.Form.NFKC);
                            String address = Normalizer.normalize(a.getAddressLine(0), Normalizer.Form.NFKC);
                            Log.d("getAddressLine", address);
                            Log.d("getFeatureName", a.getFeatureName());
                            Log.d("getAdminArea", a.getAdminArea());
                            if(a.getLocality() != null)     Log.d("getLocality", a.getLocality());
                            if(a.getSubAdminArea() != null) Log.d("getSubAdminArea", a.getSubAdminArea());
                            if(a.getSubLocality() != null)  Log.d("getSubLocality", a.getSubLocality());
                            if(a.getThoroughfare() != null) Log.d("getThoroughfare", a.getThoroughfare());
                            Log.d("getMaxAddressLineIndex", Integer.toString(a.getMaxAddressLineIndex()));
                            if(featureName.matches("^[\\d\\-−－]++$")){
                                // U+002DとU+2212とU+2015。
                                // このAPI＋Normalizerの組み合わせだと2212が返るみたいだが
                                // 警戒してほかにも来そうな奴らをregexに入れてある
                                if(address.matches("^(日本、).+")){ //他国にもこういう余計なものがつくんだろうか？
                                    resultData.putExtra("location", address.replaceFirst("日本、", ""));
                                }else {
                                    resultData.putExtra("location", address);
                                }
                            }else{
                                resultData.putExtra("location", featureName);
                            }
                        }
                    }
                }
                setResult(RESULT_OK, resultData);
                finish();
            }
        });
        Button cb = findViewById(R.id.btnCancel);
        cb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });
        Button del = findViewById(R.id.btnDelete);
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_DELETE, null);
                finish();
            }
        });
    }

    private void setMyLocationEnabledAndGetLastLocation() {
        if(mMap == null) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, RETCODE_LOC_PERM);
            return;
        }
        mMap.setMyLocationEnabled(true);
        Intent data = getIntent();
        double lat, lng;
        lat = data.getDoubleExtra("latitude", 999);
        lng = data.getDoubleExtra("longitude", 999);
        if(lat != 999 && lng != 999) {
            LatLng latlng = new LatLng(lat, lng);
            mMap.moveCamera(CameraUpdateFactory.zoomTo(17));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
            if(mMarker != null) mMarker.remove();
            mMarker = mMap.addMarker(new MarkerOptions().position(latlng));
        } else {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.moveCamera(CameraUpdateFactory.zoomTo(17));
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
                                if (mMarker != null) mMarker.remove();
                                mMarker = mMap.addMarker(new MarkerOptions().position(latlng));
                            }
                        }
                    });
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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setMyLocationEnabledAndGetLastLocation();
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(mMarker != null) mMarker.remove();
                mMarker = mMap.addMarker(new MarkerOptions().position(latLng));
            }
        });
        //mMap.setOn
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RETCODE_LOC_PERM) {
            onRequestLocationPermissionsResult(permissions, grantResults);
        }
    }

    void onRequestLocationPermissionsResult(String[] permissions, int[] grantResults) {
        int[] granted2 = {PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};
        if (Arrays.equals(permissions, PERMISSIONS) && Arrays.equals(grantResults, granted2)) {
            setMyLocationEnabledAndGetLastLocation();
        } else {
            Toast.makeText(this, getString(R.string.error_permission_location), Toast.LENGTH_LONG).show();
        }
    }


}
