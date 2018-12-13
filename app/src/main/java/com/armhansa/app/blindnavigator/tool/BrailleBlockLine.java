package com.armhansa.app.blindnavigator.tool;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import com.armhansa.app.blindnavigator.MainActivity_show_camera;
import com.armhansa.app.blindnavigator.model.CaseName;
import com.armhansa.app.blindnavigator.model.Line;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.text.DecimalFormat;

public class BrailleBlockLine implements SensorEventListener {

    private static final String TAG = "BrailleBlockLineStore";
    private static BrailleBlockLine storageInstance;

    private static final int LEFT = 0;
    private static final int RIGHT = 1;

    private boolean intersectGeted = false;
    private Point intersect;

    private Line leftLine;
    private Line rightLine;
    private Line firstStopLine;
    private Line secondStopLine;
    private double stopX = 0;
    private int notStopCount = 0;
    private double lastIntersectX;
    private boolean hasStopLane;
    private Mat leftStopCropped;
    private Mat rightStopCropped;

    private Accelerometer sensor;
    private float lastAngleY;
    private float angleY;

    private int width;
    private int height;

    private BrailleBlockLine() {
        this.width = MainActivity_show_camera.width;
        this.height = MainActivity_show_camera.height;
        sensor = Accelerometer.getInstance();
        sensor.addListener(this);
        hasStopLane = false;
    }

    public static BrailleBlockLine getInstance() {
        if (storageInstance == null)
            storageInstance = new BrailleBlockLine();
        return storageInstance;
    }

    public void reset() {
        intersectGeted = false;
        if (intersect != null) {
            lastIntersectX = intersect.x;
            intersect = null;
        }
        leftLine = null;
        rightLine = null;
        firstStopLine = null;
        secondStopLine = null;
    }

    public void autoSet(Line line) {
        if (isLeftLane(line)) {
            if (leftLine != null && line.getY(width) < leftLine.getY(width)) {
                leftLine.setShow(false);
                leftLine = line;
            } else {
                leftLine = line;
            }
        } else if (isRightLane(line)) {
            if (rightLine != null && line.getY(width) > rightLine.getY(width)) {
                rightLine.setShow(false);
            } else {
                rightLine = line;
            }
        } else if (isStopLane(line)) {
            setStopLine(line);

            // Old Code
            DecimalFormat df = new DecimalFormat("#.##");
            Log.d(TAG, "Is Stop 1: distance(("+df.format(line.getX(height/2))+
                    "-(Math.max(0, "+df.format(lastIntersectX)+"+"+df.format(stopX)+")))");
            hasStopLane = true;

            if (hasStopLane) {
                Log.d(TAG, "Is Stop 2: value( 0 - ("+(50 + (angleY-lastAngleY)*50)+")");
            } else {
                Log.d(TAG, "Is Stop 2: value("+(100 + (angleY-lastAngleY)*50)+" - ("+(150 + (angleY-lastAngleY)*50)+")");
            }
            Log.d(TAG, "Is Stop 3: ____________________________________________");

        } else {
            line.setShow(false);
            DecimalFormat df = new DecimalFormat("#.####");
            Log.d(TAG, "Not Stop 1: theta,b("+df.format(line.getTheta())+","+df.format(line.getB())+")");
            Log.d(TAG, "Not Stop 2: x("+df.format(line.getX(height/2))+")");
            if(getIntersect() != null)
                Log.d(TAG, "Not Stop 3: intersectX("+df.format(getIntersect().x)+")");
            Log.d(TAG, "Not Stop 4 ____________________________________________");
        }
    }

    public int[] getStatus() {
        // For reset stop line
        if (firstStopLine == null && ++notStopCount > 60) {
            stopX = 0;
            hasStopLane = false;
            notStopCount = 0;
        }
        if (isStandOnLane()) {
            Point intersect = getIntersect();
            if (firstStopLine != null && secondStopLine != null) {
                int distance = (int) (((width-stopX)/width)*Math.pow(angleY, 1.5));
                Log.d(TAG, "Distance to Stop: (("+width+"-"+stopX+")/"+width+")*"+Math.pow(angleY, 1.5));
                Log.d(TAG, "Distance to Stop: = "+distance);
                Log.d(TAG, "Distance to Stop: ______________________________________________");
                return new int[]{getTypeOfStop(), distance};
            } else if (intersect.y > height*3/4) {
                return new int[]{CaseName.CASE_FACING_LEFT};
            } else if (intersect.y < height/4) {
                return new int[]{CaseName.CASE_FACING_RIGHT};
            }
            return new int[]{CaseName.CASE_FOUND};
        }
        return new int[]{CaseName.CASE_NOT_FOUND};
    }

    public Point getIntersect() {
        if (intersectGeted) {
            return intersect;
        } else if (leftLine != null && rightLine != null) {
            double x = (rightLine.getB() - leftLine.getB()) / (leftLine.getM() - rightLine.getM());
            double y = leftLine.getM() * x + leftLine.getB();
            Log.d(TAG, "getIntersect: x,y = (" + x + "," + y + ")");
            intersect = new Point((int) x, (int) y);
            intersectGeted = true;
        } else {
//            lastIntersectX = 0;
        }
        return intersect;
    }

