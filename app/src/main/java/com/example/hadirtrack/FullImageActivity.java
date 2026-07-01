package com.example.hadirtrack;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class FullImageActivity extends AppCompatActivity {

    ImageView fullImageView;
    Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        fullImageView = findViewById(R.id.fullImageView);
        backButton = findViewById(R.id.backButton);

        String imageUrl = getIntent().getStringExtra("imageUrl");

        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "Image not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Glide.with(this)
                .load(imageUrl)
                .into(fullImageView);

        backButton.setOnClickListener(v -> finish());
    }
}