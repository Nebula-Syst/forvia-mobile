package fit.forvia.app;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import com.getcapacitor.BridgeActivity;

// The WebView starts loading the live site the instant super.onCreate() below runs, but a cold
// process start (fresh WebView, JS parse, the network round trip for /api/me + state) can easily
// outlast SplashActivity's own brief animation — without this overlay, the gap between
// SplashActivity finishing and the page actually painting something showed a blank black WebView
// (its background, set below) with nothing on it for however long that takes. This overlays a
// plain static logo on solid black on top of the WebView (added after it in the view hierarchy,
// not replacing it — the page underneath keeps loading normally) until the web app itself calls
// back through AppReadyPlugin (window.Capacitor.Plugins.AppReady.ready(), fired from
// lib/mobile.js's notifyNativeReady() once real content — Home or Login — has actually rendered).
//
// A static PNG, not SplashActivity's animated vector — that same drawable never actually animated
// when added to a view this way (confirmed on a real device, cause not pinned down without one to
// debug on directly — see git log). A static bitmap has no start()/attach-timing/callback path to
// go wrong.
//
// Hides only once BOTH are true, so it never reads as a flash (MIN_DISPLAY_MS, in case the page
// is ready almost instantly) or traps someone forever (MAX_WAIT_MS forces pageReady too, in case
// the page genuinely never calls back — a crash, a network that never resolves).
public class MainActivity extends BridgeActivity {
    private static final long MIN_DISPLAY_MS = 900;
    private static final long MAX_WAIT_MS = 8000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private View overlay;
    private boolean minTimeDone = false;
    private boolean pageReady = false;
    private boolean hidden = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AppReadyPlugin.class);
        super.onCreate(savedInstanceState);
        getBridge().getWebView().setBackgroundColor(Color.BLACK);
        addLoadingOverlay();
        handler.postDelayed(() -> { minTimeDone = true; hideOverlay(); }, MIN_DISPLAY_MS);
        handler.postDelayed(() -> { pageReady = true; hideOverlay(); }, MAX_WAIT_MS);
    }

    private void addLoadingOverlay() {
        overlay = LayoutInflater.from(this).inflate(R.layout.overlay_app_loading, null);
        ViewGroup root = findViewById(android.R.id.content);
        root.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // Called (on the UI thread, via AppReadyPlugin) once the web app has real content on screen.
    void onWebAppReady() {
        pageReady = true;
        hideOverlay();
    }

    // The session cookie (forvia-core's gymsid) is a normal persistent cookie (Max-Age set), so
    // Android's CookieManager should write it to disk on its own — but that's a background,
    // debounced sync, and Android is free to kill this process outright (low memory, swiped from
    // recents) without ever giving it a graceful shutdown to finish that sync. Forcing a flush on
    // every pause closes that window: worst case one extra disk write, best case it's the reason
    // login didn't stick across a cold start.
    @Override
    public void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    private void hideOverlay() {
        if (hidden || overlay == null || !minTimeDone || !pageReady) return;
        hidden = true;
        handler.removeCallbacksAndMessages(null);
        final View toRemove = overlay;
        toRemove.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> ((ViewGroup) toRemove.getParent()).removeView(toRemove))
                .start();
    }
}
