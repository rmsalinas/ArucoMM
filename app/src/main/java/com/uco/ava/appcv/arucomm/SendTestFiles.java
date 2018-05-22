package com.uco.ava.appcv.arucomm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

class SendTestFiles extends DialogPreference {

    private static final String storePath = Environment.getExternalStorageDirectory() + File.separator + "AVA";
    private static final String filesPath = storePath + "/Files";
    private static final String fileName = "markers_images.zip",
            gridName = "calibration_grid.pdf";
    private static final File filesDir = new File(filesPath);


    private static Context applicationcontext;

    public SendTestFiles(Context context, AttributeSet attrs) {
        super(context, attrs);

        applicationcontext = context;

        // Set the layout here
        setDialogMessage("Do you wan to open the folder of these files?");
        setPositiveButtonText("Open");
        setNegativeButtonText("Cancel");
        setDialogIcon(null);
    }

    /**
     * alertdialog that asks the user to send test data
     *
     * @param positiveResult
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {


        // When the user selects "OK", persist the new value
        if (positiveResult) {

            if (!filesDir.exists())
                filesDir.mkdirs();

            sendTestFiler();
        } else {
            // User selected Cancel
        }

    }

    /**
     * Open files in fileManager
     */
    private void sendTestFiler() {
        final String[] file = new String[2];
        file[0] = gridName;
        file[1] = fileName;

        final InputStream[] rawStream = new InputStream[2];
        rawStream[0] = applicationcontext.getResources().openRawResource(R.raw.calibration_grid);
        rawStream[1] = applicationcontext.getResources().openRawResource(R.raw.markers_images);

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
            applicationcontext.startActivity(intent);
        }
        catch (android.content.ActivityNotFoundException ex)
        {
//            Toast.makeText(getContext(), "Please install a File Manager.", Toast.LENGTH_SHORT).show();
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


        AlertDialog.Builder builder = new AlertDialog.Builder(applicationcontext, R.style.StyledDialog);
        builder.setTitle("Pick file to share")
                .setMultiChoiceItems(names, choosen, new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialogInterface, int item, boolean isChecked) {
                        if (isChecked)  // If the user checked the item, add it to the selected items
                            choosen[item]=true;
                        else // Else, if the item is already in the array, remove it
                            choosen[item]=false;
                    }
                });

        // add OK and Cancel buttons
        builder.setPositiveButton("Attach", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // user clicked OK
                if (!filesDir.exists())
                    filesDir.mkdirs();

                ArrayList<Uri> uris = new ArrayList<Uri>();
                Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                emailIntent.setType("vnd.android.cursor.dir/email");


                for (int i=0; i<2; i++) {

                    if (choosen[i]) {

                        try {
                            InputStream in = applicationcontext.getResources().openRawResource(R.raw.test_files);
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
                applicationcontext.startActivity(Intent.createChooser(emailIntent, "Send email..."));
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
