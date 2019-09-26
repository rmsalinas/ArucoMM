#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"
#include "aruco/aruco.h"
#include "marker_mapper/markermapper.h"
#include <opencv2/calib3d.hpp>
#include <marker_mapper/debug.h>
#include <android/log.h>
#include "MapperSceneDrawer.h"
#include "GameClass.h"

using namespace std;
using namespace cv;
using namespace aruco;

template<typename T>
std::string _to_string(const T& i){ std::stringstream str;str<<i;return str.str();};

class androidbuf : public std::streambuf {
public:
    enum { bufsize = 128 }; // .. or some other suitable buffer size
    androidbuf() { this->setp(buffer, buffer + bufsize - 1); }

private:
    int overflow(int c)
    {
        if (c == traits_type::eof()) {
            *this->pptr() = traits_type::to_char_type(c);
            this->sbumpc();
        }
        return this->sync()? traits_type::eof(): traits_type::not_eof(c);
    }

    int sync()
    {
        int rc = 0;
        if (this->pbase() != this->pptr()) {
            char writebuf[bufsize+1];
            memcpy(writebuf, this->pbase(), this->pptr() - this->pbase());
            writebuf[this->pptr() - this->pbase()] = '\0';

            rc = __android_log_write(ANDROID_LOG_INFO, "processDebugCOUT", writebuf) > 0;
            this->setp(buffer, buffer + bufsize - 1);
        }
        return rc;
    }

    char buffer[bufsize];
};


String params2String(CameraParameters cp){
    stringstream sstr;
    sstr<<cp;
    string str=sstr.str();

    return str;
}

class MyData{
public:
    MapperSceneDrawer drawer;
    Game cube;
    aruco::MarkerDetector mdetector;
    vector<aruco::Marker> frameMarkers;
    vector<aruco::MarkerMapPoseTracker> poseTrackerVec;
    aruco::MarkerMapPoseTracker poseTracker;
    vector<vector<aruco::Marker>> markerslist;
    vector<vector<aruco::Marker>> contMarkerList;
    vector<vector<aruco::Marker>> maperlist;
    shared_ptr<aruco_mm::MarkerMapper> mmaper;
    float markerSize=1;
    int originMarker=-1, currentWidth, currentHeight;
    bool waitingForClose = false;
    string currentDictionary="ARUCO_MIP_36h12";

    aruco::CameraParameters currentCameraParameters;


    static std::shared_ptr<MyData> singleton(){
        if (!Ptr) {
            Ptr = std::make_shared<MyData>();
            Ptr->mdetector.setDictionary(Ptr->currentDictionary);
            Ptr->mdetector.setDetectionMode(aruco::DM_FAST);

            std::cout.rdbuf(new androidbuf);
        }
        return Ptr;
    }
    static void createMarkerMapper(){
        singleton()->mmaper=aruco_mm::MarkerMapper::create();
        singleton()->mmaper->setParams(singleton()->currentCameraParameters,singleton()->markerSize,singleton()->originMarker);
        singleton()->mmaper->getMarkerDetector ().setDictionary(singleton()->currentDictionary);
    }

    static void setCurrentResolution(int width, int height){

        singleton()->currentWidth = width;
        singleton()->currentHeight = height;
    }

    static void setCurrentCameraParameters(string strparams){

        std::stringstream sstr(strparams);
        sstr>>singleton()->currentCameraParameters;

        __android_log_write(ANDROID_LOG_INFO, "processDebug", strparams.c_str());
    }
    static void setMarkerSize(float size){
        singleton()->markerSize=size;
    }

    static void setMarkerDictionary(string dict){
        singleton()->currentDictionary=dict;
        singleton()->mdetector.setDictionary(dict);
    }

    static void setDrawerParams(int w, int h, aruco::MarkerMap map) {
        singleton()->drawer.setParams(w, h, map);
    }

    static void passZoom(float value){
        singleton()->drawer.zoom(value);
    }

    static void passRotate(float x, float y){
        singleton()->drawer.rotate(x, y);
    }

    static void passTranslate(float x, float y){
        singleton()->drawer.translate(x, y);
    }

    static void dawerDraw(Mat* image, bool showNumbers){
        singleton()->drawer.draw(*image, showNumbers);
//          singleton()->drawer.draw(*image, true, singleton()->poseTracker.getRTMatrix());
    }

    static string toString(CameraParameters cp){

        stringstream sstr;
        sstr << "CameraParameters" << endl;
        sstr << "resolution = " << cp.CamSize.width << "x" << cp.CamSize.height << endl;
        sstr << "fx = " << cp.CameraMatrix.at<float>(0, 0) << endl;
        sstr << "cx = " << cp.CameraMatrix.at<float>(0, 2) << endl;
        sstr << "fy = " << cp.CameraMatrix.at<float>(1, 1) << endl;
        sstr << "cy = " << cp.CameraMatrix.at<float>(1, 2) << endl;
        sstr << "k1 = " << cp.Distorsion.ptr<float>(0)[2] << endl;
        sstr << "k2 = " << cp.Distorsion.ptr<float>(0)[3] << endl;
        sstr << "p1 = " << cp.Distorsion.ptr<float>(0)[0] << endl;
        sstr << "p2 = " << cp.Distorsion.ptr<float>(0)[1] << endl;
        sstr << "p3 = " << cp.Distorsion.ptr<float>(0)[4] << endl;
        return sstr.str();
    }



private:
    static std::shared_ptr<MyData> Ptr;

};


