package com.armhansa.app.blindnavigator;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import com.armhansa.app.blindnavigator.tool.BrailleBlockLine;
import com.armhansa.app.blindnavigator.model.Line;
import com.armhansa.app.blindnavigator.tool.StatusAlert;
import com.armhansa.app.blindnavigator.tool.Accelerometer;
import com.armhansa.app.blindnavigator.tool.MyTTS;

import org.opencv.android.JavaCameraView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MainActivity_show_camera extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";

    public static int width, height;
    public static final int RATIO = 1;

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;

    // Initial View
    private TextView showAngle;

    // For Store Line
    private BrailleBlockLine lineStore;
    // For Alert
    private StatusAlert alert;

    // These variables are used
    private Mat mRgba;
    private Mat mRgbaReduced;
    private Mat mHsv;
    private Mat mCanny;
    private Mat mLine;

    // For Switch Method Color
//    private byte methodSelected = 2;

    private Mat bwYellow;

    // MyTTS
    private MyTTS myTTS;

    // Accelerometer
    private Accelerometer sensor;
    private int phoneAngle;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity_show_camera() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.show_camera);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        showAngle = findViewById(R.id.show_angle);
        showAngle.setText(R.string.hsv_method);

        sensor = Accelerometer.getInstance();
        sensor.setContext(this);
        sensor.addListener(this);
        phoneAngle = -1;

        myTTS = MyTTS.getInstance(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        sensor.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onStop() {
        sensor.onStop();
        super.onStop();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        MainActivity_show_camera.width = width/RATIO;
        MainActivity_show_camera.height = height/RATIO;

        Log.d(TAG, "OnStart: width="+width+" height="+height);

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaReduced = new Mat(height/RATIO, width/RATIO, CvType.CV_8UC4);
        mHsv = new Mat(height/RATIO, width/RATIO, CvType.CV_8UC3);

        bwYellow = new Mat(height/RATIO, width/RATIO, CvType.CV_8UC1);
        mCanny = new Mat(width/RATIO, height/RATIO, CvType.CV_8UC1);


        lineStore = BrailleBlockLine.getInstance();
        alert = StatusAlert.getInstance();

    }

    public void onCameraViewStopped() {
        mRgba.release();
    }










    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // Get Image Frames From Phone Camera
        mRgba = inputFrame.rgba();
        Log.d(TAG, "Width, Height(mRgba) : ("+mRgba.width()+"*"+mRgba.height()+")");
//        setOrientationView(mRgba); // For use in Portrait
        Imgproc.resize(mRgba, mRgbaReduced, mRgbaReduced.size());
        Log.d(TAG, "Width, Height(mRgbaReduced) : ("+mRgbaReduced.width()+"*"+mRgbaReduced.height()+")");
        // RGB COLOR to HSV COLOR
        Imgproc.cvtColor(mRgbaReduced, mHsv, Imgproc.COLOR_RGB2HSV);
        Log.d(TAG, "Width, Height(mHsv) : ("+mHsv.width()+"*"+mHsv.height()+")");

//        Imgproc.cvtColor(mRgba, mHls, Imgproc.COLOR_RGB2HLS);
        // Separate Braille Block Color From Frame
//        if(methodSelected == 0) Core.inRange(mRgba
//                , new Scalar(10, 18, 19, 100)
//                , new Scalar(111, 121, 126, 255)
//                , bwYellow);
//        else if(methodSelected == 1) Core.inRange(mHls
//                , new Scalar(15, 40, 20)
//                , new Scalar(60, 240, 255)
//                , bwYellow);
//        else
        Core.inRange(mHsv, new Scalar(10, 10, 40), new Scalar(35, 255, 255), bwYellow);
        Log.d(TAG, "Width, Height(bwYellow) : ("+bwYellow.width()+"*"+bwYellow.height()+")");

        // Image Dilation
        Mat mask = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(5,5));
        Imgproc.erode(bwYellow, bwYellow, mask);
        Imgproc.dilate(bwYellow, bwYellow, mask);

        // Edge detection
        Imgproc.Canny(bwYellow, mCanny, 50, 200, 3, false);
        // Get Line Mat From Image and Combine SrcImage and Line Detected
        getHoughTransform(mCanny, 15, 1, Math.PI/180, 100);
        Log.d(TAG, "Width, Height(mLine) : ("+mLine.width()+"*"+mLine.height()+")");
        Core.add(bwYellow, mLine, mLine);
        // Crop image around left/right stop lane
        int stopPos[] = lineStore.getStopPosition();
        if (stopPos != null) {
            Log.d(TAG, "ImageRect : "+bwYellow.width()+"*"+bwYellow.height());
            Log.d(TAG, "SpecialRect : "+stopPos[0]+","+stopPos[1]+","+stopPos[2]+","+stopPos[3]);
            Rect leftCrop = new Rect(stopPos[1], height/12*11, Math.abs(stopPos[0]-stopPos[1]), height/12);
            Log.d(TAG, "LeftRect : "+leftCrop.x+","+leftCrop.y+" ("+leftCrop.width+"*"+leftCrop.height+")");
            Rect rightCrop = new Rect(stopPos[3], 0, Math.abs(stopPos[2]-stopPos[3]), height/12);
            Log.d(TAG, "RightRect : "+rightCrop.x+","+rightCrop.y+" ("+rightCrop.width+"*"+rightCrop.height+")");
            Log.d(TAG, "Rect : _______________________________________");
            Mat eachLine = new Mat(height, width, CvType.CV_8UC1);
            // Show Left Lane Cropped
            Imgproc.rectangle(eachLine, new Point(stopPos[1], height), new Point(stopPos[0]
                    , height/12*11), new Scalar(255), 5);
            Core.add(eachLine, mLine, mLine);
            // Show Right Lane Cropped
            Imgproc.rectangle(eachLine, new Point(stopPos[3], 0), new Point(stopPos[2]
                    , height/12), new Scalar(255), 5);
            Core.add(eachLine, mLine, mLine);
            // Crop and set to BrailleBlockLine to Check Again
            Mat leftCropped = new Mat(bwYellow, leftCrop);
            Mat rightCropped = new Mat(bwYellow, rightCrop);
            lineStore.setStopMat(leftCropped, rightCropped);
        }
        // Check And Alert
        if(phoneAngle == 0) alert.run();
        else if(!myTTS.isSpeaking()){
            if(phoneAngle == -1) myTTS.addSpeak("โปรดเงยโทรศัพท์ขึ้นเล็กน้อย");
            else myTTS.addSpeak("โปรดก้มโทรศัพท์ลงเล็กน้อย");
        }

        return mLine; // This function must return
