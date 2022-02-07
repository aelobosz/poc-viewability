package com.iab.omid.sampleapp.fragments;

import android.app.Activity;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.iab.omid.library.mercadolibrecl.adsession.AdSession;
import com.iab.omid.library.mercadolibrecl.adsession.CreativeType;
import com.iab.omid.library.mercadolibrecl.adsession.FriendlyObstructionPurpose;
import com.iab.omid.sampleapp.AdDetailActivity;
import com.iab.omid.sampleapp.BuildConfig;
import com.iab.omid.sampleapp.R;
import com.iab.omid.sampleapp.content.TestPages;
import com.iab.omid.sampleapp.util.AdLoader;
import com.iab.omid.sampleapp.util.AdSessionUtil;
import com.iab.omid.sampleapp.util.LoggingWebChromeClient;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class VideoAdHtmlFragment extends Fragment {
    private static final String TAG = "VideoAdHtmlFragment";

    private static final String CUSTOM_REFERENCE_DATA = "{ \"birthday\":-310957844000, \"user\":\"me\" }";

    private AdSession adSession;
    private WebView webView;
    private TextView waitingTextView;
    private FloatingActionButton fab;
    private boolean haveObstruction;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public VideoAdHtmlFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(AdDetailActivity.ARG_ITEM_ID)) {
            TestPages.TestPage item = TestPages.ITEM_MAP.get(getArguments().getString(AdDetailActivity.ARG_ITEM_ID));
            if (item == null) {
                throw new RuntimeException("Couldn't find matching test page!");
            }

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = activity != null ? activity.findViewById(R.id.toolbar_layout) : null;
            if (appBarLayout != null) {
                appBarLayout.setTitle(item.getTitle());
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, this.getClass().getSimpleName() + " created");
        View rootView = inflater.inflate(R.layout.html_ad, container, false);
        waitingTextView = rootView.findViewById(R.id.waitingTextView);
        webView = rootView.findViewById(R.id.sample_display_webview);
        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setPluginState(WebSettings.PluginState.ON);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        fab = container.getRootView().findViewById(R.id.fab);		// this becomes a friendly obstruction
        if (fab != null) {
            fab.setOnClickListener(this::toggleFriendlyObstruction);
        }

        getAdHtml();
        return rootView;
    }

    private void getAdHtml() {
        final AdLoader adLoader = AdLoader.getInstance();
        adLoader.setupAdHtml(BuildConfig.HTML_VIDEO_AD, getActivity())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<String>() {
                    @Override
                    public void onSuccess(String html) {
                        setupAdView(html);
                    }

                    @Override
                    public void onError(Throwable error) {
                        adLoader.setupAdHtmlErrorHandler(getContext(), error);
                    }
                });
    }

    private void toggleFriendlyObstruction(View fab) {
        String actionText;
        if (haveObstruction) {
            adSession.removeFriendlyObstruction(fab);
            haveObstruction = false;
            actionText = "removed";
        } else {
            adSession.addFriendlyObstruction(fab, FriendlyObstructionPurpose.OTHER, null);
            haveObstruction = true;
            actionText = "added";
        }

        Snackbar.make(fab, "Friendly obstruction " + actionText, Snackbar.LENGTH_SHORT).show();
    }

    private void setupAdView(String html) {
        webView.setWebChromeClient(new LoggingWebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "onPageFinished() called with: view = [" + view + "], url = [" + url + "]");
                setupAdSession();
            }

            @Override
            public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });

        webView.loadDataWithBaseURL(BuildConfig.HTML_VIDEO_AD, html, "text/html", "UTF-8", null);

        waitingTextView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }

    private void setupAdSession() {
        adSession = AdSessionUtil.getHtmlAdSession(getContext(), webView, CUSTOM_REFERENCE_DATA, CreativeType.DEFINED_BY_JAVASCRIPT);
        if (fab != null) {
            adSession.addFriendlyObstruction(fab, FriendlyObstructionPurpose.OTHER, null);        // initially a friendly
            haveObstruction = true;
        }
        adSession.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adSession.finish();
        adSession = null;

        destroyWebViewDelayed();
        Log.d(TAG, this.getClass().getSimpleName() + " destroyed");
    }

    private void destroyWebViewDelayed() {
        final ViewGroup rootView = (ViewGroup)getView();
        if (rootView != null) {
            rootView.removeView(webView);
        }

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            webView.loadUrl("about:blank");
            webView.destroy();
        }, 1000);
    }
}
