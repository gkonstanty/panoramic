package pl.edu.agh.iet.wm.panoramic;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.opencv.calib3d.Calib3d.RANSAC;

// Odpalenie maszyny wirtualnej:
// emulator -use-system-libs -avd 4.7_WXGA_API_22_-_widzenie_maszynowe
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private int PICK_IMAGE_REQUEST = 1;
    private int clickedButtonId;
    private Uri[] imageURIs = new Uri[2];


    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "not inited");
        } else {
            Log.d(TAG, "HURRAY! inited openCV");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                // Always show the chooser (if there are multiple options available)
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);

//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();


            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void selectImage(View view) {

        this.clickedButtonId = view.getId();

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra("buttonName", "helloButton");
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            // Wyciagniecie scierzki z obrazka galerii (druga odpowiedz)
            // http://stackoverflow.com/questions/20067508/get-real-path-from-uri-android-kitkat-new-storage-access-framework


            Uri selectedImageUri = data.getData();
            String selectedImagePath = getPath(this, selectedImageUri);
            Log.d(TAG, "Uri2 (lib): " + selectedImagePath);

            // Typ 4 (kanaly) wczytywanego obrazka
            Mat selectedImage = Highgui.imread(selectedImagePath, 4);

            Mat destinationMatrix = new Mat(selectedImage.height(), selectedImage.width(), CvType.CV_8U, new Scalar(4));

            Log.d(TAG, "Img height: " + selectedImage.height() + ", width: " + selectedImage.width());
            Bitmap grayBitmap = null;

            if (selectedImage.empty()) {
                Log.d(TAG, "meh, test2Img nie znalazlo");

            } else {
                Log.d(TAG, "wow, test2Img znalazlo sie");

                // Zamiana na Gray:
                Imgproc.cvtColor(selectedImage, destinationMatrix, Imgproc.COLOR_RGBA2GRAY, 4);

                try {
                    grayBitmap = Bitmap.createBitmap(destinationMatrix.cols(), destinationMatrix.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(destinationMatrix, grayBitmap);
                } catch (CvException e) {
                    Log.d("Exception", e.getMessage());
                }
            }

            Log.d(TAG, "end");

            if (this.clickedButtonId == R.id.button_image1) {
                imageURIs[0] = selectedImageUri;
            } else {
                imageURIs[1] = selectedImageUri;

            }

            // set image as background of clicked button
            Button clickedButton = (Button) findViewById(this.clickedButtonId);
            int buttonHeight = pxToDp(clickedButton.getHeight());
            int buttonWidth = pxToDp(clickedButton.getWidth());

            BitmapDrawable bdrawable = new BitmapDrawable(getResources(),
                    Bitmap.createScaledBitmap(grayBitmap, buttonWidth, buttonHeight, false));

            clickedButton.setBackground(bdrawable);
            clickedButton.setText(R.string.change_image);

        }
    }

    private int pxToDp(int px) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    public void onClickStitch(View view) {

        String image1Path = getPath(this, imageURIs[0]);
        String image2Path = getPath(this, imageURIs[1]);

        Bitmap stitchedBitmap = stitchImages(image1Path, image2Path);

        ImageView imageView = (ImageView) findViewById(R.id.imageView);

        imageView.setImageBitmap(Bitmap.createScaledBitmap(stitchedBitmap, 600, 300, false));

    }

    private Bitmap stitchImages(String imagePath1, String imagePath2) {
        Bitmap stitchedImage = null;

        Mat firstImage = Highgui.imread(imagePath1, 4);
        Mat secondImage = Highgui.imread(imagePath2, 4);

        Mat firstImageGray = new Mat(firstImage.height(), firstImage.width(), CvType.CV_8U, new Scalar(4));
        if (firstImageGray.empty()) {
            Log.d(TAG, "Something is wrong with the first image.");
        } else {
            Imgproc.cvtColor(firstImage, firstImageGray, Imgproc.COLOR_RGBA2GRAY, 4);
        }

        Mat secondImageGray = new Mat(secondImage.height(), secondImage.width(), CvType.CV_8U, new Scalar(4));
        if (secondImageGray.empty()) {
            Log.d(TAG, "Something is wrong with the second image.");
        } else {
            Imgproc.cvtColor(secondImage, secondImageGray, Imgproc.COLOR_RGBA2GRAY, 4);
        }


//        int minHessian = 400;
//        SurfFeatureDetector detector( minHessian );
//
//        FeatureDetector fd = FeatureDetector.create(FeatureDetector.BRISK);
//        DescriptorExtractor fe = DescriptorExtractor.create(DescriptorExtractor.SURF);

        FeatureDetector fd = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor fe = DescriptorExtractor.create(DescriptorExtractor.ORB);
        DescriptorMatcher fm = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);


        //structures for the keypoints from the 2 images
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();

        //structures for the computed descriptors
        Mat descriptors1 = new Mat();
        Mat descriptors2 = new Mat();

        //structure for the matches
        MatOfDMatch matches = new MatOfDMatch();

        //getting the keypoints
        fd.detect(firstImageGray, keypoints1);
        fd.detect(secondImageGray, keypoints2);

        Log.d(TAG, "keypoints1: " + keypoints1.size());
        Log.d(TAG, "keypoints2: " + keypoints2.size());

        Log.d(TAG, "test7");

        //getting the descriptors from the keypoints
        fe.compute(firstImageGray, keypoints1, descriptors1);
        fe.compute(secondImageGray, keypoints2, descriptors2);

        Log.d(TAG, "test8");
        //getting the matches the 2 sets of descriptors
        fm.match(descriptors2, descriptors1, matches);

        //turn the matches to a list
        List<DMatch> matchesList = matches.toList();

        Double maxDist = 0.0; //keep track of max distance from the matches
        Double minDist = 10000.0; //keep track of min distance from the matches

        //calculate max & min distances between keypoints
        for (int i = 0; i < keypoints1.rows(); i++) {
            Double dist = (double) matchesList.get(i).distance;
            if (dist < minDist) minDist = dist;
            if (dist > maxDist) maxDist = dist;
        }

        Log.d(TAG, "max dist: " + maxDist);
        Log.d(TAG, "min dist: " + minDist);


        //structure for the good matches
        LinkedList<DMatch> goodMatches = new LinkedList<>();

        //use only the good matches (i.e. whose distance is less than 3*min_dist)
        for (int i = 0; i < descriptors1.rows(); i++) {
            if (matchesList.get(i).distance < 3 * minDist) {
                goodMatches.addLast(matchesList.get(i));
            }
        }


        //structures to hold points of the good matches (coordinates)
        LinkedList<Point> objList = new LinkedList<>(); // image1
        LinkedList<Point> sceneList = new LinkedList<>(); //image 2

        List<KeyPoint> keypoints_objectList = keypoints1.toList();
        List<KeyPoint> keypoints_sceneList = keypoints2.toList();

        //putting the points of the good matches into above structures
        for (int i = 0; i < goodMatches.size(); i++) {
            objList.addLast(keypoints_objectList.get(goodMatches.get(i).queryIdx).pt);
            sceneList.addLast(keypoints_sceneList.get(goodMatches.get(i).trainIdx).pt);
        }

        System.out.println("\nNum. of good matches " + goodMatches.size());

        MatOfDMatch gm = new MatOfDMatch();
        gm.fromList(goodMatches);

        //converting the points into the appropriate data structure
        MatOfPoint2f obj = new MatOfPoint2f();
        obj.fromList(objList);

        MatOfPoint2f scene = new MatOfPoint2f();
        scene.fromList(sceneList);

        //finding the homography matrix
