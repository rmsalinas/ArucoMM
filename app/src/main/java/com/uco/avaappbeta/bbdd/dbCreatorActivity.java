package com.uco.avaappbeta.bbdd;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Camera;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;


import com.uco.avaappbeta.MainActivity;

import java.util.ArrayList;
import java.util.List;


public class dbCreatorActivity extends AppCompatActivity {

    private ConexionSQLiteHelper conn=new ConexionSQLiteHelper(this, "bd_resolutions", null, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registrarResolucion();
        leerResoluciones();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();

    }

    @Override
    public void onResume() {
        super.onResume();
    }



    public void registrarResolucion(){


        SQLiteDatabase db = conn.getWritableDatabase();

        Camera mCamera = Camera.open();
        List<Camera.Size> resList = mCamera.getParameters().getSupportedPreviewSizes();
        Log.i("focusmodes: ",  mCamera.getParameters().getSupportedFocusModes().toString());

        for (int i=0;i<resList.size();i++) {
            ContentValues values= new ContentValues();
            values.put(ConstantNames.CAMPO_ID, i);
            values.put(ConstantNames.CAMPO_WIDTH, resList.get(i).width);
            values.put(ConstantNames.CAMPO_HEIGHT, resList.get(i).height);
            values.put(ConstantNames.CAMPO_RESOLUTION, resList.get(i).width+"x"+resList.get(i).height);
            values.put(ConstantNames.CAMPO_STRINGPARAMS, "");

            Long idResultante =  db.insertWithOnConflict(ConstantNames.TABLA_RESOLUTIONS, ConstantNames.CAMPO_ID, values, SQLiteDatabase.CONFLICT_REPLACE);

            Log.i("dbCreatorRegistrarResol", "Added successfully, id: "+idResultante);

        }

        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);

        mCamera.release();
        mCamera = null;
        db.close();
    }


    public void leerResoluciones() {
        try {
            SQLiteDatabase db = conn.getReadableDatabase();
            List<CalibrationClass> lista = new ArrayList<CalibrationClass>();
            String[] valores_recuperar = {ConstantNames.CAMPO_ID, ConstantNames.CAMPO_WIDTH, ConstantNames.CAMPO_HEIGHT, ConstantNames.CAMPO_RESOLUTION, ConstantNames.CAMPO_STRINGPARAMS};

            Cursor cursor = db.query(ConstantNames.TABLA_RESOLUTIONS, valores_recuperar,
                    null, null, null, null, null, null);
            cursor.moveToFirst();
            do {
                CalibrationClass resolution = new CalibrationClass(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getString(3), cursor.getString(4));

                lista.add(resolution);
            } while (cursor.moveToNext());
            Log.i("dbCreatorLeerResol", lista.toString());

            db.close();
            cursor.close();

        } catch (Exception e) {
            Log.i("dbCreatorLeerResol", "The bbdd hasn't been consulted");

        }
    }
}
