package com.iab.omid.sampleapp.fragments

import com.iab.omid.library.mercadolibrecl.adsession.AdSession
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.ImageView
import android.os.Bundle
import com.iab.omid.sampleapp.AdDetailActivity
import com.iab.omid.sampleapp.content.TestPages.TestPage
import com.iab.omid.sampleapp.content.TestPages
import java.lang.RuntimeException
import android.app.Activity
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.iab.omid.sampleapp.R
import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.iab.omid.sampleapp.fragments.DisplayAdNativeFragment
import com.iab.omid.library.mercadolibrecl.adsession.FriendlyObstructionPurpose
import com.google.android.material.snackbar.Snackbar
import com.iab.omid.sampleapp.util.AdSessionUtil
import com.iab.omid.library.mercadolibrecl.adsession.CreativeType
import java.net.MalformedURLException
import com.iab.omid.library.mercadolibrecl.adsession.AdEvents
import com.iab.omid.sampleapp.databinding.ActivityDisplayAdDetailBinding
import com.iab.omid.sampleapp.databinding.NativeAdBinding
import com.iab.omid.sampleapp.util.Util

/**
 * A fragment representing a single ad detail screen.
 *
 *
 * This fragment is either contained in a [AdListActivity] in two-pane mode (on tablets) or a
 * [AdDetailActivity] on handsets.
 *
 *
 * This sample shows loading a Native display ad, passing a url to the Omid js, and marking the impression
 */
class DisplayAdNativeFragment : Fragment() {
    private var adSession: AdSession? = null
    private var fab: FloatingActionButton? = null
    private var iabCertified: ImageView? = null
    private var haveObstruction = false
    private var _binding: NativeAdBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments?.containsKey(AdDetailActivity.ARG_ITEM_ID) == true) {
            val item = TestPages.ITEM_MAP[arguments!!.getString(AdDetailActivity.ARG_ITEM_ID)]
                ?: throw RuntimeException("Couldn't find matching test page!")
            val activity: Activity? = this.activity
            val appBarLayout: CollapsingToolbarLayout? = activity?.findViewById(R.id.toolbar_layout)
            if (appBarLayout != null) {
                appBarLayout.title = item.title
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, this.javaClass.simpleName + " created")
        _binding = NativeAdBinding.inflate(layoutInflater)
        fab = container!!.rootView.findViewById(R.id.fab) // this becomes a friendly obstruction
        if (fab != null) {
            fab!!.setOnClickListener { fab: View -> toggleFriendlyObstruction(fab) }
        }
        iabCertified = binding.iabCertified

        setupAdView()
        return binding.root
    }

    private fun toggleFriendlyObstruction(fab: View) {
        val actionText: String
        if (haveObstruction) {
            adSession?.removeFriendlyObstruction(fab)
            haveObstruction = false
            actionText = "removed"
        } else {
            adSession?.addFriendlyObstruction(fab, FriendlyObstructionPurpose.OTHER, null)
            haveObstruction = true
            actionText = "added"
        }
        Snackbar.make(fab, "Friendly obstruction $actionText", Snackbar.LENGTH_SHORT).show()
    }

    private fun setupAdView() {
        try {
            adSession = AdSessionUtil.getNativeAdSession(
                requireContext(),
                CUSTOM_REFERENCE_DATA,
                CreativeType.NATIVE_DISPLAY
            )
        } catch (e: MalformedURLException) {
            Log.d(TAG, "setupAdSession failed", e)
            AlertDialog.Builder(requireActivity())
                .setTitle("Ad Setup error")
                .setMessage("""MalformedURLException [$e${Util.getStackTrace(e)}""".trimIndent())
                .setPositiveButton("OK", null)
                .show()
        }
        adSession?.registerAdView(iabCertified)
        if (fab != null) {
            adSession?.addFriendlyObstruction(
                fab,
                FriendlyObstructionPurpose.OTHER,
                null
            ) // initially a friendly
            haveObstruction = true
        }
        adSession?.start()
        val adEvents = AdEvents.createAdEvents(adSession)
        adEvents.loaded()
        adEvents.impressionOccurred()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adSession?.finish()
        adSession = null
        Log.d(TAG, this.javaClass.simpleName + " destroyed")
        _binding = null
    }

    companion object {
        private const val TAG = "DisplayAdNativeFragment"
        private const val CUSTOM_REFERENCE_DATA = "{ \"birthday\":-310957844000, \"user\":\"me\" }"
    }
}