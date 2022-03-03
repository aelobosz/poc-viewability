package com.iab.omid.sampleapp.util

import android.content.Context
import kotlin.Throws
import java.net.MalformedURLException
import com.iab.omid.sampleapp.util.AdSessionUtil
import com.iab.omid.sampleapp.util.OmidJsLoader
import android.webkit.WebView
import java.net.URL
import com.iab.omid.library.mercadolibrecl.Omid
import com.iab.omid.library.mercadolibrecl.adsession.*
import com.iab.omid.sampleapp.BuildConfig

/**
 * AdSessionUtil
 */
object AdSessionUtil {
    @JvmStatic
    @Throws(MalformedURLException::class)
    fun getNativeAdSession(
        context: Context,
        customReferenceData: String?,
        creativeType: CreativeType
    ): AdSession {
        ensureOmidActivated(context)
        val adSessionConfiguration = AdSessionConfiguration.createAdSessionConfiguration(
            creativeType,
            if (creativeType == CreativeType.AUDIO) ImpressionType.AUDIBLE else ImpressionType.VIEWABLE,
            Owner.NATIVE,
            if (creativeType == CreativeType.HTML_DISPLAY || creativeType == CreativeType.NATIVE_DISPLAY) Owner.NONE else Owner.NATIVE,
            false
        )
        val partner = Partner.createPartner(BuildConfig.PARTNER_NAME, BuildConfig.VERSION_NAME)
        val omidJs = OmidJsLoader.getOmidJs(context)
        val verificationScripts = verificationScriptResources
        val adSessionContext = AdSessionContext.createNativeAdSessionContext(
            partner,
            omidJs,
            verificationScripts,
            null,
            customReferenceData
        )
        return AdSession.createAdSession(adSessionConfiguration, adSessionContext)
    }

    @JvmStatic
    fun getHtmlAdSession(
        context: Context,
        webView: WebView?,
        customReferenceData: String?,
        creativeType: CreativeType
    ): AdSession {
        ensureOmidActivated(context)
        val adSessionConfiguration = AdSessionConfiguration.createAdSessionConfiguration(
            creativeType,
            ImpressionType.BEGIN_TO_RENDER,
            Owner.JAVASCRIPT,
            if (creativeType == CreativeType.HTML_DISPLAY || creativeType == CreativeType.DEFINED_BY_JAVASCRIPT) Owner.NONE else Owner.NATIVE,
            false
        )
        val partner = Partner.createPartner(BuildConfig.PARTNER_NAME, BuildConfig.VERSION_NAME)
        val adSessionContext =
            AdSessionContext.createHtmlAdSessionContext(partner, webView, null, customReferenceData)
        val adSession = AdSession.createAdSession(adSessionConfiguration, adSessionContext)
        adSession.registerAdView(webView)
        return adSession
    }

    @JvmStatic
    fun getJsAdSession(
        context: Context,
        webView: WebView?,
        customReferenceData: String?,
        creativeType: CreativeType
    ): AdSession {
        ensureOmidActivated(context)
        val adSessionConfiguration =
            AdSessionConfiguration.createAdSessionConfiguration(
                creativeType,
                ImpressionType.VIEWABLE,
                Owner.NATIVE,
                if (creativeType == CreativeType.NATIVE_DISPLAY) Owner.NONE else Owner.NATIVE,
                false
            )
        val partner = Partner.createPartner(
            BuildConfig.PARTNER_NAME,
            BuildConfig.VERSION_NAME
        )
        val adSessionContext = AdSessionContext.createJavascriptAdSessionContext(
            partner,
            webView,
            null,
            customReferenceData
        )
        return AdSession.createAdSession(adSessionConfiguration, adSessionContext)
    }

    @get:Throws(MalformedURLException::class)
    private val verificationScriptResources: List<VerificationScriptResource>
        get() {
            val verificationScriptResource =
                if (BuildConfig.VERIFICATION_PARAMETERS == null) VerificationScriptResource.createVerificationScriptResourceWithoutParameters(
                    uRL
                ) else VerificationScriptResource.createVerificationScriptResourceWithParameters(
                    BuildConfig.VENDOR_KEY, uRL, BuildConfig.VERIFICATION_PARAMETERS
                )
            return listOf(verificationScriptResource)
        }

    @get:Throws(MalformedURLException::class)
    private val uRL: URL
        get() = URL(BuildConfig.VERIFICATION_URL)

    /**
     * Lazily activate the OMID API.
     *
     * @param context any context
     */
    private fun ensureOmidActivated(context: Context) {
        Omid.activate(context.applicationContext)
    }
}