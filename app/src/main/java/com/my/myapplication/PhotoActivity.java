package com.my.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

//import org.opencv.highgui.Highgui;

public class PhotoActivity extends Activity implements OnClickListener {
	private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
	public static int pickCode = 1;
	public static int captureCode = 2;
	private Button btPick;
	private Button btCapture;
	private ImageView ivDisplay;
	private LinearLayout layoutResult;
	private Button btDetect;
	private Uri mUri;
	private Mat photoMat;
	private CascadeClassifier cascadeClassifier;
	private Detector detector;
	private ArrayList<Sign> listSign;
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                	photoMat = new Mat();
                	detector = new Detector(PhotoActivity.this);
                	break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.photo_layout);
		Initialize();
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
	}
	
	public void Initialize(){
		btPick = (Button)findViewById(R.id.btPick);
		btCapture = (Button)findViewById(R.id.btCapture);
		btDetect = (Button)findViewById(R.id.btDetect);
		ivDisplay = (ImageView)findViewById(R.id.ivDisplay);
		layoutResult = (LinearLayout)findViewById(R.id.layoutResult);
		//layoutResult.setVisibility(View.GONE);
		btDetect.setVisibility(View.GONE);
		btPick.setOnClickListener(this);
		btCapture.setOnClickListener(this);
		btDetect.setOnClickListener(this);
	}
	public void loadCascadeFile(int detectTypeId){
		try {
			InputStream is = null;
			File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
			File cascadeFile = null;
			
			switch (detectTypeId) {
			case 1:
				is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
				cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
				break;
			case 2:
				is = getResources().openRawResource(R.raw.circle);
				cascadeFile = new File(cascadeDir, "traffic_signs.xml");
				break;
			case 3:
				is = getResources().openRawResource(R.raw.haarcascade_eye);
				cascadeFile = new File(cascadeDir, "haarcascade_eye.xml");
				break;
			default:
				break;
			}
			
			FileOutputStream os = new FileOutputStream(cascadeFile);
			byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
 
 
            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void Detect(Mat mGray){
		
		Imgproc.equalizeHist(mGray, mGray);
		MatOfRect signs = new MatOfRect();
		listSign = new ArrayList<Sign>();
        
        detector.Detect(mGray, signs,1);
        Rect[] prohibitionArray = signs.toArray();
        Imgproc.cvtColor(photoMat, photoMat, Imgproc.COLOR_RGBA2BGR, 3);
        //Imgproc.cvtColor(photoMat, photoMat, Imgproc.COLOR_RGBA2BGR, 3);
        Draw(prohibitionArray);
        
        detector.Detect(mGray, signs,2);
        Rect[] dangerArray = signs.toArray();
       // Imgproc.cvtColor(photoMat, photoMat, Imgproc.COLOR_RGBA2BGR, 3);
       // Imgproc.cvtColor(photoMat, photoMat, Imgproc.COLOR_RGBA2BGR, 3);
        Draw(dangerArray); 
        
        //get signs from photo
        ivDisplay.setImageBitmap(Utilities.convertMatToBitmap(photoMat));
    }
	public void Draw(Rect[] signsArray){
		
		for(int i = 0; i < signsArray.length; i++){
        	Mat subMat = new Mat();
        	subMat = photoMat.submat(signsArray[i]);
        	
        	ImageView ivv = new ImageView(this);
        	ivv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
        	ivv.setImageBitmap(Utilities.convertMatToBitmap(subMat));
        	
//        	Sign.myMap.put("image"+i, Utilities.convertMatToBitmap(subMat));
//        	Sign sign = new Sign("unknown", "image"+i);
        	
//        	listSign.add(sign);
        	layoutResult.addView(ivv);
        	btDetect.setVisibility(View.GONE);
        	
//        	layoutResult.setOnTouchListener(new OnTouchListener() {
//
//				@Override
//				public boolean onTouch(View arg0, MotionEvent arg1) {
//					// TODO Auto-generated method stub
//					Intent intent = new Intent(PhotoActivity.this,RegconitionActivity.class);
//					intent.putParcelableArrayListExtra("key", (ArrayList<? extends Parcelable>) listSign);
//					startActivity(intent);
//					return false;
//				}
//			});
        }
        //draw rectangle
        for (int i = 0; i < signsArray.length; i++){
			Imgproc.rectangle(photoMat, signsArray[i].tl(), signsArray[i].br(), FACE_RECT_COLOR, 3);
        }
       
       
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.btPick:
			layoutResult.removeAllViews();
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*");
			startActivityForResult(intent, 1);
			break;
		case R.id.btCapture:
			layoutResult.removeAllViews();
			intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
			mUri = Utilities.getOutputMediaFileUri(Utilities.MEDIA_TYPE_IMAGE);  // create a file to save the video in specific folder (this works for video only)
			intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
			startActivityForResult(intent, captureCode);
			break;
		case R.id.btDetect:
			String photoPath = Utilities.getRealPathFromURI(mUri,PhotoActivity.this);
			photoMat= Imgcodecs.imread(mUri.toString());
			Mat mGray = new Mat();
			Imgproc.cvtColor(photoMat, mGray, Imgproc.COLOR_BGR2GRAY, 3);
			loadCascadeFile(2);
			Detect(mGray);
			photoPath = "";
			break;
		default:
			break;
		}
	}
	
	@SuppressLint("NewApi")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		//super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == pickCode && data!=null){
			mUri = data.getData();
			ivDisplay.setImageURI(mUri);
			btDetect.setVisibility(View.VISIBLE);
			
		}
		if(requestCode == captureCode && resultCode == RESULT_OK){
			ivDisplay.setImageURI(mUri);
			btDetect.setVisibility(View.VISIBLE);
		}
	}
	
}
