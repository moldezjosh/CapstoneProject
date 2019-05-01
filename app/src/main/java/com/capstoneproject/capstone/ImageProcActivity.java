package com.capstoneproject.capstone;

/**
 * Created by moldezjosh on 3/15/2019.
 */

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
//import org.opencv.core.DMatch;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
//import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

public class ImageProcActivity extends AbsRuntimePermission implements LocationListener {

    private static final String TAG = "ImageProcActivity";
    ImageView imgView;
    Integer REQUEST_CAMERA = 1, SELECT_FILE = 0;
    private static Bitmap selectedImage, result, sampleBitmap, datasetBitmap, bmp, croppedBitmap, rectBitmap;
    ProgressBar pbar;
    FloatingActionButton auth;
    Mat Rgba, imgGray, sampleMat, graySampleMat, grayDatasetMat, sampleDescriptors, datasetDescriptors, imageMat;
    FeatureDetector detector;
    private String mCurrentPhotoPath = null, authenticity, matchResult, dataset, detail, label, finishVal, shadesVal, testNum;
    private String[] finish = {"amplified","cremesheen","frost","lustre","matte","powder kiss","retro matte","satin"};
    boolean isFakeShade = true;
    //image holder
    //Mat bwIMG, hsvIMG, lrrIMG, urrIMG, dsIMG, usIMG, cIMG, hovIMG;
    MatOfPoint2f approxCurve;

