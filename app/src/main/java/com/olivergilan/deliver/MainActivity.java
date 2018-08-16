package com.olivergilan.deliver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMyLocationButtonClickListener {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseStorage storage;
    private FirebaseFirestore database;
    private StorageReference sRef;
    private Location mCurrentLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private EditText selectItems;
    private boolean activeOrder;
    private String orderRef;

    GoogleMap map;
    private Marker userOrder;
    private ArrayList<Marker> orders;
    Button logOutBtn;
    final int REQUEST_LOCATION = 1;
    final int REQUEST_CHECK_SETTINGS = 2;

    private RelativeLayout acceptOrderPanel;
    private Button acceptOrder;
    private TextView itemSummary, estimatedCost;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, SignUp.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            MainActivity.this.finish();
            killActivity();
        }

        Intent intent = getIntent();
        if(intent.hasExtra("status")){
            activeOrder = intent.getExtras().getBoolean("status");
        }
        if(intent.hasExtra("order")){
            orderRef = intent.getExtras().getString("order");
        }

        database = FirebaseFirestore.getInstance();
        logOutBtn = (Button) findViewById(R.id.logOut);

        storage = FirebaseStorage.getInstance();
        sRef = storage.getReference();
        selectItems = (EditText) findViewById(R.id.chooseItems);
        acceptOrderPanel = (RelativeLayout) findViewById(R.id.orderSummary);
        acceptOrder = (Button) findViewById(R.id.acceptOrder);
        itemSummary = (TextView) findViewById(R.id.itemSummary);
        estimatedCost = (TextView) findViewById(R.id.totalCostSummary);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
