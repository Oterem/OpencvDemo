package omri.opencvdemo;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import org.opencv.imgproc.Imgproc;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.photo.Photo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.xml.transform.Result;

import static java.security.AccessController.getContext;


public class MainActivity extends AppCompatActivity {


    private ImageView mImageView;
    private Bitmap currentBitmap, calculatedBitmap, calculatedHistogram;
    private Button browse_btn, camera_btn, analyze_btn;
    private static final String TAG = "MainActivity";
    private String currentPhotoPath, currentGalleryPath;
    private String current_open_image_path;
    static final int ACTION_IMAGE_CAPTURE = 1;
    static final int ACTION_GET_CONTENT = 2;
    private static final int REQUEST_CAMERA = 100;
    private Uri photoURI;
    private ProgressBar pb;


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "static initializer: failed to load copenCV");
        } else {
            Log.d(TAG, "static initializer: openCV loaded");
        }
    }


    /*----------------------------------------------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pb = (ProgressBar) findViewById(R.id.progressbar_loading);
        pb.setVisibility(View.GONE);
        mImageView = (ImageView) findViewById(R.id.pic1);
        analyze_btn = (Button)findViewById(R.id.analyze_btn);
        analyze_btn.setEnabled(false);

        //handling permissions in case of SDK >=23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();


    /*----------------------------------------------------------------------------*/
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /*----------------------------------------------------------------------------*/
    public void launchCamera(View v) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getApplicationContext(), "Error occurred while creating the File", Toast.LENGTH_LONG).show();
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.omri.opencvdemo.fileprovider", photoFile);
                getBaseContext().grantUriPermission("com.omri.opencvdemo", photoURI, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, ACTION_IMAGE_CAPTURE);
            }
        }


    }


     /*----------------------------------------------------------------------------*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            switch (requestCode) {
                case ACTION_IMAGE_CAPTURE: //in case user is taking a picture

                    try {
                        Bitmap bm = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                        currentBitmap = bm;
                        setPic(bm);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Error loading picture", Toast.LENGTH_LONG).show();
                    }
                    break;

                case ACTION_GET_CONTENT: //in case user is loading picture from gallery
                    try {
                        Uri receivedUri = data.getData();
                        photoURI = receivedUri;
                        Bitmap bm = MediaStore.Images.Media.getBitmap(this.getContentResolver(), receivedUri);
                        currentBitmap = bm;
                        setPic(bm);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Error loading picture", Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
    }

    /*----------------------------------------------------------------------------*/
    private void setPic(Bitmap bm) {
        mImageView = (ImageView) findViewById(R.id.pic1);
        mImageView.setImageBitmap(bm);
        analyze_btn.setEnabled(true);
    }


    /*----------------------------------------------------------------------------*/
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }


    /*----------------------------------------------------------------------------*/
    public void onBrowseClick(View v) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), ACTION_GET_CONTENT);
    }
    /*----------------------------------------------------------------------------*/

    public void onAnalyzeClick(View v) {

        MyAsyncTask work = new MyAsyncTask();
        calculatedBitmap = currentBitmap;
        work.execute(calculatedBitmap);
        Button b = (Button)findViewById(R.id.analyze_btn);
        b.setEnabled(false);
    }
    /*----------------------------------------------------------------------------*/

    /*This class perform image processing*/
    private class MyAsyncTask extends AsyncTask<Bitmap, Void, Bitmap> {


        private Bitmap bm;

        /*-----------------------------------------------------------*/
        @Override
        protected void onPreExecute() {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            View v = findViewById(R.id.my_layout);
            v.setAlpha(.5f);
            pb.setVisibility(View.VISIBLE);
            pb.animate().setDuration(shortAnimTime).alpha(
                    1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    pb.setVisibility(View.VISIBLE);

                }
            });
        }
        /*----------------------------------------------------------*/

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            pb.setVisibility(View.INVISIBLE);
            pb.animate().setDuration(0).alpha(
                    0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    pb.setVisibility(View.INVISIBLE);

                }
            });
            View v = findViewById(R.id.my_layout);
            v.setAlpha(1f);
            calculatedBitmap = bitmap;
            mImageView.setImageBitmap(calculatedBitmap);
        }

        /*----------------------------------------------------------*/

        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {


            bm = bitmaps[0];
            Mat src = new Mat();
            Mat dest = new Mat();

            Utils.bitmapToMat(bm, src);

            Imgproc.cvtColor(src, dest, Imgproc.COLOR_BGR2GRAY);



            //TODO: find a "smart" threshold and apply it

            Scalar lower = new Scalar(160,140,140);
            Scalar upper = new Scalar(230,200,200);
            Mat kernel = new Mat();

          //  Core.inRange(dest,lower,upper,dest);

         //  Imgproc.medianBlur(dest,dest,5);
           /*Convert the image to black and white based on a threshold*/
            Imgproc.threshold(dest,dest,0,255,Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);
            Imgproc.morphologyEx(dest,dest,Imgproc.MORPH_OPEN,kernel);




            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();//for findContours calculation. Do not touch.
           Imgproc.findContours(dest, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
            /*Convert picture back to colors in order to see the red border surrounding the melanoma*/
            Imgproc.cvtColor(dest, dest, Imgproc.COLOR_GRAY2RGB);
            /*Painting red border around the melanoma based on the contour vector*/
            Imgproc.drawContours(dest, contours, -1, new Scalar(255, 0, 0), 10);
            /*Filling the inside of the contours in white color in order to get rid of "noises" */
            Imgproc.drawContours(dest, contours, -1, new Scalar(255, 255, 255), -1);
            Bitmap bm = Bitmap.createBitmap(dest.cols(), dest.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(dest, bm);
            return bm;
        }
      /*----------------------------------------------------------*/

    }

    public void onHistogramClicked(View v)
    {
        Mat src = new Mat();
        Mat dest = new Mat();

        Utils.bitmapToMat(currentBitmap, src);
        Size rgbaSize = src.size();
        // Set the amount of bars in the histogram.
        int histSize = 256;
        MatOfInt histogramSize = new MatOfInt(histSize);

// Set the height of the histogram and width of the bar.
        int histogramHeight = (int) rgbaSize.height;
        int binWidth = 5;

// Set the value range.
        MatOfFloat histogramRange = new MatOfFloat(0f, 256f);

// Create two separate lists: one for colors and one for channels (these will be used as separate datasets).
        Scalar[] colorsRgb = new Scalar[]{new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255), new Scalar(0, 0, 200, 255)};
        MatOfInt[] channels = new MatOfInt[]{new MatOfInt(0), new MatOfInt(1), new MatOfInt(2)};

// Create an array to be saved in the histogram and a second array, on which the histogram chart will be drawn.
        Mat[] histograms = new Mat[]{new Mat(), new Mat(), new Mat()};
        Mat histMatBitmap = new Mat(rgbaSize, src.type());

        for (int i = 0; i < channels.length; i++) {
            Imgproc.calcHist(Collections.singletonList(src), channels[i], new Mat(), histograms[i], histogramSize, histogramRange);
            Core.normalize(histograms[i], histograms[i], histogramHeight, 0, Core.NORM_INF);
            for (int j = 0; j < histSize; j++) {
                Point p1 = new Point(binWidth * (j - 1), histogramHeight - Math.round(histograms[i].get(j - 1, 0)[0]));
                Point p2 = new Point(binWidth * j, histogramHeight - Math.round(histograms[i].get(j, 0)[0]));
                Imgproc.line(histMatBitmap, p1, p2, colorsRgb[i], 2, 8, 0);
            }
        }

        Bitmap bm = Bitmap.createBitmap(histMatBitmap.cols(), histMatBitmap.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(histMatBitmap, bm);
        mImageView.setImageBitmap(bm);
        Button b = (Button)findViewById(R.id.analyze_btn);
        b.setEnabled(true);

    }
}
