package com.armhansa.app.blindnavigator;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
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
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MainActivity_show_camera extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;

    // Initial View
    private TextView showMethod;
    private TextView showTheta;

    // For Store Line
    private BrailleBlockLine lineStore;
    // For Alert
    private StatusAlert alert;

    // These variables are used
    private Mat mRgba;
    private Mat mHsv;
    private Mat mHls;
    private Mat mCanny;
    private Mat mLine;

    // For Switch Method Color
    private byte methodSelected = 2;

    private Mat bwYellow;

    // Will Hold width and height of view
    private int width;
    private int height;

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

        showMethod = findViewById(R.id.show_method);
        showMethod.setText(R.string.hsv_method);

        showTheta = findViewById(R.id.show_theta);

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
        this.width = width;
        this.height = height;

        Log.d(TAG, "OnStart: width="+width+" height="+height);

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mHsv = new Mat(height, width, CvType.CV_8UC3);
        mHls = new Mat(height, width, CvType.CV_8UC3);

        bwYellow = new Mat(height, width, CvType.CV_8UC1);
        mCanny = new Mat(width, height, CvType.CV_8UC1);

        lineStore = BrailleBlockLine.getInstance(width, height);
        alert = StatusAlert.getInstance(width, height);

    }

    public void onCameraViewStopped() {
        mRgba.release();
    }










    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // Get Image Frames From Phone Camera
        mRgba = inputFrame.rgba();

//        setOrientationView(mRgba); // For use in Portrait
        // RGB COLOR to HSV COLOR
        Imgproc.cvtColor(mRgba, mHsv, Imgproc.COLOR_RGB2HSV);
        Imgproc.cvtColor(mRgba, mHls, Imgproc.COLOR_RGB2HLS);

        // Separate Braille Block Color From Frame
        if(methodSelected == 0) Core.inRange(mRgba
                , new Scalar(10, 18, 19, 100)
                , new Scalar(111, 121, 126, 255)
                , bwYellow);
        else if(methodSelected == 1) Core.inRange(mHls
                , new Scalar(15, 40, 20)
                , new Scalar(60, 240, 255)
                , bwYellow);
        else Core.inRange(mHsv
                , new Scalar(10, 10, 40)
                , new Scalar(35, 255, 255)
                , bwYellow);

        // Image Dilation
        Mat mask = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(5,5));
        Imgproc.erode(bwYellow, bwYellow, mask);
        Imgproc.dilate(bwYellow, bwYellow, mask);
        // Edge detection
        Imgproc.Canny(bwYellow, mCanny, 50, 200, 3, false);
        // Get Line Mat From Image and Combine SrcImage and Line Detected
        getHoughTransform(mCanny, 10, 1, Math.PI/180, 100);
        Core.add(bwYellow, mLine, mLine);
        // Check And Alert
        if(phoneAngle == 0) alert.run();
        else if(!myTTS.isSpeaking()){
            if(phoneAngle == -1) {
                myTTS.addSpeak("โปรดเงยโทรศัพท์ขึ้นเล็กน้อย");
            } else {
                myTTS.addSpeak("โปรดก้มโทรศัพท์ลงเล็กน้อย");
            }
        }

        return mLine; // This function must return
    }

    public void getHoughTransform(Mat image, int numberLines, double rho, double theta, int threshold) {
        Mat lines = new Mat();
        mLine = new Mat(height, width, CvType.CV_8UC1);
        Mat eachLine = new Mat(height, width, CvType.CV_8UC1);

        // Add Middle Vertical lines
//        Imgproc.line(eachLine, new Point(0, height/2), new Point(width, height/2), new Scalar(255), 5);
//        Core.add(eachLine, mLine, mLine);

//        for (int i=1; i<10; i++) {
//            Imgproc.line(eachLine, new Point(width/10*i, 0), new Point(width/10*i, height), new Scalar(255), 1);
//            Core.add(eachLine, mLine, mLine);
//        }

        Imgproc.HoughLines(image, lines, rho, theta, threshold);
        // Reset Braille Block Data
        lineStore.reset();

        for (int i = 0; i < Math.min(numberLines, lines.rows()); i++) {
            double data[] = lines.get(i, 0);

            if (data != null && data.length > 0) {   // Add to protect when data null
                Line tmp = new Line(data[0], data[1]);

                lineStore.autoSet(tmp);

                Point pt[] = tmp.getPoints();
                Imgproc.line(eachLine, pt[0], pt[1], new Scalar(255), 2);

                // Sum All Line in this frame
                Core.add(eachLine, mLine, mLine);
            }
        }
        // To draw point of intersect of two lines
        Point pt = lineStore.getIntersect();
        if (pt != null) {
            Imgproc.line(eachLine, pt, pt, new Scalar(255), 30);
            Core.add(eachLine, mLine, mLine);
        }

        // Draw StopLimit Line
//        lineStore.getMinLineStop(eachLine);
//        Core.add(eachLine, mLine, mLine);
//        lineStore.getMaxLineStop(eachLine);
//        Core.add(eachLine, mLine, mLine);

    }

    public void switchMethod(View view) {
        myTTS.speakNow("");
//        methodSelected = (byte) ((methodSelected+1)%3);
//        switch (methodSelected) {
//            case 0x0:
//                showMethod.setText(R.string.rgb_method);
//                MyTTS.getInstance(this).speak("เปลี่ยนเป็นโหมด R G B");
//                break;
//            case 0x1:
//                showMethod.setText(R.string.hsl_method);
//                MyTTS.getInstance(this).speak("เปลี่ยนเป็นโหมด H S L");
//                break;
//            case 0x2:
//                showMethod.setText(R.string.hsv_method);
//                MyTTS.getInstance(this).speak("เปลี่ยนเป็นโหมด H S V");
//                break;
//            default:
//                showMethod.setText(R.string.error);
//                MyTTS.getInstance(this).speak("เอาแล้วไง เกิดปัญหาแล้วจ้า");
//        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float y = sensorEvent.values[1];
        showMethod.setText("Y : "+ ((int)y*10)/10);
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

    private void reduceSize(Mat src, float ratio) {
        Imgproc.resize(src, src, new Size(mHsv.width()/ratio, mHsv.height()/ratio));
    }

    private void setOrientationView(Mat src) { // For Use when app run in Portrait Mode
        // Rotate mRgba 90 degrees
        Core.transpose(src, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, src, 1 );

    }

    */



}
