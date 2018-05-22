package com.uco.avaappbeta.bbdd;

/**
 * Created by FJMaestre on 16/10/17.
 */

public class ConstantNames{

    public static final String TABLA_RESOLUTIONS = "resolutions";
    public static final String CAMPO_ID = "id";
    public static final String CAMPO_WIDTH = "width";
    public static final String CAMPO_HEIGHT = "height";
    public static final String CAMPO_RESOLUTION = "resolution";
    public static final String CAMPO_STRINGPARAMS = "stringParams";

    public static final String Create_resolutiondb ="CREATE TABLE IF NOT EXISTS "+TABLA_RESOLUTIONS+" ("+CAMPO_ID+" INTEGER, "+CAMPO_WIDTH+" INTEGER, "+CAMPO_HEIGHT+" INTEGER, "+CAMPO_RESOLUTION+" TEXT, "+CAMPO_STRINGPARAMS+" TEXT)";

}
