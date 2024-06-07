package com.example.passwordmanagersql;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PasswordAdapter extends ListAdapter<PasswordEntry, PasswordAdapter.PasswordHolder> {
    private OnItemClickListener listener;

    public PasswordAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<PasswordEntry> DIFF_CALLBACK = new DiffUtil.ItemCallback<PasswordEntry>() {
        @Override
        public boolean areItemsTheSame(@NonNull PasswordEntry oldItem, @NonNull PasswordEntry newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull PasswordEntry oldItem, @NonNull PasswordEntry newItem) {
            return oldItem.website.equals(newItem.website) && oldItem.encryptedPassword.equals(newItem.encryptedPassword);
        }
    };

    @NonNull
    @Override
    public PasswordHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.password_item, parent, false);
        return new PasswordHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PasswordHolder holder, int position) {
        PasswordEntry currentPassword = getItem(position);
        holder.textViewWebsite.setText(currentPassword.website);
    }

    public PasswordEntry getPasswordAt(int position) {
        return getItem(position);
    }

    class PasswordHolder extends RecyclerView.ViewHolder {
        private TextView textViewWebsite;

        public PasswordHolder(View itemView) {
            super(itemView);
            textViewWebsite = itemView.findViewById(R.id.text_view_website);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(position));
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(PasswordEntry passwordEntry);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
