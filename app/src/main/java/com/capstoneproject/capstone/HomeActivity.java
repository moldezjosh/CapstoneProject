package com.capstoneproject.capstone;

/**
 * Created by moldezjosh on 4/09/2019.
 */

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {
    Button btnSignOut, btnLogo, btnSticker;
    String dataset;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_home);

        //Get Firebase auth instance
        mAuth = FirebaseAuth.getInstance();

        btnLogo = findViewById(R.id.btn_mac_logo);
        btnSticker = findViewById(R.id.btn_bottom_sticker);
        btnSignOut = findViewById(R.id.btn_sign_out);


        btnSignOut = findViewById(R.id.btn_sign_out);
        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                finish();
            }
        });

        btnLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataset = "maclogo";

                Intent i = new Intent(HomeActivity.this, ImageProcActivity.class);
                Bundle extras = new Bundle();
                extras.putString("dataset", dataset);
                i.putExtras(extras);
                startActivity(i);
                finish();
            }
        });

        btnSticker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataset = "macsticker";

                Intent i = new Intent(HomeActivity.this, ImageProcActivity.class);
                Bundle extras = new Bundle();
                extras.putString("dataset", dataset);
                i.putExtras(extras);
                startActivity(i);
                finish();
            }
        });
    }
}
