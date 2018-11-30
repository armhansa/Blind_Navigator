package com.armhansa.app.blindnavigator.tool;

import java.util.HashMap;

public class Summary {

    private HashMap<String, Integer> holder;

    public Summary(String keys[]) {
        holder = new HashMap<>();
        for(String i: keys) {
            holder.put(i, 0);
        }
    }

    public void add(String key) {
        holder.put(key, holder.get(key)+1);
    }

    public String getMax() {
        int maxCount = -1;
        String maxCase = "";
        for(String i: holder.keySet()) {
            if(holder.get(i) > maxCount) {
                maxCount = holder.get(i);
                maxCase = i;
            }
        }
        return maxCase;
    }

    public void reset() {
        for(String i: holder.keySet()) {
            holder.put(i, 0);
        }
    }

}
