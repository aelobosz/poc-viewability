package com.iab.omid.sampleapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.iab.omid.sampleapp.content.TestPages;
import com.iab.omid.sampleapp.fragments.AudioAdNativeFragment;
import com.iab.omid.sampleapp.fragments.DisplayAdHtmlFragment;
import com.iab.omid.sampleapp.fragments.DisplayAdJsFragment;
import com.iab.omid.sampleapp.fragments.DisplayAdNativeFragment;
import com.iab.omid.sampleapp.fragments.DisplayAdPrerenderHtmlFragment;
import com.iab.omid.sampleapp.fragments.VideoAdHtmlFragment;
import com.iab.omid.sampleapp.fragments.VideoAdJsFragment;
import com.iab.omid.sampleapp.fragments.VideoAdNativeFragment;

/**
 * An activity representing a single ad detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link AdListActivity}.
 */
public class AdDetailActivity extends AppCompatActivity {

	/**
	 * The fragment argument representing the TestPage for this fragment.
	 */
	public static final String ARG_ITEM_ID = "item_id";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final String itemId = getIntent().getStringExtra(ARG_ITEM_ID);
		final TestPages.TestPage testPage = TestPages.ITEM_MAP.get(itemId);
		switch (testPage.adType) {
			case DISPLAY:
				setContentView(R.layout.activity_display_ad_detail);
				break;
			case VIDEO:
				setContentView(R.layout.activity_video_ad_detail);
				break;
			case AUDIO:
				setContentView(R.layout.activity_video_ad_detail);
				break;
		}
		Toolbar toolbar = findViewById(R.id.detail_toolbar);
		setSupportActionBar(toolbar);

		// Show the Up button in the action bar.
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// savedInstanceState is non-null when there is fragment state
		// saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape).
		// In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		// For more information, see the Fragments API guide at:
		//
		// http://developer.android.com/guide/components/fragments.html
		//
		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			addTestAdFragment(testPage, getSupportFragmentManager(), itemId);
		}
	}

	public static void addTestAdFragment(final TestPages.TestPage testPage, FragmentManager fragmentManager, String itemId) {
		Fragment fragment;
		switch (testPage.adType) {
			case DISPLAY:
				fragment = createDisplayAdFragment(testPage.contentType, testPage.prerender);
				break;
			case VIDEO:
				fragment = createVideoAdFragment(testPage.contentType);
				break;
			case AUDIO:
				fragment = createAudioAdFragment(testPage.contentType);
				break;
			default:
				throw new IllegalArgumentException("unknown AdType: " + testPage.adType);
		}
		Bundle arguments = new Bundle();
		arguments.putString(ARG_ITEM_ID, itemId);
		fragment.setArguments(arguments);

		fragmentManager.beginTransaction()
				.add(R.id.ad_detail_container, fragment)
				.commit();
	}

	@NonNull
	private static Fragment createDisplayAdFragment(TestPages.ContentType contentType, TestPages.Prerender prerender) {
		switch (contentType) {
			case NATIVE:
				return new DisplayAdNativeFragment();
			case HTML:
				return prerender == TestPages.Prerender.YES ? new DisplayAdPrerenderHtmlFragment() : new DisplayAdHtmlFragment();
			case JS:
				return new DisplayAdJsFragment();
			default:
				throw new IllegalArgumentException("unknown content type: " + contentType);
		}
	}

	@NonNull
	private static Fragment createVideoAdFragment(TestPages.ContentType contentType) {
		switch (contentType) {
			case NATIVE:
				return new VideoAdNativeFragment();
			case HTML:
				return new VideoAdHtmlFragment();
			case JS:
				return new VideoAdJsFragment();
			default:
				throw new IllegalArgumentException("unknown content type: " + contentType);
		}
	}

	@NonNull
	private static Fragment createAudioAdFragment(TestPages.ContentType contentType) {
		switch (contentType) {
			case NATIVE:
				return new AudioAdNativeFragment();
			default:
				throw new IllegalArgumentException("unknown content type: " + contentType);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpTo(this, new Intent(this, AdListActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
