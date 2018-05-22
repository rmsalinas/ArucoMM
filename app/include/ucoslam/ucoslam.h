#ifndef UCOSLAM_H
#define UCOSLAM_H
#include <vector>
#include <cstdint>
#include <string>
#include <iostream>
#include <aruco/cameraparameters.h>
#include <aruco/markerdetector.h>
namespace ucoslam {

//types of descriptors that can be used
class DescriptorTypes  {
public:
    enum Type: std::int8_t {DESC_ORB=0,DESC_AKAZE=1,DESC_BRISK=2,DESC_FREAK=3,DESC_SURF=4};

   static std::string toString(DescriptorTypes::Type type){
        switch (type)
        {
        case DESC_ORB: return "orb";
        case DESC_AKAZE: return "akaze";
        case DESC_BRISK: return "brisk";
        case DESC_FREAK: return "freak";
        case DESC_SURF: return "surf";
        }
        throw std::runtime_error("DescriptorType::toString invalid descriptor ");
    }

   static DescriptorTypes::Type fromString(const std::string & str){
        if (str=="orb")return DESC_ORB;
        if (str=="akaze")return DESC_AKAZE;
        if (str=="brisk")return DESC_BRISK;
        if (str=="freak")return DESC_FREAK;
        if (str=="surf")return DESC_SURF;
        throw std::runtime_error("DescriptorType::fromString invalid descriptor string :"+str);
    }

};


//Processing parameters for the SLAM
struct Params{
    Params();
    bool detectMarkers=true;
    bool detectKeyPoints=true;
    float effectiveFocus=-1;//value to enable normalization across different cameras and resolutions
    bool removeKeyPointsIntoMarkers=true;
    bool forceInitializationFromMarkers=false;
    float minDescDistance=50;//minimum distance between descriptors to consider a possible match
    float baseline_medianDepth_ratio_min=0.01;
    int projDistThr=15;//when searching for points by projection, maximum 2d distance for search radius
    int nthreads_feature_detector=2;
    std::string global_optimizer= "g2o";//which global optimizer to use
    int minNumProjPoints=3;//minimum number of keyframes in which a point must be seen to keep it
    float keyFrameCullingPercentage=0.8;
    int fps=30;//Frames per second of the video sequence
    float thRefRatio=0.9;//ratoi of matches found in current frame compared to ref keyframe to consider a new keyframe to be inserted
    int maxFeatures=2000;
    int nOctaveLevels=8;
    float scaleFactor=1.2;
    DescriptorTypes::Type kpDescriptorType=DescriptorTypes::Type::DESC_ORB;


    int maxVisibleFramesPerMarker=10;
    float minBaseLine=0.07;//minimum preffered distance  between keyframes


    float aruco_markerSize=1;            //! Size of markers in meters
    int aruco_minNumFramesRequired=3;            //minimum number of frames
    float aruco_minerrratio_valid=3;//minimum error ratio between two solutions to consider a initial pose valid
    bool aruco_allowOneFrameInitialization=false;
     aruco::MarkerDetector::Params aruco_DetectorParams;


    void toStream(std::ostream &str);
    void fromStream(std::istream &str);
    uint64_t getSignature()const;


    void saveToYMLFile(const std::string &path);
    void readFromYMLFile(const std::string &path);
    //--- do not use
    bool runSequential=false;//avoid parallel processing

    private:
    template<typename Type>
    void attemtpRead(const std::string &name,Type &var,cv::FileStorage&fs ){
        if ( fs[name].type()!=cv::FileNode::NONE)
            fs[name]>>var;
    }


};




}

#endif
