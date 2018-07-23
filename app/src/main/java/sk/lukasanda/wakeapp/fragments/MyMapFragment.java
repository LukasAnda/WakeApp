package sk.lukasanda.wakeapp.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

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

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sk.lukasanda.wakeapp.R;
import sk.lukasanda.wakeapp.activities.MapsActivity;
import sk.lukasanda.wakeapp.geofencing.Constants;
import sk.lukasanda.wakeapp.model.DbGeofence;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MyMapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MyMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyMapFragment extends Fragment implements OnMapReadyCallback {
    
    private static final String TAG = MyMapFragment.class.getSimpleName();
    
    private static final int RADIUS_OF_EARTH_METERS = 6371009;
    
    private OnFragmentInteractionListener mListener;
    
    private GoogleMap map;
    
    private DraggableCircle draggableCircle;
    
    private List<DbGeofence> localGeofences = new ArrayList<>();
    
    public MyMapFragment() {
        // Required empty public constructor
    }
    
    public static MyMapFragment newInstance() {
        MyMapFragment fragment = new MyMapFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_my_map, container, false);
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager().beginTransaction().replace(R.id.map_container, mapFragment)
                .commitAllowingStateLoss();
        mapFragment.getMapAsync(this);
        final FloatingActionButton floatingActionButton = v.findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFabClicked();
            }
        });
        return v;
    }
    
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
        updateMarkers(localGeofences);
    }
    
    private void onFabClicked() {
        if (draggableCircle == null) {
            draggableCircle = new DraggableCircle(map.getCameraPosition().target, Constants
                    .GEOFENCE_RADIUS_IN_METERS, true);
            Toast.makeText(getActivity(), R.string.marker_info, Toast.LENGTH_LONG)
                    .show();
        } else {
            final EditText taskEditText = new EditText(getActivity());
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle("Add a new alarm")
                    .setMessage("Set the name of this alarm")
                    .setView(taskEditText)
                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(TextUtils.isEmpty(taskEditText.getText().toString())){
                                Log.d(TAG, "Empty alarm name");
                                Toast.makeText(getContext(), R.string.empty_alarm_name_error, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            map.clear();
                            if (mListener != null)
                                mListener.onGeofenceAdded(new DbGeofence(taskEditText
                                        .getText().toString(),
                                        draggableCircle.mCenterMarker.getPosition().latitude,
                                        draggableCircle.mCenterMarker
                                                .getPosition().longitude, draggableCircle.mCircle
                                        .getRadius()));
                            draggableCircle = null;
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            draggableCircle.remove();
                            draggableCircle = null;
                        }
                    })
                    .create();
            dialog.show();
        }
    }
    
    public void updateMarkers(List<DbGeofence> geofences) {
        localGeofences = geofences;
        if (map == null) return;
        map.clear();
        int lastElement = geofences.size() - 1;
        if (lastElement >= 0) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(geofences.get
                    (lastElement)
                    .getLatitude(), geofences.get(lastElement).getLongitude()), 15f));
        }
        for (DbGeofence geofence : geofences) {
            new DraggableCircle(new LatLng(geofence.getLatitude(), geofence.getLongitude()),
                    geofence.getRadius(), false);
        }
    }
    
    public void updateMapPosition(String location) {
        List<Address> addressList;
        
        if (location != null && !location.equals("")) {
            Geocoder geocoder = new Geocoder(getActivity());
            try {
                addressList = geocoder.getFromLocationName(location, 1);
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public interface OnFragmentInteractionListener {
        void onGeofenceAdded(DbGeofence geofence);
    }
    
    private class DraggableCircle {
        private final Marker mCenterMarker;
        private final Marker mRadiusMarker;
        private final Circle mCircle;
        private double mRadiusMeters;
        
        DraggableCircle(LatLng center, double radiusMeters, boolean draggable) {
            mRadiusMeters = radiusMeters;
            mCenterMarker = map.addMarker(new MarkerOptions()
                    .position(center)
                    .draggable(draggable));
            mRadiusMarker = map.addMarker(new MarkerOptions()
                    .position(toRadiusLatLng(center, radiusMeters))
                    .draggable(draggable)
                    .visible(draggable)
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
        
        private void remove() {
            mCenterMarker.remove();
            mCircle.remove();
            mRadiusMarker.remove();
        }
        
        void onMarkerMoved(Marker marker) {
            if (marker.equals(mCenterMarker)) {
                mCircle.setCenter(marker.getPosition());
                mRadiusMarker.setPosition(toRadiusLatLng(marker.getPosition(), mRadiusMeters));
                return;
            }
            if (marker.equals(mRadiusMarker)) {
                mRadiusMeters =
                        toRadiusMeters(mCenterMarker.getPosition(), mRadiusMarker.getPosition());
                mCircle.setRadius(mRadiusMeters);
            }
        }
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
    
}
