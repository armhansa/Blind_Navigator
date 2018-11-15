package com.armhansa.app.blindnavigator.model;

import android.util.Log;

import org.opencv.core.Point;

public class BrailleBlockLine {

    private static final String TAG = "BrailleBlockLineStore";
    private static BrailleBlockLine storageInstance;

    private boolean intersectGeted = false;
    private Point intersect;

    private Line leftLine;
    private Line rightLine;
    private Line stopLine;

    private int width;
    private int height;

    private BrailleBlockLine(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static BrailleBlockLine getInstance(int width, int height) {
        if (storageInstance == null)
            storageInstance = new BrailleBlockLine(width, height);
        return storageInstance;
    }

    public void reset() {
        intersectGeted = false;
        intersect = null;
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
            Log.d(TAG, "IsRight");
        } else if (isStopLane(line)) {
            stopLine = line;
            Log.d(TAG, "IsStop");
        } else {
            Log.d(TAG, "Not left/right");
        }
    }

    public String[] getStatus() {
        if (isStandOnLane()) {
            Point intersect = getIntersect();
            if (stopLine != null && stopLine.getX(height/2)-10 > intersect.x) {
                return new String[]{"Stop", String.valueOf(/*distance :*/10)};
            } else if (intersect.y > height*3/4) {
                return new String[]{"Facing Left"};
            } else if (intersect.y < height/4) {
                return new String[]{"Facing Right"};
            }
            return new String[]{"Found"};
        }
        return new String[]{"!Found"};

//        Old
//        boolean isOnLane[] = isStandOnLane();
//        if(start) {
//            if (getIntersect() != null && isOnLane[0] && isOnLane[1]) {
//                start = false;
//            } else {
//                return "Start";
//            }
//        }
//        if (isOnLane[0] && isOnLane[1] ) {
//            return "Walk";
//        } else if(getIntersect() != null) {
//            return "Turn Around";
//        } else {
//            return "Another";
//        }

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
            Log.d(TAG, "notIntersect: haveLeftLine,haveRightLine = "+(leftLine!=null)+","+(rightLine!=null));
        }
        return intersect;

    }

    private boolean isStandOnLane() {
        return leftLine != null && rightLine != null
                && leftLine.getY(width) >= height / 2 && rightLine.getY(width) <= height / 2;
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
        return theta < getPi(5) && theta > getPi(175);
    }

    private double getPi(double degree) {
        // Degree start from left to right (MirrorY from math)
        return Math.PI*(degree/180);
    }



}
