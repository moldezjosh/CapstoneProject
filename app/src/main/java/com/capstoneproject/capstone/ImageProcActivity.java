package com.capstoneproject.capstone;

/**
 * Created by moldezjosh on 3/15/2019.
 */

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
//import org.opencv.features2d.DMatch;
//import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
//import java.lang.annotation.Target;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImageProcActivity extends AbsRuntimePermission  {

    private static final String TAG = "MainActivity";
    ImageView imgView;
    Integer REQUEST_CAMERA = 1, SELECT_FILE = 0;
    private static Bitmap selectedImage, result, sampleBitmap, datasetBitmap;
    ProgressBar pbar;
    FloatingActionButton fab, auth;
    Mat Rgba, imgGray, sampleMat, datasetMat, graySampleMat, grayDatasetMat, sampleDescriptors, datasetDescriptors;
    Toast toast;
    FeatureDetector detector;
    private String mCurrentPhotoPath = null, authenticity, label = null;
    private static final int REQUEST_PERMISSION = 10;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Rgba = new Mat();
                    imgGray = new Mat();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private DescriptorExtractor descExtractor;
    private MatOfKeyPoint sampleKeypoints, datasetKeypoints;
    private MatOfDMatch matches;
    private DescriptorMatcher matcher;
    private Context context;
    private LocationManager locationManager;
    private static Location location;
    private double latitude, longitude;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReferenceDataset;
    private DatabaseReference connectedRef;
    Target targetDataset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imageproc);


        captureImage();
        imgView = findViewById(R.id.imgView);

    }

//    private void reqPermission() {
//        requestAppPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
//                R.string.msg,REQUEST_PERMISSION);
//    }

    @Override
    public void onPermissionsGranted(int requestCode) {
//        toast = Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG);
//        toast.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mCurrentPhotoPath != null) {
            Uri imageUri = Uri.parse(mCurrentPhotoPath);
            File file = new File(imageUri.getPath());

            if(file.exists()) {
                file.delete();
                mCurrentPhotoPath = null;
            }
        }
    }
    private void resultView() {
        FirebaseUser user = mAuth.getCurrentUser();
        Intent i = new Intent(getBaseContext(), ResultActivity.class);
        Bundle extras = new Bundle();
        extras.putString("label", label);
        extras.putString("email", user.getEmail());
        extras.putString("authenticity", authenticity);
        extras.putStringArray("location", new String[]{String.valueOf(latitude), String.valueOf(longitude)});
        i.putExtras(extras);
        getApplicationContext().startActivity(i);

        label = null;
        authenticity = null;
        latitude = 0;
        longitude = 0;

    }

    public static Bitmap queryImage() {
        return selectedImage;
    }

    private void captureImage() {
        final CharSequence[] items = {"Camera", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Capture Image");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(items[which].equals("Camera")) {
                    //reqPermission();

                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    // Ensure that there's a camera activity to handle the intent
                    if (intent.resolveActivity(getPackageManager()) != null) {

                        // Create the File where the photo should go
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                        } catch (IOException ex) {
                            // Error occurred while creating the File
                            ex.printStackTrace();
                        }

                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            Uri photoURI = FileProvider.getUriForFile(ImageProcActivity.this, BuildConfig.APPLICATION_ID + ".provider", photoFile);
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            startActivityForResult (intent, REQUEST_CAMERA);
                        }
                    }
                } else if(items[which].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });

        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode== Activity.RESULT_OK) {
            if(requestCode==REQUEST_CAMERA) {
                if(mCurrentPhotoPath != null) {
                    Uri imageUri = Uri.parse(mCurrentPhotoPath);
                    File file = new File(imageUri.getPath());

                    try {
                        InputStream ims = new FileInputStream(file);
                        selectedImage = BitmapFactory.decodeStream(ims);

                        if(selectedImage != null) {
//                            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//                            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
//                                buildAlertMessageNoGps();
//                            } else {
//                                getLocation();
//                            }
                        }

                        imgView.setImageBitmap(selectedImage);
                        mCurrentPhotoPath = null;
                    } catch (FileNotFoundException e) {
                        return;
                    }

                    // ScanFile so it will be appeared on Gallery
                    MediaScannerConnection.scanFile(ImageProcActivity.this,
                            new String[]{imageUri.getPath()}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.d(TAG, "onScanCompleted: " + path + " - " + uri);
                                    //load image in background
//                                new ImageLoaderClass().execute(path);
                                }
                            });
                }
            }
            //else if(requestCode==SELECT_FILE){
