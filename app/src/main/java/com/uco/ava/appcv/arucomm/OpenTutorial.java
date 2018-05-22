package com.uco.ava.appcv.arucomm;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import java.io.File;

class OpenTutorial extends DialogPreference {

    static final int REQUEST_CODE_INTRO=222;


    private static final String storePath = Environment.getExternalStorageDirectory() + File.separator + "AVA";
    private static final String filesPath = storePath + "/Files";
    private static final String fileName = "markers_images.zip",
            gridName = "intro_calib_grid.pdf";
    private static final File filesDir = new File(filesPath);


    private static Context applicationcontext;

    public OpenTutorial(Context context, AttributeSet attrs) {
        super(context, attrs);

        applicationcontext = context;

        // Set the layout here
        setDialogMessage("Do you want to open the tutorial?");
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

                    Intent introIntent = new Intent(getContext(), MaterialIntro.class);
                    introIntent.putExtra("FILES_PATH", filesPath);
                    introIntent.putExtra("FILE_NAME", fileName);
                    introIntent.putExtra("GRID_NAME", gridName);
            applicationcontext.startActivity(introIntent);

        } else {
            // User selected Cancel
        }

    }
}