//        return null;
    }

    public void getHoughTransform(Mat image, int numberLines, double rho, double theta, int threshold) {
        Mat lines = new Mat();
        mLine = new Mat(height, width, CvType.CV_8UC1);
        Mat eachLine = new Mat(height, width, CvType.CV_8UC1);

        // Add Middle Vertical lines
//        Imgproc.rectangle(eachLine, new Point(50, 50), new Point(100, 100), new Scalar(255), 5);
//        Core.add(eachLine, mLine, mLine);

        Imgproc.HoughLines(image, lines, rho, theta, threshold);
        // Reset Braille Block Data
        lineStore.reset();

        for (int i = 0; i < Math.min(numberLines, lines.rows()); i++) {
            double data[] = lines.get(i, 0);

            if (data != null && data.length > 0) {   // Add to protect when data null
                Line tmp = new Line(data[0], data[1]);

                lineStore.autoSet(tmp);
                if (tmp.isShow()) {
                    Point pt[] = tmp.getPoints();
                    Imgproc.line(eachLine, pt[0], pt[1], new Scalar(255), 5);

                    // Sum All Line in this frame
                    Core.add(eachLine, mLine, mLine);
                } else {
                    Point pt[] = tmp.getPoints();
                    Imgproc.line(eachLine, pt[0], pt[1], new Scalar(255), 1);

                    // Sum All Line in this frame
                    Core.add(eachLine, mLine, mLine);
                }
            }
        }
        // To draw point of intersect of two lines
        Point pt = lineStore.getIntersect();
        if (pt != null) {
            Imgproc.line(eachLine, pt, pt, new Scalar(255), 30);
            Core.add(eachLine, mLine, mLine);
        }

        // Draw StopLimit Line
        lineStore.getMinLineStop(eachLine);
        Core.add(eachLine, mLine, mLine);
        lineStore.getMaxLineStop(eachLine);
        Core.add(eachLine, mLine, mLine);

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float y = sensorEvent.values[1];
        showAngle.setText("Y : "+ ((int)(y*10))/10);
        if(y >= 8.5) {
            phoneAngle = 1;
        } else if(y <= 6) {
            phoneAngle = -1;
        } else phoneAngle = 0;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }





    /* NotUse

    private void setOrientationView(Mat src) { // For Use when app run in Portrait Mode
        // Rotate mRgba 90 degrees
        Core.transpose(src, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, src, 1 );

    }

    */



}
