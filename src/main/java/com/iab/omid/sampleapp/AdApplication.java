package com.iab.omid.sampleapp;

import android.app.Application;
import com.facebook.stetho.Stetho;
import com.iab.omid.sampleapp.util.AdLoader;
import com.squareup.leakcanary.LeakCanary;

/**
 * AdApplication - application subclass. Init Omid SDK, AdLoader, and debug libraries
 *
 */

public class AdApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();

		LeakCanary.install(this);
		Stetho.initializeWithDefaults(this);
        AdLoader.initialize(this);
	}
}
