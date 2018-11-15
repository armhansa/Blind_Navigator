package com.armhansa.app.blindnavigator.model;

import android.content.Context;
import android.util.Log;

import com.armhansa.app.blindnavigator.tool.MyTTS;

import java.util.Calendar;

public class StatusAlert {

    private static StatusAlert alertInstance;

    private static final String TAG = "StatusAlert";

    private BrailleBlockLine lineStore;

    private boolean isStart = true;
    private long lastAlertTime;

    private String status[];
    private int distanceStop;
    private int countStatus[]; // ["Normal", "Turn Left", "Turn Right", "Stop", "AbNormal"]

    private int alertTime = 7000;

    private MyTTS myTTS;

    public static StatusAlert getInstance(Context context, int width, int height) {
        if (alertInstance == null) {
            alertInstance = new StatusAlert(context, width, height);
        }
        return alertInstance;
    }

    private StatusAlert(Context context, int width, int height) {
        lineStore = BrailleBlockLine.getInstance(width, height);
        myTTS = MyTTS.getInstance(context);
        myTTS.speak("สวัสดี");
        lastAlertTime = Calendar.getInstance().getTimeInMillis()-4000;
        status = new String[]{"Normal", "Facing Left", "Facing Right", "Turn Left", "Turn Right", "Stop", "AbNormal"};
        reset();
    }

    private void reset() {
        countStatus = new int[]{0, 0, 0, 0, 0, 0, 0};
        distanceStop = 0;
    }

    public void run() {
        // check and alert
        long currentTime = Calendar.getInstance().getTimeInMillis();
        if (currentTime - lastAlertTime >= 7000 /*7sec*/) {
            // Alert
            if (isStart) {
                // 1 - Not Found Way on Start
                //      Start and still not found way (start && error > normal)
                if (findMaxCase().equals("AbNormal")) {
                    myTTS.speak("โปรดยืนบนทางเดิน");
                    lastAlertTime = currentTime;
                }
                // 2 - Found Way (First Time)
                //      Start found way (start && normal > error)) !start
                else {
                    myTTS.speak("พบทางเดินแล้ว");
                    lastAlertTime = currentTime-3000;
                    isStart = false;
                }
            } else {
                String maxStatus = findMaxCase();
                // 3 - Walk Straight
                //      (!start && normal > error && thetaL is YMirror thetaR)
                if (maxStatus.equals("Normal")) {
                    // Tids Tids
                    myTTS.speak("เดินตรงต่อไป");
                    lastAlertTime = currentTime;
                }
                // 4 - Facing Left/Right
                //      (!start && normal > error)
                else if (maxStatus.equals("Facing Left")){
                    myTTS.speak("โปรดหันกล้องไปทางซ้าย");
                    lastAlertTime = currentTime;
                } else if (maxStatus.equals("Facing Right")) {
                    myTTS.speak("โปรดหันกล้องไปทางขวา");
                    lastAlertTime = currentTime;
                }
                // 5 - Stop in x Distance matter
                //      (!start && normal > error && found 3thLine && haven'tBlockOnIntersectPoint)
                else if (maxStatus.equals("Stop")){
                    myTTS.speak("อีก "+distanceStop+" เป็นทางหยุดเดิน");
                    lastAlertTime = currentTime;
                }
                // 6 - Turn Left/Right
                //      (!start && error > normal)
                else if (maxStatus.equals("Turn Left")) {
                    myTTS.speak("ทางแยกไปทางซ้าย");
                    lastAlertTime = currentTime;
                } else if (maxStatus.equals("Turn Right")) {
                    myTTS.speak("ทางแยกไปทางขวา");
                    lastAlertTime = currentTime;
                } else if (findMaxCase().equals("AbNormal")) {
                    myTTS.speak("หาทางเดินไม่เจอ");
                    lastAlertTime = currentTime;
                }
                // (7 - Scan Crossroad separate from Found Stop)
                //      (check block found in left/right)
                //      (may have a lot of delays)
            }
            reset();
        }

        // Process
        String tmpStatus[] = lineStore.getStatus();
        Log.d(TAG, "run: "+tmpStatus[0]);
        switch (tmpStatus[0]) {
            case "Found":
                countStatus[0]++;break;
            case "Facing Left":
                countStatus[1]++;break;
            case "Facing Right":
                countStatus[2]++;break;
            case "Stop":
                countStatus[5]++;
                distanceStop = Integer.parseInt(tmpStatus[1]);break;
            case "!Found":
                countStatus[6]++;break;

        }

    }

    private String findMaxCase() {
        String maxCase = status[0];
        int maxCount = countStatus[0];
        for (int i=1; i<countStatus.length; i++) {
            if (countStatus[i] >= maxCount) {
                maxCase = status[i];
            }
        }
        return maxCase;
    }

}
