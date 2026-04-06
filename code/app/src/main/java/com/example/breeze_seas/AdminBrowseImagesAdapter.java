package com.example.breeze_seas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for the image gallery in the AdminBrowseImages screen.
 * Displays each image as a tile with a delete button overlaid in the corner.
 */
public class AdminBrowseImagesAdapter extends RecyclerView.Adapter<AdminBrowseImagesAdapter.ImageViewHolder> {

    /**
     * Receives delete actions for one image tile in the admin image browser.
     */
    public interface OnImageDeleteListener {
        void onImageDelete(Image image);
    }

    private final List<Image> imageList;
    private final OnImageDeleteListener deleteListener;

    public AdminBrowseImagesAdapter(List<Image> imageList, OnImageDeleteListener deleteListener) {
        this.imageList = imageList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Image image = imageList.get(position);

        if (image.getImageData() != null) {
            // Clear the tint to show image
            holder.ivImage.setImageTintList(null);
            holder.ivImage.setImageBitmap(image.getImageData());
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && deleteListener != null) {
                deleteListener.onImageDelete(imageList.get(currentPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    /**
     * Replaces the current list contents and refreshes the grid.
     * Called by the fragment whenever the LiveData updates.
     */
    public void setImages(List<Image> newImages) {
        imageList.clear();
        imageList.addAll(newImages);
        notifyDataSetChanged();
    }

    /**
     * Holds the image preview and delete affordance for one admin gallery tile.
     */
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageButton btnDelete;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.abi_iv_gallery_image);
            btnDelete = itemView.findViewById(R.id.abi_btn_delete_image);
        }
    }
}
