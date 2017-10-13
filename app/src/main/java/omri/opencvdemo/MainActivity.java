package omri.opencvdemo;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import android.provider.MediaStore;

import android.support.v4.content.FileProvider;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import android.widget.Toast;

import org.opencv.android.Utils;

import org.opencv.core.Core;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;


import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.photo.Photo;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import com.bumptech.glide.Glide;
import com.theartofdev.edmodo.cropper.CropImage;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.FLOODFILL_MASK_ONLY;

public class MainActivity extends AppCompatActivity {


    private ImageView mImageView;
    private Bitmap currentBitmap, calculatedBitmap, calculatedHistogram;
    private ImageButton browse_btn, camera_btn, analyze_btn, histogram_btn;
    private static final String TAG = "MainActivity";
    private String currentPhotoPath, currentGalleryPath;
    private String current_open_image_path;
    private static final int ACTION_IMAGE_CAPTURE = 1;
    private static final int ACTION_GET_CONTENT = 2;
    private static final int REQUEST_CAMERA = 100;
    private static int STATE = 3;
    private static final int SAMPLE_BLOB = 3;
    private static final int SAMPLE_SKIN = 4;
    private Uri photoURI;
    private ProgressBar pb;
    private Point seed, skin;


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
        analyze_btn = (ImageButton) findViewById(R.id.analyze_btn);
        analyze_btn.setEnabled(false);
        histogram_btn = (ImageButton) findViewById(R.id.hostogram_btn);
        histogram_btn.setEnabled(false);

        //handling permissions in case of SDK >=23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }


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

            histogram_btn.setEnabled(true);


