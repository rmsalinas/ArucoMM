package com.uco.ava.appcv.arucomm;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;

import com.uco.ava.appcv.arucomm.bbdd.ConexionSQLiteHelper;
import com.uco.ava.appcv.arucomm.bbdd.ConstantNames;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by FJMaestre on 20/10/17.
 */

public class AvailableResolutions extends ListPreference {

    private ConexionSQLiteHelper conn=new ConexionSQLiteHelper(getContext(), "bd_resolutions", null, 1);


    public AvailableResolutions(Context context, AttributeSet attrs) {
        super(context, attrs);

        try {
            SQLiteDatabase db = conn.getReadableDatabase();
            List<String> lista = new ArrayList<String>();
            String[] valores_recuperar = {ConstantNames.CAMPO_RESOLUTION, ConstantNames.CAMPO_STRINGPARAMS};

            Cursor cursor = db.query(ConstantNames.TABLA_RESOLUTIONS, valores_recuperar, null, null, null, null, null, null);
            cursor.moveToFirst();
            int tam = cursor.getCount();
            String[] valuesss = new String[tam];
            String[] entriess =  new String[tam];

            for(int i = 0; i<tam; i++){
                String resolution = new String(cursor.getString(0));
                String params = new String(cursor.getString(1));
                cursor.moveToNext();


                if (params.isEmpty()){
                    entriess[i] = resolution+" (Not calibrated)";
                }
                else {
                    entriess[i] = resolution;
                }
                valuesss[i] = String.valueOf(i);
//                setValueIndex(initializeIndex());
            }
            Log.i("dbCreator", lista.toString());

            db.close();
            cursor.close();

            setEntries(entriess);
            setEntryValues(valuesss);
            setPositiveButtonText("Accept");
            setNegativeButtonText("Cancel");

        } catch (Exception e) {
            Log.i("MyCustomlistPrefecence", "The bbdd hasn't been consulted");
        }

    }

    public AvailableResolutions(Context context) {
        this(context, null);
    }

    private int initializeIndex() {
        //here you can provide the value to set (typically retrieved from the SharedPreferences)
        //...

        int i = 1;
        return i;
    }

}