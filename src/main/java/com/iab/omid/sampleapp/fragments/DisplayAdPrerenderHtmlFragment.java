package com.iab.omid.sampleapp.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.iab.omid.library.mercadolibrecl.adsession.AdSession;
import com.iab.omid.library.mercadolibrecl.adsession.CreativeType;
import com.iab.omid.library.mercadolibrecl.adsession.FriendlyObstructionPurpose;
import com.iab.omid.sampleapp.AdDetailActivity;
import com.iab.omid.sampleapp.AdListActivity;
import com.iab.omid.sampleapp.R;
import com.iab.omid.sampleapp.content.TestPages;
import com.iab.omid.sampleapp.util.AdLoader;
import com.iab.omid.sampleapp.util.AdSessionUtil;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * A fragment representing a single ad detail screen.

 * This fragment is either contained in a {@link AdListActivity} in two-pane mode (on tablets) or a
 * {@link AdDetailActivity} on handsets.
 *
 * This sample shows loading an HTML display ad using a pre-rendered (and omid js injected) webview
 * and marking the impression 
 */
public class DisplayAdPrerenderHtmlFragment extends Fragment {
	private static final String TAG = "DisplayAdHtmlFragment";

	private static final String CUSTOM_REFERENCE_DATA = "{ \"birthday\":-310957844000, \"user\":\"me\" }";

	private AdSession adSession;
	private FloatingActionButton fab;
	private ViewGroup prerenderLayout;
	private Subscription subscription;
    private boolean haveObstruction;
    private WebView webView;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public DisplayAdPrerenderHtmlFragment() {
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

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, this.getClass().getSimpleName() + " created");
		prerenderLayout = (ViewGroup)inflater.inflate(R.layout.prerender_html_ad, container, false);
		TextView waitingTextView = prerenderLayout.findViewById(R.id.waitingTextView);
		waitingTextView.setVisibility(View.GONE);

		fab = container.getRootView().findViewById(R.id.fab);		// this becomes a friendly obstruction
		if (fab != null) {
			fab.setOnClickListener(this::toggleFriendlyObstruction);
		}

		subscription = AdLoader.getInstance().getPreloadedWebView()
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(webView -> {
				setupAdView(webView);
				AdLoader.getInstance().prerenderHtmlDisplayAd();        // preload next
			}
		);

		return prerenderLayout;
	}

	private void toggleFriendlyObstruction(View fab) {
		if (adSession == null) {
			return;
		}

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

	private void setupAdView(WebView webView) {
        this.webView = webView;
		webView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		prerenderLayout.addView(webView);
		webView.setVisibility(View.VISIBLE);
		adSession = AdSessionUtil.getHtmlAdSession(getContext(), webView, CUSTOM_REFERENCE_DATA, CreativeType.HTML_DISPLAY);

		if (fab != null) {
			adSession.addFriendlyObstruction(fab, FriendlyObstructionPurpose.OTHER, null);        // initially a friendly
            haveObstruction = true;
		}
		adSession.start();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (subscription != null) {
			subscription.unsubscribe();
		}

		if (adSession != null) {
			adSession.finish();
			adSession = null;

            destroyWebViewDelayed();
		}
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
