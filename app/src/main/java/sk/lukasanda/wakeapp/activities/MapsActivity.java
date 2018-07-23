package sk.lukasanda.wakeapp.activities;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import sk.lukasanda.wakeapp.AlarmsWidgetIntentService;
import sk.lukasanda.wakeapp.R;
import sk.lukasanda.wakeapp.adapters.ViewPagerAdapter;
import sk.lukasanda.wakeapp.fragments.MarkersFragment;
import sk.lukasanda.wakeapp.fragments.MyMapFragment;
import sk.lukasanda.wakeapp.geofencing.Constants;
import sk.lukasanda.wakeapp.geofencing.GeofenceBroadcastReceiver;
import sk.lukasanda.wakeapp.geofencing.GeofenceErrorMessages;
import sk.lukasanda.wakeapp.model.DbGeofence;

public class MapsActivity extends AppCompatActivity implements
        OnCompleteListener<Void>, MarkersFragment.OnFragmentInteractionListener, MyMapFragment
                .OnFragmentInteractionListener {
    
    private static final String TAG = MapsActivity.class.getSimpleName();
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
    
    private DatabaseReference mDatabase;
    
    private ValueEventListener listener;
    
    private List<DbGeofence> geofencesList = new ArrayList<>();
    
    private MarkersFragment markersFragment;
    
    private MyMapFragment myMapFragment;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        mDatabase = FirebaseDatabase.getInstance().getReference("users");
        
        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<>();
        
        
        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;
        
        
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        markersFragment = MarkersFragment.newInstance();
        
        myMapFragment = MyMapFragment.newInstance();
        
        List<String> names = new ArrayList<>();
        names.add("MAP");
        names.add("LIST");
        
        final ViewPager vp = findViewById(R.id.container);
        setupViewPager(vp, names, myMapFragment, markersFragment);
        
        
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(vp);
    
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        
        registerGeofenceChangeListener();
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
                myMapFragment.updateMapPosition(query);
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
    protected void onDestroy() {
        mDatabase.child(Constants.getUniqueID(MapsActivity.this)).removeEventListener(listener);
        super.onDestroy();
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
    public void onGeofenceAdded(DbGeofence geofence) {
        geofencesList.add(geofence);
        mDatabase.child(Constants.getUniqueID(MapsActivity.this)).setValue(geofencesList);
        AlarmsWidgetIntentService.startActionUpdateWidget(MapsActivity.this);
    }
    
    @Override
    public void onGeofenceDeleted(DbGeofence geofence) {
        if (geofencesList == null || geofencesList.size() == 0)
            return;
        if (geofencesList.contains(geofence))
            geofencesList.remove(geofence);
        mDatabase.child(Constants.getUniqueID(MapsActivity.this)).setValue(geofencesList);
        AlarmsWidgetIntentService.startActionUpdateWidget(MapsActivity.this);
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
    
    private void registerGeofenceChangeListener() {
        if (listener == null) {
            listener = new ValueEventListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    geofencesList.clear();
                    mGeofenceList.clear();
                    
                    
                    
                    for (DataSnapshot d : dataSnapshot.getChildren()) {
                        
                        DbGeofence geofence = d.getValue(DbGeofence.class);
                        if (geofence == null) continue;
                        
                        geofencesList.add(geofence);
                        
                        if (geofence.getRadius() > 0) {
                            mGeofenceList.add(new Geofence.Builder()
                                    .setRequestId(geofence.getName())
                                    .setCircularRegion(
                                            geofence.getLatitude(),
                                            geofence.getLongitude(),
                                            (float) geofence.getRadius()
                                    )
                                    .setExpirationDuration(Constants
                                            .GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                            Geofence.GEOFENCE_TRANSITION_EXIT)
                                    .build());
                            
                        }
                        
                    }
                    markersFragment.setAdapter(geofencesList);
                    myMapFragment.updateMarkers(geofencesList);
                    
                    if (mGeofenceList.size() > 0) {
                        mGeofencingClient.addGeofences(getGeofencingRequest(),
                                getGeofencePendingIntent())
                                .addOnCompleteListener(MapsActivity.this);
                    } else {
                        Log.d(TAG, "No geofences to add");
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                
                }
            };
        }
        mDatabase.child(Constants.getUniqueID(MapsActivity.this)).addValueEventListener(listener);
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
}
