package com.posbillingapp.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.posbillingapp.databinding.ItemMenuItemBinding;
import com.posbillingapp.models.MenuItemModel;

import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {

    private List<MenuItemModel> items;
    private OnMenuItemActionListener listener;
    
    public interface OnMenuItemActionListener {
        void onEdit(MenuItemModel item);
        void onDelete(long id);
        // We can add onToggleAvailability if needed, but for now Edit covers it
    }

    public MenuAdapter(List<MenuItemModel> items, OnMenuItemActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMenuItemBinding binding = ItemMenuItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItemModel item = items.get(position);
        holder.binding.tvItemName.setText(item.getName());
        holder.binding.tvItemCategory.setText("Category: " + item.getCategory());
        holder.binding.tvItemPrice.setText(String.format(com.posbillingapp.utils.CurrencyUtils.getCurrencySymbol() + "%.2f", item.getPrice()));

        if (!item.isAvailable()) {
            holder.binding.tvItemName.setTextColor(android.graphics.Color.LTGRAY);
            holder.binding.tvItemCategory.setTextColor(android.graphics.Color.LTGRAY);
            holder.binding.tvItemPrice.setTextColor(android.graphics.Color.LTGRAY);
             // Make image grayscale or dim if inactive? optional but good
             holder.binding.ivItemImage.setAlpha(0.5f);
        } else {
            holder.binding.tvItemName.setTextColor(android.graphics.Color.BLACK); 
            holder.binding.tvItemCategory.setTextColor(android.graphics.Color.DKGRAY);
            // Reset price color to primary? Hard to get ?attr/colorPrimary programmatically easily without context helper, 
            // so we stick to defined color or black/dark for now.
            holder.binding.tvItemPrice.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black)); 
            holder.binding.ivItemImage.setAlpha(1.0f);
        }
        
        // Load Image
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            String fullUrl = item.getImageUrl().startsWith("http") ? item.getImageUrl() 
                             : com.posbillingapp.network.RetrofitClient.getBaseUrl() + item.getImageUrl().replaceAll("^/", "");
                             
             // Create a request option to cl ear the tint
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                .load(fullUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.binding.ivItemImage);
            holder.binding.ivItemImage.setImageTintList(null); // Clear tint for real image
        } else {
            holder.binding.ivItemImage.setImageResource(android.R.drawable.ic_menu_gallery);
            holder.binding.ivItemImage.setImageTintList(android.content.res.ColorStateList.valueOf(0xFFCCCCCC)); // Light Grey
        }

        holder.binding.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(item);
        });
        
        holder.binding.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item.getId());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateList(List<MenuItemModel> newList) {
        this.items = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemMenuItemBinding binding;
        public ViewHolder(ItemMenuItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
