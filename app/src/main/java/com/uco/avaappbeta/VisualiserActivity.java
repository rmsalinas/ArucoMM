package com.uco.avaappbeta;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.github.clans.fab.FloatingActionMenu;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.lang.reflect.Method;

import static org.opencv.core.CvType.CV_8UC3;

public class VisualiserActivity extends Activity implements View.OnTouchListener {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private ImageView imageMap;
    FloatingActionMenu floatMenu;
    private float scalediff;
    private static final int NONE = 0, ROTATE = 1, ZOOM = 2, DRAG=3, FLIP=4;
    private int mode = NONE, fingers=0;
    private static int density;
    private float oldDist = 1f, currentAngle = 0f, currentDist=0, oldScale=1;

    private int leftMargin=0, topMargin=0, FirstLeftMargin = 0, FirstTopMargin = 0;

    private String completeMapPath, completeMarkersPath, filesPath, markerPath, mapPath, name;

    static final int REQUEST_CODE_INTRO=222;

    private Bitmap bitmapImage;
    private Canvas canvas;
    private Paint paint;
    private float downx = 0, downy = 0,downx2=0, downy2=0, downx3=0, downy3=0;
    private float realx = 0, realy = 0,realx2=0, realy2=0, realx3=0, realy3=0;
    private Mat mapImage;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;


