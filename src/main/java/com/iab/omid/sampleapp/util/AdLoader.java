package com.iab.omid.sampleapp.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import com.iab.omid.library.mercadolibrecl.ScriptInjector;
import com.iab.omid.sampleapp.BuildConfig;
import com.iab.omid.sampleapp.fragments.DisplayAdPrerenderHtmlFragment;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import rx.Single;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

/**
 * 
 * AdPreloader - class that pre-loads a WebView available for use. Also includes utilities for
 * downloading ad html and injecting Omid JS.
 */

public class AdLoader {
	private static final String TAG = "AdLoader";
	public static final String ASSET_SCHEME = "asset:///";	// a private scheme for referencing asset urls

	// Holds and emits the preloaded webview 
	private final BehaviorSubject<WebView> subject = BehaviorSubject.create();

	@SuppressLint("StaticFieldLeak")	// it's the application context
	private static AdLoader instance;

	/**
	 * initialize - must be called early in app startup to begin pre-rendering process
	 * @param application - the application context
	 */
	public static void initialize(@NonNull Application application) {
		instance = new AdLoader(application);
	}

	/**
	 * getInstance - returns the singleton instance
	 * @return the current singleton instance or null, if {@link #initialize(Application)} has not yet been called }
	 */
	public static AdLoader getInstance() {
		return instance;
	}

	private final Context application;

	/**
	 * Constructor
	 * @param context an Android Context
	 */
	private AdLoader(@NonNull Context context) {
		this.application = context.getApplicationContext();
		prerenderHtmlDisplayAd();
	}

	/**
	 * prerenderHtmlAd - call to initiate prerender process.
	 * 
	 * Note: making this public is a bit of a hack. Ideally we'd know when the webview is consumed and
	 * just prerender the next automatically. As is, it relies on {@link DisplayAdPrerenderHtmlFragment} to call
	 * this, as it's the only consumer of preloaded WebViews.  
	 */
	public void prerenderHtmlDisplayAd() {
		setupAdHtml(BuildConfig.HTML_DISPLAY_AD, application)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(new SingleSubscriber<String>() {
				@SuppressLint("SetJavaScriptEnabled")
				@Override
				public void onSuccess(String html) {
					WebView webView = new WebView(application);
					webView.setWebChromeClient(new LoggingWebChromeClient());

					final WebSettings webSettings = webView.getSettings();
					webSettings.setJavaScriptEnabled(true);
					webView.setWebViewClient(new WebViewClient() {
						@Override
						public void onPageFinished(WebView view, String url) {
							Log.d(TAG, "onPageFinished() called with: view = [" + view + "], url = [" + url + "]");
							subject.onNext(webView);
						}
					});
					webView.loadDataWithBaseURL(BuildConfig.HTML_DISPLAY_AD, html, "text/html", "utf-8", null);
				}

				@Override
				public void onError(Throwable error) {
					setupAdHtmlErrorHandler(application, error);
				}
			});
	}

	/**
	 * Call to get a preloaded WebView. The webview you get has fixed url and comes preloaded with the Omid
	 * JavaScript as well as the ad html.
	 * 
	 * @return A {@link Single} to which you can subscribe to get the webview.
	 */
	public @NonNull Single<WebView> getPreloadedWebView() {
		return subject.take(1).toSingle();
	}

	/**
	 * Just downloads ad html and injects the Omid JavaScript
	 * @param creativeUrl the url of the ad html to download. This can be a regular url or, if you use
	 *            an internal asset scheme, {@link #ASSET_SCHEME}, the url path references an
	 *            assert file resource.
	 * @param context used for accessing resources
	 * @return A {@link Single} to which you can subscribe to get the Omid JS injected ad html represented by {@code url}.
	 */
	public Single<String> setupAdHtml(@NonNull String creativeUrl, @NonNull Context context) {
		return downloadUrlToString(creativeUrl, context)
				.map(adHtml -> injectOmsdkJavascript(context, adHtml))
				.map(AdLoader::insertVerficationScriptUrl);
	}

	/**
	 * Call to get the verification script in form the of a Single<String>
	 * @return A {@Link Single} to which you can subscribe to get the verification script as a string
	 */
	public Single<String> getVerificationScript() {
		return downloadUrlToString(BuildConfig.VERIFICATION_URL, null);
	}

	/**
	 * Utility to show a generic html setup error dialog
	 * @param context the dialog UI context
	 * @param error the Throwable that caused the setup failure
	 */
	public void setupAdHtmlErrorHandler(Context context, Throwable error) {
		Log.e(TAG, "Ad Html setup failed with [" + error + "]", error);
		new AlertDialog.Builder(context)
				.setTitle("Ad HTML setup error")
				.setMessage("Ad HTML setup error [" + error + "\n" + Util.getStackTrace(error))
				.setPositiveButton("OK", null)
				.show();
	}

	@WorkerThread
	private Single<String> downloadUrlToString(String urlToDownload, Context context) {
		return Single.fromCallable(() -> {
			//
			// Get input stream from either the assets resources or just from the url
			//
			try (InputStream is =
				urlToDownload.startsWith(ASSET_SCHEME)
						? context.getAssets().open(urlToDownload.substring(ASSET_SCHEME.length()))
						: new URL(urlToDownload).openStream()) {

				try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

					String line;
					StringBuilder stringBuilder = new StringBuilder();
					while ((line = br.readLine()) != null) {
						stringBuilder.append(line + "\n");
					}

					if (stringBuilder.length() == 0) {
						throw new IOException("No bytes downloaded");
					} else {
						return stringBuilder.toString();
					}
				}
			}
		});
	}

	/**
	 * Loads all the content of {@code fileName}.
	 *
	 * @param fileName The name of the file.
	 * @return The content of the file.
	 */
	private String loadAssetContent(Context context, String fileName) {
		InputStream input = null;

		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			input = context.getAssets().open(fileName);
			byte[] buffer = new byte[1024];
			int size;
			while (-1 != (size = input.read(buffer))) {
				output.write(buffer, 0, size);
			}
			output.flush();
			Log.d(TAG, "Serving: " + fileName + " with length: " + output.toByteArray().length);
			return new String(output.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (null != input) {
				try {
					input.close();
				} catch (IOException ex) {}
			}
		}
	}

	static private String injectOmsdkJavascript(Context context, String adHtml) {
		String omidJavascript = OmidJsLoader.getOmidJs(context);

		return ScriptInjector.injectScriptContentIntoHtml(omidJavascript, adHtml);
	}

	static private String insertVerficationScriptUrl(String adHtml) {
		return adHtml.replace("[INSERT RESOURCE URL]", BuildConfig.VERIFICATION_URL);
	}
}
