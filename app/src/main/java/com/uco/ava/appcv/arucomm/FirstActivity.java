package com.uco.ava.appcv.arucomm;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.uco.ava.appcv.arucomm.bbdd.dbCreatorActivity;


public class FirstActivity extends AppCompatActivity {

    private static final int SOLICITUD_PERMISO = 0;
    private Boolean permissionCamera=false, permissionWriteStorage=false;
    private TextView permissionGrantedText;
    private Button button;


    /**
     * Called when the activity is first created.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permission_layout);

        button = (Button) findViewById(R.id.buttPermissions);
        permissionGrantedText = (TextView) findViewById(R.id.PermissionsState);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkPermissions();
            }
        });


        checkPermissions();
    }

    /**
     * check the granted permits
     */
    private void checkPermissions(){

        permissionCamera= ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        permissionWriteStorage= ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if(permissionCamera && permissionWriteStorage){
            FirstRun();
            finish();
        }

        if (!permissionCamera) {

            Intent intent =  new Intent(this, RequestPermissionActivity.class);
            intent.putExtra("permiso", Manifest.permission.CAMERA);
            intent.putExtra("justificacion", "Without camera permission, the app cannot be opened");
            intent.putExtra("requestCode", SOLICITUD_PERMISO);
            startActivityForResult(intent, 987);
        }

        if (!permissionWriteStorage) {

            Intent intent =  new Intent(this, RequestPermissionActivity.class);
            intent.putExtra("permiso", Manifest.permission.WRITE_EXTERNAL_STORAGE);
            intent.putExtra("justificacion", "Without the permission to write, maps cannot be stored");
            intent.putExtra("requestCode", SOLICITUD_PERMISO);
            startActivityForResult(intent, 123);
        }


    }

    /**
     * Wait for the result of some activities
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){

        if (requestCode == 987) {
            if (resultCode == RESULT_OK) {
                permissionCamera=true;
            } else if (resultCode == RESULT_CANCELED) {
                permissionCamera=false;

            }
        }
        if (requestCode == 123) {
            if (resultCode == RESULT_OK) {
                permissionWriteStorage=true;
            } else if (resultCode == RESULT_CANCELED) {
                permissionWriteStorage=false;
            }
        }

        nextActivity();
    }

    /**
     * Decide what activity should be the next activity
     */
    private void nextActivity(){

        if(permissionCamera && permissionWriteStorage){
            FirstRun();

        }
        else{
            button.setVisibility(View.VISIBLE);
            permissionGrantedText.setVisibility(View.VISIBLE);
            permissionGrantedText.setText("Some permission denied.\nMaybe I should change it in the android settings.\nWithout the permissions, the application will not work.");
        }
    }


    //Solo si es la primera vez que se
    private void FirstRun(){
        SharedPreferences pref;
        pref = getSharedPreferences("firstrun", MODE_PRIVATE);
        boolean firstRun = pref.getBoolean("firstRun", true);
        Log.i("firstRun", ""+firstRun);

        Intent intent;
        if (firstRun)
        {
            SharedPreferences.Editor editor;
            editor = pref.edit();
            editor.putBoolean("firstRun", false);
            editor.commit();
            Log.i("nextActivity", "dbCreator");
            intent = new Intent(this, dbCreatorActivity.class);
            startActivity(intent);
        }
        else{
            Log.i("nextActivity", "MainActivity");
            intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }





}

