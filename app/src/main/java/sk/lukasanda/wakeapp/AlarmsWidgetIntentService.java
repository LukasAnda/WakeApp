package sk.lukasanda.wakeapp;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import sk.lukasanda.wakeapp.geofencing.Constants;
import sk.lukasanda.wakeapp.model.DbGeofence;

public class AlarmsWidgetIntentService extends IntentService {
    
    private static final String ACTION_UPDATE = "sk.lukasanda.wakeapp.action.UPDATE_WIDGETS";
    
    private static final String TAG = AlarmsWidgetIntentService.class.getSimpleName();
    
    public AlarmsWidgetIntentService() {
        super("AlarmsWidgetIntentService");
    }
    
    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUpdateWidget(Context context) {
        Intent intent = new Intent(context, AlarmsWidgetIntentService.class);
        intent.setAction(ACTION_UPDATE);
        try {
            context.startService(intent);
        } catch (IllegalStateException e) {
            Log.d(TAG, "Cannot start service intent while app is in the background");
        }
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE.equals(action)) {
                handleActionUpdateWidgets();
            }
        }
    }
    
    
    private void handleActionUpdateWidgets() {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("users");
        mDatabase.child(Constants.getUniqueID(getApplicationContext()))
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                StringBuilder sb = new StringBuilder();
                for (DataSnapshot d : dataSnapshot.getChildren()) {
                    DbGeofence geofence = d.getValue(DbGeofence.class);
                    if (geofence != null) {
                        sb.append(geofence.getName());
                        sb.append("\n");
                        sb.append(new LatLng(geofence.getLatitude(),geofence.getLongitude()));
                        sb.append("\n");
                    }
                }
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(AlarmsWidgetIntentService.this);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(AlarmsWidgetIntentService.this,AlarmsWidget.class));
                AlarmsWidget.setAlarms(sb.toString());
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds,
                        R.id.alarms_widget_container);
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds,
                        R.id.alarms_widget_text);
                AlarmsWidget.updateAlarmWidgets(AlarmsWidgetIntentService.this,appWidgetManager,appWidgetIds);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            
            }
        });
    }
    
}