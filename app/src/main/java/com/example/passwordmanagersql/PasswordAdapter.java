package com.example.passwordmanagersql;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
        holder.buttonShowPassword.setOnClickListener(v -> {
            if (listener != null && position != RecyclerView.NO_POSITION) {
                listener.onShowPasswordClick(currentPassword);
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
        private TextView textViewWebsite;
        private Button buttonShowPassword;

        public PasswordHolder(View itemView) {
            super(itemView);
            textViewWebsite = itemView.findViewById(R.id.text_view_website);
            buttonShowPassword = itemView.findViewById(R.id.button_show_password);

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
