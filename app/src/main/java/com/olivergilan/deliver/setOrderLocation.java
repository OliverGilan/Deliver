package com.olivergilan.deliver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class setOrderLocation extends AppCompatActivity implements OnMapReadyCallback {

    private LinearLayout confirmPanel;
    private Button confirmPickup, denyPickup, placeOrder;
    private RelativeLayout orderSummary;
    private TextView itemSummary, costSummary;
    private ProgressBar progressBar;

    private Place pickupLocation;

    private int itemCount = 0;
    private ArrayList<Product> products;
    private int cost = 0;

    /*
    *   Google Maps API Fragment declarations
    *
    *
     */
    GoogleMap map;
    MapFragment fragment;
    private Location mCurrentLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    final int REQUEST_LOCATION = 1;
    final int REQUEST_CHECK_SETTINGS = 2;
    private PlaceAutocompleteFragment autocompleteFragment;

    Marker pickupMarker;
    Marker dropOffMarker;

    //Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_order_location);

        Bundle bundle = this.getIntent().getExtras();
        products = bundle.getParcelableArrayList("items");
        for(Product p: products){
            itemCount++;
            cost += p.getCost();
            Log.i("Deliver", p.getName().toString());
        }

        confirmPanel = (LinearLayout) findViewById(R.id.confirmPanel);
        confirmPickup = (Button) findViewById(R.id.confirmBtn);
        denyPickup = (Button) findViewById(R.id.denyBtn);
        placeOrder = (Button) findViewById(R.id.placeOrder);
        orderSummary = (RelativeLayout) findViewById(R.id.orderSummary);
        itemSummary = (TextView) findViewById(R.id.itemSummary);
        costSummary = (TextView) findViewById(R.id.totalCostSummary);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);


        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        //Location Requests
        mLocationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if(pickupMarker == null) {
                        updateLocation(location);
                    }
                }
            };
        };

        //Map
        mapInit();

        /*
        *   Autocomplete Fragment Functions
         */
        autocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setHint("Where can your items be found?");
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                if(pickupMarker != null){
                    pickupMarker.remove();
                }
                pickupMarker = map.addMarker(new MarkerOptions()
                                    .position(place.getLatLng())
                                    .title(place.getName().toString()));
                selectPickup(place);
            }

            @Override
            public void onError(Status status) {
                //Error
                System.out.println("Error selecting place");
            }
        });

        //Only allow locations with specific addresses
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ESTABLISHMENT)
                .build();
        autocompleteFragment.setFilter(typeFilter);
    }

    //Sets bounds bias
    private void setBoundsBias(Location location){
        autocompleteFragment.setBoundsBias(new LatLngBounds(
                new LatLng(location.getLatitude() - .1, location.getLongitude() - .1),
                new LatLng(location.getLatitude() + .1, location.getLongitude() + .1)));
    }

    /*
    *   Maps API Functions
     */
    private void mapInit() {
        fragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragmentSmall);
        fragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        //Toast.makeText(this, "Ready", Toast.LENGTH_LONG).show();
        MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_grayscale);
        map.setMapStyle(style);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "Permission Needed", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(setOrderLocation.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        } else {

            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            mCurrentLocation = location;
                            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                            map.addMarker(new MarkerOptions()
                                .position(ll)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.location)));
                            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 15);
                            map.animateCamera(update);

                        }
                    });
            createLocationRequest();
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
            //map.setMyLocationEnabled(true);
            //map.setOnMyLocationButtonClickListener(this);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(mLocationRequest);
            LocationSettingsRequest.Builder settingsBuilder = new LocationSettingsRequest.Builder();
            SettingsClient client = LocationServices.getSettingsClient(this);
            Task<LocationSettingsResponse> task = client.checkLocationSettings(settingsBuilder.build());

            task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                }
            });

            task.addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if(e instanceof ResolvableApiException){
                        try{
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(setOrderLocation.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sendEx){
                            //Ignore
                        }
                    }
                }
            });
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(1000);
    }

    public void updateLocation(Location location){
        setBoundsBias(location);
        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(pos, 15);
        map.animateCamera(update);
    }

    public void selectPickup(final Place LOCATION){
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LOCATION.getLatLng(), 15);
        map.animateCamera(update);
        confirmPanel.setVisibility(View.VISIBLE);
        denyPickup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickupMarker.remove();
                confirmPanel.setVisibility(View.GONE);
                updateLocation(mCurrentLocation);
            }
        });
        confirmPickup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickupLocation = LOCATION;
                confirmPanel.setVisibility(View.GONE);
                resizeFragment(fragment, LinearLayout.LayoutParams.MATCH_PARENT, 500);
                confirmOrder();
            }
        });
    }
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public void resizeFragment(MapFragment f, int newWidth, int newHeight){
        if(f != null){
            View view = f.getView();
            ConstraintLayout.LayoutParams p = new ConstraintLayout.LayoutParams(newWidth, newHeight);
            view.setLayoutParams(p);
            view.requestLayout();
        }
    }

    public void confirmOrder(){
        LatLng coordinates = pickupLocation.getLatLng();
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(
                    coordinates.latitude,
                    coordinates.longitude,
                    1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Address address = addresses.get(0);
        orderSummary.setVisibility(View.VISIBLE);
        itemSummary.setText(itemCount + " items from " + pickupLocation.getName());
        costSummary.setText("Estimated cost: $ " + (cost+1));
        placeOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                Order newOrder = new Order(products, pickupLocation, currentUser.getUid());
                db.collection("allOrders")
                        .document(address.getCountryCode().toString())
                        .collection("orders")
                        .add(newOrder)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(setOrderLocation.this, "Order submitted", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(setOrderLocation.this, MainActivity.class);
                                intent.putExtra("status", true);
                                intent.putExtra("order", documentReference.getPath());
                                startActivity(intent);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("Database", "Didn't work");
                            }
                        });
            }
        });
    }


}
