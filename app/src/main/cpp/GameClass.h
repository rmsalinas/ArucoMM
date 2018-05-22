#ifndef _GameClass_H
#define _GameClass_H

class Game{

    struct Cube{
      cv::Point3f center=cv::Point3f(0,0,0);
      float size;
      std::vector<cv::Point3f> getCubeCorners()const {
          std::vector<cv::Point3f> corners={
cv::Point3f(0,0,0),cv::Point3f(size,0,0),cv::Point3f(size,size,0),cv::Point3f(0,size,0),
cv::Point3f(0,0,size),cv::Point3f(size,0,size),cv::Point3f(size,size,size),cv::Point3f(0,size,size)
            };
            for(auto &c:corners)
                c+=center;

            return corners;

      }
    };

    aruco::MarkerMap _mmap;
    aruco::CameraParameters _camParams;

    aruco::MarkerDetector mdetector;
    aruco::MarkerMapPoseTracker PoseTracker;
    Cube _cube;
    bool isInited=false;

public:
    void setParams(const aruco::CameraParameters &camParams, const aruco::MarkerMap &mmap ) {

        __android_log_write(ANDROID_LOG_INFO, "cubedraw", "0");
        _camParams=camParams;
        _mmap=mmap;
        //put the Cube in the center of the scene
       _cube.size=mmap[0].getMarkerSize();
       mdetector.setDictionary(mmap.getDictionary());
        isInited=false;
    }

    void translateCube(float tx,float ty,float tz){
        _cube.center+=cv::Point3f(tx,ty,tz);
    }

    void draw(cv::Mat &image,const std::vector<aruco::Marker> &markers){

        if (!isInited){
            __android_log_write(ANDROID_LOG_INFO, "cubedraw", "1");
            isInited=true;
            __android_log_write(ANDROID_LOG_INFO, "cubedraw", "2");
            _camParams.resize(image.size());
            __android_log_write(ANDROID_LOG_INFO, "cubedraw", "3");
            PoseTracker.setParams(_camParams,_mmap);
            __android_log_write(ANDROID_LOG_INFO, "cubedraw", "4");
        }

        //find the projection of the intro_tester corners
        if (PoseTracker.estimatePose(markers)){
            __android_log_write(ANDROID_LOG_INFO, "cubedraw", "5");
            auto corners=_cube.getCubeCorners();
            __android_log_write(ANDROID_LOG_INFO, "cubedraw", "6");
            //project on image
            std::vector<cv::Point2f> points2d;
            __android_log_write(ANDROID_LOG_INFO, "cubedraw", "7");
            cv::projectPoints(corners,PoseTracker.getRvec(),PoseTracker.getTvec(),_camParams.CameraMatrix,_camParams.Distorsion,points2d);
            __android_log_write(ANDROID_LOG_INFO, "cubedraw", "8");
            //now, draw lines between them
            for(int i=0;i<4;i++){
                cv::line(image,points2d[i],points2d[(i+1)%4],cv::Scalar(0,255,0),2);
                cv::line(image,points2d[4+i],points2d[4+(i+1)%4],cv::Scalar(255,0,0),2);
                cv::line(image,points2d[i],points2d[(i+4)],cv::Scalar(0,0,255),2);
            }
            __android_log_write(ANDROID_LOG_INFO, "cubedraw", "9");
            //draw markers too
            for(auto m:markers)
                m.draw(image,cv::Scalar(0,0,255),-1,false);
            __android_log_write(ANDROID_LOG_INFO, "cubedraw", "10");
        }
    }

    void draw(cv::Mat &image){
        __android_log_write(ANDROID_LOG_INFO, "cubedraw", "11");
        draw(image, mdetector.detect(image));
        __android_log_write(ANDROID_LOG_INFO, "cubedraw", "12");

    }

};

#endif