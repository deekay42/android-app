package com.leagueiq.app;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.leagueiq.app.R;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private ArrayList<Integer> imgPaths;
    private ArrayList<String> itemNames;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView itemName;
        public TextView downArrow;
        public ImageView itemImg;
        public MyViewHolder(View v) {
            super(v);
            this.itemImg = (ImageView) v.findViewById(R.id.itemImg);
            this.itemName = (TextView) v.findViewById(R.id.itemName);
            this.downArrow = (TextView) v.findViewById(R.id.downArrow);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAdapter(ArrayList<Integer> paths, ArrayList<String> names) {
        this.imgPaths = paths;
        this.itemNames = names;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View v = inflater.inflate(R.layout.recycle_cell, parent, false);

        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.itemImg.setImageResource(imgPaths.get(position));
        holder.itemName.setText(itemNames.get(position));
        if(position == getItemCount() - 1)
            holder.downArrow.setVisibility(View.GONE);
        else
            holder.downArrow.setVisibility(View.VISIBLE);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return imgPaths.size();
    }
}