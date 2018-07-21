package sk.lukasanda.wakeapp;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.Geofence;

import java.util.List;


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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
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
    
    public void setAdapter(List<DbGeofence> geofences){
        if(recyclerView!=null){
            recyclerView.setAdapter(new MarkerListAdapter(getActivity(), geofences, new MarkerListAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {
        
                }
            }));
        }
    }
    
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Geofence geofence);
    }
}
