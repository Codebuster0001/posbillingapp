package com.posbillingapp.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.posbillingapp.databinding.ItemBillingProductBinding;
import com.posbillingapp.models.MenuItemModel;
import java.util.ArrayList;
import java.util.List;

import android.view.View;
import java.util.HashMap;
import java.util.Map;

public class BillingProductAdapter extends RecyclerView.Adapter<BillingProductAdapter.ViewHolder> {

    private List<MenuItemModel> items;
    private OnCartUpdateListener listener;
    private Map<Long, Integer> cartItems = new HashMap<>();

    public interface OnCartUpdateListener {
        void onCartUpdate(MenuItemModel product, int quantity);
    }

    public BillingProductAdapter(List<MenuItemModel> items, OnCartUpdateListener listener) {
        this.items = items != null ? items : new ArrayList<>();
        this.listener = listener;
    }

    public Map<Long, Integer> getCartItems() {
        return cartItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBillingProductBinding binding = ItemBillingProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItemModel item = items.get(position);
        holder.binding.tvProductName.setText(item.getName());
        holder.binding.tvProductCategory.setText(item.getCategory());
        holder.binding.tvProductPrice.setText(String.format(com.posbillingapp.utils.CurrencyUtils.getCurrencySymbol() + "%.2f", item.getPrice()));

        // Load Image with Glide
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            String fullUrl = item.getImageUrl().startsWith("http") ? item.getImageUrl() 
                             : com.posbillingapp.network.RetrofitClient.getBaseUrl() + item.getImageUrl().replaceAll("^/", "");
            
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                .load(fullUrl)
                .placeholder(android.R.drawable.ic_menu_gallery) 
                .error(android.R.drawable.ic_menu_gallery)      
                .into(holder.binding.imgProduct);
            holder.binding.imgProduct.setImageTintList(null); // Clear tint!
        } else {
            holder.binding.imgProduct.setImageResource(android.R.drawable.ic_menu_gallery);
            holder.binding.imgProduct.setImageTintList(android.content.res.ColorStateList.valueOf(0xFFCCCCCC)); // Re-apply tint for placeholder
        }

        int qty = cartItems.getOrDefault(item.getId(), 0);

        // ... (rest of logic)
        
        // Ensure visibility updates happen correctly based on QTY
        if (qty > 0) {
            holder.binding.btnAdd.setVisibility(View.GONE);
            holder.binding.layoutQty.setVisibility(View.VISIBLE);
            holder.binding.tvQuantity.setText(String.valueOf(qty));
        } else {
            holder.binding.btnAdd.setVisibility(View.VISIBLE);
            holder.binding.layoutQty.setVisibility(View.GONE);
        }

        holder.binding.btnAdd.setOnClickListener(v -> updateQuantity(item, 1));
        holder.binding.btnPlus.setOnClickListener(v -> updateQuantity(item, cartItems.getOrDefault(item.getId(), 0) + 1));
        holder.binding.btnMinus.setOnClickListener(v -> updateQuantity(item, cartItems.getOrDefault(item.getId(), 0) - 1));
    }

    private void updateQuantity(MenuItemModel item, int newQty) {
        if (newQty < 0) return;
        
        if (newQty == 0) {
            cartItems.remove(item.getId());
        } else {
            cartItems.put(item.getId(), newQty);
        }
        
        notifyItemChanged(items.indexOf(item));
        if (listener != null) listener.onCartUpdate(item, newQty);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateList(List<MenuItemModel> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemBillingProductBinding binding;

        public ViewHolder(ItemBillingProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