//                Uri selectImageUri = data.getData();
//                Log.d(TAG, "onScanCompleted: " + selectImageUri);
//
//                //path converted from Uri
//                String convertedPath = getRealPathFromURI(selectImageUri);
//
//                //load image in background
//                new ImageLoaderClass().execute(convertedPath);
//            }
        } else {
            if(mCurrentPhotoPath != null) {
                Uri imageUri = Uri.parse(mCurrentPhotoPath);
                File file = new File(imageUri.getPath());

                if(file.exists()) {
                    file.delete();
                    mCurrentPhotoPath = null;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        } else if (id == R.id.action_sign_out) {
//            mAuth.signOut();
//        }

        return super.onOptionsItemSelected(item);
    }

    public static int calculateImageSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int size = 1;

        if(height > reqHeight || width > reqWidth) {
            final int halfHeight = height/2;
            final int halfWidth = width/2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / size) >= reqHeight
                    && (halfWidth / size) >= reqWidth) {
                size *= 2;
            }
        }

        return size;
    }

    public static Bitmap decodeSampledBitmapFromResource(String path, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateImageSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };

        //This method was deprecated in API level 11
        CursorLoader cursorLoader = new CursorLoader(
                this,
                contentUri, proj, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        int column_index =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private class ImageProcessing extends AsyncTask<Bitmap, String, Boolean> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Rgba = new Mat();
            imgGray = new Mat();
            sampleMat = new Mat();
            datasetMat = new Mat();

            sampleKeypoints = new MatOfKeyPoint();
            datasetKeypoints = new MatOfKeyPoint();

            sampleDescriptors = new Mat();
            datasetDescriptors = new Mat();

            matches = new MatOfDMatch();

            progressDialog = new ProgressDialog(context);
            progressDialog.show();
            progressDialog.setCancelable(false);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            progressDialog.dismiss();

            if(aBoolean) {
                //resultView();
            } else {
                Toast.makeText(getApplicationContext(), "Query image is not qualified to continue the process. Try another one.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            progressDialog.setMessage(values[0]);
        }

        @Override
        protected Boolean doInBackground(Bitmap... bitmaps) {
            //Dataset image
            Bitmap bitmap = bitmaps[0];

            //Newly captured image
            sampleBitmap = selectedImage.copy(Bitmap.Config.ARGB_8888, true);

            publishProgress("Images Preprocessing");

            //Newly captured image preprocessing, image binarization
            Utils.bitmapToMat(sampleBitmap, sampleMat);
            Imgproc.cvtColor(sampleMat, sampleMat, Imgproc.COLOR_RGB2GRAY);
            Imgproc.adaptiveThreshold(sampleMat, sampleMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
            Imgproc.GaussianBlur(sampleMat, sampleMat, new Size(5,5), 0);
            Imgproc.threshold(sampleMat, sampleMat, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

            //Dataset image preprocessing, image binarization
            Utils.bitmapToMat(bitmap, datasetMat);
            Imgproc.cvtColor(datasetMat, datasetMat, Imgproc.COLOR_RGB2GRAY);
            Imgproc.adaptiveThreshold(datasetMat, datasetMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
            Imgproc.GaussianBlur(datasetMat, datasetMat, new Size(5,5), 0);
            Imgproc.threshold(datasetMat, datasetMat, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

            //initializing feature detector and descriptor
            detector = FeatureDetector.create(FeatureDetector.ORB);
            descExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

            //initializing feature matcher
            matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

            publishProgress("Scanning for stable image features");

            //detecting stable image keypoints
            detector.detect(sampleMat, sampleKeypoints);
            detector.detect(datasetMat, datasetKeypoints);

            //Describe keypoints, extract distinct image features
            descExtractor.compute(sampleMat, sampleKeypoints, sampleDescriptors);
            descExtractor.compute(datasetMat, datasetKeypoints, datasetDescriptors);

            publishProgress("Matching");

            //Matching extracted features
            matcher.match(datasetDescriptors, sampleDescriptors, matches);

            List<DMatch> matchList = matches.toList();
            ArrayList<DMatch> finalMatch = new ArrayList<>();

            double threshold = 70;

            //Method for finding best matches
            for (int a = 0; a < matchList.size(); a++) {
                if(matchList.get(a).distance <= threshold) {
                    finalMatch.add(matches.toList().get(a));
                }
            }

            MatOfDMatch finalMatchMat = new MatOfDMatch();
            finalMatchMat.fromList(finalMatch);
            List<DMatch> finalMatchList = finalMatchMat.toList();

            double intGMatch = finalMatchList.size();
            double fMatchAve = 0;

            for (int a = 0; a < intGMatch; a++) {
                fMatchAve = fMatchAve + finalMatchList.get(a).distance;
            }

            fMatchAve = fMatchAve/intGMatch;

            if(intGMatch <= 60) {
                return false;
            } else {
                if(fMatchAve >= 65) {
                    authenticity = "Counterfeit product";
                } else {
                    authenticity = "Authentic product";
                }
            }

            return true;
        }
    }

    private void getColor() {
        int r = 255, g = 165, b = 0;
        int width = selectedImage.getWidth();
        int height = selectedImage.getHeight();
        int Red = 0, Green = 0, Blue = 0;
        int pixelColor;
        int size = 0;

        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                pixelColor = selectedImage.getPixel(x, y);
                Red = Color.red(pixelColor);
                Green = Color.green(pixelColor);
                Blue = Color.blue(pixelColor);

                if((Red < r - 70 || Red > r + 70) && (Green < g - 70 || Green > g + 70) && (Blue < b - 70 || Blue > b + 70)) {
                    Red += Red;
                    Green += Green;
                    Blue += Blue;
                    size++;
                }
            }
        }

        Red /= size;
        Green /= size;
        Blue /= size;

        Log.d(TAG, "getColor: R="+Red+", G="+Green+", B="+Blue);
    }

    private class ImageLoaderClass extends AsyncTask<String, String, Bitmap> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pbar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if(bitmap != null) {
                imgView.setImageBitmap(bitmap);
                pbar.setVisibility(View.GONE);
            }

            if (selectedImage != null) {
//                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
//                    buildAlertMessageNoGps();
//                } else {
//                    getLocation();
//                }

                auth.setEnabled(true);
            } else {
                auth.setEnabled(false);
            }
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                selectedImage = decodeSampledBitmapFromResource(strings[0], 1000, 1000);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return selectedImage;
        }
    }

    private void hasDataset(final String drugname, final CustomCallback customCallback) {
        databaseReferenceDataset = firebaseDatabase.getReference("DATASETS");

        databaseReferenceDataset.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int dataSnapshotLength = (int) dataSnapshot.getChildrenCount();

                for (DataSnapshot dataset: dataSnapshot.getChildren()) {
                    String drugnameDataset = dataset.child("drug").getValue(String.class);
                    dataSnapshotLength--;

                    if(drugname.toLowerCase().contains(drugnameDataset.toLowerCase())){
                        final String url = dataset.child("url").getValue(String.class);
                        final Uri uri = Uri.parse(url);
                        label = drugnameDataset;

                        Picasso.get().load(uri).fetch(new Callback() {
                            @Override
                            public void onSuccess() {
                                customCallback.onCallback(url);
                            }

                            @Override
                            public void onError(Exception e) {
                                customCallback.onCallback(null);
                            }
                        });

                        return;
                    } else {
                        if(dataSnapshotLength == 0) {
                            customCallback.onCallback(null);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                customCallback.onCallback(null);
            }
        });
    }

    public void authProcess(final Uri datasetUri, final View view) {
        targetDataset = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                new ImageProcessing().execute(bitmap);

                pbar.setVisibility(View.GONE);

                if (selectedImage != null) {
                    auth.setEnabled(true);
                } else {
                    auth.setEnabled(false);
                }
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                if (selectedImage != null) {
                    auth.setEnabled(true);
                } else {
                    auth.setEnabled(false);
                }
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                Log.d(TAG, "onPrepareLoad: ");
                auth.setEnabled(false);
                pbar.setVisibility(View.VISIBLE);
            }
        };

        Picasso.get()
                .load(datasetUri)
                .fetch(new Callback() {
                    @Override
                    public void onSuccess() {
                        Picasso.get()
                                .load(datasetUri)
                                .memoryPolicy(MemoryPolicy.NO_CACHE)
                                .into(targetDataset);
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MAC");


        if(!storageDir.exists()){
            storageDir.mkdirs();
        }else if (storageDir.exists()) {
            storageDir.delete();
            storageDir.mkdirs();
        }

        File image = new File(storageDir, imageFileName + ".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();

        return image;
    }
}
