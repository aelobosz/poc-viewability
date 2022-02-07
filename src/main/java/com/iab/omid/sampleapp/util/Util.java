package com.iab.omid.sampleapp.util;

import com.iab.omid.sampleapp.BuildConfig;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Util
 */

public final class Util {
	/**
	 * getStackTrace - get a stack trace from a Throwable as a string 
	 * @param throwable - the Throwable whose exception is to be converted to a string
	 * @return - the sting representation of the Throwable's stack trace  
	 */
	public static String getStackTrace(final Throwable throwable) {
		final StringWriter sw = new StringWriter();
		try (final PrintWriter pw = new PrintWriter(sw, true)) {
			throwable.printStackTrace(pw);
			return sw.getBuffer().toString();
		}
	}

	public static String overrideValidationScriptUrl(String validationScript) {
        return validationScript.replace(BuildConfig.VALIDATION_SCRIPT_URL, BuildConfig.VALIDATION_SCRIPT_URL_OVERRIDE);
    }
}
