package com.iab.omid.sampleapp.fragments;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.iab.omid.library.mercadolibrecl.adsession.AdEvents;
import com.iab.omid.library.mercadolibrecl.adsession.AdSession;
import com.iab.omid.library.mercadolibrecl.adsession.CreativeType;
import com.iab.omid.library.mercadolibrecl.adsession.media.MediaEvents;
import com.iab.omid.library.mercadolibrecl.adsession.media.Position;
import com.iab.omid.library.mercadolibrecl.adsession.media.VastProperties;
import com.iab.omid.sampleapp.AdDetailActivity;
import com.iab.omid.sampleapp.AdListActivity;
import com.iab.omid.sampleapp.R;
import com.iab.omid.sampleapp.content.TestPages;
import com.iab.omid.sampleapp.util.AdSessionUtil;
import com.iab.omid.sampleapp.util.EventLogger;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;

/**
 * A fragment representing a single ad detail screen.
 * 
 * This fragment is either contained in a {@link AdListActivity} in two-pane mode (on tablets) or a
 * {@link AdDetailActivity} on handsets.
 *
 * This sample shows loading a Native ad, passing a url to the Omid js, and marking the impression 
 */
public class AudioAdNativeFragment extends Fragment implements ExoPlayer.EventListener {
	private static final String TAG = "AudioAdNativeFragment";

	private static final String CUSTOM_REFERENCE_DATA = "{ \"birthday\":-310957844000, \"user\":\"me\" }";
	private static final String VIDEO_URL = "asset:///video_ad_asset.mp4";
	private static final int PROGRESS_INTERVAL_MS = 100;
	private static final double EPSILON = .000001;
    private static final int PLAYER_VOLUME = 1;

	private AdSession adSession;
	private MediaEvents mediaEvents;
	private AdEvents adEvents;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private SimpleExoPlayerView simpleExoPlayerView;
	private View audioAdOverlayView;
	private Quartile lastSentQuartile = Quartile.UNKNOWN;
	private boolean complete;
    // Media will always be playing when 1st progress event occurs
	private boolean onProgressIsMediaPlaying = true;
    private boolean loaded;
	private SimpleExoPlayer player;
	private DefaultTrackSelector trackSelector;
	private EventLogger eventLogger;
	private Button volumeUpButton;
	private Button volumeDownButton;
	private TextView volumeView;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public AudioAdNativeFragment() {
	}

	private class VolumeButtonListener implements View.OnClickListener {
		boolean isUp;
		TextView volumeView;

		public VolumeButtonListener(boolean isUp, TextView volumeView) {
			this.isUp = isUp;
			this.volumeView = volumeView;
		}

		@Override
		public void onClick(View v) {
			float newVol = onVolumeButtonClick(isUp);
			if(volumeView != null) volumeView.setText("Volume = " + String.format("%1.1f", newVol));
		}
	};

	private float onVolumeButtonClick(boolean isUp) {
		final float amount = isUp ? 0.1f : -0.1f;
		float newVol = player.getVolume();
		if(player != null) {
			newVol = getAdjustedVolume(newVol, isUp);
			player.setVolume(newVol);
			if(mediaEvents != null) mediaEvents.volumeChange(newVol);
		}
		return newVol;
	}

	private float getAdjustedVolume(float currVol, boolean isUp) {
		final float amount = isUp ? 0.1f : -0.1f;
		currVol += amount;
		if(isUp && currVol >= 1) currVol = 1f;
		else if(currVol < 0f) currVol = 0f;
		return currVol;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Bundle arguments = getArguments();
		if (arguments != null && arguments.containsKey(AdDetailActivity.ARG_ITEM_ID)) {
			// Load the content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
			TestPages.TestPage item = TestPages.ITEM_MAP.get(arguments.getString(AdDetailActivity.ARG_ITEM_ID));
			if (item == null) {
				throw new RuntimeException("Couldn't find matching test page!");
			}

			Activity activity = this.getActivity();
			if (activity != null) {
				CollapsingToolbarLayout appBarLayout = activity.findViewById(R.id.toolbar_layout);
				if (appBarLayout != null) {
					appBarLayout.setTitle(item.getTitle());
				}
			}
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, this.getClass().getSimpleName() + " created");
		View rootView = inflater.inflate(R.layout.audio_ad_native_detail, container, false);

		volumeView = rootView.findViewById(R.id.volumeValue);
		volumeUpButton = rootView.findViewById(R.id.volumeUp);
		volumeDownButton = rootView.findViewById(R.id.volumeDown);
		volumeUpButton.setOnClickListener(new VolumeButtonListener(true, volumeView));
		volumeDownButton.setOnClickListener(new VolumeButtonListener(false, volumeView));

		// Show the content as text in a TextView.
		simpleExoPlayerView = rootView.findViewById(R.id.audioView);
		audioAdOverlayView = rootView.findViewById(R.id.audio_ad_overlay);

		initializePlayer(container.getContext());		

		//Setting MediaController and URI, then starting the simpleExoPlayerView
		simpleExoPlayerView.requestFocus();

		try {
			adSession = AdSessionUtil.getNativeAdSession(getContext(), CUSTOM_REFERENCE_DATA, CreativeType.AUDIO);
		} catch (MalformedURLException e) {
			Log.d(TAG, "setupAdSession failed", e);
			throw new UnsupportedOperationException(e);
		}
		mediaEvents = MediaEvents.createMediaEvents(adSession);
		adEvents = AdEvents.createAdEvents(adSession);
		adSession.registerAdView(rootView);
		adSession.start();

		return rootView;
	}

