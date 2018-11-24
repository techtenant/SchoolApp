package techtenant.co.ke.schoolapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Interpolator;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class WelcomeActivity extends FragmentActivity implements GeoQueryEventListener, GoogleMap.OnCameraChangeListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static GeoLocation INITIAL_CENTER;
    private static final int INITIAL_ZOOM_LEVEL = 10;
    private static final String GEO_FIRE_DB = "https://schoolapp-8399b.firebaseio.com";
    private static final String GEO_FIRE_REF = GEO_FIRE_DB + "/_geofire";

    private GoogleMap map;
    private Circle searchCircle;
    private GeoFire geoFire;
    private GeoQuery geoQuery;
    Location currentLocation;

    private Map<String, Marker> markers;
    FirebaseApp app;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        FirebaseOptions options = new FirebaseOptions.Builder().setApplicationId("schools").setDatabaseUrl(GEO_FIRE_DB).build();
        try {
            app = FirebaseApp.getInstance();
        } catch (IllegalStateException e) {
            //Firebase not initialized automatically, do it manually
            app = FirebaseApp.initializeApp(this, options);

        }
        INITIAL_CENTER = new GeoLocation(-1.259470, 36.796120);


        //Initializing googleApiClient
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
         currentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);


        // setup GeoFire
        this.geoFire = new GeoFire(FirebaseDatabase.getInstance(app).getReferenceFromUrl(GEO_FIRE_REF));
        // radius in km

        if (currentLocation != null) {
            //Getting longitude and latitude
            this.geoQuery = this.geoFire.queryAtLocation(new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()), 1);
            // setup markers
            this.markers = new HashMap<String, Marker>();
        } else {
            //Getting longitude and latitude
            this.geoQuery = this.geoFire.queryAtLocation(INITIAL_CENTER,1);
            // setup markers
            this.markers = new HashMap<String, Marker>();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // remove all event listeners to stop updating in the background
        this.geoQuery.removeAllListeners();
        for (Marker marker : this.markers.values()) {
            marker.remove();
        }
        this.markers.clear();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // add an event listener to start updating locations again
        this.geoQuery.addGeoQueryEventListener(this);
    }


    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        Marker marker = this.map.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude)));
        this.markers.put(key, marker);
    }

    @Override
    public void onKeyExited(String key) {
        Marker marker = this.markers.get(key);
        if (marker != null) {
            marker.remove();
            this.markers.remove(key);


        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {

    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("There was an unexpected error querying GeoFire: " + error.getMessage())
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

        // Update the search criteria for this geoQuery and the circle on the map
        LatLng center = cameraPosition.target;
        double radius = zoomLevelToRadius(cameraPosition.zoom);
        this.searchCircle.setCenter(center);
        this.searchCircle.setRadius(radius);
        this.geoQuery.setCenter(new GeoLocation(center.latitude, center.longitude));
        // radius in km
        this.geoQuery.setRadius(radius/1000);

    }



    private double zoomLevelToRadius(double zoomLevel) {
        // Approximation to fit circle into view
        return 16384000/Math.pow(2, zoomLevel);
    }




    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;

        if (currentLocation != null) {
            //Getting longitude and latitude

            LatLng latLngCenter = new LatLng(currentLocation.getLongitude(), currentLocation.getLatitude());
            this.searchCircle = this.map.addCircle(new CircleOptions().center(latLngCenter).radius(1000));
            this.searchCircle.setFillColor(Color.argb(66, 255, 0, 255));
            this.searchCircle.setStrokeColor(Color.argb(66, 0, 0, 0));
            this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngCenter, INITIAL_ZOOM_LEVEL));
            this.map.setOnCameraChangeListener(this);


        }else {


            LatLng latLngCenter = new LatLng(INITIAL_CENTER.longitude, INITIAL_CENTER.longitude);
            this.searchCircle = this.map.addCircle(new CircleOptions().center(latLngCenter).radius(1000));
            this.searchCircle.setFillColor(Color.argb(66, 255, 0, 255));
            this.searchCircle.setStrokeColor(Color.argb(66, 0, 0, 0));
            this.map.setOnCameraChangeListener(this);


            LatLng nairobi = new LatLng(INITIAL_CENTER.longitude, INITIAL_CENTER.longitude);
            this.map.addMarker(new MarkerOptions().position(nairobi)

                    .title("Nairobi School"));
            this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(nairobi,INITIAL_ZOOM_LEVEL));


            this.map.addMarker(new MarkerOptions()
                    .position(new LatLng(-1.365470, 36.758690))
                    .title("The Banda ")
                    .snippet("The Banda school"));


            this.map.addMarker(new MarkerOptions()
                    .position(new LatLng(-1.3361,36.7368))
                    .title("Hillcrest School")
                    .snippet("Hillcrest International Schools is a British Curriculum school based in,\n" +
                            " Nairobi, Kenya. It has three sections namely: Hillcrest Early Years, "));



            this.map.addMarker(new MarkerOptions()
                    .position(new LatLng(-1.333320,36.753510))
                    .title("Nairobi Academy")
                    .snippet("Nairobi Academy\n" +
                            "School "));

        }



    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
