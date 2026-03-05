package com.posbillingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.posbillingapp.R;
import com.posbillingapp.models.BillingModels.OrderHistoryResponse;
import com.posbillingapp.utils.CurrencyUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReceiptListAdapter extends RecyclerView.Adapter<ReceiptListAdapter.ReceiptViewHolder> {

    private List<OrderHistoryResponse> receiptList;
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    public ReceiptListAdapter(List<OrderHistoryResponse> receiptList) {
        this.receiptList = receiptList;
    }

    public void updateList(List<OrderHistoryResponse> newList) {
        receiptList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReceiptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receipt, parent, false);
        return new ReceiptViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReceiptViewHolder holder, int position) {
        OrderHistoryResponse receipt = receiptList.get(position);

        holder.tvBillNumber.setText((receipt.billNumber != null && !receipt.billNumber.isEmpty()) ? receipt.billNumber : "Bill #" + receipt.id);
        holder.tvCreator.setText(receipt.userName + " (" + receipt.userRole + ")");
        holder.tvAmount.setText(CurrencyUtils.format(receipt.totalAmount));

        try {
            if (receipt.createdAt != null) {
                Date date = inputFormat.parse(receipt.createdAt);
                if (date != null) {
                    holder.tvDate.setText(outputFormat.format(date));
                }
            } else {
                holder.tvDate.setText("Unknown Date");
            }
        } catch (Exception e) {
            holder.tvDate.setText(receipt.createdAt != null ? receipt.createdAt : "Unknown Date");
        }

        holder.llItemsContainer.removeAllViews();
        if (receipt.items != null && !receipt.items.isEmpty()) {
            holder.llItemsContainer.setVisibility(View.VISIBLE);
            for (com.posbillingapp.models.BillingModels.OrderItemResponse item : receipt.items) {
                // Dynamically create item views
                android.widget.LinearLayout itemLayout = new android.widget.LinearLayout(holder.itemView.getContext());
                itemLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                itemLayout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                itemLayout.setPadding(0, 4, 0, 4);

                TextView tvName = new TextView(holder.itemView.getContext());
                tvName.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                tvName.setText(item.quantity + "x " + item.itemName);
                tvName.setTextSize(13);
                tvName.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));

                TextView tvPrice = new TextView(holder.itemView.getContext());
                tvPrice.setLayoutParams(new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                tvPrice.setText(CurrencyUtils.format(item.price * item.quantity));
                tvPrice.setTextSize(13);
                tvPrice.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));

                itemLayout.addView(tvName);
                itemLayout.addView(tvPrice);
                holder.llItemsContainer.addView(itemLayout);
            }
        } else {
            holder.llItemsContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return receiptList.size();
    }

    static class ReceiptViewHolder extends RecyclerView.ViewHolder {
        TextView tvBillNumber, tvCreator, tvDate, tvAmount;
        android.widget.LinearLayout llItemsContainer;

        public ReceiptViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBillNumber = itemView.findViewById(R.id.tvBillNumber);
            tvCreator = itemView.findViewById(R.id.tvCreator);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            llItemsContainer = itemView.findViewById(R.id.llItemsContainer);
        }
    }
}
