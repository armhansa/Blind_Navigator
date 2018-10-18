package com.armhansa.app.blindnavigator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

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

import java.util.ArrayList;

import static org.opencv.core.CvType.CV_32FC1;

public class MainActivity_show_camera extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;

    // Initial View
    private TextView showMethod;

    // Used in Camera selection from menu (when implemented)
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;

    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;

    // For Switch Method Color
    private boolean useRgb = false;

    // For do somethings with frame
    private Mat mHsv;
    private Mat mCanny;
    private Mat mLine;

    private Mat bwYellow;

    // Will Hold width and height of view
    private int width;
    private int height;


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
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
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
        mHsv = new Mat(height, width, CvType.CV_8UC4);
        bwYellow = new Mat(height, width, CvType.CV_8UC4);
        mCanny = new Mat(width, height, CvType.CV_8UC4);

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
        // Separate Braille Block Color From Frame
        if(useRgb) Core.inRange(mRgba, new Scalar(150, 150, 0, 0), new Scalar(255, 255, 150, 255), bwYellow);
        else Core.inRange(mHsv, new Scalar(20, 100, 10), new Scalar(40, 255, 255), bwYellow);
        // Image Dilation
        Imgproc.dilate(bwYellow, bwYellow, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
        // Edge detection
        Imgproc.Canny(bwYellow, mCanny, 50, 200, 3, false);
        // Get Line Mat From Image
        getHoughTransform(mCanny, 2, 1, Math.PI/180, 190);

        // Combine SrcImage and Line Detected
        Core.add(bwYellow, getBlueChannel(mLine), mLine);
        return mLine; // This function must return

    }













    private void reduceSize(Mat src, float ratio) {
        Imgproc.resize(src, src, new Size(mHsv.width()/ratio, mHsv.height()/ratio));
    }

    private void setOrientationView(Mat src) {
        // Rotate mRgba 90 degrees
        Core.transpose(src, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, src, 1 );

    }

    public Mat getHoughTransform(Mat image, int numberLines, double rho, double theta, int threshold) {
        Mat lines = new Mat();
        mLine = new Mat(height, width, CvType.CV_8UC4);
        Mat eachLine = new Mat(height, width, CvType.CV_8UC4);
        Imgproc.HoughLines(image, lines, rho, theta, threshold);
        for (int i = 0; i < min(numberLines, lines.rows()); i++) {
            double data[] = lines.get(i, 0);

            if (data != null && data.length > 0) {   // Add to protect when data null
                double rho1 = data[0];
                double theta1 = data[1];
                double cosTheta = Math.cos(theta1);
                double sinTheta = Math.sin(theta1);
                double x = cosTheta * rho1;
                double y = sinTheta * rho1;

                // Position of X, Y point 1 and 2
                double x1 = x + 10000 * (-sinTheta);
                double y1 = y + 10000 * cosTheta;

                double x2 = x - 10000 * (-sinTheta);
                double y2 = y - 10000 * cosTheta;

                Point pt1 = new Point(x1, y1);
                Point pt2 = new Point(x2, y2);

                Imgproc.line(eachLine, pt1, pt2, new Scalar(0, 0, 255), 2);
                // Sum All Line in this frame
                Core.add(eachLine, mLine, mLine);

            }
        }
        return mLine;
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
        ArrayList<Mat> bgr = new ArrayList<>();
        Core.split(src, bgr);
        return bgr.get(channel);
    }

    private int min(int value1, int value2) {
        if(value1 < value2) return value1;
        else return value2;
    }

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

    public void switchMethod(View view) {
        useRgb = !useRgb;
        showMethod.setText(useRgb ? R.string.rgb_method : R.string.hsv_method);
    }

}
