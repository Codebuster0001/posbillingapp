package com.posbillingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.posbillingapp.R;
import com.posbillingapp.models.PermissionModels;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermissionManagementAdapter extends RecyclerView.Adapter<PermissionManagementAdapter.ViewHolder> {

    private List<PermissionModels.Permission> allPermissions;
    private Set<String> activePermissionKeys;

    public PermissionManagementAdapter(List<PermissionModels.Permission> allPermissions, Set<String> activePermissionKeys) {
        this.allPermissions = allPermissions;
        this.activePermissionKeys = activePermissionKeys != null ? activePermissionKeys : new HashSet<>();
    }

    public void updateActivePermissions(Set<String> newKeys) {
        this.activePermissionKeys = newKeys != null ? newKeys : new HashSet<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_permission, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PermissionModels.Permission permission = allPermissions.get(position);
        
        holder.cbPermission.setText(permission.permissionKey.replace("_", " "));
        holder.tvDescription.setText(permission.description);
        
        holder.cbPermission.setOnCheckedChangeListener(null); // Clear listener before setting checked state
        holder.cbPermission.setChecked(activePermissionKeys.contains(permission.permissionKey));
        
        holder.cbPermission.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                activePermissionKeys.add(permission.permissionKey);
            } else {
                activePermissionKeys.remove(permission.permissionKey);
            }
        });
    }

    @Override
    public int getItemCount() {
        return allPermissions != null ? allPermissions.size() : 0;
    }

    public List<String> getSelectedKeys() {
        return new ArrayList<>(activePermissionKeys);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbPermission;
        TextView tvDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbPermission = itemView.findViewById(R.id.cbPermission);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}
