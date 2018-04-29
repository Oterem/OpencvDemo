package omri.opencvdemo;

import android.provider.Settings.Secure;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.services.s3.AmazonS3Client;
import com.bumptech.glide.Glide;
import com.theartofdev.edmodo.cropper.CropImage;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Vector;
import com.amazonaws.mobileconnectors.s3.transferutility.*;
import com.amazonaws.mobile.client.*;
//import org.bytedeco.javacpp.opencv_core;
//import static org.bytedeco.javacpp.opencv_core.*;

public class MainActivity extends AppCompatActivity {


    private ImageView mImageView;
    private Bitmap currentBitmap, calculatedBitmap;
    private ImageButton analyze_btn, histogram_btn;
    private static final String TAG = "MainActivity";
    private String currentPhotoPath, currentGalleryPath;
    private static final int ACTION_IMAGE_CAPTURE = 1;
    private static final int ACTION_GET_CONTENT = 2;
    private static final int REQUEST_CAMERA = 100;
    private static int STATE = 3;
    private static final int SAMPLE_BLOB = 3;
    private static final int SAMPLE_SKIN = 4;
    private Uri photoURI;
    private ProgressBar pb, uploading_bar;
    private Point seed, skin;
    private double[] seedRGB, skinRGB, seedAvgColor, skinAvgColor;
    private double threshold;
    private static int SCALING_DIVIDER = 2;
    private String imageName = "";
    private double diff = 0;
    private boolean isImageSegmented = false;
    private Uri currentUri;
    private String uploadedKey = "";


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "static initializer: failed to load openCV");
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
        uploading_bar = (ProgressBar) findViewById(R.id.loading_bar);
        pb.setVisibility(View.GONE);
        mImageView = (ImageView) findViewById(R.id.pic1);
        analyze_btn = (ImageButton) findViewById(R.id.analyze_btn);
        analyze_btn.setEnabled(false);
//        histogram_btn = (ImageButton) findViewById(R.id.hostogram_btn);
//        histogram_btn.setEnabled(false);
        AWSMobileClient.getInstance().initialize(this).execute();


        //handling permissions in case of SDK >=23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }






    /*----------------------------------------------------------------------------*/

    /**
     * Returns a unique file for an image.
     *
     * @return the created file for saving an image.
     * @throws IOException in case of failure in file allocation
     */
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
        imageName = imageFileName;
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /*----------------------------------------------------------------------------*/

    /**
     * Creates an image capture intent, launching camera and after taking picture,
     * stores the image on internal storage.
     *
     * @param v current view.
     */
    public void launchCamera(View v) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getApplicationContext(), R.string.create_file_error, Toast.LENGTH_LONG).show();
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
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

