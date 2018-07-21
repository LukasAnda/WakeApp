package sk.lukasanda.wakeapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        OnCompleteListener<Void>, MarkersFragment.OnFragmentInteractionListener {
    
    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int RADIUS_OF_EARTH_METERS = 6371009;
    
    private GoogleMap map;
    
    /**
     * Provides access to the Geofencing API.
     */
    private GeofencingClient mGeofencingClient;
    
    /**
     * The list of geofences used in this sample.
     */
    private ArrayList<Geofence> mGeofenceList;
    
    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent;
    
    DatabaseReference mDatabase;
    
    private ValueEventListener listener;
    
    private DraggableCircle draggableCircle;
    private DbUser currentUser;
    
    private MarkersFragment markersFragment;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        mDatabase = FirebaseDatabase.getInstance().getReference("users");
        
        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<>();
        
        registerGeofenceChangeListener();
        
        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;
        
        
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        
        final SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        markersFragment = MarkersFragment.newInstance();
        
        
        List<String> names = new ArrayList<>();
        names.add("MAP");
        names.add("LIST");
        
        final ViewPager vp = findViewById(R.id.container);
        setupViewPager(vp, names, mapFragment, markersFragment);
        
        final FloatingActionButton floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFabClicked();
            }
        });
        
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(vp);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                vp.setCurrentItem(tab.getPosition());
                if (tab.getPosition() != 0) {
                    floatingActionButton.setVisibility(View.GONE);
                } else {
                    floatingActionButton.setVisibility(View.VISIBLE);
                }
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        
        vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int
                    positionOffsetPixels) {
                
            }
            
            @Override
            public void onPageSelected(int position) {
                if (position != 0) {
                    floatingActionButton.setVisibility(View.GONE);
                } else {
                    floatingActionButton.setVisibility(View.VISIBLE);
                }
            }
            
            @Override
            public void onPageScrollStateChanged(int state) {
            
            }
        });
        
        mapFragment.getMapAsync(this);
    }
    
    private void onFabClicked() {
        if (draggableCircle == null) {
            draggableCircle = new DraggableCircle(map.getCameraPosition().target, Constants
                    .GEOFENCE_RADIUS_IN_METERS);
            Toast.makeText(MapsActivity.this, R.string.marker_info, Toast.LENGTH_LONG)
                    .show();
        } else {
            final EditText taskEditText = new EditText(MapsActivity.this);
            AlertDialog dialog = new AlertDialog.Builder(MapsActivity.this)
                    .setTitle("Add a new alarm")
                    .setMessage("Set the name of this alarm")
                    .setView(taskEditText)
                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            map.clear();
                            if (currentUser == null) {
                                currentUser = new DbUser(new ArrayList<DbGeofence>());
                            }
                            currentUser.getGeofences().add(new DbGeofence(taskEditText
                                    .getText().toString(),
                                    draggableCircle.mCenterMarker.getPosition().latitude,
                                    draggableCircle.mCenterMarker
                                            .getPosition().longitude, draggableCircle.mCircle
                                    .getRadius()));
                            mDatabase.child(getUniqueID()).setValue(currentUser);
                            draggableCircle = null;
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            draggableCircle = null;
                            map.clear();
                            for (DbGeofence geofence : currentUser.getGeofences()) {
                                map.addMarker(new MarkerOptions().position(new LatLng(geofence
                                        .getLatitude(), geofence.getLongitude())).draggable(false));
                            }
                        }
                    })
                    .create();
            dialog.show();
        }
    }
    
    
    private void registerGeofenceChangeListener() {
        if (listener == null) {
            listener = new ValueEventListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    currentUser = dataSnapshot.getValue(DbUser.class);
                    mGeofenceList.clear();
                    if (currentUser != null) {
                        if (map != null && map.getCameraPosition().zoom <= 5) {
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng
                                    (currentUser.getGeofences().get(0).getLatitude(), currentUser
                                            .getGeofences().get(0).getLongitude()), 15f));
                        }
                        for (DbGeofence geofence : currentUser.getGeofences()) {
                            if (geofence.getRadius() > 0) {
                                mGeofenceList.add(new Geofence.Builder()
                                        // Set the request ID of the geofence. This is a string to
                                        // identify this
                                        // geofence.
                                        .setRequestId(geofence.getName())
                                        
                                        // Set the circular region of this geofence.
                                        .setCircularRegion(
                                                geofence.getLatitude(),
                                                geofence.getLongitude(),
                                                (float) geofence.getRadius()
                                        )
                                        
                                        // Set the expiration duration of the geofence. This
                                        // geofence
                                        // gets automatically
                                        // removed after this period of time.
                                        .setExpirationDuration(Constants
                                                .GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                                        
                                        // Set the transition types of interest. Alerts are only
                                        // generated for these
                                        // transition. We track entry and exit transitions in this
                                        // sample.
                                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                                Geofence.GEOFENCE_TRANSITION_EXIT)
                                        
                                        // Create the geofence.
                                        .build());
                                if (map != null)
                                    map.addMarker(new MarkerOptions().position(new LatLng(geofence
                                            .getLatitude(), geofence.getLongitude())).draggable
                                            (false));
                            }
                        }
                        markersFragment.setAdapter(currentUser.getGeofences());
                        if (mGeofenceList.size() > 0) {
                            mGeofencingClient.addGeofences(getGeofencingRequest(),
                                    getGeofencePendingIntent())
                                    .addOnCompleteListener(MapsActivity.this);
                        }
                    }
                    
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                
                }
            };
        }
        mDatabase.child(getUniqueID()).addValueEventListener(listener);
    }
    
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent
                .FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }
    
    
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker arg0) {
                draggableCircle.onMarkerMoved(arg0);
            }
            
            @SuppressWarnings("unchecked")
            @Override
            public void onMarkerDragEnd(Marker arg0) {
                draggableCircle.onMarkerMoved(arg0);
                map.animateCamera(CameraUpdateFactory.newLatLng(arg0.getPosition()));
            }
            
            @Override
            public void onMarkerDrag(Marker arg0) {
                draggableCircle.onMarkerMoved(arg0);
            }
        });
        map.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle circle) {
                // Flip the red, green and blue components of the circle's stroke color.
                circle.setStrokeColor(circle.getStrokeColor() ^ 0x00ffffff);
            }
        });
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                updateMapPosition(query);
                return false;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        
        return true;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mDatabase.removeEventListener(listener);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (listener != null) mDatabase.addValueEventListener(listener);
    }
    
    @SuppressLint("MissingPermission")
    public String getUniqueID() {
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context
                .TELEPHONY_SERVICE);
        
        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android
                .provider.Settings.Secure.ANDROID_ID);
        
        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) |
                tmSerial.hashCode());
        String deviceId = deviceUuid.toString();
        return deviceId;
    }
    
    private void setupViewPager(ViewPager viewPager, List<String> names, Fragment... fragments) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        int counter = 0;
        for (Fragment f : fragments) {
            adapter.addFrag(f, names.get(counter));
            counter++;
        }
        viewPager.setAdapter(adapter);
    }
    
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        
        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        
        // Add the geofences to be monitored by geofencing service.
        
        builder.addGeofences(mGeofenceList);
        
        // Return a GeofencingRequest.
        return builder.build();
    }
    
    public void updateMapPosition(String location) {
        List<Address> addressList;
        
        if (location != null && !location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                map.addMarker(new MarkerOptions().position(latLng).title(location));
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void onComplete(@NonNull Task<Void> task) {
        if (!task.isSuccessful()) {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this, task.getException());
            Log.w(TAG, errorMessage);
        }
    }
    
    @Override
    public void onFragmentInteraction(Geofence geofence) {
    }
    
    /**
     * Generate LatLng of radius marker
     */
    private static LatLng toRadiusLatLng(LatLng center, double radiusMeters) {
        double radiusAngle = Math.toDegrees(radiusMeters / RADIUS_OF_EARTH_METERS) /
                Math.cos(Math.toRadians(center.latitude));
        return new LatLng(center.latitude, center.longitude + radiusAngle);
    }
    
    private static double toRadiusMeters(LatLng center, LatLng radius) {
        float[] result = new float[1];
        Location.distanceBetween(center.latitude, center.longitude,
                radius.latitude, radius.longitude, result);
        return result[0];
    }
    
    private class DraggableCircle {
        private final Marker mCenterMarker;
        private final Marker mRadiusMarker;
        private final Circle mCircle;
        private double mRadiusMeters;
        
        public DraggableCircle(LatLng center, double radiusMeters) {
            mRadiusMeters = radiusMeters;
            mCenterMarker = map.addMarker(new MarkerOptions()
                    .position(center)
                    .draggable(true));
            mRadiusMarker = map.addMarker(new MarkerOptions()
                    .position(toRadiusLatLng(center, radiusMeters))
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE)));
            
            int color = getResources().getColor(R.color.colorPrimary);
            int alphaColor = Color.argb(100, Color.red(color), Color.green(color), Color.blue
                    (color));
            mCircle = map.addCircle(new CircleOptions()
                    .center(center)
                    .radius(radiusMeters)
                    .strokeColor(alphaColor)
                    .fillColor(alphaColor).clickable(true));
        }
        
        public Marker getmCenterMarker() {
            return mCenterMarker;
        }
        
        public boolean onMarkerMoved(Marker marker) {
            if (marker.equals(mCenterMarker)) {
                mCircle.setCenter(marker.getPosition());
                mRadiusMarker.setPosition(toRadiusLatLng(marker.getPosition(), mRadiusMeters));
                return true;
            }
            if (marker.equals(mRadiusMarker)) {
                mRadiusMeters =
                        toRadiusMeters(mCenterMarker.getPosition(), mRadiusMarker.getPosition());
                mCircle.setRadius(mRadiusMeters);
                return true;
            }
            return false;
        }

//        public void onStyleChange() {
//            mCircle.setStrokeWidth(mStrokeWidthBar.getProgress());
//            mCircle.setStrokeColor(mStrokeColorArgb);
//            mCircle.setFillColor(mFillColorArgb);
//        }
//
//        public void setStrokePattern(List<PatternItem> pattern) {
//            mCircle.setStrokePattern(pattern);
//        }
//
//        public void setClickable(boolean clickable) {
//            mCircle.setClickable(clickable);
//        }
    }
}
