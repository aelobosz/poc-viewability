package com.iab.omid.sampleapp.util;

import android.content.Context;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.iab.omid.library.mercadolibrecl.Omid;
import com.iab.omid.library.mercadolibrecl.adsession.AdSession;
import com.iab.omid.library.mercadolibrecl.adsession.AdSessionConfiguration;
import com.iab.omid.library.mercadolibrecl.adsession.AdSessionContext;
import com.iab.omid.library.mercadolibrecl.adsession.CreativeType;
import com.iab.omid.library.mercadolibrecl.adsession.ImpressionType;
import com.iab.omid.library.mercadolibrecl.adsession.Owner;
import com.iab.omid.library.mercadolibrecl.adsession.Partner;
import com.iab.omid.library.mercadolibrecl.adsession.VerificationScriptResource;
import com.iab.omid.sampleapp.BuildConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * AdSessionUtil
 */

public final class AdSessionUtil {

    @NonNull
    public static AdSession getNativeAdSession(Context context, String customReferenceData, CreativeType creativeType) throws MalformedURLException {
        ensureOmidActivated(context);

        AdSessionConfiguration adSessionConfiguration =
                AdSessionConfiguration.createAdSessionConfiguration(creativeType,
                        (creativeType == CreativeType.AUDIO ? ImpressionType.AUDIBLE : ImpressionType.VIEWABLE),
                        Owner.NATIVE,
                        (creativeType == CreativeType.HTML_DISPLAY || creativeType == CreativeType.NATIVE_DISPLAY) ?
                                Owner.NONE : Owner.NATIVE, false);

        Partner partner = Partner.createPartner(BuildConfig.PARTNER_NAME, BuildConfig.VERSION_NAME);
        final String omidJs = OmidJsLoader.getOmidJs(context);
        List<VerificationScriptResource> verificationScripts = AdSessionUtil.getVerificationScriptResources();
        AdSessionContext adSessionContext = AdSessionContext.createNativeAdSessionContext(partner, omidJs, verificationScripts, null, customReferenceData);
        return AdSession.createAdSession(adSessionConfiguration, adSessionContext);
    }

    @NonNull
    public static AdSession getHtmlAdSession(Context context, WebView webView, String customReferenceData, CreativeType creativeType) {
        ensureOmidActivated(context);

        AdSessionConfiguration adSessionConfiguration =
                AdSessionConfiguration.createAdSessionConfiguration(
                        creativeType,
                        ImpressionType.BEGIN_TO_RENDER, Owner.JAVASCRIPT,
                        creativeType == CreativeType.HTML_DISPLAY || creativeType == CreativeType.DEFINED_BY_JAVASCRIPT ? Owner.NONE : Owner.NATIVE, false);
        Partner partner = Partner.createPartner(BuildConfig.PARTNER_NAME, BuildConfig.VERSION_NAME);
        AdSessionContext adSessionContext = AdSessionContext.createHtmlAdSessionContext(partner, webView, null, customReferenceData);
        AdSession adSession = AdSession.createAdSession(adSessionConfiguration, adSessionContext);

        adSession.registerAdView(webView);
        return adSession;
    }

    @NonNull
    public static AdSession getJsAdSession(Context context, WebView webView, String customReferenceData, CreativeType creativeType) {
        ensureOmidActivated(context);

        AdSessionConfiguration adSessionConfiguration =
                AdSessionConfiguration.createAdSessionConfiguration(
                        creativeType,
                        ImpressionType.VIEWABLE,
                        Owner.NATIVE,
                        (creativeType == CreativeType.NATIVE_DISPLAY) ? Owner.NONE : Owner.NATIVE,
                        false);
        Partner partner = Partner.createPartner(BuildConfig.PARTNER_NAME, BuildConfig.VERSION_NAME);
        AdSessionContext adSessionContext = AdSessionContext.createJavascriptAdSessionContext(partner, webView, null, customReferenceData);
        AdSession adSession = AdSession.createAdSession(adSessionConfiguration, adSessionContext);

        return adSession;
    }

    @NonNull
    private static List<VerificationScriptResource> getVerificationScriptResources() throws MalformedURLException {
        VerificationScriptResource verificationScriptResource = BuildConfig.VERIFICATION_PARAMETERS == null ?
                VerificationScriptResource.createVerificationScriptResourceWithoutParameters(getURL()) :
                VerificationScriptResource.createVerificationScriptResourceWithParameters(BuildConfig.VENDOR_KEY, getURL(), BuildConfig.VERIFICATION_PARAMETERS);
        return Collections.singletonList(verificationScriptResource);
    }

    @NonNull
    private static URL getURL() throws MalformedURLException {
        return new URL(BuildConfig.VERIFICATION_URL);
    }

    /**
     * Lazily activate the OMID API.
     *
     * @param context any context
     */
    private static void ensureOmidActivated(Context context) {
        Omid.activate(context.getApplicationContext());
    }
}
