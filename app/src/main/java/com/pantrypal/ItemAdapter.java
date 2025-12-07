package com.pantrypal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.pantrypal.model.Item;

import java.util.List;

public class ItemAdapter extends ArrayAdapter<Item> {

    private final Context context;
    private final List<Item> items;

    public ItemAdapter(@NonNull Context context, @NonNull List<Item> items) {
        super(context, 0, items);
        this.context = context;
        this.items = items;
    }

    // Interface for delete callback
    public interface OnDeleteClickListener {
        void onDeleteClick(Item item);
    }

    private OnDeleteClickListener deleteListener;

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.list_item_pantry, parent, false);
        }

        Item currentItem = items.get(position);

        // Find views
        TextView tvName = listItem.findViewById(R.id.tvItemName);
        TextView tvQuantity = listItem.findViewById(R.id.tvItemQuantity);
        TextView tvExpiry = listItem.findViewById(R.id.tvItemExpiry);
        View viewExpiryIndicator = listItem.findViewById(R.id.viewExpiryIndicator);
        android.widget.ImageButton btnDelete = listItem.findViewById(R.id.btnDeleteItem);

        // Set item name
        tvName.setText(currentItem.getName());

        // Handle delete button click
        btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(currentItem);
            }
        });

        // Set quantity with unit
        String quantityText;
        if (currentItem.getUnit().equalsIgnoreCase("pcs")) {
            // Show as integer for pieces
            quantityText = String.format("%d %s", (int) currentItem.getQuantity(), currentItem.getUnit());
        } else {
            // Show with decimal for other units
            quantityText = String.format("%.1f %s", currentItem.getQuantity(), currentItem.getUnit());
        }
        tvQuantity.setText(quantityText);

        // Calculate days until expiry
        long daysUntilExpiry = currentItem.getDaysUntilExpiry();

        // Set expiry text and color indicator
        String expiryText;
        int indicatorColor;

        if (daysUntilExpiry < 0) {
            // Expired
            expiryText = "Expired " + Math.abs(daysUntilExpiry) + " days ago";
            indicatorColor = ContextCompat.getColor(context, android.R.color.holo_red_dark);
        } else if (daysUntilExpiry == 0) {
            // Expires today
            expiryText = "Expires TODAY!";
            indicatorColor = ContextCompat.getColor(context, android.R.color.holo_orange_dark);
        } else if (daysUntilExpiry <= 3) {
            // Expiring soon (1-3 days)
            expiryText = "Expires in " + daysUntilExpiry + " day" + (daysUntilExpiry > 1 ? "s" : "");
            indicatorColor = ContextCompat.getColor(context, android.R.color.holo_orange_light);
        } else if (daysUntilExpiry <= 7) {
            // Expiring this week (4-7 days)
            expiryText = "Expires in " + daysUntilExpiry + " days";
            indicatorColor = ContextCompat.getColor(context, android.R.color.holo_orange_light);
        } else {
            // Fresh (8+ days)
            expiryText = "Expires in " + daysUntilExpiry + " days";
            indicatorColor = ContextCompat.getColor(context, android.R.color.holo_green_dark);
        }

        tvExpiry.setText(expiryText);
        viewExpiryIndicator.setBackgroundColor(indicatorColor);

        return listItem;
    }
}