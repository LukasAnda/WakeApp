package sk.lukasanda.wakeapp.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import sk.lukasanda.wakeapp.model.DbGeofence;
import sk.lukasanda.wakeapp.adapters.MarkerListAdapter;
import sk.lukasanda.wakeapp.R;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MarkersFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MarkersFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MarkersFragment extends Fragment {
    
    private OnFragmentInteractionListener mListener;
    private RecyclerView recyclerView;
    
    public MarkersFragment() {
    }

    public static MarkersFragment newInstance() {
        MarkersFragment fragment = new MarkersFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_markers, container, false);
        recyclerView = v.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
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
    
    public void setAdapter(final List<DbGeofence> geofences){
        if(recyclerView!=null){
            recyclerView.setAdapter(new MarkerListAdapter(getActivity(), geofences, new MarkerListAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    makeDialog(geofences.get(position));
                }
            }));
        }
    }
    
    private void makeDialog(final DbGeofence geofence){
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle("Remove this alarm?")
                .setMessage("Are you sure ?")
                .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(mListener!=null)mListener.onFragmentInteraction(geofence);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    
                    }
                })
                .create();
        dialog.show();
    }
    
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(DbGeofence geofence);
    }
}