/*-------------------------------------------------------------------*/

    /**
     * For handling different intents
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            //histogram_btn.setEnabled(true);


            switch (requestCode) {
                case ACTION_IMAGE_CAPTURE: //in case user is taking a picture

                    try {

                        Intent intent = CropImage.activity(photoURI).getIntent(getBaseContext());
                        startActivityForResult(intent, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), R.string.create_file_error, Toast.LENGTH_LONG).show();
                    }
                    break;

                case ACTION_GET_CONTENT: //in case user is loading picture from gallery
                    try {
                        photoURI = data.getData();
                        imageName = getPath(getApplicationContext(), photoURI);
                        imageName = imageName.replaceFirst(".*/(\\w+).*", "$1");
                        CropImage.activity(photoURI).start(this);
                        //setPic(currentBitmap, photoURI);
                        //startActivityForResult(intent, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), R.string.load_image_error, Toast.LENGTH_LONG).show();
                    }
                    break;
                case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE: //cropping image
                    CropImage.ActivityResult result = CropImage.getActivityResult(data);
                    Uri resultUri = result.getUri();
                    currentUri = resultUri;
                    photoURI = resultUri;
                    try {
                        Bitmap bm = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                        currentBitmap = bm;
                        setPic(null, resultUri);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), R.string.crop_image_error, Toast.LENGTH_LONG).show();
                    }
                    break;


            }
        }
    }

    /*----------------------------------------------------------------------------*/

    /**
     * This method asks the user to click on relevant location on screen.
     * After that, saves the coordinates of that location.
     */
    private void getBlobCoordinates() {

        final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setMessage(MainActivity.this.getString(R.string.sample_from_blob))
                .setPositiveButton("GOT IT", null).show();

        //setting a listener on imageview for sampling points
        mImageView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {


                //for sampling the point from not (not blob)
                if (STATE == SAMPLE_SKIN) {
                    skin = new Point();
                    DecodeActionDownEvent(view, motionEvent, skin);
                    try {
                        mImageView.buildDrawingCache();
                        Bitmap bitmap = mImageView.getDrawingCache();
                        int pixel = bitmap.getPixel((int) skin.x, (int) skin.y);
                        skinRGB = new double[]{Color.red(pixel), Color.green(pixel), Color.blue(pixel)};
                        // Log.i(TAG, "seed - r:" + seedRGB[2] + " ,g:" + seedRGB[1] + " b:" + seedRGB[0]);
                        // Log.i(TAG, "skin - r:" + skinRGB[2] + " ,g:" + skinRGB[1] + " b:" + skinRGB[0]);
                        mImageView.setOnTouchListener(null);
                        skinRGB = null;
                        seedRGB = null;

                        STATE = SAMPLE_BLOB;
                        // drawPointsOnImage();


                        seedAvgColor = PixelCalc.avgSurround(seed, bitmap);
                        skinAvgColor = PixelCalc.avgSurround(skin, bitmap);
                        // Log.i(TAG, "avgSeed - r:" + (int) seedAvgColor[0] + " ,g:" + (int) seedAvgColor[1] + " b:" + (int) seedAvgColor[2]);
                        // Log.i(TAG, "avgSkin - r:" + (int) skinAvgColor[0] + " ,g:" + (int) skinAvgColor[1] + " b:" + (int) skinAvgColor[2]);

                        threshold = PixelCalc.calcDistance(seedAvgColor, skinAvgColor) / SCALING_DIVIDER;
                        // Log.i(TAG, "Threshold is: " + threshold);
                        ImageButton b = (ImageButton) findViewById(R.id.analyze_btn);
                        b.setEnabled(false);

                        //uncomment this section to process image
                        SegmentAsyncTask work = new SegmentAsyncTask();
                        calculatedBitmap = currentBitmap;
                        Bitmap[] array = {calculatedBitmap, bitmap};
                        work.execute(array);


                        return false;

                    } catch (Exception e) {
                        e.printStackTrace();
                        //Toast.makeText(getBaseContext(), "Error while sampling colors", Toast.LENGTH_LONG).show();
                    }

                    return false;
                }

                //for sampling the point from suspicious blob
                if (STATE == SAMPLE_BLOB) {
                    seed = new Point();
                    DecodeActionDownEvent(view, motionEvent, seed);
                    try {
                        mImageView.buildDrawingCache();
                        Bitmap bitmap = mImageView.getDrawingCache();
                        int pixel = bitmap.getPixel((int) seed.x, (int) seed.y);
                        seedRGB = new double[]{Color.red(pixel), Color.green(pixel), Color.blue(pixel)};
                        STATE = SAMPLE_SKIN;
                        alertDialog.setMessage(MainActivity.this.getString(R.string.sample_from_skin));
                        alertDialog.show();

                        return false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Toast.makeText(getBaseContext(), "Error while sampling colors", Toast.LENGTH_LONG).show();
                    }

                }
                return false;

            }
        });
    }

    /*-----------------------------------------------------------------------------------*/

    /**
     * Calculate coordinates relative to Imageview based on click location.
     * After calculation, this method set the X,Y coordinates correspond to the given Point object.
     *
     * @param v  Current view.
     * @param ev Relevant click event.
     * @param p  A Point object that it's coordinates will update after calculation.
     */
    private void DecodeActionDownEvent(View v, MotionEvent ev, Point p) {
        Matrix inverse = new Matrix();
        mImageView.getImageMatrix().invert(inverse);
        float[] touchPoint = new float[]{ev.getX(), ev.getY()};
        // inverse.mapPoints(touchPoint);
        p.x = touchPoint[0];
        p.y = touchPoint[1];


    }

    /*------------------------------------------------------------------------------------------------------*/

    /**
     * This method draws the two clicked points by user.
     * In red color it is the suspicious blob.
     * In blue color it is the "clean" skin.
     */
    private void drawPointsOnImage() {

        Bitmap bitmap;
        bitmap = currentBitmap.copy(currentBitmap.getConfig(), true);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bitmap, 0, 0, null);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle((float) seed.x, (float) seed.y, 20, paint);
        paint.setColor(Color.BLUE);
        canvas.drawCircle((float) skin.x, (float) skin.y, 20, paint);
        mImageView.setImageDrawable(new BitmapDrawable(getBaseContext().getResources(), bitmap));
    }

    /*----------------------------------------------------------------------------*/

    /**
     * Setting picture using Glide library in the ImageView based on given Bitmap.
     *
     * @param bm        Given Bitmap to present.
     * @param resultUri Given Uri of an image to present
     */
    private void setPic(Bitmap bm, Uri resultUri) {

        mImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        if (bm != null) {
            mImageView.setImageBitmap(bm);
            return;
        }
        mImageView = (ImageView) findViewById(R.id.pic1);
        Glide
                .with(getBaseContext())
                .asBitmap().load(resultUri)
                .into(mImageView);
        analyze_btn.setEnabled(true);
    }

    String parseImageName(String path) {
        String[] tokens = path.split("/");
        String name = tokens[tokens.length - 1];
        return name;
    }


    /*----------------------------------------------------------------------------*/

    /**
     * Firing new  intent for browsing images from device.
     *
     * @param v Provided view.
     */
    public void onBrowseClick(View v) {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        TextView t = (TextView) findViewById(R.id.textView);
        t.setText("");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), ACTION_GET_CONTENT);
    }
    /*----------------------------------------------------------------------------*/

    public void onAnalyzeClick(View v) {

        getBlobCoordinates();


    }

    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        Log.i("URI", uri + "");
        String result = uri + "";
        // DocumentProvider
        //  if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
        if (isKitKat && (result.contains("media.documents"))) {
            String[] ary = result.split("/");
            int length = ary.length;
            String imgary = ary[length - 1];
            final String[] dat = imgary.split("%3A");
            final String docId = dat[1];
            final String type = dat[0];
            Uri contentUri = null;
            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
            } else if ("audio".equals(type)) {
            }
            final String selection = "_id=?";
            final String[] selectionArgs = new String[]{
                    dat[1]
            };
            return getDataColumn(context, contentUri, selection, selectionArgs);
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /*----------------------------------------------------------------------------*/

    /**
     * The SegmentAsyncTask class implements the AsyncTask class.
     * It is used for a "heavy" image process
     */
    public class SegmentAsyncTask extends AsyncTask<Bitmap, Integer, Bitmap> {


        private Bitmap bm;
        private Bitmap flooded;

        /**
         * @return File that contain the name and the directory of the segment image file
         */

        private File getOutputSegmentFile() {//Give Name to the segment file.
            // To be safe, you should check that the SDCard is mounted
            // using Environment.getExternalStorageState() before doing this.
            File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                    + "/Android/data/"
                    + getApplicationContext().getPackageName()
                    + "/Files/SegmentFiles");


            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null;
                }
            }
            // Create a media file name
            String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
            File mediaFile;
            String mImageName = "MI_" + timeStamp + ".jpg";
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + imageName + "_seg.jpg");
            return mediaFile;
        }


        private void edgeTest(Bitmap bmp, Point p, int threshold, int replacementColor) {
            if (PixelCalc.calcDistance(seedAvgColor, new Point(p.x + 1, p.y), bmp) > threshold)//right neighbor
            {
                if (PixelCalc.calcDistance(seedAvgColor, new Point(p.x - 1, p.y), bmp) > threshold)//left neighbor
                {
                    if (PixelCalc.calcDistance(seedAvgColor, new Point(p.x, p.y - 1), bmp) > threshold)//up neighbor
                    {
                        if (PixelCalc.calcDistance(seedAvgColor, new Point(p.x, p.y + 1), bmp) > threshold)//down neighbor
                            bmp.setPixel((int) p.x, (int) p.y, Color.RED);
                    }
                }
            }
        }

        /*----------------------------------------------------------*/
        private void regionGrowing(Bitmap bmp, Point seed, int threshold, int replacementColor) {

            int x = (int) seed.x;
            int y = (int) seed.y;
            Queue<Point> q = new LinkedList<>();
            q.add(seed);
            while (q.size() > 0) {
                Point n = q.poll();//n is the head of list
                edgeTest(bmp, n, threshold, 1);
                if (PixelCalc.calcDistance(seedAvgColor, n, bmp) > threshold)//in case pixel does not belong
                    continue;

                Point e = new Point(n.x + 1, n.y);//right neighbor
                while ((n.x > 0) && (PixelCalc.calcDistance(seedAvgColor, n, bmp) <= threshold)) {
                    bmp.setPixel((int) n.x, (int) n.y, replacementColor);
                    if ((n.y > 0) && (PixelCalc.calcDistance(seedAvgColor, new Point(n.x, n.y - 1), bmp) <= threshold))//up
                        q.add(new Point(n.x, n.y - 1));
                    if ((n.y < bmp.getHeight() - 1) && (PixelCalc.calcDistance(seedAvgColor, new Point(n.x, n.y + 1), bmp) <= threshold))
                        q.add(new Point(n.x, n.y + 1));
                    n.x--;
                }
                while ((e.x < bmp.getWidth() - 1) && (PixelCalc.calcDistance(seedAvgColor, new Point(e.x, e.y), bmp) <= threshold)) {
                    bmp.setPixel((int) e.x, (int) e.y, replacementColor);

                    if ((e.y > 0) && (PixelCalc.calcDistance(seedAvgColor, new Point(e.x, e.y - 1), bmp) <= threshold))
                        q.add(new Point(e.x, e.y - 1));
                    if ((e.y < bmp.getHeight() - 1) && (PixelCalc.calcDistance(seedAvgColor, new Point(e.x, e.y + 1), bmp) <= threshold))
                        q.add(new Point(e.x, e.y + 1));
                    e.x++;
                }
            }
        }


        /*-----------------------------------------------------------*/


        /**
         * Set the view before image process.
         */
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


        /**
         * After image process, this method stops the progress bar and present the processed image
         */
        @Override
        protected void onPostExecute(final Bitmap bitmap) {

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

            /*Validation mechanism*/
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this)
                    .setMessage(MainActivity.this.getString(R.string.validate_segment))
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {


                            String path = getPath(getApplicationContext(), currentUri);
//                            uploadWithTransferUtility(path);
                            UploadToS3 job = new UploadToS3();
                            job.execute(currentUri);

                        }
                    })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            setPic(null, photoURI);
                            getBlobCoordinates();
                        }
                    });
            AlertDialog alert = alertDialog.create();
            ColorDrawable dialogColor = new ColorDrawable(0x88000000);
            // alert.getWindow().getAttributes().x = 100;
            alert.getWindow().setGravity(Gravity.CENTER);
            alert.getWindow().getAttributes().y = -200;
            alert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);//disable dimmed background
            alert.show();
            // calculatedBitmap = Bitmap.createBitmap(bitmap);//aliasing
            mImageView.setImageResource(0);
            mImageView.destroyDrawingCache();
            TextView t = (TextView) findViewById(R.id.textView);
            // diff =Double.parseDouble(new DecimalFormat("##.##").format(diff));
            t.setText("difference is " + diff + "%");

            //---------- image saving-----------
            File pictureFile = null;
            BitmapFactory.Options myOptions = new BitmapFactory.Options();
            myOptions.inScaled = false;
            myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;// important

            //mImageView.setImageBitmap(bitmap);
            calculatedBitmap = bitmap;
            isImageSegmented = true;
            setPic(bitmap, null);

            try {
                //pictureFile = createImageFile();
                pictureFile = getOutputSegmentFile();
            } catch (Exception e) {
                Toast.makeText(getBaseContext(), R.string.create_file_error, Toast.LENGTH_LONG).show();
            }

            if (pictureFile == null) {
                Log.d(TAG,
                        "Error creating media file, check storage permissions: ");// e.getMessage());
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);

                calculatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }


        /*-----------------------------------------------------*/

        private double getMean(double[] array) {
            double sum = 0.0;
            for (double val : array) {
                sum += val;
            }
            return sum / array.length;
        }

        /*-----------------------------------------------------*/
        protected double getVariance(double[] array) {
            double mean = getMean(array);
            double temp = 0;
            for (double val : array) {
                temp += (val - mean) * (val - mean);
            }
            return temp / (array.length - 1);
        }

        /*-----------------------------------------------------*/
        protected double calcDistance(Point a, Point b) {

            double x = (a.x - b.x) * (a.x - b.x);
            double y = (a.y - b.y) * (a.y - b.y);
            return Math.sqrt(x + y);
        }

        /*-----------------------------------------------------*/
        protected double getCovariance(double[] x, double[] y) {
            int n = x.length;
            double averageX = getMean(x);
            double averageY = getMean(y);
            double sum = 0.0;
            for (int i = 0; i < n; i++) {
                sum += (x[i] - averageX) * (y[i] - averageY);
            }
            return sum / n;
        }

        /**
         * This method perform image process
         */
        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {

            bm = bitmaps[0];
            Mat src = new Mat();
            Mat dest = new Mat();
            Utils.bitmapToMat(bm, src);
            flooded = bitmaps[1];
            int red = android.graphics.Color.rgb(255, 255, 255);
            regionGrowing(flooded, seed, (int) threshold, red);
            Utils.bitmapToMat(flooded, src);
            //Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
            //Imgproc.threshold(src, src, 254, 254, Imgproc.THRESH_BINARY);



           /*create a mask with only the circle - to compare later with matchShapes*/
            /*Mat circle = new Mat(src.rows(), src.cols(), CvType.CV_8UC1);
            Imgproc.circle(circle, new Point(x, y), (int) maxRadius, new Scalar(255, 255, 255), 5);
            List<MatOfPoint> contours1 = new ArrayList<>();
            Mat hierarchy1 = new Mat();//for findContours calculation. Do not touch.
            Imgproc.threshold(circle, circle, 254, 254, Imgproc.THRESH_BINARY);
            Imgproc.findContours(circle, contours1, hierarchy1, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);
            */


            //this section is for masking the segment the mole in full color
            /*
            Mat original = new  Mat(1,1,CvType.CV_8UC3);//this is the original colored image
            Utils.bitmapToMat(bm,original);//loading original colored image to the matrix
            Imgproc.resize(original,original,new Size(src.width(),src.height()));//adapting and resizing the original to be same as src matrix dimentions
            Mat result = Mat.zeros(bm.getWidth(),bm.getHeight(),CvType.CV_8UC3);//creating result matrix full of zeros at the begining
            original.copyTo(result,src);//perform copy from original to result and using src matrix as mask
            */


            Bitmap bm = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(src, bm);


            return bm;
        }
      /*----------------------------------------------------------*/

    }


    private void downloadWithTransferUtility() {

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();

        String path = getPath(getApplicationContext(), currentUri);
        path+="_shai";
        TransferObserver downloadObserver =
                transferUtility.download(
                        uploadedKey,
                        new File("/storage/emulated/0/Download/shai.json"));

        // Attach a listener to the observer to get state update and progress notifications
        downloadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed upload.
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float)bytesCurrent/(float)bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                Log.d("MainActivity", "   ID:" + id + "   bytesCurrent: " + bytesCurrent + "   bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
            }

        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == downloadObserver.getState()) {
            // Handle a completed upload.
        }

        Log.d("YourActivity", "Bytes Transferrred: " + downloadObserver.getBytesTransferred());
        Log.d("YourActivity", "Bytes Total: " + downloadObserver.getBytesTotal());
    }


    public void uploadWithTransferUtility(String path) {
         String android_id = Secure.getString(getBaseContext().getContentResolver(),
                Secure.ANDROID_ID);
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();

        uploadedKey = android_id+"_"+imageName+".jpg";
        TransferObserver uploadObserver =
                transferUtility.upload(
                        uploadedKey,
                        new File(path));


        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(new TransferListener() {


            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed upload.
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int) percentDonef;


                Log.d("YourActivity", "ID:" + id + " bytesCurrent: " + bytesCurrent
                        + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
            }

        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == uploadObserver.getState()) {

        }
//        Toast.makeText(getApplicationContext(), "Upload to server completed", Toast.LENGTH_LONG);
        Log.d("YourActivity", "Bytes Transferrred: " + uploadObserver.getBytesTransferred());
        Log.d("YourActivity", "Bytes Total: " + uploadObserver.getBytesTotal());
    }



    private class UploadToS3 extends AsyncTask<Uri, Integer, Void> {


        /**
         * Set the view before image process.
         */
        @Override
        protected void onPreExecute() {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            View v = findViewById(R.id.my_layout);
            v.setAlpha(.5f);
            uploading_bar.setVisibility(View.VISIBLE);
            uploading_bar.setMax(100);
            uploading_bar.animate().setDuration(shortAnimTime).alpha(
                    1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    uploading_bar.setVisibility(View.VISIBLE);

                }
            });
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            uploading_bar.setProgress(values[0]);

        }
//        @Override
//        protected void onPostExecute(Void aVoid) {
//            super.onPostExecute(aVoid);
//        }

        @Override
        protected void onPostExecute(Void Void) {

            uploading_bar.setVisibility(View.INVISIBLE);

            uploading_bar.animate().setDuration(0).alpha(
                    0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    uploading_bar.setVisibility(View.INVISIBLE);

                }
            });
            View v = findViewById(R.id.my_layout);
            v.setAlpha(1f);
            downloadWithTransferUtility();
            //setPic(bitmap,null);

        }

        @Override
        protected Void doInBackground(Uri... Uri) {

            String path = getPath(getApplicationContext(), Uri[0]);
            uploadWithTransferUtility(path);
            for(int i=0;i<10;i++){
                try {
                    Thread.sleep(1000);
                    publishProgress(i*10);

                }catch (Exception e){
                    e.printStackTrace();;
                }
            }
            return null;
        }


    }

}



