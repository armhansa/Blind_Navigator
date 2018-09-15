package com.armhansa.app.blindnavigator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

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
import org.opencv.imgproc.Imgproc;

public class MainActivity_show_camera extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;

    // Used in Camera selection from menu (when implemented)
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;

    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;

    // For do somethings with frame
    private Mat mGray;
    private Mat mCanny;
    private Mat mLine;

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

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);

        mGray = new Mat(width, width, CvType.CV_8UC4);
        mCanny = new Mat(width, width, CvType.CV_8UC4);
        mLine = new Mat(width, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        // Declare the output variables
        Mat result = new Mat(height, width, CvType.CV_8UC4);

        // TODO Auto-generated method stub
        mRgba = inputFrame.rgba();
        // Rotate mRgba 90 degrees
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );

        // To GrayScale
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGB2GRAY);

        // Edge detection
        Imgproc.Canny(mGray, mCanny, 50, 200, 3, false);

        getHoughTransform(mCanny, result, 1, Math.PI/180, 50);

        // Combine SrcImage and Line Detected
        Core.add(mRgba, result, result);

        return result; // This function must return
    }

    public void getHoughTransform(Mat image, Mat result, double rho, double theta, int threshold) {
        Mat lines = new Mat();
        Imgproc.HoughLines(image, lines, rho, theta, threshold);

        for (int i = 0; i < lines.cols(); i++) {
            double data[] = lines.get(0, i);
            if(data != null && data.length > 0) {   // Add to protect when data null
                double rho1 = data[0];
                double theta1 = data[1];
                double cosTheta = Math.cos(theta1);
                double sinTheta = Math.sin(theta1);
                double x0 = cosTheta * rho1;
                double y0 = sinTheta * rho1;
                Point pt1 = new Point(x0 + 10000 * (-sinTheta), y0 + 10000 * cosTheta);
                Point pt2 = new Point(x0 - 10000 * (-sinTheta), y0 - 10000 * cosTheta);
                Imgproc.line(result, pt1, pt2, new Scalar(0, 0, 255), 2);
            }
        }
    }

    public Mat getHoughPTransform(Mat image, double rho, double theta, int threshold) {
        Mat result = new Mat(width, width, CvType.CV_8UC4);
        Mat lines = new Mat();
        Imgproc.HoughLinesP(image, lines, rho, theta, threshold);

        for (int i = 0; i < lines.cols(); i++) {
            double[] val = lines.get(0, i);
            Imgproc.line(result, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(0, 0, 255), 2);
        }
        return result;
    }

}
