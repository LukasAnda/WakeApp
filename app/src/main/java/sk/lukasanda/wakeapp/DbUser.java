package sk.lukasanda.wakeapp;

import java.util.ArrayList;

public class DbUser {
    private ArrayList<DbGeofence> geofences;
    
    public DbUser() {
    }
    
    public DbUser(ArrayList<DbGeofence> geofences) {
        this.geofences = geofences;
    }
    
    public ArrayList<DbGeofence> getGeofences() {
        return geofences;
    }
    
    public void setGeofences(ArrayList<DbGeofence> geofences) {
        this.geofences = geofences;
    }
}
