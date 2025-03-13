package com.example.feedbackapp;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    private List<ApplicationInfo> appList;
    private PackageManager pm;
    private OnAppSelectedListener listener;

    public AppListAdapter(List<ApplicationInfo> appList, PackageManager pm) {
        this.appList = appList;
        this.pm = pm;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ApplicationInfo appInfo = appList.get(position);
        holder.appName.setText(pm.getApplicationLabel(appInfo));
        holder.appIcon.setImageDrawable(pm.getApplicationIcon(appInfo));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppSelected(appInfo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public void setOnAppSelectedListener(OnAppSelectedListener listener) {
        this.listener = listener;
    }

    public interface OnAppSelectedListener {
        void onAppSelected(ApplicationInfo appInfo);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;

        public ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
        }
    }
}