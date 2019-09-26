package com.uco.avaappbeta;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;

public class RequestPermissionActivity extends Activity {


    private static final int SOLICITUD_PERMISO = 0;
    Intent intent = new Intent();


    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permission_layout);


        Bundle extras = getIntent().getExtras();
        String permiso = extras.getString("permiso");
        String justificacion = extras.getString("justificacion");
        int requestCode = extras.getInt("requestCode");


        checkCameraPermission(requestCode, permiso, justificacion);
    }

    /**
     * Check if the camera permission is granted
     * @param requestCode
     * @param permiso
     * @param justificacion
     */
    private void checkCameraPermission(int requestCode, String permiso, String justificacion) {

        if (ContextCompat.checkSelfPermission(this,
                permiso) == PackageManager.PERMISSION_GRANTED) {
            Log.i("Mensaje", "You already have the requested permission.");
            setResult(RESULT_OK, intent);
            finish();
        } else {
            Log.i("Mensaje", "The permission is not approved");
            solicitarPermiso(permiso, justificacion, requestCode, this);
        }
    }

    /**
     * Request the necessary permission
     * @param permiso  - Necessary permission
     * @param justificacion  - Reasons for the request
     * @param requestCode  - Request id code
     * @param actividad  - current activity
     */
    private static void solicitarPermiso(final String permiso, String justificacion,
                                         final int requestCode, final Activity actividad) {

        if (ActivityCompat.shouldShowRequestPermissionRationale(actividad,
                permiso)){
            Log.i("Mensaje", "The intention of the permission is reported");
            new AlertDialog.Builder(actividad)
                    .setTitle("Solicitud de permiso")
                    .setMessage(justificacion)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ActivityCompat.requestPermissions(actividad,
                                    new String[]{permiso}, requestCode);
                        }})
                    .show();
        }
        else {
            Log.i("Mensaje", "The permission is requested");
            ActivityCompat.requestPermissions(actividad, new String[]{permiso}, requestCode);
        }
    }


    /**
     * Callback for the result from requesting permissions
     * @param requestCode  - Request id code
     * @param permissions  - The requested permissions
     * @param grantResults  - The grant results for the corresponding permissions which is either PERMISSION_GRANTED or PERMISSION_DENIED.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == SOLICITUD_PERMISO) {
            if (grantResults.length== 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Mensaje", "The permission is approved");
                setResult(RESULT_OK, intent);
            }
            else {
                setResult(RESULT_CANCELED, intent);
            }
            finish();
        }
    }
}

