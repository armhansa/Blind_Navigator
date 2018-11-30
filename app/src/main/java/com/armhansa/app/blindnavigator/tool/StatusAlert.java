package com.armhansa.app.blindnavigator.tool;

import android.util.Log;

import java.util.Calendar;

public class StatusAlert {

    private static StatusAlert alertInstance;

    private static final String TAG = "StatusAlert";

    private BrailleBlockLine lineStore;

    private boolean isStart = true;
    private long lastAlertTime;

    private Summary summary;
//    private String status[];
    private float distanceStop;
//    private int countStatus[]; // ["Normal", "Turn Left", "Turn Right", "Stop", "AbNormal"]

    private int alertTime = 5000;

    private MyTTS myTTS;

    public static StatusAlert getInstance(int width, int height) {
        if (alertInstance == null) {
            alertInstance = new StatusAlert(width, height);
        }
        return alertInstance;
    }

    private StatusAlert(int width, int height) {
        lineStore = BrailleBlockLine.getInstance(width, height);
        myTTS = MyTTS.getInstance();
        lastAlertTime = Calendar.getInstance().getTimeInMillis();
        summary = new Summary(new String[]{"Normal", "Facing Left", "Facing Right"
                , "Turn Left", "Turn Right", "Stop", "AbNormal"});
    }

    private void reset() {
        distanceStop = 0;
    }

    public void run() {
        // check and alert
        long currentTime = Calendar.getInstance().getTimeInMillis();
        if (currentTime - lastAlertTime >= alertTime) {
            // Alert
            String maxCase = summary.getMax();
            if (isStart) {
                // 1 - Not Found Way on Start
                //      Start and still not found way (start && error > normal)
                if (maxCase.equals("AbNormal")) {
                    myTTS.addSpeak("โปรดยืนบนทางเดิน");
                    lastAlertTime = currentTime;
                }
                // 2 - Found Way (First Time)
                //      Start found way (start && normal > error)) !start
                else {
                    myTTS.addSpeak("พบทางเดินแล้ว");
                    lastAlertTime = currentTime-3000;
                    isStart = false;
                }
            } else {
                switch (maxCase) {
                    // 3 - Walk Straight
                    //      (!start && normal > error && thetaL is YMirror thetaR)
                    case "Normal":
                        // Tids Tids
                        myTTS.addSpeak("เดินตรงต่อไป");
                        lastAlertTime = currentTime;
                        break;
                    // 4 - Facing Left/Right
                    //      (!start && normal > error)
                    case "Facing Left":
                        myTTS.addSpeak("โปรดหันกล้องไปทางซ้าย");
                        lastAlertTime = currentTime;
                        break;
                    case "Facing Right":
                        myTTS.addSpeak("โปรดหันกล้องไปทางขวา");
                        lastAlertTime = currentTime;
                        break;
                    // 5 - Stop in x Distance matter
                    //      (!start && normal > error && found 3thLine && haven'tBlockOnIntersectPoint)
                    case "Stop":
                        myTTS.addSpeak("อีกประมาณ " + (int)distanceStop + "เมตร เป็นทางหยุดเดิน");
                        lastAlertTime = currentTime;
                        break;
                    // 6 - Turn Left/Right
                    //      (!start && error > normal)
                    case "Turn Left":
                        myTTS.addSpeak("ทางแยกไปทางซ้าย");
                        lastAlertTime = currentTime;
                        break;
                    case "Turn Right":
                        myTTS.addSpeak("ทางแยกไปทางขวา");
                        lastAlertTime = currentTime;
                        break;
                    case "Three Ways":
                        myTTS.addSpeak("พบทางแยกไป");
                        lastAlertTime = currentTime;
                        break;
                    case "AbNormal":
                        myTTS.addSpeak("หาทางเดินไม่เจอ");
                        lastAlertTime = currentTime;
                        break;
                    case "End":
                        myTTS.addSpeak("สิ้นสุดทางเดิน");
                        lastAlertTime = currentTime;
                        break;
                }
                // (7 - Scan Crossroad separate from Found Stop)
                //      (check block found in left/right)
                //      (may have a lot of delays)
            }
            reset();
            summary.reset();
        }

        // Process
        String tmpStatus[] = lineStore.getStatus();
        Log.d(TAG, "run: "+tmpStatus[0]);
        switch (tmpStatus[0]) {
            case "Found":
                summary.add("Normal");break;
            case "Facing Left":
                summary.add("Facing Left");break;
            case "Facing Right":
                summary.add("Facing Right");break;
            case "Stop":
                summary.add("Stop");
                distanceStop = Float.parseFloat(tmpStatus[1]);break;
            case "!Found":
                summary.add("AbNormal");break;
        }

    }

}
