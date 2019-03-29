package com.capstoneproject.capstone;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by moldezjosh on 3/29/2019.
 */

public class ReportActivity extends AppCompatActivity {
    private static String TAG = "ReportActivity";
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReferenceReport;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    Bitmap result;
    Toast toast;
    EditText inputlocation, inputproduct;
    Spinner inlocationtype, inputdetail;
    String locationtype, detail, email, inlocation, inproduct;
    String[] location;
    Button submitBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_report);

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        result = ImageProcActivity.queryImage();
        Bundle extras = getIntent().getExtras();

        inputlocation = findViewById(R.id.location_et);
        inlocationtype = findViewById(R.id.location_spin);
        inputproduct = findViewById(R.id.product);
        inputdetail = findViewById(R.id.detail_spin);

        locationtype = inlocationtype.getSelectedItem().toString();
        detail = inputdetail.getSelectedItem().toString();

        location = extras.getStringArray("location");
        email = extras.getString("email");

        String loc;
        try {
            loc = getAddress(location);
            inputlocation.setText(loc);
        } catch (IOException ex) {
            // Error occurred while creating the File
            ex.printStackTrace();
        }

        inputproduct.setText("MAC Bullet Lipstick");
        inlocation = inputlocation.getText().toString();
        inproduct = inputproduct.getText().toString();


        submitBtn = findViewById(R.id.btn_submit);

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Report rep = new Report(inlocation, location[0], location[1], locationtype, inproduct, detail, email, null);
                sendReport(rep);
            }
        });
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

    private String getAddress(String[] loc) throws IOException {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        addresses = geocoder.getFromLocation(Double.valueOf(loc[0]), Double.parseDouble(loc[1]), 1);

        String address = addresses.get(0).getAddressLine(0);

        return address;
    }
}
