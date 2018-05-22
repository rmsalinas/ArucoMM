package com.uco.ava.appcv.arucomm;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import com.uco.ava.appcv.arucomm.bbdd.CalibrationClass;
import com.uco.ava.appcv.arucomm.bbdd.ConexionSQLiteHelper;
import com.uco.ava.appcv.arucomm.bbdd.ConstantNames;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SendParamsFile extends ListPreference implements OnClickListener {



    private static final String storePath = Environment.getExternalStorageDirectory()+ File.separator + "AVA";
    private static final String paramsPath = storePath + "/CamParams";
    private static final File paramsDir = new File(paramsPath);

    private int choosenOption, tam;
    private ConexionSQLiteHelper conn=new ConexionSQLiteHelper(getContext(), "bd_resolutions", null, 1);

    private List<CalibrationClass> CalibrationClassList = new ArrayList<CalibrationClass>();
    private CalibrationClass CalibClassInstance;



    ArrayList<String> valuess = new ArrayList<String>();
    ArrayList<String> entriess = new ArrayList<String>();
    ArrayList<String> paramss = new ArrayList<String>();
    String positiveButtonStr = "Open";

    Context applicationcontext;


    public SendParamsFile(Context context, AttributeSet attrs) {
        super(context, attrs);

        applicationcontext = context;

        try {
            SQLiteDatabase db = conn.getReadableDatabase();

            List<String> lista = new ArrayList<String>();

            String[] valores_recuperar = {ConstantNames.CAMPO_RESOLUTION, ConstantNames.CAMPO_STRINGPARAMS};

            Cursor cursor = db.query(ConstantNames.TABLA_RESOLUTIONS, valores_recuperar, null, null, null, null, null, null);
            cursor.moveToFirst();

            tam = cursor.getCount();

                for (int i = 0; i < tam; i++) {

                    String resolution = new String(cursor.getString(0));
                    String params = new String(cursor.getString(1));
                    cursor.moveToNext();

                    if (!params.isEmpty()) {
                        entriess.add(resolution);
                        valuess.add(String.valueOf(i));
                        paramss.add(params);
                    }

                }
                Log.i("dbCreator", lista.toString());

                db.close();
                cursor.close();

            if (paramss.isEmpty()) {
                positiveButtonStr = "Close";
                entriess.add("No one resolution calibrated");
                valuess.add(String.valueOf(0));
                tam=0;
            }

                String[] entries = entriess.toArray(new String[entriess.size()]);
                String[] valuess = this.valuess.toArray(new String[this.valuess.size()]);


                setEntries(entries);
                setEntryValues(valuess);
                setValueIndex(0);


        } catch(Exception e){
                Log.i("MyCustomlistView", "The bbdd hasn't been consulted");
            }

    }

    private int getValueIndex() {
        return findIndexOfValue(this.getValue() +"");
    }


    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        choosenOption = getValueIndex();
        builder.setSingleChoiceItems(this.getEntries(), choosenOption, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                choosenOption = which;
            }
        });

        builder.setPositiveButton(positiveButtonStr, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (tam == 0) {
                    dialog.dismiss();
                } else {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(applicationcontext, R.style.StyledDialog);
                    alertDialog.setTitle("Calibration parameters");
                    alertDialog.setMessage(paramsToCompleteString(paramss.get(choosenOption)));
                    alertDialog.setCancelable(false);

                    alertDialog.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface alertDialog, int id) {

                            saveCameraParamsJNI(paramss.get(choosenOption), paramsPath+ File.separator+entriess.get(choosenOption));
                            email(entriess.get(choosenOption));
                            alertDialog.dismiss();
                        }
                    });



                    alertDialog.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface alertDialog, int id) {
                            try {
                                File marker = new File(paramsPath+ File.separator+entriess.get(choosenOption)+".yml");
                                marker.delete();
                            }
                            catch (Exception e){
                            }
                            Toast.makeText(applicationcontext, "File deleted", Toast.LENGTH_SHORT).show();
                            alertDialog.dismiss();                        }
                    });


                    alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface alertDialog, int id) {
                            alertDialog.dismiss();
                        }
                    });
                    alertDialog.setCancelable(false);
                    alertDialog.show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    /**
     * Send camera parameter file for a given resolution
     * @param resolution  - camera esolution
     */
    private void email(String resolution){

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent .setType("vnd.android.cursor.dir/email");

        Context context = applicationcontext;
        Uri filePath = Uri.parse("file://"+paramsPath+ File.separator+resolution+".yml");
//
        // the attachment
        emailIntent.putExtra(Intent.EXTRA_STREAM, filePath);
        // the mail subject
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Aruco camera parameters");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Latest Aruco parameters file attached");

        applicationcontext.startActivity(Intent.createChooser(emailIntent , "Send email..."));
    }


    public  void onClick (DialogInterface dialog, int which)
    {
        this.setValue(this.getEntryValues()[choosenOption]+"");
    }


    /**
     * This function can read the database
     */
    public void leerResoluciones() {
        Log.i("SendParamsFile", "called leerResoluciones");
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


    private native String paramsToCompleteString(String cameraParams);
    private native void saveCameraParamsJNI(String params, String path);
}