            switch (requestCode) {
                case ACTION_IMAGE_CAPTURE: //in case user is taking a picture

                    try {

                        Intent intent = CropImage.activity(photoURI).getIntent(getBaseContext());
                        startActivityForResult(intent, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE);
                        Bitmap bm = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                        currentBitmap = bm;
                        setPic(bm, photoURI);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Error taking picture", Toast.LENGTH_LONG).show();
                    }
                    break;

                case ACTION_GET_CONTENT: //in case user is loading picture from gallery
                    try {
                        Uri receivedUri = data.getData();
                        photoURI = receivedUri;
                        Intent intent = CropImage.activity(photoURI).getIntent(getBaseContext());
                        startActivityForResult(intent, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE);
                       /* Bitmap bm = MediaStore.Images.Media.getBitmap(this.getContentResolver(), receivedUri);
                        currentBitmap = bm;
                        setPic(bm, receivedUri);*/
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Error loading picture", Toast.LENGTH_LONG).show();
                    }
                    break;
                case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                    CropImage.ActivityResult result = CropImage.getActivityResult(data);
                    Uri resultUri = result.getUri();
                    try {

                        Bitmap bm = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                        currentBitmap = bm;
                        //getBlobCoordinates();
                        setPic(bm, resultUri);


                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Error cropping picture", Toast.LENGTH_LONG).show();
                    }
                    break;


            }
        }
    }

    /*----------------------------------------------------------------------------*/
    private void getBlobCoordinates() {
        seed = new Point();


        final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage("Click on suspicions blob");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
        mImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {


                if (STATE == SAMPLE_SKIN) {
                    skin = new Point();
                    int[] locations = new int[2];
                    view.getLocationOnScreen(locations);
                    Log.i(TAG, "onTouch: locations are:" + locations[0] + ", " + locations[1]);


                    skin.x = (int) motionEvent.getRawX();
                    skin.y = (int) motionEvent.getRawY();

                    mImageView.setOnTouchListener(null);
                    STATE=SAMPLE_BLOB;


                    Log.i(TAG, "onTouch: seed is:" + seed.x + ", " + seed.y);
                    Log.i(TAG, "onTouch: skin is:" + skin.x + ", " + skin.y);
                   // drawCircle();


                    MyAsyncTask work = new MyAsyncTask();
                    calculatedBitmap = currentBitmap;
                    work.execute(calculatedBitmap);
                    ImageButton b = (ImageButton) findViewById(R.id.analyze_btn);
                    b.setEnabled(false);
                    return false;

                }

                if (STATE == SAMPLE_BLOB) {
                    seed.x = (int) motionEvent.getRawX();
                    seed.y = (int) motionEvent.getRawY();

                    alertDialog.setTitle("Alert");
                    alertDialog.setMessage("Click on the skin");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                    STATE = SAMPLE_SKIN;
                    return false;
                }






             /*   MyAsyncTask work = new MyAsyncTask();
                calculatedBitmap = currentBitmap;
                work.execute(calculatedBitmap);
                ImageButton b = (ImageButton) findViewById(R.id.analyze_btn);
                b.setEnabled(false);*/
                return false;

            }
        });


    }


    private void drawCircle() {
        Bitmap bitmap = Bitmap.createBitmap(currentBitmap.getWidth(),currentBitmap.getHeight(), Bitmap.Config.RGB_565);

        //bitmap.setPixel(mImageView.getWidth(), mImageView.getHeight(), 0);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bitmap, 0, 0, null);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle((float)seed.x,(float)seed.y,10,paint);
        mImageView.setImageDrawable(new BitmapDrawable(getBaseContext().getResources(),bitmap)) ;


    }
    /*-------------------------------------------------------------------*/
    public void getSkinCoordinates() {
        skin = new Point();
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage("Please click on skin in picture, but not the blob");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        mImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                skin.x = (int) motionEvent.getRawX();
                skin.y = (int) motionEvent.getRawY();


                Log.i(TAG, "onTouch: skin is:" + skin.x + ", " + skin.y);
              //  mImageView.setImageBitmap(drawCircle());
                return true;
            }
        });

        // mImageView.setOnTouchListener(null);
    }


    /*----------------------------------------------------------------------------*/
    private void setPic(Bitmap bm, Uri resultUri) {


        mImageView = (ImageView) findViewById(R.id.pic1);

        Glide
                .with(this)
                .asBitmap().load(resultUri).into(mImageView);

        analyze_btn.setEnabled(true);
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

        //insert here info dialog fro user
        getBlobCoordinates();


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


            BitmapFactory.Options myOptions = new BitmapFactory.Options();

            myOptions.inScaled = false;
            myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;// important


            mImageView.setImageBitmap(calculatedBitmap);
        }

        /*----------------------------------------------------------*/

        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {

            bm = bitmaps[0];
            Mat src = new Mat(bm.getHeight(), bm.getWidth(), CvType.CV_8SC3);


            Mat dest = new Mat();

          //  Bitmap resultBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth() - 1, bm.getHeight() - 1);
           // Utils.bitmapToMat(resultBitmap, src);
          /*  Mat mask = new Mat(dest.cols()+2,dest.rows()+2,CvType.CV_8UC1);
            Color blob = new Color();
            int pixel = bm.getPixel((int)seed.x,(int)seed.y);
            Vector<Integer> redChannel =  new Vector<>();
            redChannel=  avarageColorChannel(seed,1);
            Vector<Integer> blueChannel = new Vector<>();
            blueChannel=     avarageColorChannel(seed,2);
            Vector<Integer> greenChannel =  new Vector<>();
            greenChannel=  avarageColorChannel(seed,3);

            int avarageRed = avarageValue(redChannel);
            int avarageBlue = avarageValue(blueChannel);
            int avaragegreen = avarageValue(greenChannel);

            int redValue = Color.red(pixel);
            int blueValue = Color.blue(pixel);
            int greenValue = Color.green(pixel);

            int redValueSkin = 164;
            int blueValueSkin = 126;
            int greenValueSkin = 134;*/

            //Log.i(TAG, "doInBackground: red: "+ redValue+" , blue: "+ blueValue+" , green: "+greenValue);

            // Imgproc.floodFill(src,mask,seed,new Scalar(255,255,255),null,)


            // Imgproc.cvtColor(src, dest, Imgproc.Co);
         /*   int pixel = resultBitmap.getPixel((int) seed.x, (int) seed.y);

            int blueValue = Color.blue(pixel);
            int greenValue = Color.green(pixel);
            int redValue = Color.red(pixel);
            Mat mask = new Mat(src.rows() + 2, src.cols() + 2, CvType.CV_8UC1);
            Scalar lower = new Scalar(blueValue - 20, greenValue - 20, redValue - 20);
            Scalar upper = new Scalar(blueValue + 20, greenValue + 20, redValue + 20);

            Imgproc.floodFill(src, mask, seed, new Scalar(0, 0, 0), new Rect(), lower, upper, FLOODFILL_MASK_ONLY);*/

            Utils.bitmapToMat(bm,src);
            Imgproc.cvtColor(src, dest, Imgproc.COLOR_BGR2GRAY);
            Mat kernel = new Mat();

            Imgproc.threshold(dest, dest, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);


            Imgproc.morphologyEx(dest, dest, Imgproc.MORPH_OPEN, kernel);


            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();//for findContours calculation. Do not touch.

            Imgproc.findContours(dest, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_TC89_KCOS);


            List<MatOfPoint> contoursClone = new ArrayList<>();

            int scale = 0;
            Mat cloneDest = dest.clone();
            double area = 0;


            int num_of_contours = contours.size();
            while (num_of_contours > 5 & scale <= 14) {
                contours.clear();
                contoursClone.clear();
                scale++;
                Imgproc.blur(dest, dest, new org.opencv.core.Size(scale, scale));
                Imgproc.findContours(dest, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
                for (int i = 0; i < contours.size(); i++) {

                    area = Imgproc.contourArea(contours.get(i));
                    if (area > 500)
                        contoursClone.add(contours.get(i));

                }


                num_of_contours = contoursClone.size();
            }






            Imgproc.erode(cloneDest, cloneDest, new Mat(15, 15, CvType.CV_8U));
            Imgproc.cvtColor(cloneDest, cloneDest, Imgproc.COLOR_GRAY2RGB);
            Imgproc.drawContours(cloneDest, contoursClone, -1, new Scalar(255, 0, 0), 6);







            double minimalRadius = 0;
            Imgproc.drawContours(cloneDest, contoursClone, -1, new Scalar(255, 255, 255), -1);
            List<Moments> mu = new ArrayList<Moments>(contours.size());
            for (int i = 0; i < contoursClone.size(); i++) {
                mu.add(i, Imgproc.moments(contoursClone.get(i), false));
                Moments p = mu.get(i);
                int x = (int) (p.get_m10() / p.get_m00());
                int y = (int) (p.get_m01() / p.get_m00());
                Imgproc.circle(cloneDest, new Point(x, y), 10, new Scalar(0, 0, 255), 8);
            }


            Bitmap bm = Bitmap.createBitmap(dest.cols(), dest.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(dest, bm);
            src.release();
            dest.release();
              cloneDest.release();
             hierarchy.release();
             kernel.release();
            return bm;
        }
      /*----------------------------------------------------------*/

    }

    private Vector<Integer> avarageColorChannel(Point seed, int flag) {
        Bitmap bm = currentBitmap;
        Vector<Integer> vector = new Vector<>();
        int pixelUp = bm.getPixel((int) seed.x, (int) seed.y - 1);
        int pixelDown = bm.getPixel((int) seed.x, (int) seed.y + 1);
        int pixelLeft = bm.getPixel((int) seed.x - 1, (int) seed.y);
        int pixelRight = bm.getPixel((int) seed.x + 1, (int) seed.y);
        if (flag == 1) {
            vector.add(Color.red(pixelUp));
            vector.add(Color.red(pixelDown));
            vector.add(Color.red(pixelLeft));
            vector.add(Color.red(pixelRight));
            vector.add(Color.red(bm.getPixel((int) seed.x, (int) seed.y)));

        }
        if (flag == 2) {
            vector.add(Color.blue(pixelUp));
            vector.add(Color.blue(pixelDown));
            vector.add(Color.blue(pixelLeft));
            vector.add(Color.blue(pixelRight));
            vector.add(Color.blue(bm.getPixel((int) seed.x, (int) seed.y)));

        }
        if (flag == 3) {
            vector.add(Color.green(pixelUp));
            vector.add(Color.green(pixelDown));
            vector.add(Color.green(pixelLeft));
            vector.add(Color.green(pixelRight));
            vector.add(Color.green(bm.getPixel((int) seed.x, (int) seed.y)));

        }

        return vector;
    }

    private int avarageValue(Vector<Integer> vector) {
        int sum = 0;
        for (int val : vector) {
            sum += val;
        }
        return sum / vector.size();
    }

    public void onHistogramClicked(View v) {
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
        ImageButton b = (ImageButton) findViewById(R.id.analyze_btn);
        b.setEnabled(true);
        src.release();
        dest.release();
        histogramSize.release();
        histMatBitmap.release();

        for (MatOfInt mat : channels) {
            mat.release();
        }
        for (Mat mat : histograms) {
            mat.release();
        }


    }
}
