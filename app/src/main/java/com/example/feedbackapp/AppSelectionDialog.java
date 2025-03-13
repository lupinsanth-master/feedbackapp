package com.example.feedbackapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppSelectionDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_app_selection, null);
        RecyclerView recyclerView = view.findViewById(R.id.app_list_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        PackageManager pm = getActivity().getPackageManager();
        List<ApplicationInfo> appList = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        AppListAdapter adapter = new AppListAdapter(appList, pm);
        adapter.setOnAppSelectedListener(appInfo -> {
            ((MainActivity) getActivity()).onAppSelected(appInfo);
            dismiss();
        });
        recyclerView.setAdapter(adapter);

        builder.setView(view)
                .setTitle("选择应用")
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }
}