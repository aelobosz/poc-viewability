package com.iab.omid.sampleapp.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

/**
 * TestPages - class that defines the available test pages. Add new ones here to test different combinations.
 */
public class TestPages {

	/**
	 * An array of test pages.
	 */
	public static final List<TestPage> ITEMS = new ArrayList<>();

	/**
	 * A map of test pages, by ID.
	 */
	public static final Map<String, TestPage> ITEM_MAP = new HashMap<>();

	public enum AdType {DISPLAY, VIDEO, AUDIO}
	public enum ContentType {NATIVE, HTML, JS}
	public enum Prerender { YES, NO, NA }

	static {
		// Add new test pages here and handle in AdDetailActivity to create new test fragments
		addItem(new TestPage(newUuid(), AdType.DISPLAY, ContentType.NATIVE, Prerender.NA));
		addItem(new TestPage(newUuid(), AdType.DISPLAY, ContentType.HTML, Prerender.YES));
		addItem(new TestPage(newUuid(), AdType.DISPLAY, ContentType.HTML, Prerender.NO));
		addItem(new TestPage(newUuid(), AdType.DISPLAY, ContentType.JS, Prerender.NA));
		addItem(new TestPage(newUuid(), AdType.VIDEO, ContentType.NATIVE, Prerender.NA));
		addItem(new TestPage(newUuid(), AdType.VIDEO, ContentType.HTML, Prerender.NA));
		addItem(new TestPage(newUuid(), AdType.VIDEO, ContentType.JS, Prerender.NA));
		addItem(new TestPage(newUuid(), AdType.AUDIO, ContentType.NATIVE, Prerender.NA));
	}

	private static String newUuid() {
		return UUID.randomUUID().toString();
	}

	private static void addItem(TestPage item) {
		ITEMS.add(item);
		ITEM_MAP.put(item.id, item);
	}

	/**
	 * A piece of test content.
	 */
	public static class TestPage {
		private String title;
		public final String id;
		public final AdType adType;
		public final ContentType contentType;
		public final Prerender prerender;

		TestPage(String id, AdType adType, ContentType contentType, Prerender prerender) {
			this.id = id;
			this.adType = adType;
			this.contentType = contentType;
			this.prerender = prerender;
			
			title = "Test " + startCap(contentType.name()) + " " + startCap(adType.name());
			if (prerender != Prerender.NA) {
				title +=  " Prerender: " + startCap(prerender.name());
			}
		}

		public String getTitle() {
			return title;
		}
		
		private static String startCap(String str) {
			return StringUtils.capitalize(str.toLowerCase());
		}
	}
}
