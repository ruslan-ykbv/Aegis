package com.example.passwordmanagersql;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.PasswordHolder> {
    List<PasswordEntry> passwords = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnLongItemClickListener {
        void onLongItemClick(int position);
    }

    private OnLongItemClickListener longClickListener;


    public void setOnLongItemClickListener(OnLongItemClickListener listener) {
        this.longClickListener = listener;
    }


    private OnEditItemClickListener editItemClickListener;

    public interface OnEditItemClickListener {
        void onEditItemClick(PasswordEntry passwordEntry);
    }

    public void setOnEditItemClickListener(OnEditItemClickListener listener) {
        this.editItemClickListener = listener;
    }



    @NonNull
    @Override
    public PasswordHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.password_item, parent, false);
        return new PasswordHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PasswordHolder holder, int position) {
        PasswordEntry currentPassword = passwords.get(position);
        holder.textViewWebsite.setText(currentPassword.getWebsite());
        holder.textViewUsername.setText(currentPassword.getUsername());
        holder.buttonShowPassword.setOnClickListener(v -> {
            if (listener != null && position != RecyclerView.NO_POSITION) {
                listener.onShowPasswordClick(currentPassword);
            }
        });


        holder.buttonEditPassword.setOnClickListener(v -> {
            if (editItemClickListener != null && position != RecyclerView.NO_POSITION) {
                editItemClickListener.onEditItemClick(currentPassword);
            }
        });
    }

    @Override
    public int getItemCount() {
        return passwords.size();
    }

    public void submitList(List<PasswordEntry> passwords) {
        this.passwords = passwords;
        notifyDataSetChanged();
    }



    class PasswordHolder extends RecyclerView.ViewHolder {
        private final TextView textViewWebsite;
        private final TextView textViewUsername;
        private final ImageView buttonShowPassword;

        private final ImageView buttonEditPassword;

        public PasswordHolder(View itemView) {
            super(itemView);
            textViewWebsite = itemView.findViewById(R.id.text_view_website);
            textViewUsername = itemView.findViewById(R.id.text_view_username);
            buttonShowPassword = itemView.findViewById(R.id.button_show_password);
            buttonEditPassword = itemView.findViewById(R.id.button_edit_password);

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    longClickListener.onLongItemClick(getAdapterPosition());
                }
                return true;
            });
        }
    }

    public interface OnItemClickListener {
        void onShowPasswordClick(PasswordEntry passwordEntry);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
