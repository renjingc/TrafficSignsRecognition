package com.my.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utilities {
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	
	public static Bitmap convertMatToBitmap(Mat src){
		Bitmap bm = Bitmap.createBitmap(src.cols(),
				src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, bm);
        //Imgproc.cvtColor(src, dst, code, dstCn)
        return bm;
	}

	public static String getRealPathFromURI(Uri contentURI,Activity activity) {
	    String path;
	    Cursor cursor = activity.getContentResolver().query(contentURI, null, null, null, null);
	    if (cursor == null) { // Source is Dropbox or other similar local file path
	        path = contentURI.getPath();
	    } else {
	        cursor.moveToFirst();
	        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
	        path = cursor.getString(idx);
	        cursor.close();
	    }
	    return path;
	}

	public static Uri getOutputMediaFileUri(int type){
	      return Uri.fromFile(getOutputMediaFile(type));
	}
	/** Create a File for saving an image or video */
	@SuppressLint("SimpleDateFormat")
	public static File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES), "MyCameraApp");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.
	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d("MyCameraApp", "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}
	
}
