package com.wahon.shared.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Url
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSHTTPCookieStorage
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIApplication
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

@OptIn(ExperimentalForeignApi::class)
class IosWkWebViewAntiBotChallengeResolver(
    private val cookiesStorage: CookiesStorage,
) : AntiBotChallengeResolver {

    override suspend fun resolve(
        requestUrl: String,
        challenge: AntiBotChallenge,
        userAgent: String,
    ): Boolean {
        val targetUrl = runCatching { Url(requestUrl) }.getOrNull()
        if (targetUrl == null) {
            Napier.w(
                message = "Skip anti-bot resolve: invalid request url $requestUrl",
                tag = LOG_TAG,
            )
            return false
        }
        val nsUrl = NSURL.URLWithString(requestUrl)
        if (nsUrl == null) {
            Napier.w(
                message = "Skip anti-bot resolve: cannot build NSURL for $requestUrl",
                tag = LOG_TAG,
            )
            return false
        }
        val expectedCookies = challenge.expectedCookieNames()
        if (expectedCookies.isEmpty()) return false

        return withContext(Dispatchers.Main) {
            val webView = WKWebView(
                frame = CGRectMake(0.0, 0.0, 1.0, 1.0),
                configuration = WKWebViewConfiguration(),
            )
            try {
                if (userAgent.isNotBlank()) {
                    webView.customUserAgent = userAgent
                }
                webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))

                var resolvedCookieHeader: String? = null
                withTimeoutOrNull(ANTI_BOT_RESOLVE_TIMEOUT_MS) {
                    while (resolvedCookieHeader.isNullOrBlank()) {
                        val cookieHeader = readCookieHeader(nsUrl)
                        if (hasExpectedCookie(cookieHeader, expectedCookies)) {
                            resolvedCookieHeader = cookieHeader
                        } else {
                            delay(ANTI_BOT_COOKIE_POLL_INTERVAL_MS)
                        }
                    }
                }

                if (resolvedCookieHeader.isNullOrBlank()) {
                    Napier.w(
                        message = "Auto anti-bot resolve timeout for $requestUrl (${challenge.protection}). Starting manual fallback.",
                        tag = LOG_TAG,
                    )
                    webView.stopLoading()
                    resolvedCookieHeader = runManualFallback(
                        nsUrl = nsUrl,
                        userAgent = userAgent,
                        expectedCookies = expectedCookies,
                    )
                    if (resolvedCookieHeader.isNullOrBlank()) {
                        Napier.w(
                            message = "Manual anti-bot fallback did not resolve challenge for $requestUrl",
                            tag = LOG_TAG,
                        )
                        return@withContext false
                    }
                }
                val cookieHeader = resolvedCookieHeader ?: return@withContext false

                val persistedCount = persistCookieHeader(
                    requestUrl = targetUrl,
                    cookieHeader = cookieHeader,
                    cookiesStorage = cookiesStorage,
                    logTag = LOG_TAG,
                )
                if (persistedCount <= 0) {
                    Napier.w(
                        message = "Challenge cookie was detected but could not be persisted for $requestUrl",
                        tag = LOG_TAG,
                    )
                    return@withContext false
                }

                Napier.i(
                    message = "Anti-bot challenge resolved via WKWebView for $requestUrl (cookies=$persistedCount)",
                    tag = LOG_TAG,
                )
                true
            } catch (error: Throwable) {
                Napier.w(
                    message = "WKWebView anti-bot resolver failed for $requestUrl: ${error.message.orEmpty()}",
                    tag = LOG_TAG,
                )
                false
            } finally {
                webView.stopLoading()
                webView.navigationDelegate = null
                webView.removeFromSuperview()
            }
        }
    }

    private fun readCookieHeader(url: NSURL): String {
        val cookies = NSHTTPCookieStorage.sharedHTTPCookieStorage.cookiesForURL(url).orEmpty()
            .mapNotNull { item -> item as? NSHTTPCookie }
            .mapNotNull { cookie ->
                val name = cookie.name.trim()
                if (name.isBlank()) return@mapNotNull null
                "$name=${cookie.value}"
            }
        return cookies.joinToString("; ")
    }

    private suspend fun runManualFallback(
        nsUrl: NSURL,
        userAgent: String,
        expectedCookies: Set<String>,
    ): String? {
        val presenter = topViewController() ?: return null
        val manualController = UIViewController()
        val manualWebView = WKWebView(
            frame = presenter.view.bounds,
            configuration = WKWebViewConfiguration(),
        ).apply {
            autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
            if (userAgent.isNotBlank()) {
                customUserAgent = userAgent
            }
        }
        manualController.view.addSubview(manualWebView)
        presenter.presentViewController(
            viewControllerToPresent = manualController,
            animated = true,
            completion = null,
        )
        manualWebView.loadRequest(NSURLRequest.requestWithURL(nsUrl))

        var resolvedCookieHeader: String? = null
        withTimeoutOrNull(ANTI_BOT_MANUAL_TIMEOUT_MS) {
            while (resolvedCookieHeader.isNullOrBlank()) {
                val cookieHeader = readCookieHeader(nsUrl)
                if (hasExpectedCookie(cookieHeader, expectedCookies)) {
                    resolvedCookieHeader = cookieHeader
                } else {
                    delay(ANTI_BOT_COOKIE_POLL_INTERVAL_MS)
                }
            }
        }

        manualWebView.stopLoading()
        manualController.dismissViewControllerAnimated(
            flag = true,
            completion = null,
        )
        return resolvedCookieHeader
    }

    private fun topViewController(): UIViewController? {
        var current = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
        while (current.presentedViewController != null) {
            current = current.presentedViewController!!
        }
        return current
    }
}

private const val LOG_TAG = "IosAntiBotResolver"
private const val ANTI_BOT_MANUAL_TIMEOUT_MS = 60_000L
