package com.armhansa.app.blindnavigator.tool;

import java.util.HashMap;

public class Summary {

    private HashMap<Integer, Integer> holder;

    public Summary() {
        holder = new HashMap<>();
    }

    public void add(int key) {
        holder.put(key, holder.keySet().contains(key) ? holder.get(key)+1 : 1);
    }

    public int getMax() {
        int maxCount = -1;
        int maxCase = -1;
        for(int i: holder.keySet()) {
            if(holder.get(i) > maxCount) {
                maxCount = holder.get(i);
                maxCase = i;
            }
        }
        return maxCase;
    }

    public void reset() {
        for(int i: holder.keySet()) {
            holder.put(i, 0);
        }
    }

}
