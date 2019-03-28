package com.capstoneproject.capstone;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ResultActivity extends AppCompatActivity {
    private static String TAG = "ResultActivity";
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReferenceReport;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    Toast toast;
    ImageView image;
    Bitmap result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        firebaseDatabase = FirebaseDatabase.getInstance();

        firebaseStorage = FirebaseStorage.getInstance();

        toast = null;

        result = ImageProcActivity.queryImage();
        Bundle extras = getIntent().getExtras();
//        final String label = extras.getString("label");
//        final String email = extras.getString("email");
        final String authenticity = extras.getString("authenticity");
        final String[] location = extras.getStringArray("location");

        TextView locationView = findViewById(R.id.location);
        locationView.setText(location[0] + ", " + location[1]);

        TextView authRateView = findViewById(R.id.auth_rate);
        authRateView.setText(authenticity);
        image = findViewById(R.id.resultImg);

        image.setImageBitmap(result);
    }
}
