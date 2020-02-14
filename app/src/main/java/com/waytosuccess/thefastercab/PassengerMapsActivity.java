package com.waytosuccess.thefastercab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class PassengerMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private static final int CHECK_SETTINGS_CODE = 111;
    private static final int REQUEST_LOCATION_PERMISSION = 222;

    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private Button settingsButton, signOutButton, bookTaxiButton;

    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    private DatabaseReference driversGeoFire;
    private DatabaseReference nearestDriverLocation;
    private int searchRadius = 1;
    private boolean isDriverFound = false;
    private String nearestDriverId;
    private Marker driverMarker;

    private boolean isLocationUpdatesActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        settingsButton = findViewById(R.id.settingsButton);
        signOutButton = findViewById(R.id.signOutButton);
        bookTaxiButton = findViewById(R.id.bookTaxiButton);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        driversGeoFire = FirebaseDatabase.getInstance().getReference()
                .child("driversGeoFire");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        buildLocationRequest();
        buildLocationCallback();
        buildLocationSettingsRequest();

        startLocationUpdates();

        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                signOutPassenger();
            }
        });

        bookTaxiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookTaxiButton.setText("Getting your taxi...");
                gettingNearestTaxi();
            }
        });
    }

    private void gettingNearestTaxi() {
        GeoFire geoFire = new GeoFire(driversGeoFire);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(
                currentLocation.getLatitude(),
                currentLocation.getLongitude()
        ), searchRadius);

        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (!isDriverFound) {
                    isDriverFound = true;
                    nearestDriverId = key;

                    getNearestDriverLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {}

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                if (!isDriverFound) {

                    searchRadius++;
                    gettingNearestTaxi();

                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void getNearestDriverLocation() {

        bookTaxiButton.setText("Getting your driver location...");

        nearestDriverLocation = FirebaseDatabase.getInstance().getReference()
                .child("passengerGeoFire").child(nearestDriverId).child("l");

        nearestDriverLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    List<Object> driverLocationParameters =
                            (List<Object>) dataSnapshot.getValue();

                    double latitude = 0;
                    double longitude = 0;

                    if (driverLocationParameters.get(0) != null) {

                        latitude = Double.parseDouble(
                                driverLocationParameters.get(0).toString()
                        );
                    }

                    if (driverLocationParameters.get(1) != null) {

                        longitude = Double.parseDouble(
                                driverLocationParameters.get(1).toString()
                        );
                    }

                    LatLng driverLatLng = new LatLng(latitude, longitude);

                    if (driverMarker != null) {
                        driverMarker.remove();
                    }

                    Location driverLocation = new Location("");
                    driverLocation.setLatitude(latitude);
                    driverLocation.setLongitude(longitude);

                    float distanceToDriver =
                            driverLocation.distanceTo(currentLocation);

                    bookTaxiButton.setText("Distance to driver: " +
                            distanceToDriver);

                    driverMarker = mMap.addMarker(
                            new MarkerOptions().position(driverLatLng)
                                    .title("Your driver is here")
                    );
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void signOutPassenger() {
        String passengerUserId = currentUser.getUid();
        DatabaseReference passengers = FirebaseDatabase.getInstance()
                .getReference()
                .child("passengers");

        GeoFire geoFire = new GeoFire(passengers);
        geoFire.removeLocation(passengerUserId);

        Intent intent = new Intent(PassengerMapsActivity.this,
                ChooseModeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        if (currentLocation != null) {
            LatLng passengerCurLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(passengerCurLocation).title("Passenger is here"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerCurLocation));
        }
    }

    private void stopLocationUpdates() {
        if (!isLocationUpdatesActive) {
            return;
        }

        fusedLocationClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        isLocationUpdatesActive = false;
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void startLocationUpdates() {

        isLocationUpdatesActive = true;

        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        if (ActivityCompat.checkSelfPermission(
                                PassengerMapsActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION) !=
                                PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat
                                        .checkSelfPermission(PassengerMapsActivity.this,
                                                Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                        PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.myLooper()
                        );
                        updateLocationUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        int statusCod = ((ApiException) e).getStatusCode();

                        switch (statusCod) {
                            case LocationSettingsStatusCodes
                                    .RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException resolvableApiException =
                                            (ResolvableApiException) e;
                                    resolvableApiException.startResolutionForResult(
                                            PassengerMapsActivity.this,
                                            CHECK_SETTINGS_CODE
                                    );
                                } catch (IntentSender.SendIntentException ex) {
                                    ex.printStackTrace();
                                }
                                break;
                            case LocationSettingsStatusCodes
                                    .SETTINGS_CHANGE_UNAVAILABLE:
                                String message = "Adjust location settings on your device";
                                Toast.makeText(PassengerMapsActivity.this, "" + message, Toast.LENGTH_LONG).show();

                                isLocationUpdatesActive = false;
                        }
                        updateLocationUI();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case CHECK_SETTINGS_CODE:
                switch (resultCode) {

                    case Activity.RESULT_OK:
                        Log.d("SettingsLocationLog", "User was agree to change " +
                                "location settings");
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.d("SettingsLocationLog", "User wasn't agreed to change " +
                                "location settings");
                        isLocationUpdatesActive = false;
                        updateLocationUI();
                        break;
                }
                break;
        }
    }

    private void buildLocationSettingsRequest() {

        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();

                updateLocationUI();
            }
        };
    }

    private void updateLocationUI() {
        if (currentLocation != null) {

            LatLng currentLarLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(currentLarLng).title("Passenger is here"));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLarLng));

            String passengerUserId = currentUser.getUid();
            DatabaseReference passengersGeoFire = FirebaseDatabase.getInstance().getReference()
                    .child("passengersGeoFire");
//
//            DatabaseReference passengers = FirebaseDatabase.getInstance().getReference()
//                    .child("passengers");
//            passengers.setValue(true);

            GeoFire geoFire = new GeoFire(passengersGeoFire);
            geoFire.setLocation(passengerUserId, new GeoLocation(currentLocation.getLatitude(),
                    currentLocation.getLongitude()));
        }
    }

    private void buildLocationRequest() {

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isLocationUpdatesActive && checkLocationPermission()) {
            startLocationUpdates();
        } else if (!checkLocationPermission()) {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        );
        if (shouldProvideRationale) {
            showSnackBar(
                    "Location permission is needed for App functionality",
                    "OK",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(
                                    PassengerMapsActivity.this,
                                    new String[]{
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                    },
                                    REQUEST_LOCATION_PERMISSION
                            );
                        }
                    }
            );
        } else {

            ActivityCompat.requestPermissions(
                    PassengerMapsActivity.this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION
            );
        }
    }

    private void showSnackBar(final String mainText, final String action, View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                mainText, Snackbar.LENGTH_INDEFINITE)
                .setAction(
                        action,
                        listener
                ).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length <= 0) {
                Log.d("ReqPermissionsResult", "Request was canceled");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationUpdatesActive) {
                    startLocationUpdates();
                }
            } else {
                showSnackBar(
                        "Turn on location on settings",
                        "Settings",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        }
                );
            }
        }
    }

    private boolean checkLocationPermission() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
}
