package com.olivergilan.deliver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.AvoidType;
import com.akexorcist.googledirection.constant.Language;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.constant.Unit;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.request.DirectionTask;
import com.akexorcist.googledirection.util.DirectionConverter;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.collection.LLRBNode;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DeliverOrder extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener {

    GoogleMap map;
    private FirebaseFirestore database;
    private Location mCurrentLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    final int REQUEST_LOCATION = 1;
    final int REQUEST_CHECK_SETTINGS = 2;
    private String orderRef;

    private Button startNavBtn;
    private Button retrievedOrder;
    private TextView directionText;
    private LatLng start;
    private Boolean enRoute = false;
    private Boolean packageReceived = false;
    private Polyline initialPoly;
    private Polyline mainNavRoute;
    private LatLng destination;
    private LatLng customerLocation;
    final String serverKey = "AIzaSyC2M-Riz_Eiq-OFaISvh3zAKLuLChhWgNE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deliver_order);

        startNavBtn = (Button) findViewById(R.id.startNav);
        directionText = (TextView) findViewById(R.id.directionText);
        database = FirebaseFirestore.getInstance();
        retrievedOrder = (Button) findViewById(R.id.retrievedOrderBtn);
        Intent intent = getIntent();
        if(intent.hasExtra("ref")){
            orderRef = intent.getExtras().getString("ref");
            Log.i("PATH", orderRef);
        }
        DocumentReference order = database.document(orderRef);
        order.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                destination = new LatLng((double)documentSnapshot.get("latitude"), (double)documentSnapshot.get("longitude"));
            }
        });

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if(enRoute){
                        Location dest = new Location("Bearing");
                        dest.setLatitude(destination.latitude);
                        dest.setLongitude(destination.longitude);
                        if(location.distanceTo(dest) < 100){
                            retrievedOrder.setVisibility(View.VISIBLE);
                            retrievedOrder.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    enRoute = false;
                                    packageReceived = true;
                                }
                            });
                        }
                        navigate(location, destination);
                        initialPoly.remove();
                    }if(packageReceived){
                        navigate(location, customerLocation);
                    }
                }
            };
        };

        if (googleServicesAvailable()) {
            mapInit();
        }
    }


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

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(1000);
    }

    private void mapInit() {
        MapFragment fragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        fragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_grayscale);
        map.setMapStyle(style);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "Permission Needed", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(DeliverOrder.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        } else {

            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            mCurrentLocation = location;
                            start = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 15);
                                map.animateCamera(update);
                            getNavRoute(start);
                        }
                    });
            createLocationRequest();
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
            map.setMyLocationEnabled(true);
            map.setOnMyLocationButtonClickListener(this);

        }
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    private void getNavRoute(final LatLng start){
        LatLng origin = start;
        DocumentReference ref = database.document(orderRef);
        ref.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                destination = new LatLng((double)documentSnapshot.get("latitude"), (double)documentSnapshot.get("longitude"));
                Log.i("PATH", "latitude:" + documentSnapshot.get("latitude").toString() + " Destination: " + destination.toString());
                if(destination != null){
                    drawNavRoute(serverKey, start, destination);
                    zoomToFit(start, destination);
                }
                map.addMarker(new MarkerOptions()
                .position(destination)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                .title("Order"));
            }
        });
    }

    private void drawNavRoute(String key, LatLng start, final LatLng destination){
        Log.i("PATH", "ACTIVATED: " + destination.toString());
        GoogleDirection.withServerKey(key)
                .from(start)
                .to(destination)
                .transportMode(TransportMode.DRIVING)
                .avoid(AvoidType.FERRIES)
                .language(Language.ENGLISH)
                .unit(Unit.IMPERIAL)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(final Direction direction, String rawBody) {
                        Route route = direction.getRouteList().get(0);
                        Leg leg = route.getLegList().get(0);
                        final ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();
                        PolylineOptions polylineOptions = DirectionConverter.createPolyline(DeliverOrder.this, directionPositionList, 8, Color.BLUE);
                        initialPoly = map.addPolyline(polylineOptions);
                        Log.i("PATH", rawBody);
                        startNavBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                enRoute = true;
                                startNavBtn.setVisibility(View.GONE);
                                CameraPosition update = new CameraPosition.Builder()
                                        .target(directionPositionList.get(1))
                                        .bearing(0)
                                        .tilt(15f).zoom(18f).build();
                                map.moveCamera(CameraUpdateFactory.newCameraPosition(update));
                                directionText.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        Log.e("PATH", "SHIT DIDNT WORK");
                    }
                });
    }

    private void zoomToFit(LatLng start, LatLng destination){
        LatLngBounds bounds = new LatLngBounds.Builder().include(start).include(destination).build();
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
    }

    private void navigate(final Location loc, final LatLng destination){
        GoogleDirection.withServerKey(serverKey)
                .from(new LatLng(loc.getLatitude(), loc.getLongitude()))
                .to(destination)
                .transportMode(TransportMode.DRIVING)
                .avoid(AvoidType.FERRIES)
                .language(Language.ENGLISH)
                .unit(Unit.IMPERIAL)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        ArrayList<LatLng> directionPointsList = direction.getRouteList().get(0).getLegList().get(0).getDirectionPoint();
                        PolylineOptions polylineOptions = DirectionConverter.createPolyline(DeliverOrder.this, directionPointsList, 8, Color.BLUE);
                        if(mainNavRoute != null){
                            mainNavRoute.remove();
                        }
                        mainNavRoute = map.addPolyline(polylineOptions);
                        List<Step> stepList = direction.getRouteList().get(0).getLegList().get(0).getStepList();
                        Location dest = new Location("Bearing");
                        dest.setLatitude(stepList.get(0).getEndLocation().getLatitude());
                        dest.setLongitude(stepList.get(0).getEndLocation().getLongitude());
                        CameraPosition update = new CameraPosition.Builder()
                                .target(directionPointsList.get(1))
                                .bearing(loc.bearingTo(new Location(dest)))
                                .tilt(60f).zoom(19f).build();
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(update));
                        Info distance = stepList.get(0).getDistance();
                        directionText.setText(stepList.get(1).getManeuver() + " in " + distance.getText());
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {

                    }
                });
    }
}
