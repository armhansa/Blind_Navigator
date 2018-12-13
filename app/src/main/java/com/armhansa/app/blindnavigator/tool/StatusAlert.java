package com.armhansa.app.blindnavigator.tool;

import com.armhansa.app.blindnavigator.model.CaseName;

import java.util.Calendar;

public class StatusAlert {

    private static StatusAlert alertInstance;

    private static final String TAG = "StatusAlert";

    private BrailleBlockLine lineStore;

    private boolean isStart = true;
    private long lastAlertTime;

    private Summary summary;
    private float distanceStop;

    private int alertTime = 5000;

    private MyTTS myTTS;

    public static StatusAlert getInstance() {
        if (alertInstance == null) {
            alertInstance = new StatusAlert();
        }
        return alertInstance;
    }

    private StatusAlert() {
        lineStore = BrailleBlockLine.getInstance();
        myTTS = MyTTS.getInstance();
        lastAlertTime = Calendar.getInstance().getTimeInMillis();
        summary = new Summary();
    }

    private void reset() {
        distanceStop = 0;
        summary.reset();
    }

    public void run() {
        // check and alert
        long currentTime = Calendar.getInstance().getTimeInMillis();
        if (currentTime - lastAlertTime >= alertTime) {
            // Alert
            int maxCase = summary.getMax();
            if (isStart) {
                // 1 - Not Found Way on Start
                //      Start and still not found way (start && error > normal)
                if (maxCase == CaseName.CASE_NOT_FOUND) {
                    myTTS.addSpeak("โปรดยืนบนทางเดิน");
                    lastAlertTime = currentTime+1000;
                }
                // 2 - Found Way (First Time)
                //      Start found way (start && normal > error)) !start
                else {
                    myTTS.addSpeak("พบทางเดินแล้ว");
                    lastAlertTime = currentTime-3000;
                    isStart = false;
                }
            } else {
                if (maxCase == CaseName.CASE_FOUND) myTTS.addSpeak("เดินตรงต่อไป");
                else if (maxCase == CaseName.CASE_FACING_LEFT) myTTS.addSpeak("โปรดหันกล้องไปทางซ้าย");
                else if (maxCase == CaseName.CASE_FACING_RIGHT) myTTS.addSpeak("โปรดหันกล้องไปทางขวา");
                else if (maxCase == CaseName.CASE_THREE_WAYS) myTTS.addSpeak("พบทางแยกไป");
                else if (maxCase == CaseName.CASE_NOT_FOUND) myTTS.addSpeak("หาทางเดินไม่เจอ");
                else if (maxCase == CaseName.CASE_END) myTTS.addSpeak("สิ้นสุดทางเดิน");
                else if (maxCase == CaseName.CASE_STOP)
                    myTTS.addSpeak("อีกประมาณ " + (int) distanceStop + "เมตร เป็นทางหยุดเดิน");
                else if (maxCase == CaseName.CASE_TURN_LEFT)
                    myTTS.addSpeak("อีกประมาณ " + (int) distanceStop + "เมตร ทางแยกไปทางซ้าย");
                else if (maxCase == CaseName.CASE_TURN_RIGHT)
                    myTTS.addSpeak("อีกประมาณ " + (int) distanceStop + "เมตร ทางแยกไปทางขวา");
                lastAlertTime = currentTime;
            }
            reset();
        }

        // Process
        int tmpStatus[] = lineStore.getStatus();
        switch (tmpStatus[0]) {
            case CaseName.CASE_STOP:
            case CaseName.CASE_THREE_WAYS:
            case CaseName.CASE_TURN_LEFT:
            case CaseName.CASE_TURN_RIGHT:
                summary.add(tmpStatus[0]);
                distanceStop = tmpStatus[1];break;
            default :
                summary.add(tmpStatus[0]);
        }

    }

}
