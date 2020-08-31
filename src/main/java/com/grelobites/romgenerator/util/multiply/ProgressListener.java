package com.grelobites.romgenerator.util.multiply;

@FunctionalInterface
public interface ProgressListener {
    void onProgressUpdate(double value);
}
