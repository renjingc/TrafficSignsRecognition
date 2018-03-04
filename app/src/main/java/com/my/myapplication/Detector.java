package com.my.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class Detector {
	private static final String TAG = "Detector";
	private static final Logger logger = Logger.getLogger(Detector.class.getName());
	Integer minSize1;
	Integer maxSize1;
	Integer minSize2, maxSize2;
	SharedPreferences sp;
	private Activity activity;
	private CascadeClassifier cascadeClassifier1;
	private CascadeClassifier cascadeClassifier2;
	public Detector(Activity activity){
		this.activity = activity;
		//加载检测匹配文件
		loadCascadeFile(1);
		loadCascadeFile(2);
		sp = PreferenceManager.getDefaultSharedPreferences(activity);
		minSize1 = Integer.parseInt(sp.getString("minSize1", "100"));
		maxSize1 = Integer.parseInt(sp.getString("maxSize1", "400"));
	}
	public void Detect(Mat mGray,MatOfRect signs,int type){
		//loadCascadeFile(type, cascadeClassifier);
		//loadCascadeFile(type);
		//轮廓判断
		//1为检测圆形，2为检测三角形
		if (cascadeClassifier1 != null && !cascadeClassifier1.empty()) {
			cascadeClassifier1.detectMultiScale(mGray, signs, 1.1, 3, 0
					, new Size(minSize1, minSize1), new Size(maxSize1, maxSize1));
		} else {
			Log.e("s", "cascade");
			Log.i("s", "cascade");
		}
//		switch (type) {
//			case 1:
//				if (cascadeClassifier1 != null && !cascadeClassifier1.empty()) {
//					cascadeClassifier1.detectMultiScale(mGray, signs, 1.1, 3, 0
//							, new Size(minSize1, minSize1), new Size(maxSize1, maxSize1));
//				} else {
//					Log.e("s", "cascade");
//					Log.i("s", "cascade");
//				}
//				break;
//			case 2:
//			default:
//				if (cascadeClassifier2 != null && !cascadeClassifier2.empty()) {
//					cascadeClassifier2.detectMultiScale(mGray, signs, 1.1, 5, 0
//							, new Size(minSize2, minSize2), new Size(maxSize2, maxSize2));
//				} else {
//					Log.e("s", "cascade");
//					Log.i("s", "cascade");
//				}
//		}
	}
	private void loadCascadeFile(int type){
		try {
			InputStream is;
			File cascadeDir = activity.getDir("cascade", Context.MODE_PRIVATE);
			File cascadeFile;
			switch (type) {
			case 1:
				is = activity.getResources().openRawResource(R.raw.circle);
				cascadeFile = new File(cascadeDir, "circle.xml");
				break;
			case 2:
			default:
				is = activity.getResources().openRawResource(R.raw.triangle);
				cascadeFile = new File(cascadeDir, "triangle.xml");
				break;
			}
			//circle.xml
			FileOutputStream os = new FileOutputStream(cascadeFile);
			byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Load the cascade classifier
			switch (type) {
				case 1:
					cascadeClassifier1 = new CascadeClassifier(cascadeFile.getAbsolutePath());
					break;
				case 2:
				default:
					cascadeClassifier2 = new CascadeClassifier(cascadeFile.getAbsolutePath());
					break;
			}
            
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
