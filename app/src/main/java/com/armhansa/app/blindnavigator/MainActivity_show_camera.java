package com.armhansa.app.blindnavigator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.armhansa.app.blindnavigator.model.BrailleBlockLine;
import com.armhansa.app.blindnavigator.model.Line;
import com.armhansa.app.blindnavigator.model.StatusAlert;
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
        implements CameraBridgeViewBase.CvCameraViewListener2 {

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

    /*
    // Used in Camera selection from menu (when implemented)
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;

    // For Set New Orientation in Portrait Mode
    Mat mRgbaF;
    Mat mRgbaT;
     */

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
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.show_camera);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        showMethod = findViewById(R.id.show_method);
        showMethod.setText(R.string.hsv_method);

        showTheta = findViewById(R.id.show_theta);

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
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
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
        alert = StatusAlert.getInstance(this, width, height);

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
                , new Scalar(140, 140, 40, 100)
                , new Scalar(255, 255, 180, 255)
                , bwYellow);
        else if(methodSelected == 1) Core.inRange(mHls
                , new Scalar(15, 40, 20)
                , new Scalar(60, 240, 255)
                , bwYellow);
        else Core.inRange(mHsv
                , new Scalar(15, 20, 160)
                , new Scalar(60, 255, 255)
                , bwYellow);

        // Image Dilation
        Imgproc.erode(bwYellow, bwYellow, Imgproc.getStructuringElement(Imgproc.MORPH_RECT
                , new Size(5,5)));
        Imgproc.dilate(bwYellow, bwYellow, Imgproc.getStructuringElement(Imgproc.MORPH_RECT
                , new Size(5, 5)));
        // Edge detection
        Imgproc.Canny(bwYellow, mCanny, 50, 200, 3, false);
        // Get Line Mat From Image
        getHoughTransform(mCanny, 5, 1, Math.PI/180, 100);

        // Combine SrcImage and Line Detected
        Core.add(bwYellow, mLine, mLine);

        // Check And Alert
        alert.run();

        return mLine; // This function must return

    }


    public void getHoughTransform(Mat image, int numberLines, double rho, double theta, int threshold) {
        Mat lines = new Mat();
        mLine = new Mat(height, width, CvType.CV_8UC1);
        Mat eachLine = new Mat(height, width, CvType.CV_8UC1);

        Line line_store[] = new Line[numberLines];

        // Add Middle lines
        Imgproc.line(eachLine, new Point(0, height/2), new Point(width, height/2), new Scalar(255), 5);
        Core.add(eachLine, mLine, mLine);
        Imgproc.line(eachLine, new Point(0, height/4), new Point(width, height/4), new Scalar(255), 4);
        Core.add(eachLine, mLine, mLine);
        Imgproc.line(eachLine, new Point(0, height*3/4), new Point(width, height*3/4), new Scalar(255), 4);
        Core.add(eachLine, mLine, mLine);

        Imgproc.HoughLines(image, lines, rho, theta, threshold);
        // Reset Braille Block Data
        lineStore.reset();

        for (int i = 0; i < min(numberLines, lines.rows()); i++) {
            double data[] = lines.get(i, 0);

            if (data != null && data.length > 0) {   // Add to protect when data null
                Line tmp = new Line(data[0], data[1]);

                lineStore.autoSet(tmp);
                // Test
//                Imgproc.line(eachLine, pt, pt, new Scalar(255), 50);
//                Core.add(eachLine, mLine, mLine);
                Point pt[] = tmp.getPoints();
                Imgproc.line(eachLine, pt[0], pt[1], new Scalar(255), 2);

                // Sum All Line in this frame
                Core.add(eachLine, mLine, mLine);

            }
        }
        Point pt = lineStore.getIntersect();
        if (pt != null) {
            Imgproc.line(eachLine, pt, pt, new Scalar(255), 20);
            Core.add(eachLine, mLine, mLine);
        }

    }

    public void switchMethod(View view) {
        methodSelected = (byte) ((methodSelected+1)%3);
        switch (methodSelected) {
            case 0x0:
                showMethod.setText(R.string.rgb_method);
                MyTTS.getInstance(this).speak("เปลี่ยนเป็นโหมด R G B");
                break;
            case 0x1:
                showMethod.setText(R.string.hsl_method);
                MyTTS.getInstance(this).speak("เปลี่ยนเป็นโหมด H S L");
                break;
            case 0x2:
                showMethod.setText(R.string.hsv_method);
                MyTTS.getInstance(this).speak("เปลี่ยนเป็นโหมด H S V");
                break;
            default:
                showMethod.setText(R.string.error);
                MyTTS.getInstance(this).speak("เอาแล้วไง เกิดปัญหาแล้วจ้า");
        }


    }

    private int min(int value1, int value2) {
        if(value1 < value2) return value1;
        else return value2;
    }
















    /* NotUse


    private Mat hsvToYellowBw(Mat src) {
        int rows = src.rows();
        int cols = src.cols();
        Mat img_bw = Mat.zeros(rows, cols, CV_32FC1);
        int test = 0;
        for(int i=0; i<img_bw.rows(); i++) {
            for(int j=0; j<img_bw.cols(); j++) {
                double px[] = src.get(i, j);
                if((px[0] >= 40 && px[0] <= 120) && px[1] >= 40 && px[2] >= 30) {
                    img_bw.put(i, j, 255);
                    test++;
                }
            }
        }
        return img_bw;
    }

    private void reduceSize(Mat src, float ratio) {
        Imgproc.resize(src, src, new Size(mHsv.width()/ratio, mHsv.height()/ratio));
    }

    private void setOrientationView(Mat src) { // For Use when app run in Portrait Mode
        // Rotate mRgba 90 degrees
        Core.transpose(src, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, src, 1 );

    }

    private Mat getBlueChannel(Mat src) {
        return getSplitChannels(src, 0);
    }
    private Mat getGreenChannel(Mat src) {
        return getSplitChannels(src, 1);
    }
    private Mat getRedChannel(Mat src) {
        return getSplitChannels(src, 2);
    }
    private Mat getSplitChannels(Mat src, int channel) {
        // ***Bug Terminate when use this func long time
        ArrayList<Mat> bgr = new ArrayList<>();
        Core.split(src, bgr);
        return bgr.get(channel);
    }



    */



}
