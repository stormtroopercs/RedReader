/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.stormtroopercs.materialreader.navigation

import android.webkit.WebView

/**
 * Bridges the on-screen `HtmlView` route's WebView back to the host
 * activity's system-back handling, so that the Compose HtmlView screen has
 * the same behaviour the legacy HtmlViewActivity had: while the document
 * has history, the back key walks that history before it can pop the
 * navigation stack.
 *
 * The `HtmlView` entry registers the WebView when it is created and
 * unregisters it on dispose; the activity consults [goBack] / [canGoBack]
 * from its back-press overrides and re-evaluates [onBackChanged] so the
 * platform back-callback stays in the right enabled/disabled state.
 */
object HtmlViewBackHandler {
	private var webView: WebView? = null

	/**
	 * Invoked on the main thread whenever [canGoBack] might have changed
	 * (a WebView registered/unregistered, or its history grew/shrank), so
	 * that a host activity can re-evaluate whether it must intercept the
	 * system back key (see BaseActivity.invalidateBackPressedCallback()).
	 */
	var onBackChanged: (() -> Unit)? = null

	/** Whether the current WebView could consume a back press. */
	val canGoBack: Boolean
		get() = webView?.canGoBack() == true

	fun register(webView: WebView) {
		this.webView = webView
		onBackChanged?.invoke()
	}

	fun unregister(webView: WebView) {
		if (this.webView === webView) {
			this.webView = null
			onBackChanged?.invoke()
		}
	}

	/** Drop the currently registered WebView (e.g. when its route is disposed). */
	fun clear() {
		val current = webView ?: return
		this.webView = null
		onBackChanged?.invoke()
	}

	/** Walk the WebView's history. `true` if the back press was consumed. */
	fun goBack(): Boolean {
		val webView = webView ?: return false
		return if (webView.canGoBack()) {
			webView.goBack()
			onBackChanged?.invoke()
			true
		} else {
			false
		}
	}
}