std::shared_ptr<MyData> MyData::Ptr;


//portable calibration function
aruco::CameraParameters cameraCalibrate(std::vector<std::vector<aruco::Marker> >  &allMarkers, int imageWidth,int imageHeight,float markerSize,float *currRepjErr){


    unsigned char default_a4_board[] = {
            0x30, 0x20, 0x32, 0x34, 0x20, 0x31, 0x36, 0x31, 0x20, 0x34, 0x20, 0x2d,
            0x31, 0x30, 0x30, 0x30, 0x20, 0x2d, 0x31, 0x30, 0x30, 0x30, 0x20, 0x30,
            0x20, 0x2d, 0x35, 0x30, 0x30, 0x20, 0x2d, 0x31, 0x30, 0x30, 0x30, 0x20,
            0x30, 0x20, 0x2d, 0x35, 0x30, 0x30, 0x20, 0x2d, 0x31, 0x35, 0x30, 0x30,
            0x20, 0x30, 0x20, 0x2d, 0x31, 0x30, 0x30, 0x30, 0x20, 0x2d, 0x31, 0x35,
            0x30, 0x30, 0x20, 0x30, 0x20, 0x32, 0x32, 0x37, 0x20, 0x34, 0x20, 0x2d,
            0x34, 0x30, 0x30, 0x20, 0x2d, 0x31, 0x30, 0x30, 0x30, 0x20, 0x30, 0x20,
            0x31, 0x30, 0x30, 0x20, 0x2d, 0x31, 0x30, 0x30, 0x30, 0x20, 0x30, 0x20,
            0x31, 0x30, 0x30, 0x20, 0x2d, 0x31, 0x35, 0x30, 0x30, 0x20, 0x30, 0x20,
            0x2d, 0x34, 0x30, 0x30, 0x20, 0x2d, 0x31, 0x35, 0x30, 0x30, 0x20, 0x30,
            0x20, 0x38, 0x35, 0x20, 0x34, 0x20, 0x32, 0x30, 0x30, 0x20, 0x2d, 0x31,
            0x30, 0x30, 0x30, 0x20, 0x30, 0x20, 0x37, 0x30, 0x30, 0x20, 0x2d, 0x31,
            0x30, 0x30, 0x30, 0x20, 0x30, 0x20, 0x37, 0x30, 0x30, 0x20, 0x2d, 0x31,
            0x35, 0x30, 0x30, 0x20, 0x30, 0x20, 0x32, 0x30, 0x30, 0x20, 0x2d, 0x31,
            0x35, 0x30, 0x30, 0x20, 0x30, 0x20, 0x31, 0x36, 0x36, 0x20, 0x34, 0x20,
            0x38, 0x30, 0x30, 0x20, 0x2d, 0x31, 0x30, 0x30, 0x30, 0x20, 0x30, 0x20,
            0x31, 0x33, 0x30, 0x30, 0x20, 0x2d, 0x31, 0x30, 0x30, 0x30, 0x20, 0x30,
            0x20, 0x31, 0x33, 0x30, 0x30, 0x20, 0x2d, 0x31, 0x35, 0x30, 0x30, 0x20,
            0x30, 0x20, 0x38, 0x30, 0x30, 0x20, 0x2d, 0x31, 0x35, 0x30, 0x30, 0x20,
            0x30, 0x20, 0x32, 0x34, 0x34, 0x20, 0x34, 0x20, 0x2d, 0x31, 0x30, 0x30,
            0x30, 0x20, 0x2d, 0x34, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x35, 0x30,
            0x30, 0x20, 0x2d, 0x34, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x35, 0x30,
            0x30, 0x20, 0x2d, 0x39, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x31, 0x30,
            0x30, 0x30, 0x20, 0x2d, 0x39, 0x30, 0x30, 0x20, 0x30, 0x20, 0x31, 0x34,
            0x34, 0x20, 0x34, 0x20, 0x2d, 0x34, 0x30, 0x30, 0x20, 0x2d, 0x34, 0x30,
            0x30, 0x20, 0x30, 0x20, 0x31, 0x30, 0x30, 0x20, 0x2d, 0x34, 0x30, 0x30,
            0x20, 0x30, 0x20, 0x31, 0x30, 0x30, 0x20, 0x2d, 0x39, 0x30, 0x30, 0x20,
            0x30, 0x20, 0x2d, 0x34, 0x30, 0x30, 0x20, 0x2d, 0x39, 0x30, 0x30, 0x20,
            0x30, 0x20, 0x39, 0x30, 0x20, 0x34, 0x20, 0x32, 0x30, 0x30, 0x20, 0x2d,
            0x34, 0x30, 0x30, 0x20, 0x30, 0x20, 0x37, 0x30, 0x30, 0x20, 0x2d, 0x34,
            0x30, 0x30, 0x20, 0x30, 0x20, 0x37, 0x30, 0x30, 0x20, 0x2d, 0x39, 0x30,
            0x30, 0x20, 0x30, 0x20, 0x32, 0x30, 0x30, 0x20, 0x2d, 0x39, 0x30, 0x30,
            0x20, 0x30, 0x20, 0x32, 0x31, 0x34, 0x20, 0x34, 0x20, 0x38, 0x30, 0x30,
            0x20, 0x2d, 0x34, 0x30, 0x30, 0x20, 0x30, 0x20, 0x31, 0x33, 0x30, 0x30,
            0x20, 0x2d, 0x34, 0x30, 0x30, 0x20, 0x30, 0x20, 0x31, 0x33, 0x30, 0x30,
            0x20, 0x2d, 0x39, 0x30, 0x30, 0x20, 0x30, 0x20, 0x38, 0x30, 0x30, 0x20,
            0x2d, 0x39, 0x30, 0x30, 0x20, 0x30, 0x20, 0x31, 0x35, 0x33, 0x20, 0x34,
            0x20, 0x2d, 0x31, 0x30, 0x30, 0x30, 0x20, 0x32, 0x30, 0x30, 0x20, 0x30,
            0x20, 0x2d, 0x35, 0x30, 0x30, 0x20, 0x32, 0x30, 0x30, 0x20, 0x30, 0x20,
            0x2d, 0x35, 0x30, 0x30, 0x20, 0x2d, 0x33, 0x30, 0x30, 0x20, 0x30, 0x20,
            0x2d, 0x31, 0x30, 0x30, 0x30, 0x20, 0x2d, 0x33, 0x30, 0x30, 0x20, 0x30,
            0x20, 0x37, 0x20, 0x34, 0x20, 0x2d, 0x34, 0x30, 0x30, 0x20, 0x32, 0x30,
            0x30, 0x20, 0x30, 0x20, 0x31, 0x30, 0x30, 0x20, 0x32, 0x30, 0x30, 0x20,
            0x30, 0x20, 0x31, 0x30, 0x30, 0x20, 0x2d, 0x33, 0x30, 0x30, 0x20, 0x30,
            0x20, 0x2d, 0x34, 0x30, 0x30, 0x20, 0x2d, 0x33, 0x30, 0x30, 0x20, 0x30,
            0x20, 0x31, 0x34, 0x33, 0x20, 0x34, 0x20, 0x32, 0x30, 0x30, 0x20, 0x32,
            0x30, 0x30, 0x20, 0x30, 0x20, 0x37, 0x30, 0x30, 0x20, 0x32, 0x30, 0x30,
            0x20, 0x30, 0x20, 0x37, 0x30, 0x30, 0x20, 0x2d, 0x33, 0x30, 0x30, 0x20,
            0x30, 0x20, 0x32, 0x30, 0x30, 0x20, 0x2d, 0x33, 0x30, 0x30, 0x20, 0x30,
            0x20, 0x32, 0x31, 0x39, 0x20, 0x34, 0x20, 0x38, 0x30, 0x30, 0x20, 0x32,
            0x30, 0x30, 0x20, 0x30, 0x20, 0x31, 0x33, 0x30, 0x30, 0x20, 0x32, 0x30,
            0x30, 0x20, 0x30, 0x20, 0x31, 0x33, 0x30, 0x30, 0x20, 0x2d, 0x33, 0x30,
            0x30, 0x20, 0x30, 0x20, 0x38, 0x30, 0x30, 0x20, 0x2d, 0x33, 0x30, 0x30,
            0x20, 0x30, 0x20, 0x37, 0x38, 0x20, 0x34, 0x20, 0x2d, 0x31, 0x30, 0x30,
            0x30, 0x20, 0x38, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x35, 0x30, 0x30,
            0x20, 0x38, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x35, 0x30, 0x30, 0x20,
            0x33, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x31, 0x30, 0x30, 0x30, 0x20,
            0x33, 0x30, 0x30, 0x20, 0x30, 0x20, 0x31, 0x35, 0x39, 0x20, 0x34, 0x20,
            0x2d, 0x34, 0x30, 0x30, 0x20, 0x38, 0x30, 0x30, 0x20, 0x30, 0x20, 0x31,
            0x30, 0x30, 0x20, 0x38, 0x30, 0x30, 0x20, 0x30, 0x20, 0x31, 0x30, 0x30,
            0x20, 0x33, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x34, 0x30, 0x30, 0x20,
            0x33, 0x30, 0x30, 0x20, 0x30, 0x20, 0x32, 0x30, 0x39, 0x20, 0x34, 0x20,
            0x32, 0x30, 0x30, 0x20, 0x38, 0x30, 0x30, 0x20, 0x30, 0x20, 0x37, 0x30,
            0x30, 0x20, 0x38, 0x30, 0x30, 0x20, 0x30, 0x20, 0x37, 0x30, 0x30, 0x20,
            0x33, 0x30, 0x30, 0x20, 0x30, 0x20, 0x32, 0x30, 0x30, 0x20, 0x33, 0x30,
            0x30, 0x20, 0x30, 0x20, 0x31, 0x33, 0x20, 0x34, 0x20, 0x38, 0x30, 0x30,
            0x20, 0x38, 0x30, 0x30, 0x20, 0x30, 0x20, 0x31, 0x33, 0x30, 0x30, 0x20,
            0x38, 0x30, 0x30, 0x20, 0x30, 0x20, 0x31, 0x33, 0x30, 0x30, 0x20, 0x33,
            0x30, 0x30, 0x20, 0x30, 0x20, 0x38, 0x30, 0x30, 0x20, 0x33, 0x30, 0x30,
            0x20, 0x30, 0x20, 0x32, 0x34, 0x37, 0x20, 0x34, 0x20, 0x2d, 0x31, 0x30,
            0x30, 0x30, 0x20, 0x31, 0x34, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x35,
            0x30, 0x30, 0x20, 0x31, 0x34, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x35,
            0x30, 0x30, 0x20, 0x39, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x31, 0x30,
            0x30, 0x30, 0x20, 0x39, 0x30, 0x30, 0x20, 0x30, 0x20, 0x32, 0x33, 0x37,
            0x20, 0x34, 0x20, 0x2d, 0x34, 0x30, 0x30, 0x20, 0x31, 0x34, 0x30, 0x30,
            0x20, 0x30, 0x20, 0x31, 0x30, 0x30, 0x20, 0x31, 0x34, 0x30, 0x30, 0x20,
            0x30, 0x20, 0x31, 0x30, 0x30, 0x20, 0x39, 0x30, 0x30, 0x20, 0x30, 0x20,
            0x2d, 0x34, 0x30, 0x30, 0x20, 0x39, 0x30, 0x30, 0x20, 0x30, 0x20, 0x31,
            0x30, 0x30, 0x20, 0x34, 0x20, 0x32, 0x30, 0x30, 0x20, 0x31, 0x34, 0x30,
            0x30, 0x20, 0x30, 0x20, 0x37, 0x30, 0x30, 0x20, 0x31, 0x34, 0x30, 0x30,
            0x20, 0x30, 0x20, 0x37, 0x30, 0x30, 0x20, 0x39, 0x30, 0x30, 0x20, 0x30,
            0x20, 0x32, 0x30, 0x30, 0x20, 0x39, 0x30, 0x30, 0x20, 0x30, 0x20, 0x36,
            0x20, 0x34, 0x20, 0x38, 0x30, 0x30, 0x20, 0x31, 0x34, 0x30, 0x30, 0x20,
            0x30, 0x20, 0x31, 0x33, 0x30, 0x30, 0x20, 0x31, 0x34, 0x30, 0x30, 0x20,
            0x30, 0x20, 0x31, 0x33, 0x30, 0x30, 0x20, 0x39, 0x30, 0x30, 0x20, 0x30,
            0x20, 0x38, 0x30, 0x30, 0x20, 0x39, 0x30, 0x30, 0x20, 0x30, 0x20, 0x31,
            0x37, 0x37, 0x20, 0x34, 0x20, 0x2d, 0x31, 0x30, 0x30, 0x30, 0x20, 0x32,
            0x30, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x35, 0x30, 0x30, 0x20, 0x32,
            0x30, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x35, 0x30, 0x30, 0x20, 0x31,
            0x35, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x31, 0x30, 0x30, 0x30, 0x20,
            0x31, 0x35, 0x30, 0x30, 0x20, 0x30, 0x20, 0x39, 0x33, 0x20, 0x34, 0x20,
            0x2d, 0x34, 0x30, 0x30, 0x20, 0x32, 0x30, 0x30, 0x30, 0x20, 0x30, 0x20,
            0x31, 0x30, 0x30, 0x20, 0x32, 0x30, 0x30, 0x30, 0x20, 0x30, 0x20, 0x31,
            0x30, 0x30, 0x20, 0x31, 0x35, 0x30, 0x30, 0x20, 0x30, 0x20, 0x2d, 0x34,
            0x30, 0x30, 0x20, 0x31, 0x35, 0x30, 0x30, 0x20, 0x30, 0x20, 0x38, 0x36,
            0x20, 0x34, 0x20, 0x32, 0x30, 0x30, 0x20, 0x32, 0x30, 0x30, 0x30, 0x20,
            0x30, 0x20, 0x37, 0x30, 0x30, 0x20, 0x32, 0x30, 0x30, 0x30, 0x20, 0x30,
            0x20, 0x37, 0x30, 0x30, 0x20, 0x31, 0x35, 0x30, 0x30, 0x20, 0x30, 0x20,
            0x32, 0x30, 0x30, 0x20, 0x31, 0x35, 0x30, 0x30, 0x20, 0x30, 0x20, 0x32,
            0x32, 0x39, 0x20, 0x34, 0x20, 0x38, 0x30, 0x30, 0x20, 0x32, 0x30, 0x30,
            0x30, 0x20, 0x30, 0x20, 0x31, 0x33, 0x30, 0x30, 0x20, 0x32, 0x30, 0x30,
            0x30, 0x20, 0x30, 0x20, 0x31, 0x33, 0x30, 0x30, 0x20, 0x31, 0x35, 0x30,
            0x30, 0x20, 0x30, 0x20, 0x38, 0x30, 0x30, 0x20, 0x31, 0x35, 0x30, 0x30,
            0x20, 0x30, 0x20, 0x41, 0x52, 0x55, 0x43, 0x4f, 0x5f, 0x4d, 0x49, 0x50,
            0x5f, 0x33, 0x36, 0x68, 0x31, 0x32
    };
    unsigned int default_a4_board_size = 1254;
    // given the set of markers detected, the function determines the get the 2d-3d correspondes
    auto getMarker2d_3d_=[](vector<cv::Point2f>& p2d, vector<cv::Point3f>& p3d, const vector<Marker>& markers_detected,
                            const MarkerMap& bc)
    {
        p2d.clear();
        p3d.clear();
        // for each detected marker
        for (size_t i = 0; i < markers_detected.size(); i++)
        {
            // find it in the bc
            auto fidx = std::string::npos;
            for (size_t j = 0; j < bc.size() && fidx == std::string::npos; j++)
                if (bc[j].id == markers_detected[i].id)
                    fidx = j;
            if (fidx != std::string::npos)
            {
                for (int j = 0; j < 4; j++)
                {
                    p2d.push_back(markers_detected[i][j]);
                    p3d.push_back(bc[fidx][j]);
                }
            }
        }
    };

    aruco::MarkerMap mmap;
    stringstream sstr;
    sstr.write((char*)default_a4_board, default_a4_board_size);
    mmap.fromStream(sstr);
    if (!mmap.isExpressedInMeters())
        mmap = mmap.convertToMeters(static_cast<float>( markerSize));



    vector<vector<cv::Point2f> >calib_p2d;
    vector<vector<cv::Point3f> > calib_p3d;

    for(auto &detected_markers:allMarkers){
        vector<cv::Point2f> p2d;
        vector<cv::Point3f> p3d;

        getMarker2d_3d_(p2d, p3d, detected_markers, mmap);
        if (p3d.size() > 0)
        {
            calib_p2d.push_back(p2d);
            calib_p3d.push_back(p3d);
        }
    }

    vector<cv::Mat> vr, vt;
    CameraParameters cameraParams;
    cameraParams.CamSize = cv::Size(imageWidth,imageHeight);
    float err=cv::calibrateCamera(calib_p3d, calib_p2d, cameraParams.CamSize, cameraParams.CameraMatrix, cameraParams.Distorsion, vr, vt);
    cameraParams.CameraMatrix.convertTo(cameraParams.CameraMatrix,CV_32F);
    cameraParams.Distorsion.convertTo(cameraParams.Distorsion,CV_32F);
    if (currRepjErr!=0) *currRepjErr=err;

    stringstream str;
    str<<cameraParams;
    str<<" cm="<<cameraParams.CameraMatrix;
    __android_log_write(ANDROID_LOG_INFO, "processDebug calibFunction", str.str().c_str());
    return cameraParams;
}






