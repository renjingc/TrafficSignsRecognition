package com.my.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.sh1r0.caffe_android_lib.CaffeMobile;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import android.text.format.Time;
import android.os.SystemClock;

import java.io.IOException;


public class CameraActivity extends Activity implements CvCameraViewListener2 {

	private static final String TAG = "CameraActivity";
	private static final Logger logger = Logger.getLogger(CameraActivity.class.toString());
	
	private static final Scalar FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);

	private static Integer []labels={R.raw.n5,R.raw.n15,R.raw.n20,
			R.raw.n30,R.raw.n40,R.raw.n50,
			R.raw.n60,R.raw.n70,R.raw.n80,
			R.raw.n90,R.raw.n100,R.raw.n120};
	private native boolean judge(long Rect,int thres,boolean isANNJudge);
	private native void initDetect(String judgeFile);


	//加载caffe库
	static {
		System.loadLibrary("caffe");
		System.loadLibrary("caffe_jni");

	}
	static {
		System.loadLibrary("ImgFun");
	}

	//限制对比度的自适应直方图均衡化
	CLAHE clahe;
	int left, top, right, bottom;
	private CameraBridgeViewBase mCameraView;
	private ListView listDetectedSigns;
	private RelativeLayout listRelativeLayout;
	private CascadeClassifier cascadeClassifier;
	private ArrayList<Sign> listSign;
	private Detector detector;
	private CaffeMobile caffeMobile;
	private boolean change;
	private Mat mRgba;
	private Mat mGray;
	private MediaPlayer mediaPlayer;
	private Time lastTime,currentTime;
	private float runTime,runTime1,runTime2;
	private File ANNCascadeFile;
	private int []labelCounts={0,0,0,0,
					0,0,0,0,
					0,0,0,0};

	Integer colorThres;
	Integer soundSpacing;
	Integer repeatedJudge;
	SharedPreferences sp;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
	        @Override
	        public void onManagerConnected(int status) {
	            switch (status) {
	                case LoaderCallbackInterface.SUCCESS:
	                	mCameraView.enableView();
						//创建检测器
	                	detector = new Detector(CameraActivity.this);
						//直方图均衡化
						clahe = Imgproc.createCLAHE();
						//加载caffemodel
						caffeMobile = new CaffeMobile();
						File sdDir = null;
						boolean sdCardExist = Environment.getExternalStorageState()
								.equals(android.os.Environment.MEDIA_MOUNTED);//判断sd卡是否存在
						if(sdCardExist)
						{
							sdDir = Environment.getExternalStorageDirectory();//获取跟目录
						}
						Log.i(TAG,sdDir.getAbsolutePath());
						caffeMobile.loadModel("/storage/emulated/0/caffe_mobile/traffic-signs/config_deploy.prototxt",
								"/storage/emulated/0/caffe_mobile/traffic-signs/_iter_14000.caffemodel");

						//caffeMobile.loadModel("/storage/emulated/0/caffe_mobile/traffic-signs/deploy.prototxt",
						//		"/storage/emulated/0/caffe_mobile/traffic-signs/snapshot_iter_19140.caffemodel");
						//caffeMobile.setMean("/storage/emulated/0/caffe_mobile/traffic-signs/mean.binaryproto");
						caffeMobile.setScale(0.00390625F);
	                    break;
	                default:
	                    super.onManagerConnected(status);
	                    break;
	            }
	        }
	    };
	private void initJudge(){
		try {
			InputStream is;
			File cascadeDir = this.getDir("cascade", Context.MODE_PRIVATE);

			is = this.getResources().openRawResource(R.raw.myann);
			ANNCascadeFile = new File(cascadeDir, "myANN.xml");
			//myANN.xml
			FileOutputStream os = new FileOutputStream(ANNCascadeFile);
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			is.close();
			os.close();

			initDetect("/storage/emulated/0/caffe_mobile/traffic-signs/myann.xml");
			//Log.e(TAG,ANNCascadeFile.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		//detector = new Detector(CameraActivity.this);
	private void Initialze(){
		//初始化
		mCameraView = (CameraBridgeViewBase)findViewById(R.id.mCameraView);
		listDetectedSigns = (ListView)findViewById(R.id.listView1);
		listRelativeLayout = (RelativeLayout)findViewById(R.id.listViewLayout);
		mCameraView.setCvCameraViewListener(this);
		listRelativeLayout.setVisibility(View.GONE);
		lastTime=new Time();
		currentTime=new Time();
		lastTime.setToNow();
		currentTime.setToNow();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.camera_preview);
		Initialze();
		initJudge();
		PreferenceManager.setDefaultValues(this, R.xml.pref, false);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		colorThres = Integer.parseInt(sp.getString("colorThres", "10"));
		soundSpacing = Integer.parseInt(sp.getString("soundSpacing", "3"));
		repeatedJudge = Integer.parseInt(sp.getString("repeatedJudge", "3"));
	}
		
	@Override
    public void onResume() {
        super.onResume();
		if (!OpenCVLoader.initDebug()) {
            Log.d("fds", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d("fds", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub

	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		//TODO Auto-generated method stub
		mRgba = inputFrame.rgba();
		//Log.e("Ren",String.valueOf(mRgba.rows())+','+String.valueOf(mRgba.cols()));
		Size sizeRgba = mRgba.size();
		int rows = (int) sizeRgba.height;
		int cols = (int) sizeRgba.width;

		left = cols / 2;
		top = 0;
		right = cols;
		bottom = rows ;//* 3 / 4;

		mGray = inputFrame.gray();
		Log.i("STEP","step");
		mGray = mGray.submat(top, bottom, left, right);

		//Imgproc.equalizeHist(mGray, mGray);
//		CLAHE clahe = Imgproc.createCLAHE();
//		clahe.apply(mGray, mGray);
		if (!change) {
			runTime1=SystemClock.uptimeMillis();
			MatOfRect signs = new MatOfRect();
			listSign = new ArrayList<Sign>();
			Log.i("STEP", "DetectBegin");
			detector.Detect(mGray, signs, 1);
			Log.i("STEP", "DetectEnd");
			Rect[] prohibitionArray = signs.toArray();

			Draw(prohibitionArray);
			//change = true;
		} /*else{
			MatOfRect signs = new MatOfRect();
        	detector.Detect(mGray, signs, 2);
        	Rect[] dangerArray = signs.toArray();
        	Draw(dangerArray);
			change = false;
		}*/
		Imgproc.rectangle(mRgba, new Point(left, top), new Point(right, bottom), FACE_RECT_COLOR, 2);
        return mRgba;
	}
	
	public void Draw(Rect[] TrafficArray){
		Log.i(TAG,String.valueOf(TrafficArray.length));
        for (int i = 0; i <TrafficArray.length; i++){
        	final int ii = i;
			Log.e("RES", "start");
        	Mat subMat,subMatColor;
			//获取检测到的矩形
        	subMat = mGray.submat(TrafficArray[i]);
			subMatColor=mRgba.submat(TrafficArray[i]);
			//颜色判断,只有有红色且没有蓝色的时候
			Log.i(TAG,String.valueOf(colorThres));
			if(judge(subMatColor.getNativeObjAddr(),colorThres,false)) {
				Mat resizeMat = new Mat();
				//缩小到32*32
				Imgproc.resize(subMat, resizeMat, new Size(32, 32), 0, 0, Imgproc.INTER_CUBIC);
				//直方图均衡化
				clahe.apply(resizeMat, resizeMat);


				//缩小到227*227
				//Mat resizeMat = new Mat();
				//Imgproc.resize(subMatColor, resizeMat, new Size(227, 227), 0, 0, Imgproc.INTER_CUBIC);

				//将该图片写入
				File fileDir = getImageFile();
				Imgcodecs.imwrite(fileDir.toString(), resizeMat);
				//预测
				Log.e("RES", "start2");
				final int[] result = caffeMobile.predictImage(Uri.fromFile(fileDir).getPath());
				Log.e("RES", "result:" + Arrays.toString(result));
				//每张预测概率值
				final float[] rr = caffeMobile.getConfidenceScore(Uri.fromFile(fileDir).getPath());
				Log.e("RES", "rr:" + Arrays.toString(rr));
				runTime=SystemClock.uptimeMillis()-runTime1;

				//读取该图片
				Mat mat = Imgcodecs.imread(Uri.fromFile(fileDir).getPath(), -1);
				//Sign.myMap.put("image"+result[0], Utilities.convertMatToBitmap(mat));

				if (fileDir.exists()) {
					if (fileDir.delete()) {
						System.out.println("file Deleted :" + fileDir);
					} else {
						System.out.println("file not Deleted :" + fileDir);
					}
				}
				Log.e("RES", "stop");
				//左上角和右下角的点
				Point tl = TrafficArray[i].tl();
				tl.set(new double[]{tl.x + left, tl.y + top});
				Point br = TrafficArray[i].br();
				br.set(new double[]{br.x + left, br.y + top});

				Imgproc.rectangle(mRgba, tl, br, FACE_RECT_COLOR, 2);
				//如果识别率大于0.9，则输出识更新左边的Lists
				if (rr[result[0]] > 0.9&&result[0] < 12){
					labelCounts[result[0]]++;
					if(judgeLabels()!=-1) {
						//发音间隔时间判断
						currentTime.setToNow();
						int lastMinute = lastTime.minute;
						int currentMinute = currentTime.minute;
						int lastSecond = lastTime.second;
						int currentSecond = currentTime.second;
						int spacing = (currentMinute * 60 + currentSecond) - (lastMinute * 60 + lastSecond);
						//Log.e(TAG,String.valueOf(spacing));
						if (spacing > soundSpacing) {
							labelsClean();
							//发音
							mediaPlayer = MediaPlayer.create(getApplicationContext(), labels[result[0]]);
							mediaPlayer.start();
							lastTime.setToNow();

							Log.e("Ren", "rr:" + String.valueOf(result[0]));
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									// TODO Auto-generated method stub
									BigDecimal bpro = new BigDecimal(rr[result[0]]);
									float resultProbability = bpro.setScale(3, BigDecimal.ROUND_HALF_UP).floatValue();
									Log.e("Ren", "resultProbability:" + String.valueOf(resultProbability));
									Sign sign = new Sign(result[0], resultProbability, runTime);
									listSign.add(sign);
									listRelativeLayout.setVisibility(View.VISIBLE);
									itemAdapter adapter = new itemAdapter(listSign, CameraActivity.this);
									adapter.notifyDataSetChanged();
									listDetectedSigns.setAdapter(adapter);
								}
							});
						}
					}
				}
			}
        }
	}

	public File getImageFile() {
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Signs");
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.e("G", "failed to create directory");
				return null;
			}
		}
		//String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		mediaFile = new File(mediaStorageDir.getPath() + File.separator + "testimage"  + ".jpg");

		return mediaFile;
	}

	public int judgeLabels() {
		int max=0;
		int maxLabel=0;
		for(int i=0;i<12;i++){
			if(labelCounts[i]>max) {
				max=labelCounts[i];
				maxLabel=i;
			}
		}
		if(max>repeatedJudge){
			return maxLabel;
		}
		else
			return -1;
	}
	public void labelsClean() {
		for(int i=0;i<12;i++){
			labelCounts[i]=0;
		}
	}
}
