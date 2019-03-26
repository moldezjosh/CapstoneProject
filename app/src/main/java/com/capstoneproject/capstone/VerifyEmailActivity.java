package com.capstoneproject.capstone;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class VerifyEmailActivity extends AppCompatActivity {
    private Button btn_load;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_verify);

        btn_load = (Button) findViewById(R.id.btn_reload);

        btn_load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(VerifyEmailActivity.this, LoginActivity.class));
            }
        });
    }
}