/** Natine c++ functions */
/** Send a simple string to the log. INFO priority*/
int writeLog (const char* tag, const char* text){
    int a = __android_log_write(ANDROID_LOG_INFO, tag, text);
    return a;
}
/** Send a simple string to the log.*/
int writeLog (int prio, const char* tag, const char* text){
    int a = __android_log_write(prio, tag, text);
    return a;
}


void mat2markers(Mat* input, vector<Marker> &marker){
    Mat grey;
    cvtColor(*input,grey,CV_RGBA2GRAY);
    marker = MyData::singleton()->mdetector.detect(grey);
}

CameraParameters string2params(String str){
    aruco::CameraParameters cp;
    stringstream sstr(str);
    sstr>>cp;

    return cp;
}

std::string ConvertJString(JNIEnv* env, jstring str)
{
    const jsize len = env->GetStringUTFLength(str);
    const char* strChars = env->GetStringUTFChars(str, (jboolean *)0);

    std::string Result(strChars, len);
    env->ReleaseStringUTFChars(str, strChars);

    return Result;
}

void prueba(string path){


    MyData::singleton()->poseTrackerVec.clear();


    aruco::MarkerMapPoseTracker PoseTracker;

    MarkerMap map(path+".yml");
for(auto m:map)
    __android_log_write(ANDROID_LOG_INFO, "nativFlowM: ", std::string(_to_string(m.id)).c_str());


    MyData::singleton()->poseTracker.setParams(MyData::singleton()->currentCameraParameters, map);


    MyData::singleton()->poseTrackerVec.resize(MyData::singleton()->maperlist.size());

//    writeLog("nativFlowM: ", _to_string(MyData::singleton()->maperlist.size()).c_str());

    stringstream ss;
    auto vec = MyData::singleton()->maperlist[0];
    ss << MyData::singleton()->currentCameraParameters.CamSize.width << " ";
    ss << MyData::singleton()->currentCameraParameters.CamSize.height  << endl;
    ss << vec.size()<<endl;

    for (int m = 0; m < vec.size(); m++){
        ss <<vec[m].id<<" ";
        for(int c=0;c<4;c++)
            ss << vec[m][c].x <<" "<< vec[m][c].y<< " ";
        ss <<endl;
    }

    __android_log_write(ANDROID_LOG_INFO, "nativFlowM: ", ss.str().c_str());
    ss.str("");


    bool returned = MyData::singleton()->poseTracker.estimatePose(vec);
    __android_log_write(ANDROID_LOG_INFO, "nativFlowReturned: ", _to_string(returned).c_str());





    Mat mat = MyData::singleton()->poseTracker.getRTMatrix();
    cv::Size size = mat.size();

    int total = size.width * size.height * mat.channels();
    ss << "Mat size = " << total << std::endl;
    std::vector<uchar> data(mat.ptr(), mat.ptr() + total);
    std::string s(data.begin(), data.end());
    ss << "String size = " << s.length() << std::endl;

    __android_log_write(ANDROID_LOG_INFO, "nativFlowP: ", ss.str().c_str());



//    for(int i=0; i<MyData::singleton()->maperlist.size(); i++){
//        MyData::singleton()->poseTrackerVec[i].estimatePose(MyData::singleton()->maperlist[i]);
//
//        MyData::singleton()->poseTracker.estimatePose(MyData::singleton()->maperlist[i]);
//    }


}


