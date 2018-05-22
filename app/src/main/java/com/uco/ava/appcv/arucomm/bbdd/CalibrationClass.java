package com.uco.ava.appcv.arucomm.bbdd;

/**
 * Created by FJMaestre on 16/10/17.
 */

public class CalibrationClass {

    private int id;
    private int width;
    private int height;
    private String resolution;
    private String stringParams;


    public CalibrationClass() {
    }

    //constructor completo
    public CalibrationClass(int id, int width, int height, String resolution, String stringParams) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.resolution = resolution;
        this.stringParams = stringParams;
    }

    //constructor sin String resolution
    public CalibrationClass(int id, int width, int height, String stringParams) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.resolution = String.valueOf(width) + "x" + String.valueOf(height);
        this.stringParams = stringParams;
    }

    //constructor sin String resolution ni stringParams
    public CalibrationClass(int id, int width, int height) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.resolution = String.valueOf(width) + "x" + String.valueOf(height);
        this.stringParams = "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getStringParams() {
        return stringParams;
    }

    public void setStringParams(String stringParams) {
        this.stringParams = stringParams;
    }


    @Override
    public String toString() {
        return  "\nid=" + id +
                ",\nwidth=" + width +
                ",\nheight=" + height +
                ",\nresolution='" + resolution + '\'' +
                ",\nstringParams='" + stringParams + '\'' +
                ",\n-----------='"+
                '}';
    }
}
