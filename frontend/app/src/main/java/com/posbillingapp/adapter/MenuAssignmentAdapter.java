package com.posbillingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.posbillingapp.databinding.ItemMenuAssignmentBinding;
import com.posbillingapp.models.MenuItemModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MenuAssignmentAdapter extends RecyclerView.Adapter<MenuAssignmentAdapter.ViewHolder> {

    private List<MenuItemModel> items;
    private Set<Long> selectedItems;
    private boolean isSingleSelection;

    public MenuAssignmentAdapter(List<MenuItemModel> items, Set<Long> preSelected, boolean isSingleSelection) {
        this.items = items;
        this.selectedItems = new HashSet<>(preSelected);
        this.isSingleSelection = isSingleSelection;
        
        // Ensure only one item is selected if in single selection mode
        if (isSingleSelection && this.selectedItems.size() > 1) {
            Long first = this.selectedItems.iterator().next();
            this.selectedItems.clear();
            this.selectedItems.add(first);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMenuAssignmentBinding binding = ItemMenuAssignmentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItemModel item = items.get(position);
        holder.binding.cbMenuItem.setText(item.getName());
        holder.binding.tvPrice.setText(String.format("$%.2f", item.getPrice()));
        
        // Remove listener before setting checked state to avoid recursion
        holder.binding.cbMenuItem.setOnCheckedChangeListener(null);
        holder.binding.cbMenuItem.setChecked(selectedItems.contains(item.getId()));
        
        holder.binding.cbMenuItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (isSingleSelection) {
                    selectedItems.clear();
                    selectedItems.add(item.getId());
                    notifyDataSetChanged(); // Refresh all views to show only one selection
                } else {
                    selectedItems.add(item.getId());
                }
            } else {
                selectedItems.remove(item.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public Set<Long> getSelectedMenuItems() {
        return selectedItems;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemMenuAssignmentBinding binding;

        ViewHolder(ItemMenuAssignmentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