void cleanList() {
    MyData::singleton()->markerslist.clear();
    MyData::singleton()->maperlist.clear();
    MyData::singleton()->contMarkerList.clear();
}

void markersToFile(string path){

    auto vec = MyData::singleton()->maperlist;
    ofstream output_file(path + ".txt");
    output_file << MyData::singleton()->currentCameraParameters.CamSize.width << " ";
    output_file << MyData::singleton()->currentCameraParameters.CamSize.height  << endl;
    output_file << vec.size()<<endl;
    for (int i = 0; i < vec.size(); i++) {
        output_file << vec[i].size() <<endl;
        for (int m = 0; m < vec[i].size(); m++){
            output_file <<vec[i][m].id<<" ";
            for(int c=0;c<4;c++)
                output_file << vec[i][m][c].x <<" "<< vec[i][m][c].y<< " ";
            output_file <<endl;
        }
    }
    MyData::singleton()->maperlist.clear();

}

String markersFromFile(string path){

    auto &vec=MyData::singleton()->maperlist;
    vec.clear();
    ifstream file(path + ".txt");

    if (!file)
        return "The markers file doesn't exist";
    else {
        int width, height;
        file >> width >> height;
        if (width != MyData::singleton()->currentWidth ||
            height != MyData::singleton()->currentHeight)

            return "Change resolution to "+_to_string(width)+"x"+_to_string(height)+" and try again";
        else{

            int size;
            file >> size;
            vec.resize(size);

            for (int i = 0; i < vec.size(); i++) {
                file >> size;
                vec[i].resize(size);
                for (int m = 0; m < vec[i].size(); m++) {
                    vec[i][m].resize(4);
                    file >> vec[i][m].id;
                    for (int c = 0; c < 4; c++)
                        file >> vec[i][m][c].x >> vec[i][m][c].y;
                }
            }
            return "";
        }
    }
}




