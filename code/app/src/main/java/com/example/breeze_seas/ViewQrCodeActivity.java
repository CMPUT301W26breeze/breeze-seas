package com.example.breeze_seas;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class ViewQrCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_qr_code);

        String eventId = getIntent().getStringExtra("eventId");
        String eventName = getIntent().getStringExtra("eventName");

        ((TextView) findViewById(R.id.tvEventName)).setText(eventName == null ? "" : eventName);

        ImageView iv = findViewById(R.id.ivQr);
        iv.setImageBitmap(makeQr("event:" + eventId));
    }

    private Bitmap makeQr(String content) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 800, 800);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}