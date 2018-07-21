package sk.lukasanda.wakeapp;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class MarkerListAdapter extends
        RecyclerView.Adapter<MarkerListAdapter.ViewHolder> {

    private static final String TAG = MarkerListAdapter.class.getSimpleName();

    private Context context;
    private List<DbGeofence> list;
    private OnItemClickListener onItemClickListener;

    public MarkerListAdapter(Context context, List<DbGeofence> list,
     OnItemClickListener onItemClickListener) {
        this.context = context;
        this.list = list;
        this.onItemClickListener = onItemClickListener;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        
        private TextView name;
        private TextView location;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            location = itemView.findViewById(R.id.location);

        }

        public void bind(final DbGeofence model,
                         final OnItemClickListener listener) {
            name.setText(model.getName());
            location.setText(new LatLng(model.getLatitude(),model.getLongitude()).toString());
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.item_recycler, parent, false);
    
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DbGeofence item = list.get(position);
        
        holder.bind(item, onItemClickListener);
    }


    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

}