package com.uco.avaappbeta;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.app.NavigationPolicy;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by FJMaestre on 9/03/18.
 */

public class MaterialIntro extends IntroActivity {

    private static String filesPath, fileName, gridName;
    File filesDir;
    private Toast toastMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setFullscreen(true);
        setContentView(R.layout.intro_get_files);
        super.onCreate(savedInstanceState);

        toastMain = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        setNavigationPolicy(new NavigationPolicy() {
            @Override public boolean canGoForward(int position) {
                return true;
            }

            @Override public boolean canGoBackward(int position) {
                return true;
            }
        });

        filesPath = getIntent().getStringExtra("FILES_PATH");
        fileName = getIntent().getStringExtra("FILE_NAME");
        gridName = getIntent().getStringExtra("GRID_NAME");
        final int slide = getIntent().getIntExtra("SLIDE", 0);

        try {
            filesDir = new File(filesPath);
        }
        catch (Exception e){
            finish();
        }

        if(slide > 0) {
            View ContentView = getContentView();
            ContentView.post(new Runnable() {
                public void run() {
                    goToSlide(slide);
                }
            });
        }

        addSlide(new SimpleSlide.Builder()
                .title("Print your files")
                .description(R.string.print_files)
                .image(R.drawable.intro_calib_grid)
                .background(R.color.colorRedAva)
                .backgroundDark(R.color.colorRedDark)
                .scrollable(true)
                .build());

        addSlide(new SimpleSlide.Builder()
                .description(R.string.print_files)
                .background(R.color.colorGreenLight)
                .backgroundDark(R.color.colorGreenDark)
                .layout(R.layout.intro_get_files)
                .build());


        addSlide(new SimpleSlide.Builder()
                .title("Calibration")
                .description(R.string.calib_body)
                .image(R.drawable.intro_calib)
                .background(R.color.colorBlueLight)
                .backgroundDark(R.color.colorBlueDark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title("Preparing workspace")
                .description(R.string.marker_first)
                .image(R.drawable.intro_workspace)
                .background(R.color.colorBrownLight)
                .backgroundDark(R.color.colorBrownDark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title("Mapping")
                .description(R.string.marker_body)
                .image(R.drawable.intro_mapper)
                .background(R.color.colorLimeLight)
                .backgroundDark(R.color.colorLimeDark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title("Visualiser")
                .description(R.string.visualiser_body)
                .image(R.drawable.intro_visualiser)
                .background(R.color.colorPurpleLight)
                .backgroundDark(R.color.colorPurpleDark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title("Visualiser control")
                .description(R.string.visualiser_control)
                .image(R.drawable.intro_control)
                .background(R.color.colorBlueGreyLight)
                .backgroundDark(R.color.colorBlueGreyDark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title("Visualiser options")
                .description(R.string.visualiser_options)
                .image(R.drawable.intro_visualiser_opt)
                .background(R.color.colorGreyLight)
                .backgroundDark(R.color.colorGreyDark)
                .buttonCtaLabel("Start again")
                .buttonCtaClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        goToFirstSlide();
                    }
                })
                .build());

//        addSlide(new SimpleSlide.Builder()
//                .title("Tester")
//                .description(R.string.tester_body)
//                .image(R.drawable.intro_tester)
//                .background(R.color.colorOrangeLight)
//                .backgroundDark(R.color.colorOrangeDark)
//                .buttonCtaLabel("Start again")
//                .buttonCtaClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        goToFirstSlide();
//                    }
//                })
//                .build());

        setButtonBackFunction(BUTTON_BACK_FUNCTION_SKIP);
    }

    /**
     * Open files in fileManager
     * @param view
     */
    public void openFiles (View view){
        final String[] file = new String[2];
        file[0] = gridName;
        file[1] = fileName;

        final InputStream[] rawStream = new InputStream[2];
        rawStream[0] = getResources().openRawResource(R.raw.calibration_grid);
        rawStream[1] = getResources().openRawResource(R.raw.markers_images);


        if (!filesDir.exists())
            filesDir.mkdirs();

        for (int i=0; i<file.length; i++) {

            try {
                InputStream in = rawStream[i];
                FileOutputStream out = null;
                out = new FileOutputStream(filesPath + File.separator + file[i]);
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
        }
        Uri uri = Uri.parse(filesPath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");

        try {
            startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex)
        {
            sendTestFilerEmail();
        }

//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setDataAndType(uri, "text/csv");
//        startActivity(Intent.createChooser(intent, "Open folder"));

    }

    /**
     * Atach files to email if fileManager is not installed
     */
    private void sendTestFilerEmail() {

        final String[] names = {"Calibration grid","Markers images"};
        final String[] file = {gridName, fileName};
        final boolean [] choosen = {false, false};

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.StyledDialog);
        builder.setTitle("Pick file to share")
                .setMultiChoiceItems(names, choosen, new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialogInterface, int item, boolean isChecked) {
                        if (isChecked)  // If the user checked the item, add it to the selected items
                            choosen[item]=true;
                         else // Else, if the item is already in the array, remove it
                            choosen[item]=false;
                    }
                });


        builder.setPositiveButton("Attach", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // user clicked OK
                if (!filesDir.exists())
                    filesDir.mkdirs();

                ArrayList<Uri> uris = new ArrayList<Uri>();
                Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                emailIntent.setType("vnd.android.cursor.dir/email");


                for (int i=0; i<2; i++){

                    if(choosen[i]){
                        try {
                            InputStream in = getResources().openRawResource(R.raw.test_files);
                            FileOutputStream out = null;
                            out = new FileOutputStream(filesPath + File.separator + file[i]);
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
                        Uri filePath = Uri.parse("file://" + filesPath + File.separator + file[i]);
                        uris.add(filePath);
                    }
                }


                emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Aruco test files");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Print these files to test your projects.\n" +
                        "\nEnjoy Aruco for android");
                startActivity(Intent.createChooser(emailIntent, "Send email..."));
            }
        });


        builder.setNegativeButton("Cancel", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
