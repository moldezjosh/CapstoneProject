package com.capstoneproject.capstone;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

/**
 * Created by moldezjosh on 3/22/2019.
 */

public class ResultActivity extends AppCompatActivity {
    private static String TAG = "ResultActivity";
    ImageView image;
    Button btnHome;
    Bitmap result;
    private static int ALERT_TIME_OUT=2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);


        result = ImageProcActivity.queryImage();
        final Bundle extras = getIntent().getExtras();


        final String authenticity = extras.getString("authenticity");
        TextView authRateView = findViewById(R.id.auth_rate);
        authRateView.setText(authenticity);

        final String detail = extras.getString("detail");
        TextView detView = findViewById(R.id.detail);
        detView.setText(detail);


        if(authenticity.equals("AUTHENTIC")){
            authRateView.setTextColor(Color.GREEN);
        }else if(authenticity.equals("COUNTERFEIT")){
            authRateView.setTextColor(Color.RED);
            detView.setText(detail);
        }


        image = findViewById(R.id.resultImg);

        //match result
        final String matchResult = extras.getString("result");
        TextView info = findViewById(R.id.information);
        info.setText(matchResult);

        image.setImageBitmap(result);

        btnHome = findViewById(R.id.btn_home);
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ResultActivity.this, HomeActivity.class));
                finish();
            }
        });

        new Handler().
                postDelayed(new Runnable(){
            @Override
            public void run(){
                if(authenticity.equals("COUNTERFEIT")){
                    warning(extras);
                }
            }
        }, ALERT_TIME_OUT);
    }

    private void warning(final Bundle extras){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ResultActivity.this);

        alertDialogBuilder
                .setMessage("A counterfeit of MAC Bullet Lipstick was detected. Do you want to report it?")
                .setCancelable(false)
                .setPositiveButton("Report",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, int which) {
                                Intent i = new Intent(ResultActivity.this, ReportActivity.class);
                                i.putExtras(extras);
                                startActivity(i);
                                finish();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                dialog.cancel();
                            }
                        })
                .setTitle("Warning!")
                .setIcon(R.drawable.ic_warning_black_24dp);
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }
}