    int threshold;
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
    private FirebaseDatabase firebaseDatabase;
    private StorageReference storageRef;
    private FirebaseStorage storage;
    private DatabaseReference databaseReferenceDataset;
    Target targetDataset;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_imageproc);

        //Get Firebase auth instance
        mAuth = FirebaseAuth.getInstance();

        storage = FirebaseStorage.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        storageRef = storage.getReference();
        context = this;

        Bundle extras = getIntent().getExtras();
        testNum = extras.getString("testNum");
        if(extras.getString("dataset")!=null){
            dataset = extras.getString("dataset");
        }

        alertInstruction(testNum);

        imgView = findViewById(R.id.imgView);
        pbar = findViewById(R.id.progressBar);
        pbar.setVisibility(View.GONE);

        auth = findViewById(R.id.auth);
        auth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (selectedImage != null) {

                    if(testNum.equals("1")){

                        label = textRecognizer(croppedBitmap);
                        Log.d(TAG, "authentikit (Label): " + label);

                        checkLabel();
                    }else{

                        cropRect();
                        new GetDataset().execute(view);
                    }
                } else {
                    Snackbar.make(view, "Please add image", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
            }
        });


    }

    @Override
    public void onPermissionsGranted(int requestCode) { }

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
        Intent i = new Intent(ImageProcActivity.this, ResultActivity.class);
        Bundle extras = new Bundle();

        extras.putString("email", user.getEmail());
        extras.putString("authenticity", authenticity);
        extras.putStringArray("location", new String[]{String.valueOf(latitude), String.valueOf(longitude)});
        extras.putString("result", matchResult);
        extras.putString("detail", detail);
        i.putExtras(extras);
        startActivity(i);
        finish();


        authenticity = null;
        latitude = 0;
        longitude = 0;
        matchResult = null;
        detail = null;

    }

    public static Bitmap queryImage() {
        return selectedImage;
    }

    private void captureImage() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (intent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                //cropCircle(photoFile);
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
    }

    private void cropCircle(){

        /* convert bitmap to mat */
        Mat mat = new Mat(selectedImage.getWidth(), selectedImage.getHeight(),
                CvType.CV_8UC1);
        Mat grayMat = new Mat(selectedImage.getWidth(), selectedImage.getHeight(),
                CvType.CV_8UC1);

        Utils.bitmapToMat(selectedImage, mat);

        /* convert to grayscale */
        int colorChannels = (mat.channels() == 3) ? Imgproc.COLOR_BGR2GRAY
                : ((mat.channels() == 4) ? Imgproc.COLOR_BGRA2GRAY : 1);

        Imgproc.cvtColor(mat, grayMat, colorChannels);

        /* reduce the noise so we avoid false circle detection */
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(9, 9), 2, 2);

        // accumulator value
        double dp = 1.2d;
        // minimum distance between the center coordinates of detected circles in pixels
        double minDist = 100;

        // min and max radii (set these values as you desire)
        int minRadius = 0, maxRadius = 0;

        // param1 = gradient value used to handle edge detection
        // param2 = Accumulator threshold value for the
        // cv2.CV_HOUGH_GRADIENT method.
        // The smaller the threshold is, the more circles will be
        // detected (including false circles).
        // The larger the threshold is, the more circles will
        // potentially be returned.
        double param1 = 70, param2 = 100;

        /* create a Mat object to store the circles detected */
        Mat circles = new Mat(selectedImage.getWidth(),
                selectedImage.getHeight(), CvType.CV_8UC1);

        /* find the circle in the image */
        Imgproc.HoughCircles(grayMat, circles,
                Imgproc.CV_HOUGH_GRADIENT, dp, minDist, param1,
                param2, minRadius, maxRadius);

        //draw circle
            /* get the circle details, circleCoordinates[0, 1, 2] = (x,y,r)
             * (x,y) are the coordinates of the circle's center
             */
            double[] circleCoordinates = circles.get(0, 0);


            int x = (int) circleCoordinates[0], y = (int) circleCoordinates[1];

            Point center = new Point(x, y);

            int radius = (int) circleCoordinates[2];

            /* circle's outline */
            Imgproc.circle(mat, center, radius, new Scalar(0,
                    255, 0), 4);

            /* circle's center outline */
            Imgproc.rectangle(mat, new Point(x - 5, y - 5),
                    new Point(x + 5, y + 5),
                    new Scalar(0, 128, 255), -1);

        /* convert back to bitmap */
        Utils.matToBitmap(mat, selectedImage);

        final int width = radius * 2;
        final int height = radius * 2;
        int xVal = x - radius;
        int yVal = y - radius;

        croppedBitmap = Bitmap.createBitmap(selectedImage, xVal, yVal, width, height);


        //------------------
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AuthentiKit");
        myDir.mkdirs();
        String fname = "Image-"+timeStamp+".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        //Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cropRect(){
        imageMat=new Mat();
        int xRect=0, yRect=0, wRect=0, hRect=0;

        Utils.bitmapToMat(selectedImage,imageMat);
        Mat imgSource=imageMat.clone();

        Mat imageHSV = new Mat(imgSource.size(), CvType.CV_8UC4);
        Mat imageBlurr = new Mat(imgSource.size(),CvType.CV_8UC4);
        Mat imageA = new Mat(imgSource.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(imgSource, imageHSV, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(imageHSV, imageBlurr, new Size(5,5), 0);
        Imgproc.adaptiveThreshold(imageBlurr, imageA, 255,Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY,7, 5);

        Bitmap analyzed=Bitmap.createBitmap(imgSource.cols(),imgSource.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageA,analyzed);
        //------------------
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AuthentiKit");
        myDir.mkdirs();
        String fname = "Image-"+timeStamp+".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        //Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            analyzed.compress(Bitmap.CompressFormat.JPEG, 90, out);
            Log.d(TAG, "authentikit - COMPLETED: ");
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imageA, contours, new Mat(), Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE);
        //Imgproc.findContours(imageA, contours, new Mat(), Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);

        Vector<Mat> rectangles = new Vector<Mat>();

        for(int i=0; i< contours.size();i++){
            if (Imgproc.contourArea(contours.get(i)) > 50 )
            {
                Rect rect = Imgproc.boundingRect(contours.get(i));
                if ((rect.height > 30 && rect.height < 120) && (rect.width > 120 && rect.width < 500))
                {
                    xRect = rect.x - (rect.height/2);
                    yRect = rect.y - (rect.height/2);
                    wRect = rect.width + rect.height;
                    hRect = rect.height * 2;

                    Rect rec = new Rect(rect.x, rect.y, rect.width, rect.height);
                    rectangles.add(new Mat(imgSource, rec));
                    //Imgproc.rectangle(imgSource, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(0,0,255));
                    Imgproc.rectangle(imgSource, new Point(xRect,yRect), new Point(xRect+wRect,yRect+hRect),new Scalar(0,0,255));
                }


            }
        }

        Utils.matToBitmap(imageA, selectedImage);
        rectBitmap = Bitmap.createBitmap(selectedImage, xRect, yRect, wRect, hRect);
//        Bitmap analyzed2=Bitmap.createBitmap(imgSource.cols(),imgSource.rows(),Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(imgSource,analyzed2);


        fname = "Image-"+timeStamp+"-2"+".jpg";
        file = new File(myDir, fname);
        if (file.exists()) file.delete();
        //Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            rectBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            Log.d(TAG, "authentikit - COMPLETED: 2");
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

                        imgView.setImageBitmap(selectedImage);
                        mCurrentPhotoPath = null;

                        //cropCircle();
                    } catch (FileNotFoundException e) {
                        return;
                    }

                    MediaScannerConnection.scanFile(ImageProcActivity.this,
                            new String[]{imageUri.getPath()}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.d(TAG, "authentikit - onScanCompleted (crop): " + path + " - " + uri);
                                    //load image in background
                                    new ImageLoaderClass().execute(path);
                                }
                            });
                }
            }
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    private class GetDataset extends AsyncTask<View, String, View> {
        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(final View view) {
            super.onPostExecute(view);
            progressDialog.dismiss();

                hasDataset(new CustomCallback() {
                    @Override
                    public void onCallback(String url) {
                        if(url != null) {
                            authProcess(Uri.parse(url), view);
                        } else {
                            authProcess(Uri.parse(url), view);
                        }
                    }
                });

        }

        @Override
        protected View doInBackground(View... strings) {
            return strings[0];
        }
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

    private class ImageProcessing extends AsyncTask<Bitmap, String, Boolean> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Rgba = new Mat();
            imgGray = new Mat();
            sampleMat = new Mat();
            //datasetMat = new Mat();

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
                resultView();
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
           //bmp = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888);

            //Newly captured image
            //sampleBitmap = selectedImage.copy(Bitmap.Config.ARGB_8888, true);

            publishProgress("Images Preprocessing");


            Mat sampleMat = new Mat(rectBitmap.getWidth(), rectBitmap.getHeight(),
                    CvType.CV_8UC1);
            Utils.bitmapToMat(rectBitmap, sampleMat);


            //dataset image binarization
            /* convert bitmap to mat */
            Mat datasetMat = new Mat(bitmap.getWidth(), bitmap.getHeight(),
                    CvType.CV_8UC1);
            Mat datasetGrayMat = new Mat(bitmap.getWidth(), bitmap.getHeight(),
                    CvType.CV_8UC1);

            Utils.bitmapToMat(bitmap, datasetMat);

            /* convert to grayscale */
            int colorChannels = (datasetMat.channels() == 3) ? Imgproc.COLOR_BGR2GRAY
                    : ((datasetMat.channels() == 4) ? Imgproc.COLOR_BGRA2GRAY : 1);

            Imgproc.cvtColor(datasetMat, datasetGrayMat, colorChannels);
            Imgproc.GaussianBlur(datasetGrayMat, datasetGrayMat, new Size(9, 9), 2, 2);
            Imgproc.adaptiveThreshold(datasetGrayMat, datasetGrayMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
            Utils.matToBitmap(datasetGrayMat, bitmap);

            //initializing feature detector and descriptor
            detector = FeatureDetector.create(FeatureDetector.ORB);
            descExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

            //initializing feature matcher
            matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

            publishProgress("Scanning for stable image features");

            //detecting stable image keypoints
            detector.detect(sampleMat, sampleKeypoints);
            detector.detect(datasetGrayMat, datasetKeypoints);

            //Describe keypoints, extract distinct image features
            descExtractor.compute(sampleMat, sampleKeypoints, sampleDescriptors);
            descExtractor.compute(datasetGrayMat, datasetKeypoints, datasetDescriptors);

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

            Log.d(TAG, "authentikit - finalMatchList: " + String.valueOf(finalMatchList.size()));

            double intGMatch = finalMatchList.size();
            double fMatchAve = 0;

            for (int a = 0; a < intGMatch; a++) {
                fMatchAve = fMatchAve + finalMatchList.get(a).distance;
            }

            fMatchAve = fMatchAve/intGMatch;


                if(Math.round(fMatchAve) > 60) {
                    authenticity = "AUTHENTIC";
                } else {
                    authenticity = "COUNTERFEIT";
                }

            Log.d(TAG, "authentikit - fMatchAve: " + String.valueOf(Math.round(fMatchAve)));

            return true;
        }
    }

    private class ImageLoaderClass extends AsyncTask<String, String, Bitmap> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if(bitmap != null) {
                imgView.setImageBitmap(bitmap);
            }

            if (selectedImage != null) {
                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    buildAlertMessageNoGps();
                } else {
                    getLocation();
                }

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

    private void hasDataset(final CustomCallback customCallback) {
        String datasetURL = "datasets/mac/"+dataset+".jpg";

        storageRef.child(datasetURL).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Got the download URL for 'datasets/mac/maclogo.jpg'

                Log.d(TAG, "authentikit (SUCCESS): " + uri);
                final String url = uri.toString();
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
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                Log.d(TAG, "authentikit (ERROR): " + exception);
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
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AuthentiKit");


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

    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ImageProcActivity.this, new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET
            }, 10);

        } else {
            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if(location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
            }

            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && location == null) {

                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if(location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
            }
        }

        Log.d(TAG, "getLocation: " + location);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: " + location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Please turn on GPS")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void secondTest(){
        testNum = "2";

        dataset = "maclogo";
        Intent i = new Intent(ImageProcActivity.this, ImageProcActivity.class);
        Bundle extras = new Bundle();
        extras.putString("dataset", dataset);
        extras.putString("testNum", testNum);
        i.putExtras(extras);
        startActivity(i);
        finish();
    }

    public String textRecognizer(Bitmap bitmap){
        //Text Recognition using Mobile Vision API developed by Google
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        StringBuilder stringBuilder = null;

        if(textRecognizer.isOperational()) {

            /* convert bitmap to mat */
            Mat mat = new Mat(bitmap.getWidth(), bitmap.getHeight(),
                    CvType.CV_8UC1);
            Mat grayMat = new Mat(bitmap.getWidth(), bitmap.getHeight(),
                    CvType.CV_8UC1);

            Utils.bitmapToMat(bitmap, mat);

            /* convert to grayscale */
            int colorChannels = (mat.channels() == 3) ? Imgproc.COLOR_BGR2GRAY
                    : ((mat.channels() == 4) ? Imgproc.COLOR_BGRA2GRAY : 1);

            Imgproc.cvtColor(mat, grayMat, colorChannels);

            Imgproc.GaussianBlur(grayMat, grayMat, new Size(9, 9), 2, 2);

            Imgproc.adaptiveThreshold(grayMat, grayMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);

            Utils.matToBitmap(grayMat, bitmap);

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            //String root = Environment.getExternalStorageDirectory().toString();
            File myDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AuthentiKit");
            myDir.mkdirs();
            String fname = "Image-"+timeStamp+".jpg";
            File file = new File(myDir, fname);
            if (file.exists()) file.delete();
            //Log.i("LOAD", root + fname);
            try {
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> items = textRecognizer.detect(frame);
            stringBuilder = new StringBuilder();

            for(int a = 0; a < items.size(); a++) {
                TextBlock textBlock = items.valueAt(a);
                stringBuilder.append(textBlock.getValue());
                stringBuilder.append(" ");
            }

        }

        String text = stringBuilder.toString().replaceAll("\n"," ").toLowerCase();
        Log.d(TAG, "authentikit (text): " + text);


        return text;
    }

    private void checkLabel(){

        List<String> myList = new ArrayList<>(Arrays.asList(label.split(" ")));
        //String st = "";
        boolean isFake = true;
        for(int i=0;i<finish.length;i++){
            ExtractedResult extractedResults = FuzzySearch.extractOne(finish[i], myList);
            Log.d(TAG, "authentikit (extracted results): " + extractedResults.getString());
            Log.d(TAG, "authentikit (extracted score): " + extractedResults.getScore());
            if(extractedResults.getScore() >= 80){
                Log.d(TAG, "authentikit : AUTHENTIC");
                isFake = false;
                getShade(finish[i]);
                break;
            }
        }

        if(isFake){
            Log.d(TAG, "authentikit : COUNTERFEIT");
        }

//        ExtractedResult extractedResults = FuzzySearch.extractOne("matte", myList);
//        Log.d(TAG, "authentikit (extracted results): " + extractedResults.getString());
//        Log.d(TAG, "authentikit (extracted score): " + extractedResults.getScore());

//        if(extractedResults.getScore() >= 80){
//            Log.d(TAG, "authentikit : AUTHENTIC");
//            getShade("matte");
//        }else{
//            Log.d(TAG, "authentikit : COUNTERFEIT");
//        }

    }

    private void getShade(final String fin){
        Log.d(TAG, "authentikit (here): " + fin);
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Processing request...");
        progressDialog.show();

        databaseReferenceDataset = firebaseDatabase.getReference("label");

        databaseReferenceDataset.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int dataSnapshotLength = (int) dataSnapshot.getChildrenCount();

                for (DataSnapshot dataset: dataSnapshot.getChildren()) {
                    Log.d(TAG, "authentikit (dataset): " + dataset);

                    String macDataset = dataset.getKey();
                    Log.d(TAG, "authentikit (macDataset): " + macDataset);
                    dataSnapshotLength--;

                    if(fin.equals(macDataset)){
                        String shadeTmp = dataset.child("shade").getValue(String.class);
                        shadesVal = shadeTmp.toLowerCase();

                            if(shadesVal!=null){
                                //Toast.makeText(getApplicationContext(),"shade: " + shadesVal,Toast.LENGTH_LONG).show();
                                Log.d(TAG, "authentikit --(after getshade): " + shadesVal);
                               String[] arr = shadesVal.split(",");
                               //String text = String.join("",arr);

                                //List<String> myList = new ArrayList<>(Arrays.asList(label.split(" ")));

                                for(int i=0;i<arr.length;i++){
                                    int searchScore = FuzzySearch.tokenSortRatio(arr[i],label);
                                            //.ratio(arr[i], label);
                                    Log.d(TAG, "authentikit : " + arr[i] + " - " + searchScore);

                                    if(searchScore >= 50){
                                        isFakeShade = false;
                                        progressDialog.dismiss();
                                        Log.d(TAG, "authentikit : AUTHENTIC");
                                        Log.d(TAG, "authentikit : (shade) " + arr[i]);
                                        break;

                                    }
                                }


                                if(isFakeShade){
                                    progressDialog.dismiss();
                                    Log.d(TAG, "authentikit : COUNTERFEIT");
                                    break;
                                }else{
                                    break;
                                }
                            }
                    } else {
                        if(dataSnapshotLength == 0) {
                           // customCallback.onCallback(null);
                        }
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //customCallback.onCallback(null);
            }
        });
    }

    private void alertInstruction(String num){
        String message;
        if(num.equals("1")){
            message = "Take a picture of the bottom sticker.";
        }else{
            message = "Remove the cap of the lipstick and take a picture of the Logo";
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ImageProcActivity.this);

        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, int which) {
                                captureImage();
                            }
                        })
                .setTitle("Instruction")
                .setIcon(R.drawable.ic_photo_camera_black_24dp);
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }
}
