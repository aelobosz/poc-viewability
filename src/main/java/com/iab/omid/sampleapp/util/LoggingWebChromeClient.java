package com.iab.omid.sampleapp.util;

import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * LoggingWebChromeClient
 *
 * Provides a hook for calling "alert" and "log" from javascript. Useful for debugging your javascript.
 */
public class LoggingWebChromeClient extends WebChromeClient {
	private static final String TAG = "JAVASCRIPT";

	@Override
	public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
		try {
			Log.i(TAG, message);
			result.confirm();
		} catch (Throwable ignored){
		}
		return true;
	}

	@SuppressWarnings("deprecation")
    @Override
	public void onConsoleMessage(String message, int lineNumber, String sourceID) {
		try {
			Log.i(TAG, "invoked: onConsoleMessage() - " + sourceID + ":" + lineNumber + " - " + message);
			super.onConsoleMessage(message, lineNumber, sourceID);
		} catch (Throwable ignored){
		}
	}

	@Override
	public boolean onConsoleMessage(ConsoleMessage cm) {
		try {
			Log.i(TAG, cm.message() + " -- line " + cm.lineNumber()+" -- source:"+cm.sourceId());
		} catch (Throwable ignored){
		}
		return true;
	}
}
