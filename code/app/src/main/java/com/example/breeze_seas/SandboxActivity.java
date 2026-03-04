package com.example.breeze_seas;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SandboxActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sandbox); // Create a totally empty layout with a FrameLayout holding ID R.id.fragment_container

        // Load your fragment directly!
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LogsFragment())
                    .commit();
        }
    }
}