    private boolean isStandOnLane() {
        return intersect != null && leftLine.getY(width) >= height / 2
                && rightLine.getY(width) <= height / 2;
    }

    private boolean isLeftLane(Line line) {
        double theta = line.getTheta();
        return theta < getPi(180-30) && theta > getPi(180-85);
    }

    private boolean isRightLane(Line line) {
        double theta = line.getTheta();
        return theta < getPi(180-95) && theta > getPi(180-150);
    }

    private boolean isStopLane(Line line) {
        double theta = line.getTheta();
        double lineX = line.getX(height/2);
        if (theta < getPi(5) || theta > getPi(175)) {
            if (hasStopLane) {
                Log.d(TAG, "valueA max: "+(stopX+50));
                return Math.max(lastIntersectX+50, stopX-200) <= lineX && lineX <= Math.max(lastIntersectX+150, stopX+50);
            } else {
                return Math.max(50, lastIntersectX+50) <= lineX
                        && lineX <= Math.max(300, lastIntersectX+300);
            }
        } else return false;
    }

    private void setStopLine(Line line) {
        if (firstStopLine == null) {
            firstStopLine = line;
            double newStopX = line.getX(height/2);
            if (stopX != 0 && Math.abs(newStopX-stopX) <= 50) stopX = newStopX;
            else if (stopX == 0) stopX = newStopX;
        } else if(secondStopLine == null) {
            if (firstStopLine.getX(width/2) > line.getX(width/2)) {
                secondStopLine = line;
            } else {
                secondStopLine = firstStopLine;
                firstStopLine = line;
                double newStopX = line.getX(height/2);
                if (stopX != 0 && Math.abs(newStopX-stopX) <= 50) stopX = newStopX;
                else if (stopX == 0) stopX = newStopX;
            }
        } else {
            if (line.getX(width/2) > firstStopLine.getX(width/2)) {
                firstStopLine = line;
                double newStopX = line.getX(height/2);
                if (stopX != 0 && Math.abs(newStopX-stopX) <= 50) stopX = newStopX;
                else if (stopX == 0) stopX = newStopX;
            } else if (line.getX(width/2) < secondStopLine.getX(width/2)) {
                secondStopLine = line;
            }
        }
    }

    private int getTypeOfStop() {
        boolean hasLeft = hasTurn(LEFT);
        boolean hasRight = hasTurn(RIGHT);
        if (!hasLeft && !hasRight) return CaseName.CASE_STOP;
        else if (hasLeft && hasRight) return CaseName.CASE_THREE_WAYS;
        else if (hasLeft) return CaseName.CASE_TURN_LEFT;
        else return CaseName.CASE_TURN_RIGHT;
    }

    public int[] getStopPosition() {
        // Return the rectangle of left and right lane just in perspective
        if (leftLine != null && rightLine != null &&
                firstStopLine != null && secondStopLine != null) {
            int leftFst = (int) Math.min(firstStopLine.getX(height), firstStopLine.getX(height / 12 * 11));
            int leftSec = (int) Math.max(secondStopLine.getX(height), secondStopLine.getX(height / 12 * 11)) + 1;
            int rightFst = (int) Math.min(firstStopLine.getX(0), firstStopLine.getX(height / 12));
            int rightSec = (int) Math.max(secondStopLine.getX(0), secondStopLine.getX(height / 12))+1;
            Log.d(TAG, "leftSec, RightSec : "+leftSec+","+rightSec);
            Log.d(TAG, "leftSec_______________________________");
            return new int[]{leftFst, leftSec, rightFst, rightSec};
        } else return null;
    }

    public void setStopMat(Mat left, Mat right) {
        this.leftStopCropped = left;
        this.rightStopCropped = right;
    }

    private boolean hasTurn(int type) {
        Mat stopCropped;
        if(type == 0) stopCropped = leftStopCropped;
        else stopCropped = rightStopCropped;

        double sum = Core.sumElems(stopCropped).val[0];
        double allPixel = stopCropped.height()*stopCropped.width();
        Log.d(TAG, "Sum of Elem "+new String[]{"Left", "Right"}[type]
                + " : "+sum+" / "+(allPixel*255)+"("+allPixel+"*255)");
        if (sum > allPixel*255/2) return true;
        return false;
    }

    public void getMinLineStop(Mat line) {
        double min;
        if (hasStopLane) {
            min = Math.max(lastIntersectX+50, stopX-200);
        } else {
            min = Math.max(50, lastIntersectX+50);
        }
        Imgproc.line(line, new Point(min, 0), new Point(min, height), new Scalar(255), 15);
    }

    public void getMaxLineStop(Mat line) {
        double max;
        if (hasStopLane) {
            max = stopX+50;
        } else {
            max = Math.max(300, lastIntersectX+300);
        }
        Imgproc.line(line, new Point(max, 0), new Point(max, height), new Scalar(255), 15);
    }

    private double getPi(double degree) {
        // Degree start from left to right (MirrorY from math)
        return Math.PI*(degree/180);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        lastAngleY = angleY;
        angleY = sensorEvent.values[1];
        Log.d(TAG, "onSensorChanged: ");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
