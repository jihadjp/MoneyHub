package com.jptechgenius.moneyhub;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

/** Entry point for Hilt. Must be declared in AndroidManifest.xml. */
@HiltAndroidApp
public class MoneyHubApplication extends Application {
    // Hilt initializes the dependency graph here automatically.
}