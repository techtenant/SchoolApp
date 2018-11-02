package techtenant.co.ke.schoolapp;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.util.Map;

public class WelcomeActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final GeoLocation INITIAL_CENTER = new GeoLocation(-1.326755, 36.852706);
    private static final int INITIAL_ZOOM_LEVEL = 14;
    private static final String GEO_FIRE_DB = "https://publicdata-transit.firebaseio.com";
    private static final String GEO_FIRE_REF = GEO_FIRE_DB + "/_geofire";

    private GoogleMap map;
    private Circle searchCircle;
    private GeoFire geoFire;
    private GeoQuery geoQuery;

    private Map<String,Marker> markers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        this.map = mapFragment.getMap();
        LatLng latLngCenter = new LatLng(INITIAL_CENTER.latitude, INITIAL_CENTER.longitude);
        this.searchCircle = this.map.addCircle(new CircleOptions().center(latLngCenter).radius(1000));
        this.searchCircle.setFillColor(Color.argb(66, 255, 0, 255));
        this.searchCircle.setStrokeColor(Color.argb(66, 0, 0, 0));
        this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngCenter, INITIAL_ZOOM_LEVEL));
        this.map.setOnCameraChangeListener(this);

        FirebaseOptions options = new FirebaseOptions.Builder().setApplicationId("geofire").setDatabaseUrl(GEO_FIRE_DB).build();
        FirebaseApp app = FirebaseApp.initializeApp(this, options);
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        map.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        map.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}
