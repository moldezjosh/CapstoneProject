package com.capstoneproject.capstone;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
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
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReferenceReport;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    Toast toast;
    ImageView image;
    Bitmap result;
    private static int ALERT_TIME_OUT=2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        firebaseDatabase = FirebaseDatabase.getInstance();

        firebaseStorage = FirebaseStorage.getInstance();

        result = ImageProcActivity.queryImage();
        Bundle extras = getIntent().getExtras();
//        final String label = extras.getString("label");
//        final String email = extras.getString("email");
        final String authenticity = extras.getString("authenticity");
        final String[] location = extras.getStringArray("location");
        final String email = "sample@gmail.com";
        TextView locationView = findViewById(R.id.location);
        locationView.setText(location[0] + ", " + location[1]);

        TextView authRateView = findViewById(R.id.auth_rate);
        authRateView.setText(authenticity);
        image = findViewById(R.id.resultImg);

        image.setImageBitmap(result);

        new Handler().
                postDelayed(new Runnable(){
            @Override
            public void run(){
                if(authenticity.equals("Counterfeit MAC")){
                    Report data = new Report(location[0], location[1], authenticity, email, null);
                    warning(data);
                }
            }
        }, ALERT_TIME_OUT);
    }

    private void warning(final Report data){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ResultActivity.this);

        alertDialogBuilder
                .setMessage("A counterfeit of MAC Bullet Lipstick was detected. Do you want to report it?")
                .setCancelable(false)
                .setPositiveButton("Report",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, int which) {
                                sendReport(data);
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

    private void sendReport(final Report data) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Reporting...");
        progressDialog.show();

        databaseReferenceReport = firebaseDatabase.getReference("reports");
        final String primaryKey = databaseReferenceReport.push().getKey();
        databaseReferenceReport.keepSynced(true);

        storageReference = firebaseStorage.getReference("reports");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] resultByte = baos.toByteArray();

        final UploadTask uploadTask = storageReference.child(primaryKey+".jpg").putBytes(resultByte);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                String url = taskSnapshot.getMetadata().toString();
                data.setUrl(url);
                progressDialog.dismiss();
                databaseReferenceReport.child(primaryKey).setValue(data);
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100*taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                        progressDialog.setMessage("Uploaded " + (int)progress + "%");
                    }
                });


        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                uploadTask.cancel();
                databaseReferenceReport.child(primaryKey).setValue(data);
            }
        });

        databaseReferenceReport.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
//                String value = dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                toast.cancel();
                toast = Toast.makeText(getApplicationContext(), "Failed to read value.", Toast.LENGTH_SHORT);
                toast.show();

                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
}
