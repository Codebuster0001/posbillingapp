package com.posbillingapp.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.posbillingapp.databinding.ItemEmployeeBinding;
import com.posbillingapp.models.EmployeeModel;

import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {

    private List<EmployeeModel> employees;
    private OnEmployeeActionListener listener;

    public interface OnEmployeeActionListener {
        void onEdit(EmployeeModel employee);
        void onDelete(long id);
        void onAssignItems(EmployeeModel employee);
    }

    public EmployeeAdapter(List<EmployeeModel> employees, OnEmployeeActionListener listener) {
        this.employees = employees;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEmployeeBinding binding = ItemEmployeeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmployeeModel emp = employees.get(position);
        holder.binding.tvEmpName.setText(emp.getName());
        holder.binding.tvEmpRole.setText("Role: " + emp.getRole());
        holder.binding.tvEmpEmail.setText(emp.getEmail());
        
        // Show assign button only for Limited Access
        if ("Limited Access".equalsIgnoreCase(emp.getRole())) {
            holder.binding.btnAssignItems.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.binding.btnAssignItems.setVisibility(android.view.View.GONE);
        }
        
        holder.binding.btnAssignItems.setOnClickListener(v -> {
            if (listener != null) listener.onAssignItems(emp);
        });
        
        holder.binding.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(emp);
        });

        holder.binding.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(emp.getId());
        });
    }

    @Override
    public int getItemCount() {
        return employees.size();
    }

    public void updateList(List<EmployeeModel> newList) {
        this.employees = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemEmployeeBinding binding;
        public ViewHolder(ItemEmployeeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
