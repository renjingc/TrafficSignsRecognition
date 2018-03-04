#include "com_my_myapplication_CameraActivity.h"
#include <jni.h>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/ml/ml.hpp>
#include <sys/time.h>
#include <vector>
#include <string>
#include <queue>

#define STANDARD_SIZE   24
#define IMGSIZE         24
#define FEATURENUM      (24*(3+3)+ 3 + 4*3)

#define LOG_TAG "Ren"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
using namespace std;
using namespace cv;


extern "C" {

bool isJudgeSign=false;
CascadeClassifier  circleDetect;
Ptr<cv::ml::ANN_MLP> mlp = cv::ml::ANN_MLP::create();
cv::Ptr<cv::ml::RTrees> m_classifier;

bool mat2feature( const cv::Mat &mat, float *feature );
bool img2feature( Mat src, float *feature );
bool judgeSign(Mat src);
void initDetect(string detectFile,string judgeFile,string RTressFile,bool _isJudgeSign);

bool judgeSign(Mat src)
{
    bool isSign = false;
    float feature[FEATURENUM];
    if (img2feature( src, feature ) )
    {
        cv::Mat featureMat( 1, FEATURENUM, CV_32F, feature );
        cv::Mat result( 1, 1, CV_32FC1 );
        mlp->predict( featureMat, result );
        float *p = result.ptr<float>(0);
        isSign = *p > 0;
    }
    else
    {
        cout<<"failed one predict isSign";
    }
    return isSign;
}

void initDetect(string detectFile,string judgeFile,string RTressFile,bool _isJudgeSign)
{
    circleDetect.load(detectFile);
    mlp->clear();
    mlp = cv::Algorithm::load<cv::ml::ANN_MLP>(judgeFile);
    isJudgeSign=_isJudgeSign;
    m_classifier=cv::Algorithm::load<cv::ml::RTrees>(RTressFile);
}

bool mat2feature( const cv::Mat &mat, float *feature )
{
    if ( mat.empty() || mat.cols != IMGSIZE || mat.rows != IMGSIZE || mat.channels() != 3 ) {
        return false;
    }

    int allCountR          =   0  , allCountG          =   0  , allCountB          =   0  ;
    int rowCountR[IMGSIZE] = { 0 }, rowCountG[IMGSIZE] = { 0 }, rowCountB[IMGSIZE] = { 0 };
    int colCountR[IMGSIZE] = { 0 }, colCountG[IMGSIZE] = { 0 }, colCountB[IMGSIZE] = { 0 };
    int tempR[4]           = { 0 }, tempG[4]           = { 0 }, tempB[4]           = { 0 };

    for( int i = 0; i < IMGSIZE; ++i ) {
        for( int j = 0; j < IMGSIZE; ++j ) {

            int b = mat.at<cv::Vec3b>(i,j)[0];
            int g = mat.at<cv::Vec3b>(i,j)[1];
            int r = mat.at<cv::Vec3b>(i,j)[2];

            allCountR += r;
            allCountG += g;
            allCountB += b;

            rowCountR[i] += r;
            rowCountG[i] += g;
            rowCountB[i] += b;

            colCountR[j] += r;
            colCountG[j] += g;
            colCountB[j] += b;

            int index = 2 * (i < IMGSIZE / 2 ? 0 : 1) + (j < IMGSIZE / 2 ? 0 : 1);
            tempR[index] += r;
            tempG[index] += g;
            tempB[index] += b;
        }
    }

    for ( int i=0; i < IMGSIZE; ++i ) {
        feature[i*6+0] = (float)rowCountR[i]/allCountR;
        feature[i*6+1] = (float)rowCountG[i]/allCountG;
        feature[i*6+2] = (float)rowCountB[i]/allCountB;
        feature[i*6+3] = (float)colCountR[i]/allCountR;
        feature[i*6+4] = (float)colCountG[i]/allCountG;
        feature[i*6+5] = (float)colCountB[i]/allCountB;
    }
    feature[IMGSIZE*6+0] = (float)allCountR / (allCountR + allCountG +allCountB);
    feature[IMGSIZE*6+1] = (float)allCountG / (allCountR + allCountG +allCountB);
    feature[IMGSIZE*6+2] = (float)allCountB / (allCountR + allCountG +allCountB);

    for ( int i = 0; i < 4; ++i ) {
        int total = tempR[i] + tempG[i] + tempB[i];
        feature[IMGSIZE*6+3 + 3*i+0] = (float)tempR[i] / total;
        feature[IMGSIZE*6+3 + 3*i+1] = (float)tempG[i] / total;
        feature[IMGSIZE*6+3 + 3*i+2] = (float)tempB[i] / total;
    }
    return true;
}
bool img2feature( Mat src, float *feature )
{
    Mat mat;
    resize( src, mat, cv::Size( IMGSIZE, IMGSIZE ), 0.0, 0.0, cv::INTER_AREA ); // interpolation method
    return mat2feature(mat, feature );
}

void circleDetection(Mat& src,vector<Rect>& objects,int min,int max)
{
    Size minSize(min,min);
    Size maxSize(max,max);
    vector<int> rejectLevels;
    circleDetect.detectMultiScale(src,objects,1.1,5,0,minSize,maxSize);
}
// Dependencies
enum ColorChannel { Red, Blue, };
enum HLS { H = 0, L = 1, S = 2, };

uchar lerp(uchar a, uchar b, double t)
{
    return (uchar)(a*(1-t)+b*t);
}
uchar smoothStep(uchar a, uchar b, double t)
{
    return (uchar)lerp(a,b,(3*pow(t,2.0))-(2*pow(t,3.0)));
}
uchar lutS(uchar v, uchar rampTo)
{
        uchar m_result = 255;
        if (v<rampTo)
            m_result = smoothStep(0,rampTo,v/(double)rampTo);
        return m_result;
}
uchar lutH(uchar v, uchar centerV, uchar interval, uchar sigma)
{
    uchar m_result = 0;
    if ((interval)&&(sigma<=(interval/2.0)))
    {
            int m_minRef = -(interval/2.0),
                    m_maxRef = (interval/2.0),
                    m_shifted = v-centerV;
            m_shifted = (m_shifted>=(256+m_minRef))?m_shifted-256:m_shifted;
            m_shifted = (m_shifted<=(-256+m_maxRef))?m_shifted+256:m_shifted;
            if ((m_shifted>=m_minRef)&&(m_shifted<=m_maxRef))
            {
                m_result = 255;
                if (m_shifted<(m_minRef+sigma))
                    m_result = smoothStep(0,255,(m_shifted-m_minRef)/float(sigma));
                if (m_shifted>(m_maxRef-sigma))
                    m_result = smoothStep(255,0,((m_shifted-m_maxRef)+sigma)/float(sigma));
            }
    }
    return m_result;
}
cv::Mat bgr_to_nhs(cv::Mat input, ColorChannel channel)
{
        cv::Mat m_result = cv::Mat::zeros(input.rows, input.cols, CV_8UC1);
        cv::Mat ihls_image(input.rows, input.cols, CV_8UC3);
        for (unsigned int j = 0; j < input.rows; j++)
        {
            const uchar* bgr_data = input.ptr<uchar> (j);
            uchar* ihls_data = ihls_image.ptr<uchar> (j);
            for (int k = 0; k < input.cols; k++)
            {
                unsigned int b = *bgr_data++;
                unsigned int g = *bgr_data++;
                unsigned int r = *bgr_data++;
                // ______________________________________________________
                // SATURATION _____
                float saturation;
                unsigned int max = b, min = b;
                if (r > max) max = r;
                if (r < min) min = r;
                if (g > max) max = g;
                if (g < min) min = g;
                saturation = max - min;
                // LUMINANCE _____
                // L = 0.210R + 0.715G + 0.072B
                float luminance = (0.210f * r) + (0.715f * g) + (0.072f * b);
                // HUE _____
                float hue = 0.0f;
                // It calculates theta in radiands based on the equation provided in Valentine thesis.
                float theta = acos((r - (g * 0.5) - (b * 0.5)) /
                  (float)sqrtf((r * r) + (g * g) + (b * b) - (r * g) - (r * b) - (g * b)));
                if (b <= g) hue = theta;
                else hue = (2 * M_PI) - theta;
                // ______________________________________________________
                *ihls_data++ = (uchar)saturation;
                *ihls_data++ = (uchar)luminance;
                *ihls_data++ = (uchar)(hue * 255 / (2 * M_PI));
            }
        }
        if (channel == Red)
        {
            for (unsigned int j = 0; j < ihls_image.rows; j++)
            {
                const uchar *ihls_data = ihls_image.ptr<uchar> (j);
                uchar *nhs_data = m_result.ptr<uchar> (j);
                for (int k = 0; k < ihls_image.cols; k++)
                {
                    uchar s = *ihls_data++;
                    uchar l = *ihls_data++;
                    uchar h = *ihls_data++;
                    *nhs_data++ = ((h < 15 || h > 240) && s > 25)?255:0;//((h < 163 && h > 134) && s > 39)?255:0;
                }
            }
        }
        if (channel == Blue)
        {
            for (unsigned int j = 0; j < ihls_image.rows; j++)
            {
                const uchar *ihls_data = ihls_image.ptr<uchar> (j);
                uchar *nhs_data = m_result.ptr<uchar> (j);
                for (int k = 0; k < ihls_image.cols; k++)
                {
                    uchar s = *ihls_data++;
                    uchar l = *ihls_data++;
                    uchar h = *ihls_data++;
                    *nhs_data++ = ((h < 163 && h > 134) && s > 39)?255:0;
                }
            }
        }
        return m_result;
}
bool colorJudge(Mat src,Mat& m_colorChannelsRed,int thresh)
{
    // Color segmentation ___________________________________________________________________________________________________
    // - Initialize result buffer _
    cv::Mat m_colorChannels[3];
    for (unsigned int j = 0; j < 3; j++)
        m_colorChannels[j] = cv::Mat::zeros(src.rows,src.cols,CV_8UC1);

    // For red color segmentation - HLS colorspace _
    cv::Mat m_hlsImage,m_hlsImageChannels[3];
    cv::cvtColor(src,m_hlsImage,CV_BGR2HLS_FULL);
    cv::split(m_hlsImage,m_hlsImageChannels);
    for (int i = 0; i < (m_hlsImage.cols*m_hlsImage.rows); i++)
        m_colorChannels[0].data[i] = ((lutH(m_hlsImageChannels[H].data[i],0,25,0) & lutS(m_hlsImageChannels[S].data[i],255)) > 30)?255:0;
    // For red color segmentation - IHLS - NHS colorspace
    m_colorChannels[1] = bgr_to_nhs(src,Red);
    // For blue color segmentation - IHLS - NHS colorspace
    m_colorChannels[2] = bgr_to_nhs(src,Blue);

    int blueCount=0;
    int redCount=0;
    m_colorChannelsRed=m_colorChannels[0]+m_colorChannels[1];
    for(int i=0;i<m_colorChannels[0].rows;i++)
    {
        for(int j=0;j<m_colorChannels[0].cols;j++)
        {
            if((int)m_colorChannelsRed.at<uchar>(i,j)!=0)
            {
                redCount++;
            }
            if((int)m_colorChannels[2].at<uchar>(i,j)!=0)
            {
                blueCount++;
            }
        }
    }
    //cout<<redCount<<" "<<blueCount<<endl;
//    int k_size = 11;
//    cv::medianBlur(m_colorChannelsRed,m_colorChannelsRed,5);
//    cv::dilate(m_colorChannelsRed,m_colorChannelsRed,getStructuringElement(cv::MORPH_RECT, cv::Size(k_size,k_size)));
//    cv::erode(m_colorChannelsRed,m_colorChannelsRed,getStructuringElement(cv::MORPH_RECT, cv::Size(k_size,k_size)));
//    cv::medianBlur(m_colorChannels[1],m_colorChannels[1],5);

//    cv::medianBlur(m_colorChannels[2],m_colorChannels[2],5);
//    cv::dilate(m_colorChannels[2],m_colorChannels[2],getStructuringElement(cv::MORPH_RECT, cv::Size(k_size,k_size)));
//    cv::erode(m_colorChannels[2],m_colorChannels[2],getStructuringElement(cv::MORPH_RECT, cv::Size(k_size,k_size)));
    //imshow("red",m_colorChannelsRed);
    if(redCount>thresh&&blueCount<10)
        return true;
    else
        return false;
}

void segmentNumber(Mat src)
{
    threshold(src,src, 50, 255, CV_THRESH_BINARY_INV);
}
float performPrediction(std::vector<float> instance)
{
        cv::Mat m_data = cv::Mat::zeros(1, instance.size(), CV_32FC1);
        for (int i = 0; i < instance.size(); i++)
            m_data.at<float>(i) = instance.at(i);
        return m_classifier->predict(m_data);
}
void classification1(Mat src,float& m_classification)
{
        cv::Mat m_hypothesis;
        cv::resize(src,m_hypothesis, cv::Size(40,40),0,0,CV_INTER_CUBIC);
        cv::HOGDescriptor hog(cv::Size(40,40), cv::Size(10,10), cv::Size(5,5), cv::Size(5,5), 8, 1, -1, cv::HOGDescriptor::L2Hys, 0.2, false, cv::HOGDescriptor::DEFAULT_NLEVELS);

        std::vector<float> descriptors;
        std::vector<cv::Point>locations;
        hog.compute(m_hypothesis,descriptors,cv::Size(0,0),cv::Size(0,0),locations);
        descriptors.push_back(0.0);
        m_classification= performPrediction(descriptors);
}
void findCircle(Mat& src,Mat& mask, vector<Vec3f>& circles)
{
//    HoughCircles(grayGaussian,circles, CV_HOUGH_GRADIENT, 1.5, 10, 100, 50, minSize/4,minSize/2);
//    if(circles.size()>0)
//    {
//        int maxId=0;
//        double maxR=circles[0][2];
//        for (size_t i = 1; i < circles.size(); i++)
//        {
//            if(circles[i][2]>maxR)
//            {
//                maxId=i;
//                maxR=circles[i][2];
//            }
//         }
//        Point center(objects[i].tl().x+round(circles[maxId][0]), objects[i].tl().y+round(circles[maxId][1]));
//        Point center1(round(circles[maxId][0]), round(circles[maxId][1]));
//        int radius = round(circles[maxId][2]);

//        //绘制圆心
//        circle(src, center, 3, Scalar(0, 255, 0), -1, 8, 0);
//         //绘制圆轮廓
//        circle(src, center, radius, Scalar(155, 50, 255), 1, 8, 0);
//        circle(mask, center1, radius, Scalar(155, 50, 255), 1, 8, 0);
//    }
}
void colorFilter(Mat src, Mat& dst)
{
    int i, j;
    CvMat *inputImage=(CvMat*)&src;
    CvMat *outputImage=(CvMat*)&dst;
    cout<<"1"<<endl;
    IplImage* image = cvCreateImage(cvGetSize(inputImage), 8, 3);
    cvGetImage(inputImage, image);
    IplImage* hsv = cvCreateImage( cvGetSize(image), 8, 3 );

    cvCvtColor(image,hsv,CV_BGR2HSV);
    int width = hsv->width;
    int height = hsv->height;
    for (i = 0; i < height; i++)
        for (j = 0; j < width; j++)
        {
            CvScalar s_hsv = cvGet2D(hsv, i, j);//获取像素点为（j, i）点的HSV的值
            /*
                opencv 的H范围是0~180，红色的H范围大概是(0~8)∪(160,180)
                S是饱和度，一般是大于一个值,S过低就是灰色（参考值S>80)，
                V是亮度，过低就是黑色，过高就是白色(参考值220>V>50)。
            */
            CvScalar s;
            if (!(((s_hsv.val[0]>0)&&(s_hsv.val[0]<8)) || (s_hsv.val[0]>120)&&(s_hsv.val[0]<180)))
            {
                s.val[0] =0;
                s.val[1]=0;
                s.val[2]=0;
                cvSet2D(hsv, i ,j, s);
            }
        }
    outputImage = cvCreateMat( hsv->height, hsv->width, CV_8UC3 );
    cvConvert(hsv, outputImage);
    cvNamedWindow("filter");
    cvShowImage("filter", hsv);
    waitKey(0);
    cvReleaseImage(&hsv);
}
jstring charTojstring(JNIEnv* env, const char* pat) {
    //定义java String类 strClass
    jclass strClass = (env)->FindClass("Ljava/lang/String;");
    //获取String(byte[],String)的构造器,用于将本地byte[]数组转换为一个新String
    jmethodID ctorID = (env)->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    //建立byte数组
    jbyteArray bytes = (env)->NewByteArray(strlen(pat));
    //将char* 转换为byte数组
    (env)->SetByteArrayRegion(bytes, 0, strlen(pat), (jbyte*) pat);
    // 设置String, 保存语言类型,用于byte数组转换至String时的参数
    jstring encoding = (env)->NewStringUTF("GB2312");
    //将byte数组转换为java String,并输出
    return (jstring) (env)->NewObject(strClass, ctorID, bytes, encoding);
}

char* jstringToChar(JNIEnv* env, jstring jstr) {
    char* rtn = NULL;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("GB2312");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
    jsize alen = env->GetArrayLength(barr);
    jbyte* ba = env->GetByteArrayElements(barr, JNI_FALSE);
    if (alen > 0) {
        rtn = (char*) malloc(alen + 1);
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    env->ReleaseByteArrayElements(barr, ba, 0);
    return rtn;
}
JNIEXPORT jboolean JNICALL Java_com_my_myapplication_CameraActivity_judge
  (JNIEnv* jenv, jobject , jlong addrRgba, jint thres, jboolean isANNJudge){
        Mat& mRgb = *(Mat*)addrRgba;
        Mat bgr(mRgb.rows,mRgb.cols,mRgb.type());
        for(int i=0;i<mRgb.rows;i++)
        {
            for(int j=0;j<mRgb.cols;j++)
            {
                bgr.at<Vec3b>(i,j)[0]=mRgb.at<Vec3b>(i,j)[2];
                bgr.at<Vec3b>(i,j)[1]=mRgb.at<Vec3b>(i,j)[1];
                bgr.at<Vec3b>(i,j)[2]=mRgb.at<Vec3b>(i,j)[0];
            }
        }

        Mat red;
        if(colorJudge(bgr,red,thres)){
            if(isANNJudge){
                if(judgeSign(bgr)){
                    return true;
                }
                else{
                    return false;
                }
            }
            else{
                return true;
            }
        }
        else{
            return false;
        }
  }
JNIEXPORT void JNICALL Java_com_my_myapplication_CameraActivity_initDetect
  (JNIEnv* jenv, jobject, jstring filePath){
      // jstring 转 char*
      char* charData = jstringToChar(jenv, filePath);
      // char* 转 string
      std::string str = charData;
      mlp->clear();
      mlp = cv::Algorithm::load<cv::ml::ANN_MLP>("/storage/emulated/0/caffe_mobile/traffic-signs/myann.xml");
      //LOGD(str.str());
  }
}
