package com.armhansa.app.blindnavigator.model;

import android.util.Log;

import org.opencv.core.Point;

public class Line {

    private double m;
    private double b;

    private double theta;
    private double cosTheta;
    private double sinTheta;
    private double x;
    private double y;

    private boolean isShow = true;

    private int distance = 10000;

    public Line(double rho, double theta) {
        this.theta = theta;
        Log.d("TEST", "Theta = "+theta);

        cosTheta = Math.cos(theta);
        sinTheta = Math.sin(theta);

        x = cosTheta * rho;
        y = sinTheta * rho;

        m = cosTheta/(-sinTheta);
        b = y - m*x;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public double getM() {
        return m;
    }
    public double getB() {
        return b;
    }
    public double getTheta() {
        return theta;
    }

    public double getY(double x) {
        return m*x + b;
    }

    public double getX(double y) {
        if(theta != 0) return (y-b)/m;
        else return x;
    }

    public Point[] getPoints() {
        // Position of X, Y point 1 and 2
        double x1 = x + distance * (-sinTheta);
        double y1 = y + distance * cosTheta;

        double x2 = x - distance * (-sinTheta);
        double y2 = y - distance * cosTheta;

//        Point pt = new Point(x, y);
        return new Point[]{new Point(x1, y1), new Point(x2, y2)};
    }

    public void setShow(boolean show) {
        isShow = show;
    }

    public boolean isShow() {
        return isShow;
    }
}