// Dentro de extern "C" deben ir las funciones con la interfaz de JNI
// Estas pueden hacer llamadas a funciones en C++ puro fuera de el modulo extern "C"

/**JNI functions*/

extern "C" {

/**MainActivity JNI functions*/

JNIEXPORT jstring JNICALL
Java_com_uco_avaappbeta_MainActivity_stringFromJNI
        (JNIEnv *env, jobject instance) {

    string s("Escribir un mensaje cualquiera");
    writeLog(ANDROID_LOG_INFO, "depuracion", "prueba de LOG");

    string hello = s;

    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_markerDetectionJNI
        (JNIEnv *env, jobject,
         jlong addrRgba, jstring markerJstr){

    Mat &mat = *(Mat *) addrRgba;
    string markerType = ConvertJString( env, markerJstr);

    mat2markers(&mat, MyData::singleton()->frameMarkers);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_drawMarkerJNI
        (JNIEnv *env, jobject,
         jlong addrRgba){

    Mat &mat = *(Mat *) addrRgba;
    for(auto m:MyData::singleton()->frameMarkers)
        m.draw(mat,Scalar(0,0,255),2,false);
}


JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_storeMarkersDetectionJNI
        (JNIEnv *env, jobject){
    MyData::singleton()->contMarkerList.push_back(MyData::singleton()->frameMarkers);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_setCurrentCameraParametersJNI
        (JNIEnv *env, jobject,
         jstring jparams){

    string params = ConvertJString(env, jparams);
    MyData::setCurrentCameraParameters(params);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_setCurrentResolutionJNI
        (JNIEnv *env, jobject,
         jint width, jint height){

    MyData::setCurrentResolution((int) width, (int) height);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_setMarkerSizeJNI
        (JNIEnv *env, jobject,
         jfloat jsize){
    MyData::setMarkerSize(jsize);
}


JNIEXPORT long JNICALL
Java_com_uco_avaappbeta_MainActivity_markerListJNI
        (JNIEnv*, jobject){

    if (!MyData::singleton()->frameMarkers.empty())
        MyData::singleton()->markerslist.push_back(MyData::singleton()->frameMarkers);

    return MyData::singleton()->markerslist.size();
}

JNIEXPORT jobjectArray JNICALL
Java_com_uco_avaappbeta_MainActivity_maperListJNI
        (JNIEnv *env, jobject){

    aruco_mm::debug::Debug::setLevel(5);
    aruco_mm::debug::Debug::clearStringDebugInfo();

    bool added = MyData::singleton()->mmaper -> process(MyData::singleton()->frameMarkers , -1, true);
    string msg;

    if (added) {
        MyData::singleton()->maperlist.push_back(MyData::singleton()->frameMarkers);
        msg="";
    }
    else {
        msg = aruco_mm::debug::Debug::getStringDebugInfo();
    }

    //returns
    //Se declra una estructura de 2 elementos para devolver el error y los parametros
    jobjectArray sizeYmsg = env->NewObjectArray(2, env->FindClass("java/lang/String"), nullptr);

    //Se introducen los valores para devolver
    env->SetObjectArrayElement(sizeYmsg, 0, env->NewStringUTF(_to_string(MyData::singleton()->mmaper->getFrameSet().size()).c_str()));
    env->SetObjectArrayElement(sizeYmsg, 1, env->NewStringUTF(msg.c_str()));

    return sizeYmsg;
}

JNIEXPORT jobjectArray JNICALL
Java_com_uco_avaappbeta_MainActivity_calibrationJNI
        (JNIEnv *env, jobject instance,
         jint width, jint height){

    float currRepjErr;
    CameraParameters params;

    //se optiene los parametros de la calibración y convierten en String
    params = cameraCalibrate(MyData::singleton()->markerslist, width, height, 1, &currRepjErr);
    string str=params2String(params);


    //se limpia la lista de marcadores para la siguiente calibración
    MyData::singleton()->markerslist.clear();

    //Se convierte el error (float) en texto
    char charcurrRepjErr[50];
    sprintf (charcurrRepjErr, "%f", currRepjErr);

    //Se declra una estructura de 2 elementos para devolver el error y los parametros
    jobjectArray ParamYErr= env->NewObjectArray(2, env->FindClass("java/lang/String"), nullptr);


    //Se introducen los valores para devolver
    const char * parameters = str.c_str();
    env->SetObjectArrayElement(ParamYErr, 0, env->NewStringUTF(parameters));
    env->SetObjectArrayElement(ParamYErr, 1, env->NewStringUTF(charcurrRepjErr));

    return ParamYErr;
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_cleanListJNI
        (JNIEnv *env, jobject){

    //se limpia la lista de marcadores para la siguiente calibración
    cleanList();
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_cleanMaperlistJNI
        (JNIEnv *env, jobject){

    MyData::singleton()->maperlist.clear();
}

JNIEXPORT long JNICALL
Java_com_uco_avaappbeta_MainActivity_addMoreMarkersJNI
        (JNIEnv *env, jobject){

    //adañir process para todos los marcadores de maperlist
    for(auto x:MyData::singleton()->maperlist)
        MyData::singleton()->mmaper -> process(x , -1, true);

    return MyData::singleton()->mmaper->getFrameSet().size();
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_mapingJNI
        (JNIEnv *env, jobject) {

    //Si no hay un hilo para mapear creado
    if(MyData::singleton()->waitingForClose==false) {

        //se crea un hilo paa crear un mapa
        MyData::singleton()->waitingForClose=true;

        //se coloca el nivel de debug
        aruco_mm::debug::Debug::setLevel(5);

        MyData::singleton()->mmaper->optimize(false);
    }
}

JNIEXPORT jobjectArray JNICALL
Java_com_uco_avaappbeta_MainActivity_isOptimizationFinishedJNI
        (JNIEnv *env, jobject) {

    bool finished = false;

    //
    jobjectArray ParamYErr = env->NewObjectArray(2, env->FindClass("java/lang/String"), nullptr);

    string cad = aruco_mm::debug::Debug::getStringDebugInfo();
    finished = MyData::singleton()->mmaper->isOptimizationFinished();

    env->SetObjectArrayElement(ParamYErr, 0, env->NewStringUTF(_to_string(finished).c_str()));
    env->SetObjectArrayElement(ParamYErr, 1, env->NewStringUTF(cad.c_str()));


    if (finished && MyData::singleton()->waitingForClose) {
        MyData::singleton()->waitingForClose=false;
        MyData::singleton()->mmaper->waitForOptimizationFinished();
        writeLog("Mapping Treat: ", "Cerrado");
    }
    return ParamYErr;
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_savecurrentMarkerMapJNI
        (JNIEnv *env, jobject,
         jstring jparams) {

    string completePath = ConvertJString(env, jparams);
    MyData::singleton()->mmaper->getMarkerMap().saveToFile(completePath + ".yml");
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_saveCameraParamsJNI
        (JNIEnv *env, jobject,
         jstring jparams){

    string completePath= ConvertJString(env, jparams);
    MyData::singleton()->currentCameraParameters.saveToFile(completePath+".yml");
}


JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_setMarkerDictionaryJNI
        (JNIEnv *env, jobject,
         jstring jparams){

    string file= ConvertJString(env, jparams);
    MyData::singleton()->setMarkerDictionary(file);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_createMarkerMapperMarkerJNI
        (JNIEnv *env, jobject ) {

    MyData::singleton()->createMarkerMapper();
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_cubeDrawJNI
        (JNIEnv *env, jobject,
         jlong addrRgba){

    Mat &mat = *(Mat *) addrRgba;
    Mat rgb;
    cvtColor(mat,rgb,CV_RGBA2RGB);

    MyData::singleton()->cube.draw(rgb, MyData::singleton()->frameMarkers);
    cvtColor(rgb,mat,CV_RGB2RGBA);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_MainActivity_markersToFileJNI
        (JNIEnv *env, jobject, jstring jpath) {

    string path = ConvertJString(env, jpath);
    markersToFile(path);
}


JNIEXPORT jstring JNICALL
Java_com_uco_avaappbeta_MainActivity_markersFromFileJNI
        (JNIEnv *env, jobject, jstring jpath) {

    string path = ConvertJString(env, jpath);
    String msg = markersFromFile(path);

    return env->NewStringUTF(msg.c_str());
}



/**VisualiserActivity JNI functions*/


JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_VisualiserActivity_zoomJNI
        (JNIEnv *env, jobject,
         jfloat value) {

    MyData::singleton()->passZoom(value);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_VisualiserActivity_rotateJNI
        (JNIEnv *env, jobject,
         jfloat x, jfloat y) {

    MyData::singleton()->passRotate(x,y);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_VisualiserActivity_translateJNI
        (JNIEnv *env, jobject,
         jfloat x, jfloat y) {

    MyData::singleton()->passTranslate(x,y);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_VisualiserActivity_setDrawerParamsJNI
        (JNIEnv *env, jobject,
         jint w, jint h, jstring jpath) {

    string path = ConvertJString( env, jpath);
    aruco::MarkerMap map(path);
    MyData::singleton()->setDrawerParams(w, h, map);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_VisualiserActivity_dawerDrawJNI
        (JNIEnv *env, jobject, jlong ntvImage, jint showNumbers) {

    Mat &image = *(Mat *) ntvImage;
    MyData::singleton()->dawerDraw(&image, (bool) showNumbers!=0);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_VisualiserActivity_setCubeParamsJNI
        (JNIEnv *env, jobject, jstring jpath) {

    string path = ConvertJString( env, jpath);
    aruco::MarkerMap map(path);

    //game
    MyData::singleton()->cube.setParams(MyData::singleton()->currentCameraParameters, map);
}



/**myPreferenceList JNI functions*/

JNIEXPORT jstring JNICALL
Java_com_uco_avaappbeta_SendParamsFile_paramsToCompleteString
        (JNIEnv *env, jobject,
         jstring jparams) {

    aruco::CameraParameters cp = string2params(ConvertJString(env, jparams));
    string params = MyData::toString(cp);

    return env->NewStringUTF(params.c_str());
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_SendParamsFile_saveCameraParamsJNI
        (JNIEnv *env, jobject,
         jstring jparams, jstring jpath){

    aruco::CameraParameters cp = string2params(ConvertJString(env, jparams));

    string completePath= ConvertJString(env, jpath);
    cp.saveToFile(completePath+".yml");
}

/**RealTime JNI functions*/


JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_RealTime_zoomJNI
        (JNIEnv *env, jobject,
         jfloat value) {

    MyData::singleton()->passZoom(value);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_RealTime_rotateJNI
        (JNIEnv *env, jobject,
         jfloat x, jfloat y) {

    MyData::singleton()->passRotate(x,y);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_RealTime_translateJNI
        (JNIEnv *env, jobject,
         jfloat x, jfloat y) {

    MyData::singleton()->passTranslate(x,y);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_RealTime_setDrawerParamsJNI
        (JNIEnv *env, jobject,
         jint w, jint h, jstring jpath) {

    string path = ConvertJString( env, jpath);
    aruco::MarkerMap map(path);
    MyData::singleton()->setDrawerParams(w, h, map);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_RealTime_dawerDrawJNI
        (JNIEnv *env, jobject, jlong ntvImage, jint showNumbers) {

    Mat &image = *(Mat *) ntvImage;
    MyData::singleton()->dawerDraw(&image, (bool) showNumbers!=0);
}

JNIEXPORT void JNICALL
Java_com_uco_avaappbeta_RealTime_setCubeParamsJNI
        (JNIEnv *env, jobject, jstring jpath) {

    string path = ConvertJString( env, jpath);
    aruco::MarkerMap map(path);

    //game
    MyData::singleton()->cube.setParams(MyData::singleton()->currentCameraParameters, map);
}

};//extern "C"
