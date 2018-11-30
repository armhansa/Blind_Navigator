package com.armhansa.app.blindnavigator.tool;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import com.armhansa.app.blindnavigator.model.Line;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.text.DecimalFormat;

public class BrailleBlockLine implements SensorEventListener {

    private static final String TAG = "BrailleBlockLineStore";
    private static BrailleBlockLine storageInstance;

    private boolean intersectGeted = false;
    private Point intersect;

    private Line leftLine;
    private Line rightLine;
    private Line stopLine;
    private double stopX = 0;
    private int notStopCount = 0;
    private double lastIntersectX;
    private boolean hasStopLane;

    private Accelerometer sensor;
    private float lastAngleY;
    private float angleY;

    private int width;
    private int height;

    private BrailleBlockLine(int width, int height) {
        this.width = width;
        this.height = height;
        sensor = Accelerometer.getInstance();
        sensor.addListener(this);
        hasStopLane = false;
    }

    public static BrailleBlockLine getInstance(int width, int height) {
        if (storageInstance == null)
            storageInstance = new BrailleBlockLine(width, height);
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
        stopLine = null;
    }

    public void autoSet(Line line) {
        Log.d(TAG, "theta,b = ("+line.getTheta()+","+line.getB()+")");
        if (isLeftLane(line)) {
            leftLine = line;
            Log.d(TAG, "Is Left");
        } else if (isRightLane(line)) {
            rightLine = line;
            Log.d(TAG, "Is Right");
        } else if (isStopLane(line)) {
            stopLine = line;
            DecimalFormat df = new DecimalFormat("#.##");
            Log.d(TAG, "Is Stop 1: distance(("+df.format(line.getX(height/2))+
                    "-(Math.max(0, "+df.format(lastIntersectX)+"+"+df.format(stopX)+")))");
            stopX = line.getX(height/2);
            hasStopLane = true;

            if (hasStopLane) {
                Log.d(TAG, "Is Stop 2: value( 0 - ("+(50 + (angleY-lastAngleY)*50)+")");
            } else {
                Log.d(TAG, "Is Stop 2: value("+(100 + (angleY-lastAngleY)*50)+" - ("+(150 + (angleY-lastAngleY)*50)+")");
            }
            Log.d(TAG, "Is Stop 3: ____________________________________________");

        } else {
            DecimalFormat df = new DecimalFormat("#.####");
            Log.d(TAG, "Not Stop 1: theta,b("+df.format(line.getTheta())+","+df.format(line.getB())+")");
            Log.d(TAG, "Not Stop 2: x("+df.format(line.getX(height/2))+")");
            if(getIntersect() != null)
                Log.d(TAG, "Not Stop 3: intersectX("+df.format(getIntersect().x)+")");
            Log.d(TAG, "Not Stop 4 ____________________________________________");
        }
    }

    public String[] getStatus() {
        // For reset stop line
        if (stopLine == null && ++notStopCount > 60) {
            stopX = 0;
            hasStopLane = false;
            notStopCount = 0;
        }
        if (isStandOnLane()) {
            Point intersect = getIntersect();
            if (stopLine != null && stopLine.getX(height/2)-10 > intersect.x) {
                Log.d(TAG, "Distance to Stop: "+(width-stopX)/width*angleY*2);
                Log.d(TAG, "Distance to Stop: ("+width+"-"+stopX+")/"+width+"*"+angleY+"*"+2);
                Log.d(TAG, "Distance to Stop: ______________________________________________");
                return new String[]{"Stop", String.valueOf((width-stopX)/width*angleY*2)};
            } else if (intersect.y > height*3/4) {
                return new String[]{"Facing Left"};
            } else if (intersect.y < height/4) {
                return new String[]{"Facing Right"};
            }
            return new String[]{"Found"};
        }
        return new String[]{"!Found"};
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
            lastIntersectX = 0;
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
                return lineX >= stopX-50 && lineX <= stopX+50;
            } else {
                return lineX >= 50+Math.max(0, lastIntersectX)
                        && lineX <= 150+Math.max(0, lastIntersectX);
            }
        } else return false;

    }

    public void getMinLineStop(Mat line) {
        double min;
        if (hasStopLane) {
            min = stopX-50;
        } else {
            min = 50+Math.max(0, lastIntersectX);
        }
        Imgproc.line(line, new Point(min, 0), new Point(min, height), new Scalar(255), 15);
    }

    public void getMaxLineStop(Mat line) {
        double max;
        if (hasStopLane) {
            max = stopX+50;
        } else {
            max = 150+Math.max(0, lastIntersectX);
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