//        Mat H = Calib3d.findHomography(obj, scene);


        //TODO: sprawdzić poprawność:
        Mat H = Calib3d.findHomography(obj, scene);


        //LinkedList<Point> cornerList = new LinkedList<Point>();
        Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
        Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

        obj_corners.put(0, 0, new double[]{0, 0});
        obj_corners.put(0, 0, new double[]{firstImageGray.cols(), 0});
        obj_corners.put(0, 0, new double[]{firstImageGray.cols(), firstImageGray.rows()});
        obj_corners.put(0, 0, new double[]{0, firstImageGray.rows()});

        Core.perspectiveTransform(obj_corners, scene_corners, H);

        //structure to hold the result of the homography matrix
        Mat result = new Mat();

        //size of the new image - i.e. image 1 + image 2
        Size s = new Size(firstImage.cols() + secondImage.cols(), firstImage.rows());

        //using the homography matrix to warp the two images
        Imgproc.warpPerspective(firstImage, result, H, s);
        int i = firstImageGray.cols();
        Mat m = new Mat(result, new Rect(i, 0, secondImageGray.cols(), secondImageGray.rows()));

        secondImageGray.copyTo(m);

        Mat img_mat = new Mat();
        img_mat = m;
//        Features2d.drawMatches(firstImageGray, keypoints1, secondImageGray, keypoints2, gm, img_mat, new Scalar(254, 0, 0), new Scalar(254, 0, 0), new MatOfByte(), 2);



        try {
            stitchedImage = Bitmap.createBitmap(img_mat.cols(), img_mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img_mat, stitchedImage);
        } catch (CvException e) {
            Log.d("Exception", e.getMessage());
        }


        //Convert to bitmap:

        /*
        try {
            stitchedImage = Bitmap.createBitmap(secondImageGray.cols(), secondImageGray.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(secondImageGray, stitchedImage);
        }
        catch (CvException e){Log.d("Exception",e.getMessage());}

         */
/*
        //Convert to bitmap:

        // Typ 4 (kanaly) wczytywanego obrazka
        Mat selectedImage = Highgui.imread(imagePath1, 4);
        Mat destinationMatrix = new Mat (selectedImage.height(), selectedImage.width(), CvType.CV_8U, new Scalar(4));

        Log.d(TAG, "Img height: " + selectedImage.height() + ", width: " + selectedImage.width());
        Bitmap grayBitmap = null;

        if (selectedImage.empty()){
            Log.d(TAG, "meh, test2Img nie znalazlo");
        } else{
            Log.d(TAG, "wow, test2Img znalazlo sie");

            // Zamiana na Gray:
            Imgproc.cvtColor( selectedImage, destinationMatrix, Imgproc.COLOR_RGBA2GRAY, 4 );

            try {
                grayBitmap = Bitmap.createBitmap(destinationMatrix.cols(), destinationMatrix.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(destinationMatrix, grayBitmap);
            }
            catch (CvException e){Log.d("Exception",e.getMessage());}
        }
*/
        Log.d(TAG, "end");
        return stitchedImage;
    }

    // END
    // Poniżej kod z biblioteki do znajdowanie scierzki z obrazka w galerii
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
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


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
