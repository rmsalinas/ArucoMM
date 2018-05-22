package com.uco.ava.appcv.arucomm;

import android.content.Context;
import android.preference.DialogPreference;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;


class AboutInfo extends DialogPreference {

    private int mDialogLayoutResId = R.layout.app_info;



    private static Context applicationcontext;

    public AboutInfo(Context context, AttributeSet attrs) {
        super(context, attrs);

        applicationcontext = context;

        // Set the layout here
        setTitle("");
        setPositiveButtonText("Ok");
        setNegativeButtonText("");
        setDialogIcon(null);
    }

    @Override
    public View onCreateDialogView() {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View view = layoutInflater.inflate(R.layout.app_info, null);

        TextView ucoUrl = (TextView) view.findViewById(R.id.uco_txt);
        ucoUrl.setMovementMethod(LinkMovementMethod.getInstance());

        TextView arucoUrl = (TextView) view.findViewById(R.id.aruco_url);
        arucoUrl.setMovementMethod(LinkMovementMethod.getInstance());

        TextView markerMapper = (TextView) view.findViewById(R.id.marker_mapper);
        markerMapper.setMovementMethod(LinkMovementMethod.getInstance());

        TextView floatingMenuUrl = (TextView) view.findViewById(R.id.floating_menu_url);
        floatingMenuUrl.setMovementMethod(LinkMovementMethod.getInstance());

        TextView tutoURL = (TextView) view.findViewById(R.id.tuto_url);
        tutoURL.setMovementMethod(LinkMovementMethod.getInstance());

        TextView apacheUrl = (TextView) view.findViewById(R.id.apache_url);
        apacheUrl.setMovementMethod(LinkMovementMethod.getInstance());

        return view;
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

        } else {
            // User selected Cancel
        }

    }
}