	private void initializePlayer(@NonNull Context context) {

// 1, Setup selector
		DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
		TrackSelection.Factory mediaTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
		trackSelector = new DefaultTrackSelector(mediaTrackSelectionFactory);

// 2. Create the player
		RenderersFactory renderersFactory = new DefaultRenderersFactory(context);
		player = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
		player.addListener(this);

		eventLogger = new EventLogger(trackSelector);
		player.addListener(eventLogger);
		player.setAudioDebugListener(eventLogger);
		player.setVideoDebugListener(eventLogger);
		player.setMetadataOutput(eventLogger);
		player.setVideoListener(videoListener);
        player.setVolume(PLAYER_VOLUME);

		simpleExoPlayerView.setPlayer(player);
		player.setPlayWhenReady(true);

		DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
				com.google.android.exoplayer2.util.Util.getUserAgent(context, "com.iab.omid"), bandwidthMeter);
		ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

		MediaSource mediaSource = new ExtractorMediaSource(Uri.parse(VIDEO_URL),
				dataSourceFactory, extractorsFactory, null, null);

// 3. Prepare the player with the source.
		player.prepare(mediaSource);

		audioAdOverlayView.setVisibility(View.VISIBLE);
		audioAdOverlayView.bringToFront();
	}

	private final SimpleExoPlayer.VideoListener videoListener = new SimpleExoPlayer.VideoListener() {
		@Override
		public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
			Log.d(TAG, "onVideoSizeChanged() called with: width = [" + width + "], height = [" + height + "], unappliedRotationDegrees = [" + unappliedRotationDegrees + "], pixelWidthHeightRatio = [" + pixelWidthHeightRatio + "]");
		}

		@Override
		public void onRenderedFirstFrame() {
			Log.d(TAG, "onRenderedFirstFrame() called");
		}
	};

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
		Log.d(TAG, "onTracksChanged() called with: trackGroups = [" + trackGroups + "], trackSelections = [" + trackSelections + "]");
	}

	@Override
	public void onLoadingChanged(boolean isLoading) {
		Log.d(TAG, "onLoadingChanged() called with: isLoading = [" + isLoading + "]");
	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		switch (playbackState) {
			case ExoPlayer.STATE_BUFFERING:
				onStateBuffering();
				break;
			case ExoPlayer.STATE_READY:
				onStateReady();
				break;
			case ExoPlayer.STATE_ENDED:
				onStateEnded();
				break;
			case ExoPlayer.STATE_IDLE:
				break;
			default:
				throw new IllegalStateException("Unknown playbackState: " + playbackState);
		}
	}

	private void onStateReady() {
	    if (!loaded) {
            VastProperties vastProperties = VastProperties.createVastPropertiesForNonSkippableMedia(false, Position.STANDALONE);
            Log.d(TAG, "mediaEvents.loaded() VastProperties = [" + vastProperties + "]");
			adEvents.loaded(vastProperties);

            loaded = true;
        }

		postProgress();
	}

	private void onStateBuffering() {
	}

	private void onStateEnded() {
		complete = true;
		Log.d(TAG, "mediaEvents.complete()");
		mediaEvents.complete();
	}

	@Override
	public void onPlayerError(ExoPlaybackException error) {
		Log.d(TAG, "onPlayerError() called with: error = [" + error + "]");
	}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
		Log.d(TAG, "onPlaybackParametersChanged() called with: playbackParameters = [" + playbackParameters + "]");
	}

	private static class ProgressRunnable implements Runnable {
		private final WeakReference<AudioAdNativeFragment> videoAdNativeFragmentWeakReference;

		ProgressRunnable(AudioAdNativeFragment videoAdNativeFragment) {
			this.videoAdNativeFragmentWeakReference = new WeakReference<>(videoAdNativeFragment);
		}

		@Override
		public void run() {
			AudioAdNativeFragment videoAdNativeFragment = videoAdNativeFragmentWeakReference.get();
			if (videoAdNativeFragment == null) {
				return;
			}

			videoAdNativeFragment.onProgress();
		}
	}

	private void onProgress() {
		if (adSession == null) {
			return;
		}

		if (complete) {
			return;
		}

		updateQuartile();
		updatePlayPause();
		postProgress();
	}

	private void updatePlayPause() {
		final boolean playing = player.getPlayWhenReady() && (player.getPlaybackState() == ExoPlayer.STATE_READY);
		if (playing != this.onProgressIsMediaPlaying) {
			if (playing) {
				Log.d(TAG, "mediaEvents.resume()");
				mediaEvents.resume();
			} else {
				Log.d(TAG, "mediaEvents.pause()");
				mediaEvents.pause();
			}

			this.onProgressIsMediaPlaying = playing;
		}
	}

	private void updateQuartile() {
		final long duration = player.getDuration();
		final long currentPosition = player.getCurrentPosition();

		if (duration != 0) {
			final Quartile currentQuartile = getQuartile(currentPosition, duration);

			// Don't send old quartile stats that we have either already sent, or passed.
			if (currentQuartile != lastSentQuartile && currentQuartile.ordinal() > lastSentQuartile.ordinal()) {
				sendQuartile(currentQuartile);
				lastSentQuartile = currentQuartile;
			}
		}
	}

	private void sendQuartile(Quartile quartile) {
		Log.d(TAG, "sendQuartile() called with: quartile = [" + quartile + "]");
		switch (quartile) {
			case START:
                Log.d(TAG, "mediaEvents.start() called with: duration = [" + player.getDuration() + "], volume = [" + PLAYER_VOLUME + "]");
				mediaEvents.start(player.getDuration(), PLAYER_VOLUME);
				adEvents.impressionOccurred();
				break;
			case FIRST:
				Log.d(TAG, "mediaEvents.firstQuartile()");
				mediaEvents.firstQuartile();
				break;
			case SECOND:
				Log.d(TAG, "mediaEvents.midpoint()");
				mediaEvents.midpoint();
				break;
			case THIRD:
				Log.d(TAG, "mediaEvents.thirdQuartile()");
				mediaEvents.thirdQuartile();
				break;
			case UNKNOWN:
			default:
				break;
		}
	}

	private void postProgress() {
		handler.removeCallbacks(progressRunnable);
		handler.postDelayed(progressRunnable, PROGRESS_INTERVAL_MS);
	}
	private final ProgressRunnable progressRunnable = new ProgressRunnable(this);

	private static Quartile getQuartile(long position, long duration) {
		final double completionFraction = position / (double) duration;
		if (lessThan(completionFraction, 0)) {
			return Quartile.UNKNOWN;
		}

		if (lessThan(completionFraction, 0.25)) {
			return Quartile.START;
		}

		if (lessThan(completionFraction, 0.5)) {
			return Quartile.FIRST;
		}

		if (lessThan(completionFraction, 0.75)) {
			return Quartile.SECOND;
		}

		// We report Quartile.THIRD when completionFraction > 1 on purpose
		// since track might technically report elapsed time after it's completion
		// and if Quartile.THIRD hasn't been reported already, it will be lost
		return Quartile.THIRD;
	}

	private static boolean lessThan(double a, double b){
		return b - a > EPSILON;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
        adSession.finish();
        adSession = null;
		handler.removeCallbacksAndMessages(null);
		releasePlayer();
		Log.d(TAG, this.getClass().getSimpleName() + " destroyed");
	}

	private void releasePlayer() {
		if (player != null) {
			player.release();
			player = null;
			trackSelector = null;
			eventLogger = null;
		}
	}

	public enum Quartile {
		UNKNOWN,
		START,
		FIRST,
		SECOND,
		THIRD,
	}
}