//                   updateLocation(location);
                    isOrderActive(location);
                }
            };
        };
        if (googleServicesAvailable()) {
            mapInit();
        }

        //
        //Button to log out of account
        //
        logOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                currentUser.delete();
                startActivity(new Intent(MainActivity.this, SignUp.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });
    }

    //Kills Activity
    private void killActivity(){
        finish();
    }

    /**
     * Checks if device is connected to Google Services
     * @return boolean if Google Services is connected and available
     */
    public boolean googleServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(this, isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(this, "Can't connect to play services", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    /**
     * Initiates map fragment
     */
    private void mapInit() {
        MapFragment fragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        fragment.getMapAsync(this);
    }

    /**
     * Callback method when map is ready. Styles and sets up main map for user
     * @param googleMap map to be passed to fragment
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //
        //Apply map to fragment, set style
        //
        map = googleMap;
        MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_grayscale);
        map.setMapStyle(style);

        //Checks if location permissions are granted on device
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "Permission Needed", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                REQUEST_LOCATION);
        } else {

            //
            // Gets initial user location
            //
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            mCurrentLocation = location;

                            //Gets pending orders for map
                            getOrders(mCurrentLocation);

                            //Checks if user has just placed an order
                            //if true, camera focuses on order
                            //else, camera focuses on user
                            if(activeOrder==true) {
                                focusOnOrder();
                            }else{
                                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 15);
                                map.animateCamera(update);
                            }
                        }
                    });

            //Creates Fused Location Client for continuous location updates
            createLocationRequest();
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);

            //Enables Maps API location handler
            map.setMyLocationEnabled(true);
            map.setOnMyLocationButtonClickListener(this);

            //Disables marker clicks
            map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    return false;
                }
            });

            //Sets Marker info window click activity
            map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    MarkerTag tag = (MarkerTag) marker.getTag();
                    final Order order = tag.getOrder();
                    final String id = tag.getId();

                    //Shows order confirmation panel
                    acceptOrderPanel.setVisibility(View.VISIBLE);
                    itemSummary.setText(order.getItemCount() + " items from " + order.getPickupLocation());
                    estimatedCost.setText("Estimated cost: $" + order.getTotalCost());
                    acceptOrder = (Button) findViewById(R.id.acceptOrder);

                    //When order is accepted by a user
                    acceptOrder.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            //Adds order to Active Order database
                            database.collection("allOrders")
                                    .document(order.getCountryCode())
                                    .collection("activeOrders")
                                    .document(id)
                                    .set(order);

                            //Removes order from Pending Order database
                            database.collection("allOrders")
                                    .document(order.getCountryCode())
                                    .collection("pendingOrders")
                                    .document(id).delete();
                            Intent intent = new Intent(MainActivity.this, DeliverOrder.class);
                            String ref = "allOrders/" + order.getCountryCode() + "/activeOrders/" + id;
                            intent.putExtra("ref", ref);
                            intent.putExtra("chatPath", "allOrders/" + order.getCountryCode() + "/chats/" + id);    //Path for order chat
                            startActivity(intent);
                        }
                    });
                }
            });

            //Closes confirmation panel if order is denied
            map.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
                @Override
                public void onInfoWindowClose(Marker marker) {
                    acceptOrderPanel.setVisibility(View.GONE);
                }
            });

            //Starts activity to create order
            selectItems.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(MainActivity.this, ListItems.class));
                }
            });

            //This doesn't need to exist I believe. Possibly redundant
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
                            resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sendEx){
                            //Ignore
                        }
                    }
                }
            });
        }
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    mapInit();
                } else {
                    Toast.makeText(this, "Permission Needed", Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        activeOrder = false;
        return false;
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(1000);
    }

    /**
     * Orients camera on user in event that they are moving
     * @param location location of user
     */
    public void updateLocation(Location location){
        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(pos, 15);
        map.animateCamera(update);
    }

    /**
     * Focuses camera on user order in event that it was recently placed
     */
    public void focusOnOrder(){
        DocumentReference orderReferance = database.document(orderRef);
        orderReferance.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        //Convert database document to Order object to get location and orient camera on it
                        Order order = document.toObject(Order.class);
                        LatLng coordinates = new LatLng(document.getDouble("latitude"), document.getDouble("longitude"));
                        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(coordinates, 15);
                        map.animateCamera(update);
                        userOrder = map.addMarker(new MarkerOptions()
                            .title("ORDER")
                            .position(coordinates)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                        userOrder.setTag(order);
                        userOrder.showInfoWindow();
                        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                            @Override
                            public void onInfoWindowClick(Marker marker) {
                                return;
                            }
                        });
                    } else {
                        Log.d("Order Focus", "No such document");
                    }
                } else {
                    Log.d("Order Focus", "get failed with ", task.getException());
                }
            }
        });
    }

    /**
     * Grabs all orders within vicinity of user and places them on map as markers
     * @param location user location
     */
    public void getOrders(final Location location){
        map.clear();

        //Creates Address object to later retrieve country code of user
        LatLng coordinates = new LatLng(location.getLatitude(), location.getLongitude());
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        orders = new ArrayList<Marker>();
        try {
            addresses = geocoder.getFromLocation(
                    coordinates.latitude,
                    coordinates.longitude,
                    1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Address address = addresses.get(0);

        //Gets all orders from Pending Orders Database
        database.collection("allOrders")
                .document(address.getCountryCode().toString())
                .collection("pendingOrders")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                //Take order reference and turn it into Order object & marker
                                Order o = document.toObject(Order.class);
                                String id = document.getId();
                                MarkerTag tag = new MarkerTag(id, o);
                                Marker m = map.addMarker(new MarkerOptions()
                                    .position(new LatLng(o.getLatitude(), o.getLongitude()))
                                    .title("$: " + Integer.toString(o.getTotalCost()))
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                                m.setTag(tag);
                                orders.add(m);
                            }
                        } else {
                            Log.d("Whoops", "Error getting documents: ", task.getException());
                        }
                    }
                });

        //Snapshot listener of Pending Orders to update map with current orders
        //Removes taken orders from map
        database.collection("allOrders")
                .document(address.getCountryCode().toString())
                .collection("pendingOrders")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("ORDERS", "Listen failed.", e);
                            return;
                        }
                        for(DocumentChange dc: queryDocumentSnapshots.getDocumentChanges()){
                            switch (dc.getType()) {
                                case ADDED:
                                    break;
                                case MODIFIED:
                                    break;
                                case REMOVED:
                                    getOrders(location);
                                    break;

                            }
                        }
                    }
                });
    }


    /**
     * Checks if user has order being fulfilled
     * @param location user location
     */
    public void isOrderActive(Location location){

        //Address Object to later retrieve user country code
        LatLng coordinates = new LatLng(location.getLatitude(), location.getLongitude());
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        orders = new ArrayList<Marker>();
        try {
            addresses = geocoder.getFromLocation(
                    coordinates.latitude,
                    coordinates.longitude,
                    1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Address address = addresses.get(0);

        //Retrieves all active orders
        database.collection("allOrders")
                .document(address.getCountryCode().toString())
                .collection("activeOrders")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for (QueryDocumentSnapshot document: task.getResult()){
                                String chatPath = "allOrders/" + address.getCountryCode() + "/chats/" + document.getId();
                                Order o = document.toObject(Order.class);
                                if(currentUser != null){

                                    //Checks if user has any order in Active Order database
                                    if(o.getCustomer().matches(currentUser.getUid())){
                                        showOrderAlert(chatPath);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    /**
     * Displays alert to user letting them know their order is being fulfilled
     * @param chatPath reference to chat location in database
     */
    public void showOrderAlert(final String chatPath){
        TextView alert = (TextView) findViewById(R.id.alert);
        alert.setVisibility(View.VISIBLE);
        alert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, FollowOrder.class);
                intent.putExtra("chatPath", chatPath);
                startActivity(intent);
            }
        });
    }
}
