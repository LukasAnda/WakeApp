package sk.lukasanda.wakeapp.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import sk.lukasanda.wakeapp.model.DbGeofence;
import sk.lukasanda.wakeapp.R;

public class MarkerListAdapter extends
        RecyclerView.Adapter<MarkerListAdapter.ViewHolder> {

    private static final String TAG = MarkerListAdapter.class.getSimpleName();
    
    private List<DbGeofence> list;
    private OnItemClickListener onItemClickListener;

    public MarkerListAdapter(List<DbGeofence> list,
     OnItemClickListener onItemClickListener) {
        this.list = list;
        this.onItemClickListener = onItemClickListener;
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        
        private TextView name;
        private TextView location;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            location = itemView.findViewById(R.id.location);

        }

        void bind(final DbGeofence model,
                         final OnItemClickListener listener) {
            name.setText(model.getName());
            location.setText(new LatLng(model.getLatitude(),model.getLongitude()).toString());
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    listener.onItemClick(getAdapterPosition());
                    return false;
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.item_recycler, parent, false);
    
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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