    //    private static final int flagX=0, flagY=1, flagZoom=2, flagRot=3, flagFlip=4;
    private static final String flagX="X", flagY="Y", flagZoom="Zoom", flagRot="rotate", flagFlip="flip";

    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visualiser_layout);

        imageMap = (ImageView) findViewById(R.id.imageMap);
        ImageView imageCanvas = (ImageView) this.findViewById(R.id.canvasSpace);

        Display currentDisplay = getWindowManager().getDefaultDisplay();
        int dw = (int) currentDisplay.getWidth();
        int dh = (int) currentDisplay.getHeight();
        density = dw*dh;



        //bitmap para dibujar el casvas
        Bitmap bitmapCanvas = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmapCanvas);
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(60);
        imageCanvas.setImageBitmap(bitmapCanvas);
        imageCanvas.setOnTouchListener(this);

        floatMenu = (FloatingActionMenu) findViewById(R.id.floating_menu_vs);

        //Imagen que albergará el mapa a mostrar por pantalla
        mapImage = new Mat(dh, dw, CV_8UC3);
        bitmapImage = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888);

        filesPath = getIntent().getStringExtra("FILES_PATH");
        mapPath = getIntent().getStringExtra("MMAP_PATH");
        markerPath = getIntent().getStringExtra("MMAP_MARKERS");
        name = getIntent().getStringExtra("MMAP_NAME");
        boolean helpBtn = getIntent().getBooleanExtra("HELP_BUTTON", true);



        ImageButton help_btn = (ImageButton) findViewById(R.id.help_btn);
        if(helpBtn){
            help_btn.setVisibility(View.VISIBLE);
        }
        else
            help_btn.setVisibility(View.GONE);

        completeMapPath = mapPath +name+".yml";
        completeMarkersPath = markerPath + name+".yml";

        setCubeParamsJNI(completeMapPath);

        Log.i("activitypath", completeMapPath);
        setDrawerParamsJNI(dw, dh, completeMapPath);

        dawerDrawJNI(mapImage.getNativeObjAddr(), 1);
        Utils.matToBitmap(mapImage, bitmapImage);
        imageMap.setImageBitmap(bitmapImage);
    }

    /**
     * Calculate the distance between two fingers
     * @param event  - event created when second button touch the screen
     * @return  - float distance
     */
    private float twoFingerDist(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
    /**
     * Calculate the distance between two fingers
     * @param pointX finger 1 x axis position
     * @param pointY finger 1 y axis position
     * @param pointX2 finger 2 x axis position
     * @param pointY2 finger 2 y axis position
     * @return  - float distance
     */
    private float twoFingerDist(float pointX, float pointY, float pointX2, float pointY2) {
        float x = pointX - pointX2;
        float y = pointY - pointY2;
        return (float) Math.sqrt(x * x + y * y);
    }


    /**
     * Calculate the degrees between two fingers
     * @param event  - event created when second button touch the screen
     * @return  - float distance
     */
    private float rotDegrees(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        double degrees = Math.toDegrees(radians);
        if (degrees < 0)
            degrees += 360;
        return (float) degrees;
    }

    /**
     * Calculate the degrees between two fingers
     * @param pointX finger 1 x axis position
     * @param pointY finger 1 y axis position
     * @param pointX2 finger 2 x axis position
     * @param pointY2 finger 2 y axis position
     * @return  - float distance
     */
    private float rotDegrees(float pointX, float pointY, float pointX2, float pointY2) {
        double delta_x = (pointX - pointX2);
        double delta_y = (pointY - pointY2);
        double radians = Math.atan2(delta_y, delta_x);
        double degrees = Math.toDegrees(radians);
        if (degrees < 0)
            degrees += 360;
        return (float) degrees;
    }


    /**
     * Share the opened map
     * @param view
     */
    public void shareMap(View view) {


        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent .setType("vnd.android.cursor.dir/email");
        Uri filePath = Uri.parse("file://"+ completeMapPath);
        // the attachment
        emailIntent .putExtra(Intent.EXTRA_STREAM, filePath);
        // the mail subject
        emailIntent .putExtra(Intent.EXTRA_SUBJECT, "Ava markerMapper");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Latest map attached");
        startActivity(Intent.createChooser(emailIntent , "Send email..."));
    }

    /**
     * Delete the opened map
     * @param view
     */
    public void deleteMap(View view){

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.StyledDialog);
        alertDialog.setTitle("Delete Map");
        alertDialog.setMessage("Are you sure you want to delete this map?");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface alertDialog, int id) {

                try {
                    File map = new File(completeMapPath);
                    map.delete();

                    File marker = new File(completeMarkersPath);
                    marker.delete();
                }
                catch (Exception e){
                    finish();
                }
                finish();
            }
        });
        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface alertDialog, int id) {
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }


    /**
     * Open the tutorial on a specific slide
     * @param view
     */
    public void openTutorial(View view){

        Intent introIntent = new Intent(this, MaterialIntro.class);
        introIntent.putExtra("FILES_PATH", filesPath);
        introIntent.putExtra("FILE_NAME", "markers_images.zip");
        introIntent.putExtra("GRID_NAME", "intro_calib_grid.pdf");
        introIntent.putExtra("SLIDE", 6);
        startActivityForResult(introIntent, REQUEST_CODE_INTRO);
    }


    /**
     * Start the implemented tester
     * @param view
     */
    public void startTester(View view){

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.StyledDialog);
        alertDialog.setTitle("Start tester");
        alertDialog.setMessage("You have to be sure that the markers are still in the same position as in this map to work correctly");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Start", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface alertDialog, int id) {

                Intent intent = new Intent();
                intent.putExtra("MODE", "TESTER");
                intent.putExtra("START_TESTER", true);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface alertDialog, int id) {
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    /**
     * This function allows you to add more markers to a opened map
     * @param view
     */
    public void continueMapingVs(View view){

        floatMenu.close(true);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.StyledDialog);
        alertDialog.setTitle("Add Markers");
        alertDialog.setMessage("Do you want to add more markers?");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface alertDialog, int id) {

                Intent intent = new Intent();
                intent.putExtra("MODE", "CONT");
                intent.putExtra("MMAP_NAME", name);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface alertDialog, int id) {
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }


    /**
     * Controls the number of fingers and movements
     * @param v
     * @param event  - even created when any finger touch the screen
     * @return  - true when function run correctly
     */
    public boolean onTouch(View v, MotionEvent event) {


        final ImageView view = imageMap;
        ((BitmapDrawable) view.getDrawable()).setAntiAlias(true);

        switch (event.getAction() & event.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                Log.i("movement", "Action-Down");

                downx = event.getX();
                downy = event.getY();
                FirstLeftMargin = leftMargin;
                FirstTopMargin = topMargin;
                mode = ROTATE;
                fingers++;

                break;

            case MotionEvent.ACTION_UP:
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
                downx = 0;
                downy = 0;
                fingers--;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                fingers++;
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
                if (fingers==2) {
                    downx2 = event.getX(1);
                    downy2 = event.getY(1);
                    oldDist = twoFingerDist(event);

                    if(oldDist > density*0.000158)
                        mode = ZOOM;
                    else
                        mode = DRAG;

                    oldScale = view.getScaleX();
                }

                break;

            case MotionEvent.ACTION_POINTER_UP:
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
                fingers--;
                break;

            case MotionEvent.ACTION_MOVE:

                dawerDrawJNI(mapImage.getNativeObjAddr(), 1);

                Utils.matToBitmap(mapImage, bitmapImage);
                imageMap.setImageBitmap(bitmapImage);

                if (event.getPointerCount() < 2 && mode == ROTATE) {

                    realx = event.getX(0);
                    realy = event.getY(0);
                    rotateJNI((float) ((realy - downy) * 0.01), (float) ((realx - downx) * 0.01));
                    downx = event.getX(0);
                    downy = event.getY(0);
                }


                else if (event.getPointerCount() == 2) {

                    //evento de rotación
                    realx = event.getX(0);
                    realy = event.getY(0);
                    realx2 = event.getX(1);
                    realy2 = event.getY(1);

                    currentDist = twoFingerDist(event);


                    if (mode == ZOOM) {

                        scalediff = oldDist - currentDist;
                        zoomJNI((float) (scalediff*0.03));
                        oldDist = twoFingerDist(event);
                    }
                    if(mode == DRAG){

                        currentDist = twoFingerDist(event);
                        if(currentDist < density*0.000158) {
                            realx = event.getX(0);
                            realy = event.getY(0);
                            translateJNI((float) ((realx - downx)*0.01), (float) ((realy - downy)*0.01));
                            downx = event.getX(0);
                            downy = event.getY(0);

                        }
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            default:
                break;
        }
        return true;
    }

    public native void zoomJNI(float value);
    public native void rotateJNI(float x, float y);
    public native void translateJNI(float x, float y);
    public native void setDrawerParamsJNI(int w, int h, String path);
    public native void dawerDrawJNI(long image, int showNumbers);
    private native void setCubeParamsJNI(String path);
}

