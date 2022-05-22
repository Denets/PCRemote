package com.denets.PCRemoteClient;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FavoriteGroupAdapter extends RecyclerView.Adapter<FavoriteGroupAdapter.FavoriteGroupVH> {
    ArrayList<String> groups;
    ArrayList<ArrayList<String>> apps;

    public FavoriteGroupAdapter(ArrayList<String> groups, ArrayList<ArrayList<String>> apps){
        this.groups = groups;
        this.apps = apps;
    }

    public void openLv(@NonNull FavoriteGroupVH holder){
        System.out.println("Click");

        if(holder.lv.getVisibility() == View.VISIBLE)
            holder.lv.setVisibility(View.GONE);
        else
            holder.lv.setVisibility(View.VISIBLE);

        holder.lv.getLayoutParams().height = 120 * holder.lv.getAdapter().getCount() + holder.lv.getDividerHeight()*(holder.lv.getAdapter().getCount());
    }

    @NonNull
    @Override
    public FavoriteGroupVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View group = LayoutInflater.from(parent.getContext()).inflate(R.layout.favorite_group, parent, false);
        return new FavoriteGroupVH(group);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteGroupVH holder, int position) {
        TextView textView = holder.textView;
        textView.setText(groups.get(position));
        textView.setOnClickListener(view -> openLv(holder));

        ListView lv = holder.lv;
        lv.setAdapter(new ArrayAdapter<>(holder.itemView.getContext(), R.layout.my_list_item, apps.get(position)));

        lv.setOnItemClickListener((parent, view, pos, id) -> MainActivity.mainActivity.openFavorite(apps.get(position).get(pos)));

        holder.itemView.setMinimumHeight(120 * apps.get(position).size() + holder.textView.getHeight());
    }

    @Override
    public int getItemCount() { try{return groups.size();}catch (Exception e){return 0;}}

    class FavoriteGroupVH extends RecyclerView.ViewHolder {
        public TextView textView;
        public ListView lv;

        public FavoriteGroupVH(View itemView){
            super(itemView);
            textView = itemView.findViewById(R.id.txtGroup);
            lv = itemView.findViewById(R.id.lvFavorites);
        }
    }
}
