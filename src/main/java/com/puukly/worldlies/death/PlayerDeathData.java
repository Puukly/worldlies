package com.puukly.worldlies.death;

import java.util.ArrayList;
import java.util.List;

public class PlayerDeathData {

    private int totalDeaths;
    private final List<Long> deathHistory; // Timestamps

    public PlayerDeathData() {
        this.totalDeaths = 0;
        this.deathHistory = new ArrayList<>();
    }

    public void addDeath(long timestamp, boolean isPvP) {
        totalDeaths++;
        deathHistory.add(timestamp);
    }

    public int getDeathsInWindow(long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        int count = 0;

        for (Long timestamp : deathHistory) {
            if (timestamp >= cutoff) {
                count++;
            }
        }

        return count;
    }

    public void cleanOldDeaths(long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        deathHistory.removeIf(timestamp -> timestamp < cutoff);
    }

    public void clearRecentDeaths() {
        deathHistory.clear();
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    public void setTotalDeaths(int totalDeaths) {
        this.totalDeaths = totalDeaths;
    }

    public List<Long> getDeathHistory() {
        return deathHistory;
    }
}