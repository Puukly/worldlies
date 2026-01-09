package com.puukly.worldlies.gauges;

import java.util.HashMap;
import java.util.Map;

public class Gauge {

    private final String name;
    private double value;
    private double rawTotal;
    private final double emaAlpha;
    private final Map<String, Double> thresholds;
    private final double maxPerPlayer;
    private long lastUpdate;

    public Gauge(String name, double emaAlpha, Map<String, Double> thresholds, double maxPerPlayer) {
        this.name = name;
        this.value = 0.0;
        this.rawTotal = 0.0;
        this.emaAlpha = emaAlpha;
        this.thresholds = new HashMap<>(thresholds);
        this.maxPerPlayer = maxPerPlayer;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void addRawValue(double amount) {
        this.rawTotal += amount;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void reset() {
        this.value = 0.0;
        this.rawTotal = 0.0;
        this.lastUpdate = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getRawTotal() {
        return rawTotal;
    }

    public void setRawTotal(double rawTotal) {
        this.rawTotal = rawTotal;
    }

    public double getEmaAlpha() {
        return emaAlpha;
    }

    public Map<String, Double> getThresholds() {
        return new HashMap<>(thresholds);
    }

    public double getMaxPerPlayer() {
        return maxPerPlayer;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public double getNormalizedValue(String stateName) {
        Double threshold = thresholds.get(stateName);
        if (threshold == null || threshold == 0) {
            return 0.0;
        }
        return Math.min(1.0, value / threshold);
    }
}