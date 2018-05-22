package com.uco.ava.appcv.arucomm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.uco.ava.appcv.arucomm.bbdd.CalibrationClass;
import com.uco.ava.appcv.arucomm.bbdd.ConexionSQLiteHelper;
import com.uco.ava.appcv.arucomm.bbdd.ConstantNames;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements CvCameraViewListener2 {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private static final String TAG = "mainAppFlow";
    static final int RESULTADO_PREFERENCIAS = 123, REQUEST_CODE_INTRO=222, REQUEST_CODE = 456;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private JavaCameraView mOpenCvCameraView;

    private Size mSize0;
    private Mat mIntermediateMat;
    private Mat mMat0;
    private MatOfInt mChannels[];
    private MatOfInt mHistSize;
    private int mHistSizeNum = 25;
    private MatOfFloat mRanges;
    private Scalar mColorsRGB[];
    private Scalar mColorsHue[];
    private Scalar mWhilte;
    private Point mP1;
    private Point mP2;
    private float mBuff[];
    private Mat mSepiaKernel;

    private boolean doubleBackToExitPressedOnce = false, cameraCalibrated = false,
            startcalib = false, CalibBool = false, startmaping = false, MaperBool = false,
            startTester = false, storeMarker = false;

    private Button sendCalib_btn, sendMapping_btn, bubble;
    private ImageButton addCalib_btn, addMaping_btn, help_btn;
    private TextView logText, logTextCopy;
    private View mview;

    private Toast toastMain;

    public int resolutionMode = 1, width, height, picCalib = 0, introSlide=0;
    public String markerType, markersizestr, mapNameStr = "ArucoMapp";

    private static final String storePath = Environment.getExternalStorageDirectory() + File.separator + "AVA";
    private static final String calibPath = storePath + "/Calib",
            mapPath = storePath + "/Mapp",
            markerPath = storePath + "/Marker",
            paramsPath = storePath + "/CamParams",
            videoPath = storePath + "/Video",
            filesPath = storePath + "/Files",
            fileName = "markers_images.zip",
            gridName = "calibration_grid.pdf";
    private static final File mapDir = new File(mapPath),
            calibDir = new File(calibPath),
            markerDir = new File(markerPath),
            paramsDir = new File(paramsPath),
            videoDir = new File(videoPath),
            filesDir = new File(filesPath);

    private static final int calibMode = 0, mapperMode = 1, resetMode = 2, testerMode =3;
    boolean VIEW_HELP_BTN = true;


    private CalibrationClass CalibClassInstance;

    FloatingActionMenu floatMenu;
    FloatingActionButton mapping_fbt;

    private List<CalibrationClass> CalibrationClassList = new ArrayList<CalibrationClass>();
    private ConexionSQLiteHelper conn = new ConexionSQLiteHelper(this, "bd_resolutions", null, 1);

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }


    private void disableExposedUri(){
        if(Build.VERSION.SDK_INT>=24){
            try{
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        disableExposedUri();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main_layout);
        pref = PreferenceManager.getDefaultSharedPreferences(this);

        toastMain = Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT);
        editor = pref.edit();

        logTextCopy = new TextView(this);

        boolean appIntro = pref.getBoolean("appIntro", true);
//        if (appIntro)
//        {
//            Intent introIntent = new Intent(this, MaterialIntro.class);
//            introIntent.putExtra("FILES_PATH", filesPath);
//            introIntent.putExtra("FILE_NAME", fileName);
//            introIntent.putExtra("GRID_NAME", gridName);
//            introIntent.putExtra("SLIDE", introSlide);
//            startActivityForResult(introIntent, REQUEST_CODE_INTRO);
//        }

        leerResoluciones();
        buttonInitializer();
        OpenCameraCV();
    }

    /**
     * Functions to initialize buttons
     */
    private void buttonInitializer() {
        Log.i(TAG, "called buttonInitializer");

        addCalib_btn = (ImageButton) findViewById(R.id.add_calib_btn);
        sendCalib_btn = (Button) findViewById(R.id.send_calib_btn);
        addMaping_btn = (ImageButton) findViewById(R.id.add_mapping_btn);
        sendMapping_btn = (Button) findViewById(R.id.send_mapping_btn);
        bubble = (Button) findViewById(R.id.count_bubble);
        floatMenu = (FloatingActionMenu) findViewById(R.id.floating_menu);
        mapping_fbt = (FloatingActionButton) findViewById(R.id.mapping_fbt);
        help_btn = (ImageButton) findViewById(R.id.help_btn);
    }

    /**
     * Initializer openCV camera
     */
    private void OpenCameraCV() {

        Log.i(TAG, "called OpenCameraCV");
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    /**
     * This función change the maximun resolution of openCV camera
     * Send to JNI functions the current resolution and current camera parameters
     */
    private void ChangeResolution() {
        Log.i(TAG, "called ChangeResolution");
        width = CalibrationClassList.get(resolutionMode).getWidth();
        height = CalibrationClassList.get(resolutionMode).getHeight();
        mOpenCvCameraView.setMaxFrameSize(width, height);
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.enableView();

        setCurrentResolutionJNI(CalibrationClassList.get(resolutionMode).getWidth(),
                CalibrationClassList.get(resolutionMode).getHeight());

        //if camera is not calibrated
        if (!CalibrationClassList.get(resolutionMode).getStringParams().isEmpty()) {
            setCurrentCameraParametersJNI(CalibrationClassList.get(resolutionMode).getStringParams());
        }
    }

    /**
     * Function called when the activity go to the foreground
     */
    @Override
    public void onPause() {
        mapdler.removeCallbacks(runmaple);
        Log.i(TAG, "called onPause");
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        super.onPause();

    }

    /**
     * Function called when the activity come from the foreground.
     */
    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG, "called onResume");
        this.doubleBackToExitPressedOnce = false;

        markerType = pref.getString("marcador", getString(R.string.defaultMarker));
        setMarkerDictionaryJNI(markerType);
        resolutionMode = Integer.parseInt(pref.getString("resolucion", "1"));

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        ChangeResolution();

        markersizestr = pref.getString("markersize", "1");

        setMarkerSizeJNI(Float.valueOf(markersizestr));
        floatMenu.close(true);

        VIEW_HELP_BTN = pref.getBoolean("help_button", true);

        if(VIEW_HELP_BTN)
            help_btn.setVisibility(View.VISIBLE);
        else
            help_btn.setVisibility(View.GONE);
    }

    /**
     * Called before the activity is destroyed
     */
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "called onDestroy");
        mapdler.removeCallbacks(runmaple);
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    /**
     * Override function when back button is pressed
     */
    @Override
    public void onBackPressed() {
        Log.i(TAG, "called onBackPressed");


        if (doubleBackToExitPressedOnce) {
            if (startcalib || startmaping || startTester) {
                cancelOperation();
                messenger("Operation stopped");
            } else {
                super.onBackPressed();
                toastMain.cancel();
                finishAffinity();
                System.exit(0);
            }
        }

        this.doubleBackToExitPressedOnce = true;
        if (startcalib) {
            messenger("Press again to stop the calibration");
        } else if (startmaping) {
            messenger("Press again to stop the mapping");
        } else if (startTester) {
            messenger("Press again to stop the tester");
        } else {
            messenger("Press again to exit");
            floatMenu.close(true);
        }
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                toastMain.cancel();
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

//    private void sendTestFilerInfo(){
//
//        Log.i(TAG, "called saveMapDialog");
//
//        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this, R.style.StyledDialog);
//        mview = getLayoutInflater().inflate(R.layout.send_test_files, null);
//
//        neverShowDialog = (CheckBox) mview.findViewById(R.id.checkBox2);
//
//        mBuilder.setPositiveButton("yes",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which){
//                                if(neverShowDialog.isChecked()) {
//                                    SharedPreferences.Editor editor;
//                                    editor = pref.edit();
//                                    editor.commit();
//                                }
//                                sendTestFiler();
//                            }
//                        }
//                )
//                .setNegativeButton("no",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                if(neverShowDialog.isChecked()) {
//                                    SharedPreferences.Editor editor;
//                                    editor = pref.edit();
//                                    editor.commit();
//                                }
//                            }
//                        }
//                );
//
//        mBuilder.setView(mview);
//        final AlertDialog dialog = mBuilder.create();
//        dialog.setCancelable(false);
//        dialog.setCanceledOnTouchOutside(false);
//
//        dialog.show();
//        Window window = dialog.getWindow();
//        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
//
//
//
//    }
//
//    private void sendTestFilercopy(){
//
//        final String[] names = new String[2];;
//        names[0] = "Calibration grid";
//        names[1] = "Markers images";
//
//        final String[] file = new String[2];;
//        file[0] = gridName;
//        file[1] = fileName;
//
//        final String[] choosen = new String[1];
//
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.StyledDialog);
//        builder.setTitle("Send test files");
//        // add a radio button list
//        builder.setSingleChoiceItems(names, -1, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                // user checked an item
//                choosen[0] = file[which];
//                messenger(file[which]);
//
//            }
//        });
//        // add OK and Cancel buttons
//        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                // user clicked OK
//                if (!filesDir.exists())
//                    filesDir.mkdirs();
//                try{
//                    InputStream in = getResources().openRawResource(R.raw.test_files);
//                    FileOutputStream out = null;
//                    out = new FileOutputStream(filesPath +File.separator+ choosen[0]);
//                    byte[] buff = new byte[1024];
//                    int read = 0;
//                    try {
//                        while ((read = in.read(buff)) > 0) {
//                            out.write(buff, 0, read);
//                        }
//                    } finally {
//                        in.close();
//                        out.close();
//                    }
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                Uri filePath = Uri.parse("file://"+filesPath +File.separator+ choosen[0]);
//                Intent emailIntent = new Intent(Intent.ACTION_SEND);
//                emailIntent .setType("vnd.android.cursor.dir/email");
//                emailIntent .putExtra(Intent.EXTRA_SUBJECT, "Aruco test files");
//                emailIntent.putExtra(Intent.EXTRA_TEXT, "Print these files to test your projects.\n"+
//                        "\nEnjoy Aruco for android");
//                emailIntent .putExtra(Intent.EXTRA_STREAM, filePath);
//                startActivity(Intent.createChooser(emailIntent , "Send email..."));
//            }
//        });
//        builder.setNegativeButton("Cancel", null);
//
//        // create and show the alert dialog
//        AlertDialog dialog = builder.create();
//        dialog.setCancelable(false);
//        dialog.setCanceledOnTouchOutside(false);
//        dialog.show();
//    }

    private void sendTestFiler(){

        final ArrayList<Integer> mSelectedItems = new ArrayList<Integer>();  // Where we track the selected items


        final String[] names = new String[2];;
        names[0] = "Calibration grid";
        names[1] = "Markers images";

        final String[] file = new String[2];;
        file[0] = gridName;
        file[1] = fileName;

        final String[] choosen = new String[1];


        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.StyledDialog);
        builder.setTitle("Send test files");
        // add a radio button list
        builder.setMultiChoiceItems(names, null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which,
                                        boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            mSelectedItems.add(which);
                        } else if (mSelectedItems.contains(which)) {
                            // Else, if the item is already in the array, remove it
                            mSelectedItems.remove(Integer.valueOf(which));
                        }
                    }
                });
        // add OK and Cancel buttons
        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // user clicked OK
                if (!filesDir.exists())
                    filesDir.mkdirs();

                ArrayList<Uri> uris = new ArrayList<Uri>();
                Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                emailIntent.setType("vnd.android.cursor.dir/email");



                for (int x : mSelectedItems) {
                    choosen[0] = file[x];

                    try {
                        InputStream in = getResources().openRawResource(R.raw.test_files);
                        FileOutputStream out = null;
                        out = new FileOutputStream(filesPath + File.separator + choosen[0]);
                        byte[] buff = new byte[1024];
                        int read = 0;
                        try {
                            while ((read = in.read(buff)) > 0) {
                                out.write(buff, 0, read);
                            }
                        } finally {
                            in.close();
                            out.close();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Uri filePath = Uri.parse("file://"+filesPath +File.separator+ choosen[0]);
                    uris.add(filePath);
                }

                emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Aruco test files");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Print these files to test your projects.\n"+
                        "\nEnjoy Aruco for android");
                startActivity(Intent.createChooser(emailIntent , "Send email..."));
            }
        });





        builder.setNegativeButton("Cancel", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }


    /**
     *
     * @param width -  the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */
    public void onCameraViewStarted(int width, int height) {

        mIntermediateMat = new Mat();
        mSize0 = new Size();
        mChannels = new MatOfInt[]{new MatOfInt(0), new MatOfInt(1), new MatOfInt(2)};
        mBuff = new float[mHistSizeNum];
        mHistSize = new MatOfInt(mHistSizeNum);
        mRanges = new MatOfFloat(0f, 256f);
        mMat0 = new Mat();
        mColorsRGB = new Scalar[]{new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255), new Scalar(0, 0, 200, 255)};
        mColorsHue = new Scalar[]{
                new Scalar(255, 0, 0, 255), new Scalar(255, 60, 0, 255), new Scalar(255, 120, 0, 255), new Scalar(255, 180, 0, 255), new Scalar(255, 240, 0, 255),
                new Scalar(215, 213, 0, 255), new Scalar(150, 255, 0, 255), new Scalar(85, 255, 0, 255), new Scalar(20, 255, 0, 255), new Scalar(0, 255, 30, 255),
                new Scalar(0, 255, 85, 255), new Scalar(0, 255, 150, 255), new Scalar(0, 255, 215, 255), new Scalar(0, 234, 255, 255), new Scalar(0, 170, 255, 255),
                new Scalar(0, 120, 255, 255), new Scalar(0, 60, 255, 255), new Scalar(0, 0, 255, 255), new Scalar(64, 0, 255, 255), new Scalar(120, 0, 255, 255),
                new Scalar(180, 0, 255, 255), new Scalar(255, 0, 255, 255), new Scalar(255, 0, 215, 255), new Scalar(255, 0, 85, 255), new Scalar(255, 0, 0, 255)
        };


        mWhilte = Scalar.all(255);
        mP1 = new Point();
        mP2 = new Point();

        // Fill sepia kernel
        mSepiaKernel = new Mat(4, 4, CvType.CV_32F);
        mSepiaKernel.put(0, 0, /* R */0.189f, 0.769f, 0.393f, 0f);
        mSepiaKernel.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
        mSepiaKernel.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
        mSepiaKernel.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);
    }

    /**
     * Function called when the activity go to the foreground
     */
    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mIntermediateMat = null;
    }

    /**
     * Funtion called with every camera frame
     * @param inputFrame -  the current frame
     * @return
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {


        Mat rgba = inputFrame.rgba();
        markerDetectionJNI(rgba.getNativeObjAddr(), markerType);


        if (CalibBool) {
            int picCalib_prev = picCalib;
            picCalib = markerListJNI();

            if (picCalib_prev == picCalib) {
                String msg = "No markers detected";
                if (picCalib == 0)
                    msg = msg + "\nCheck if the correct marker type are selected";
                final String finalMsg = msg;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        messenger(finalMsg);
                    }
                });

            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bubble.setText(String.format("%02d", picCalib));
                    }
                });
            }
            CalibBool = false;

        } else if (MaperBool) {

            int picCalib_prev = picCalib;
            String TamYErr[] = maperListJNI();
            final String msg = TamYErr[1];

            picCalib = Integer.valueOf(TamYErr[0]);

            if (!TamYErr[1].isEmpty()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        messenger(msg);
                    }
                });
            } else if (picCalib_prev < picCalib) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bubble.setText(String.format("%02d", picCalib));
                    }
                });
            }

            MaperBool = false;
        }

        drawMarkerJNI(rgba.getNativeObjAddr());

        if (startTester)
            cubeDrawJNI((rgba.getNativeObjAddr()));

        if (storeMarker)
            storeMarkersDetectionJNI();

        return rgba;
    }

    /**
     * This function call to settings activity
     * @param view
     */
    public void lanzarPreferencias(View view) {
        Log.i(TAG, "called lanzarPreferencias");

        floatMenu.close(true);
        mapdler.removeCallbacks(runmaple);
        Intent i = new Intent(this, PreferenciasActivity.class);
        i.putExtra("STORE_PATH", storePath);
        toastMain.cancel();
        startActivityForResult(i, RESULTADO_PREFERENCIAS);
        setButton(resetMode);
        onCameraViewStopped();
    }

    /**
     * This function ask to the user for marker size and set the app into a calibration mode
     * @param view
     */
    public void calibrationFunction(final View view) {
        Log.i(TAG, "called calibrationFunction");
        floatMenu.close(true);
        startTester = false;

        final EditText markerSizeDialog = new EditText(this);
        markerSizeDialog.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        markerSizeDialog.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        markerSizeDialog.setText(String.valueOf(pref.getString("markersizeCalib", "1")));

        if (!startcalib && !startmaping) {

            AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.StyledDialog).create();
            alertDialog.setTitle("Marker size");
            alertDialog.setMessage("Enter the marker size");
            alertDialog.setView(markerSizeDialog);
            alertDialog.setButton(Dialog.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Value of EditText

                    if (markerSizeChecker(markerSizeDialog.getText().toString())) {

                        markersizestr = markerSizeDialog.getText().toString().trim();
                        markerSizeDialog.setText(markersizestr);

                        setButton(calibMode);

                        messenger("Calibration started\nTake some images to calibrate");
                        setMarkerSizeJNI(Float.valueOf(markersizestr));
                        editor.putString("markersizeCalib", markersizestr);
                        editor.commit();
                    }
                    else{
                        messenger("Invalid size");
                        calibrationFunction(view);
                    }
                }
            });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (DialogInterface.OnClickListener) null);
            alertDialog.setCancelable(false);
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
    }

    private boolean markerSizeChecker(String size){

        if (!size.isEmpty() &&
                !size.toString().startsWith(".") &&
                !size.toString().startsWith(",") &&
                Float.valueOf(size) > 0 ){
            return true;
        }
        else
            return false;
    }

    /**
     * This function adds all markers detected in one picture to a calibration array
     * @param view
     */
    public void addMarkerCalib(View view) {
        Log.i(TAG, "called addMarkerCalib");


        if (picCalib > 14) {
            messenger("Limit of images reached, press the calibrate button");
        } else {
            CalibBool = true;
            MaperBool = false;
        }
    }

    /**
     * This function check calibration data before sending it to aruco library
     * @param view
     */
    public void sendCalibrationChecker(View view) {

        Log.i(TAG, "called sendCalibrationChecker");
        if (picCalib < 5) {
            messenger("You must take at least 5 images to calibrate");
        } else {
            sendCalibration();
        }
    }

    /**
     * This function send the calibration data to Aruco library
     */
    private void sendCalibration() {
        Log.i(TAG, "called sendCalibration");

        SQLiteDatabase db = conn.getReadableDatabase();
        String[] ParamYErr = calibrationJNI(width, height);
        Toast.makeText(this, "Calibration completed\nError computed: = " + ParamYErr[1], Toast.LENGTH_SHORT).show();

        ContentValues values = new ContentValues();
        values.put(ConstantNames.CAMPO_STRINGPARAMS, ParamYErr[0]);
        String[] parametros = {String.valueOf(resolutionMode)};
        db.update(ConstantNames.TABLA_RESOLUTIONS, values, ConstantNames.CAMPO_ID + "=?", parametros);
        db.close();

        leerResoluciones();

        setCurrentCameraParametersJNI(ParamYErr[0]);
        saveCameraParams();
        startcalib = false;
        setButton(resetMode);
    }

    /**
     * This function ask to the user for marker size and set the app into a mapping mode
     * @param view
     */
    public void makermapfunction(final View view) {

        if (!CalibrationClassList.get(resolutionMode).getStringParams().isEmpty()) {
            Log.i(TAG, "called makermapfunction");

            final EditText markerSizeDialog = new EditText(this);
            markerSizeDialog.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            markerSizeDialog.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            markerSizeDialog.setText(String.valueOf(pref.getString("markersizeMap", "1")));


            if (!startcalib && !startmaping) {

                AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.StyledDialog).create();
                alertDialog.setTitle("Marker size");
                alertDialog.setMessage("Enter the marker size");
                alertDialog.setView(markerSizeDialog);

                alertDialog.setButton(Dialog.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Value of EditText

                        if (markerSizeChecker(markerSizeDialog.getText().toString())) {

                            markersizestr = markerSizeDialog.getText().toString().trim();
                            markerSizeDialog.setText(markersizestr);

                            setMarkerSizeJNI(Float.valueOf(markersizestr));
                            editor.putString("markersizeMap", markersizestr);
                            editor.commit();
                            messenger("Mapping started\nTake several images to map");
                            createMarkerMapperMarkerJNI();

                            setButton(mapperMode);

                        }
                        else{
                            messenger("Invalid size");
                            makermapfunction(view);
                        }
                    }
                });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (DialogInterface.OnClickListener) null);
                alertDialog.setCancelable(false);
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();

            } else {
                //rellenar
            }
        } else
            messenger("This resolution hasn't been calibrated.\nCalibrate and try again.");
    }

    /**
     * This function adds all markers detected in one picture to a mapping structure
     * @param view
     */
    public void addMarkerMapping(View view) {
        CalibBool = false;
        MaperBool = true;
    }

    /**
     * This function check mapping data before sending it to aruco library
     * @param view
     */
    public void sendMapingChecker(View view) {

        Log.i(TAG, "called sendMapingChecker");
        if (picCalib < 5) {
            messenger("You must take at least 5 images to map");
        } else {
            sendMaping();
        }
    }

    /**
     * This function send the calibration data to Aruco library
     */
    private void sendMaping() {
        Log.i(TAG, "called sendMaping");

        mapingJNI();
        startmaping = false;
        saveMapDialog();
        mapdler.removeCallbacks(runmaple);
        mapdler.postDelayed(runmaple, 60);
    }

    /**
     * This function resets the app into an initial mode
     */
    private void cancelOperation() {
        Log.i(TAG, "called cancelOperation");
        setButton(resetMode);
    }

    /**
     * This set the app into any defined mode
     * @param buttonMode
     */
    private void setButton(int buttonMode) {
        Log.i(TAG, "called setButton" + buttonMode);

        if (buttonMode == resetMode) {

            startmaping = false;
            startcalib = false;
            startTester = false;

            sendCalib_btn.setVisibility(View.GONE);
            sendMapping_btn.setVisibility(View.GONE);
            addMaping_btn.setVisibility(View.GONE);
            addCalib_btn.setVisibility(View.GONE);
            bubble.setVisibility(View.GONE);
            floatMenu.setVisibility(View.VISIBLE);

            bubble.setText("00");
            picCalib = 0;

            floatMenu.close(true);
            cleanListJNI();
            introSlide=0;
        } else if (buttonMode == calibMode) {

            startcalib = true;
            startmaping = false;
            startTester = false;
            addCalib_btn.setVisibility(View.VISIBLE);
            sendCalib_btn.setVisibility(View.VISIBLE);
            bubble.setVisibility(View.VISIBLE);
            addMaping_btn.setVisibility(View.GONE);
            sendMapping_btn.setVisibility(View.GONE);
            floatMenu.setVisibility(View.GONE);
            introSlide=2;
        } else if (buttonMode == mapperMode) {

            startmaping = true;
            startcalib = false;
            startTester = false;
            addMaping_btn.setVisibility(View.VISIBLE);
            sendMapping_btn.setVisibility(View.VISIBLE);
            bubble.setVisibility(View.VISIBLE);
            addCalib_btn.setVisibility(View.GONE);
            sendCalib_btn.setVisibility(View.GONE);
            floatMenu.setVisibility(View.GONE);
            introSlide=3;
        } else if (buttonMode == testerMode) {
            startTester = true;
            floatMenu.setVisibility(View.GONE);
            introSlide=7;
        }
    }

    /**
     * This function can read the database
     */
    public void leerResoluciones() {
        Log.i(TAG, "called leerResoluciones");
        try {
            CalibrationClassList.clear();
            SQLiteDatabase db = conn.getReadableDatabase();
            String[] valores_recuperar = {ConstantNames.CAMPO_ID, ConstantNames.CAMPO_WIDTH, ConstantNames.CAMPO_HEIGHT, ConstantNames.CAMPO_RESOLUTION, ConstantNames.CAMPO_STRINGPARAMS};

            Cursor cursor = db.query(ConstantNames.TABLA_RESOLUTIONS, valores_recuperar,
                    null, null, null, null, null, null);
            cursor.moveToFirst();
            do {
                CalibClassInstance = new CalibrationClass(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getString(3), cursor.getString(4));

                CalibrationClassList.add(CalibClassInstance);
            } while (cursor.moveToNext());

            db.close();
            cursor.close();
        } catch (Exception e) {
            Log.i("mainLeerResol", "The bbdd hasn't been consulted");
        }
    }

    /**
     * This function show to user a toast message printing it on the screen
     * @param message -  it'll be show in toast message
     */
    private void messenger(String message) {
        Log.i(TAG, "called messenger");

        toastMain.cancel();
        toastMain = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toastMain.show();
    }

    /**
     * Save the latest map created in external memory
     * @param mapNameStr  - the map will be storaged with this name
     */
    private void saveMarkerMap(String mapNameStr) {
        Log.i(TAG, "called saveMarkerMap");


        if (!mapDir.exists())
            mapDir.mkdirs();

        if (!markerDir.exists())
            markerDir.mkdirs();

        startmaping = false;
        savecurrentMarkerMapJNI(mapPath + File.separator + mapNameStr);
        markersToFileJNI(markerPath + File.separator + mapNameStr);
        setButton(resetMode);

        Intent myIntent = new Intent(MainActivity.this, VisualiserActivity.class);

        myIntent.putExtra("FILES_PATH", filesPath);
        myIntent.putExtra("MMAP_PATH", mapPath + File.separator);
        myIntent.putExtra("MMAP_MARKERS", markerPath + File.separator);
        myIntent.putExtra("MMAP_NAME", mapNameStr);
        myIntent.putExtra("HELP_BUTTON", VIEW_HELP_BTN);


        startActivityForResult(myIntent, REQUEST_CODE);

        Log.i(TAG, "Map saved");
    }

    /**
     * Save the latest camera parameters created in external memory
     */
    private void saveCameraParams() {
        Log.i(TAG, "called saveCameraParams");

        if (!paramsDir.exists())
            paramsDir.mkdirs();

        saveCameraParamsJNI(paramsPath + File.separator + CalibrationClassList.get(resolutionMode).getResolution());

        Log.i(TAG, "Current params saved");
    }

    /**
     * Check if the name written by the user is correct
     * @param filename  - raw name written by user
     * @return  - Corrected name without prohibited characters
     */
    private static String nameChecker(String filename) {
        Log.i(TAG, "called nameChecker");

        if (filename == null)
            return null;
        else {
            final int extensionPos = filename.indexOf('.');
            if (extensionPos != -1)
                filename = filename.substring(0, extensionPos);

            filename = filename.replace(File.separator, "");
            filename = filename.replace(" ", "_");
            filename = filename.replace("\\", "");
            if (filename.isEmpty()) {
                return null;
            } else
                return filename;
        }

    }

    /**
     * Ask user for map name
     */
    private void saveMapDialog() {
        Log.i(TAG, "called saveMapDialog");

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this, R.style.StyledDialog);
        mview = getLayoutInflater().inflate(R.layout.dialog_savemap, null);
        TextView saveButton = (TextView) mview.findViewById(R.id.save_map_btn);
        TextView cancelButton = (TextView) mview.findViewById(R.id.nosave_map_btn);
        final EditText MapSaveDialog = (EditText) mview.findViewById(R.id.save_name_etx);


        MapSaveDialog.setText(mapNameStr);
        logText = (TextView) mview.findViewById(R.id.log_text_txt);
        RelativeLayout aux = (RelativeLayout) mview.findViewById(R.id.save_question_rly);


        mBuilder.setView(mview);
        final AlertDialog dialog = mBuilder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mapNameStr = nameChecker(MapSaveDialog.getText().toString().trim());

                if (mapNameStr == null) {
                    messenger("Invalid name");
                    saveMapDialog(logTextCopy);
                } else {
                    File map = new File (mapPath + File.separator + mapNameStr+".yml");
                    if (map.exists())
                        overwriteMap(mapNameStr);
                    else
                        saveMarkerMap(mapNameStr);
                }
                dialog.dismiss();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButton(resetMode);
                dialog.dismiss();
            }
        });

        dialog.show();

        Window window = dialog.getWindow();
        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    }

    /**
     *
     * @param logCopy
     */
    private void saveMapDialog(TextView logCopy) {
        Log.i(TAG, "called saveMapDialog");

        logTextCopy.setText(logText.getText());


        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this, R.style.StyledDialog);
        mview = getLayoutInflater().inflate(R.layout.dialog_savemap, null);

        TextView saveButton = (TextView) mview.findViewById(R.id.save_map_btn);
        TextView cancelButton = (TextView) mview.findViewById(R.id.nosave_map_btn);
        final EditText MapSaveDialog = (EditText) mview.findViewById(R.id.save_name_etx);
        RelativeLayout aux = (RelativeLayout) mview.findViewById(R.id.save_question_rly);
        ScrollView scroll = (ScrollView) mview.findViewById(R.id.log_boddy_scr);

        saveMapDialogFinished(true);


        MapSaveDialog.setText(mapNameStr);
        logText = (TextView) mview.findViewById(R.id.log_text_txt);
        logText.setText(logCopy.getText());

        mBuilder.setView(mview);
        final AlertDialog dialog = mBuilder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapNameStr = nameChecker(MapSaveDialog.getText().toString().trim());

                if (mapNameStr == null) {
                    messenger("Invalid name");
                    saveMapDialog(logTextCopy);
                } else{
                    File map = new File (mapPath + File.separator + mapNameStr+".yml");
                    if (map.exists())
                        overwriteMap(mapNameStr);
                    else
                        saveMarkerMap(mapNameStr);
                }
                dialog.dismiss();
            }
        });


        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();

        Window window = dialog.getWindow();
        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    }

    /**
     * This function prevent override maps
     * @param mapNameStr
     */
    private void overwriteMap(final String mapNameStr){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.StyledDialog);
        builder.setTitle("Overwrite Map");
        builder.setMessage("The map already exists. Do you want to overwrite it?");
        builder.setPositiveButton("overwrite", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which){
                saveMarkerMap(mapNameStr);
            }
        });

        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveMapDialog(logTextCopy);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    /**
     * Control the custom alerDialog while the map is being created
     * @param finished  - boolean th
     */
    private void saveMapDialogFinished(boolean finished) {


        if (finished) {
            ScrollView scroll = (ScrollView) mview.findViewById(R.id.log_boddy_scr);
            scroll.getLayoutParams().height = dpToPx(140);

            RelativeLayout aux = (RelativeLayout) mview.findViewById(R.id.save_question_rly);
            aux.setVisibility(View.VISIBLE);
        }
        else{
            ScrollView scroll = (ScrollView) mview.findViewById(R.id.log_boddy_scr);
            scroll.getLayoutParams().height = dpToPx(220);

            RelativeLayout aux = (RelativeLayout) mview.findViewById(R.id.save_question_rly);
            aux.setVisibility(View.GONE);
        }
    }

    /**
     * Can record .avi video
     * @param rgba  - current frame from oncameraFrame
     */
    private void videoRecorder(Mat rgba){

        //hacerlo global
        VideoWriter mVideoWriter;
        mVideoWriter = new VideoWriter(videoPath + "/video.avi", VideoWriter.fourcc('M', 'J', 'P', 'G'), 30, rgba.size());

        if(!videoDir.exists())
            videoDir.mkdirs();

        if (mVideoWriter == null) {
            mVideoWriter = new VideoWriter(videoPath + "/video.avi", VideoWriter.fourcc('M', 'J', 'P', 'G'), 30, rgba.size());
            mVideoWriter.open(videoPath + "/video.avi", VideoWriter.fourcc('M', 'J', 'P', 'G'), 30.0, rgba.size());
        }
        if (!mVideoWriter.isOpened()) {
            Log.w(TAG, "onCameraFrame: open");
            mVideoWriter.open(videoPath + "/video.avi", VideoWriter.fourcc('M', 'J', 'P', 'G'), 30.0, rgba.size());
        }
        mVideoWriter.write(rgba);


        //Para parar la grabación

//        if (mVideoWriter != null) {
//            mVideoWriter.release();
//            Log.d(TAG, "mVideoWriter release");
//        }
    }

    /**
     * Open the tutorial on a specific slide
     * @param view
     */
    public void openTutorial(View view){

        Intent introIntent = new Intent(this, MaterialIntro.class);
        introIntent.putExtra("FILES_PATH", filesPath);
        introIntent.putExtra("FILE_NAME", fileName);
        introIntent.putExtra("GRID_NAME", gridName);
        introIntent.putExtra("SLIDE", introSlide);
        startActivityForResult(introIntent, REQUEST_CODE_INTRO);
    }

    /**
     * open a map in intro_visualiser activity
     * @param view
     */
    public void visualiser(View view){


        Log.i(TAG, "Visualised pressed");

        File file = new File(mapPath +File.separator);
        if(file.exists() && file.list().length > 0){

            final String[] choosenMap = new String[2];
            choosenMap[0]="";
            final String[] names = file.list();

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.StyledDialog);
            builder.setTitle("Choose a map to open");
            // add a radio button list
            builder.setSingleChoiceItems(names, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // user checked an item
                    choosenMap[0] = mapPath +File.separator+names[which];
                    choosenMap[1] = names[which];
                }
            });
            // add OK and Cancel buttons
            builder.setPositiveButton("Open", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // user clicked OK
                    if(choosenMap[0].isEmpty())
                        messenger("No map selected");
                    else {
                        Intent myIntent = new Intent(MainActivity.this, VisualiserActivity.class);
                        myIntent.putExtra("FILES_PATH", filesPath);
                        myIntent.putExtra("MMAP_PATH", mapPath +File.separator);
                        myIntent.putExtra("MMAP_MARKERS", markerPath + File.separator);
                        myIntent.putExtra("MMAP_NAME", nameChecker(choosenMap[1]));
                        myIntent.putExtra("HELP_BUTTON", VIEW_HELP_BTN);
                        startActivityForResult(myIntent, REQUEST_CODE);
                    }
                }
            });
            builder.setNegativeButton("Cancel", null);

            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
        else
            messenger("Doesn't exist files for intro_visualiser");

        floatMenu.close(true);
        startTester=false;
    }

    /**
     * Transform density independent pixels in real pixel
     * @param dp
     * @return
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }

    /**
     * Wait for the result of some activities
     * @param requestCode  - activity id code
     * @param resultCode  - correct or incorrect result
     * @param data  - result returned if result is correct
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "called onActivityResult");

        // check that it is the SecondActivity with an OK result
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                String mode = data.getStringExtra("MODE");
                if(mode.equals("TESTER")) {
                    if (CalibrationClassList.get(resolutionMode).getStringParams().isEmpty()) {
                        messenger("This resolution hasn't been calibrated.\nCalibrate and try again.");
                    }
                    else if(data.getExtras().getBoolean("START_TESTER"))
                        setButton(testerMode);
                }
                if(mode.equals("CONT")) {

                    String name = data.getStringExtra("MMAP_NAME");
                    String msg = markersFromFileJNI(markerPath + File.separator + name);

                    if(msg.isEmpty()) {
                        setButton(mapperMode);
                        createMarkerMapperMarkerJNI();
                        int tam = addMoreMarkersJNI();
                        bubble.setText("" + tam);
                    }
                    else
                        messenger(msg);
                }
            }
        }

        if (requestCode == REQUEST_CODE_INTRO) {
            if (resultCode == RESULT_OK) {
                editor.putBoolean("appIntro", false);
            } else {
            }
        }
    }

    final Handler mapdler = new Handler();
    final Runnable runmaple = new Runnable() {
        public void run() {
            String solutions []= isOptimizationFinishedJNI();
            final ScrollView scroll = (ScrollView) mview.findViewById(R.id.log_boddy_scr);

            logText.append(solutions[1]);

            if(Integer.valueOf(solutions[0])==1){ // just remove call backs
                mapdler.removeCallbacks(null);
                logText.append("Finished!");

                saveMapDialogFinished(true);

            } else { // post again
                mapdler.postDelayed(this, 60);
            }

            scroll.post(new Runnable() {
                @Override
                public void run() {
                    scroll.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    };

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void markerDetectionJNI(long inputImage, String markerType);
    public native void storeMarkersDetectionJNI();
    public native int markerListJNI();
    public native String[] maperListJNI();
    public native String[] calibrationJNI(int width, int height);
    public native void cleanListJNI();
    private native void mapingJNI();
    private native void setCurrentCameraParametersJNI(String params);
    private native void setCurrentResolutionJNI(int width, int height);
    private native void setMarkerSizeJNI(float size);
    private native String[] isOptimizationFinishedJNI();
    private native void savecurrentMarkerMapJNI(String path);
    private native void saveCameraParamsJNI(String path);
    private native void setMarkerDictionaryJNI(String dictionary);
    private native void createMarkerMapperMarkerJNI();
    private native void cubeDrawJNI(long inputImage);
    private native void drawMarkerJNI(long inputImage);
    private native int addMoreMarkersJNI();
    private native void markersToFileJNI(String path);
    private native String markersFromFileJNI(String path);
}


