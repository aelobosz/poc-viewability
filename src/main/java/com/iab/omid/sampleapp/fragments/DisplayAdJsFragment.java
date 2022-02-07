package com.iab.omid.sampleapp.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.iab.omid.library.mercadolibrecl.adsession.AdEvents;
import com.iab.omid.library.mercadolibrecl.adsession.AdSession;
import com.iab.omid.library.mercadolibrecl.adsession.CreativeType;
import com.iab.omid.library.mercadolibrecl.adsession.FriendlyObstructionPurpose;
import com.iab.omid.sampleapp.AdDetailActivity;
import com.iab.omid.sampleapp.AdListActivity;
import com.iab.omid.sampleapp.R;
import com.iab.omid.sampleapp.content.TestPages;
import com.iab.omid.sampleapp.util.AdLoader;
import com.iab.omid.sampleapp.util.AdSessionUtil;
import com.iab.omid.sampleapp.util.OmidJsLoader;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * A fragment representing a single ad detail screen.
 *
 * This fragment is either contained in a {@link AdListActivity} in two-pane mode (on tablets) or a
 * {@link AdDetailActivity} on handsets.
 *
 * This sample shows loading a Native display ad, passing a url to the Omid js, and marking the impression
 */
public class DisplayAdJsFragment extends Fragment {
  private static final String TAG = "DisplayAdJsFragment";

  private static final String CUSTOM_REFERENCE_DATA = "{ \"birthday\":-310957844000, \"user\":\"me\" }";

  private AdSession adSession;
  private WebView webView;
  private TextView waitingTextView;
  private FloatingActionButton fab;
  private ImageView iabCertified;
  private boolean haveObstruction;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public DisplayAdJsFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
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

  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  @SuppressLint("SetJavaScriptEnabled")
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, this.getClass().getSimpleName() + " created");
    View rootView = inflater.inflate(R.layout.js_ad, container, false);
    waitingTextView = rootView.findViewById(R.id.waitingTextView);
    webView = rootView.findViewById(R.id.sample_display_webview);
    final WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);

    fab = container.getRootView().findViewById(R.id.fab);		// this becomes a friendly obstruction
    if (fab != null) {
      fab.setOnClickListener(this::toggleFriendlyObstruction);
    }

    iabCertified = rootView.findViewById(R.id.iab_certified);
    getVerificationScript();
    return rootView;
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

  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  private void getVerificationScript() {
    final AdLoader adLoader = AdLoader.getInstance();
    adLoader.getVerificationScript()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new SingleSubscriber<String>() {
          @RequiresApi(api = Build.VERSION_CODES.KITKAT)
          @Override
          public void onSuccess(String verificationScript) {
            setupWebView(verificationScript);
          }

          @Override
          public void onError(Throwable error) {
            adLoader.setupAdHtmlErrorHandler(getContext(), error);
          }
        });
  }

  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  private void setupWebView(String verificationScript) {
    String omidJavascript = OmidJsLoader.getOmidJs(getContext());
    webView.evaluateJavascript(omidJavascript, null);
    webView.evaluateJavascript(verificationScript, null);

    waitingTextView.setVisibility(View.GONE);
    webView.setVisibility(View.VISIBLE);

    setupAdSession();
  }

  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  private void setupAdSession() {
    adSession = AdSessionUtil.getJsAdSession(getContext(), webView, CUSTOM_REFERENCE_DATA, CreativeType.NATIVE_DISPLAY);
    adSession.registerAdView(iabCertified);
    if (fab != null) {
      adSession.addFriendlyObstruction(fab, FriendlyObstructionPurpose.OTHER, null);        // initially a friendly
      haveObstruction = true;
    }
    adSession.start();
    AdEvents adEvents = AdEvents.createAdEvents(adSession);
    adEvents.loaded();
    adEvents.impressionOccurred();
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
