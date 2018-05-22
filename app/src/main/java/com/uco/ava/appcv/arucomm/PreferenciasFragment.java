package com.uco.ava.appcv.arucomm;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by fran on 19/07/17.
 */

public class PreferenciasFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferencias);
    }


}