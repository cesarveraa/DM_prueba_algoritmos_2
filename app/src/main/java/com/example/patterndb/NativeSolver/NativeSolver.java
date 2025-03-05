package com.example.patterndb.NativeSolver;

import android.content.res.AssetManager;

public class NativeSolver {
    static {
        System.loadLibrary("native-lib");
    }
    public native void setAssetManager(AssetManager assetManager);
    public native String solvePuzzle(String puzzleMatrix);
}
