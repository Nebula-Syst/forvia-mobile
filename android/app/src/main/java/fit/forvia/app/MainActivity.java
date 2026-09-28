package fit.forvia.app;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.webkit.CookieManager;
import android.widget.ImageView;
import com.getcapacitor.BridgeActivity;

// The WebView starts loading the live site the instant super.onCreate() below runs, but a cold
// process start (fresh WebView, JS parse, the network round trip for /api/me + state) can easily
// outlast SplashActivity's own brief animation — without this overlay, the gap between
// SplashActivity finishing and the page actually painting something showed a blank black WebView
// (its background, set below) with nothing on it for however long that takes. This overlays a
// static logo — revealed left-to-right, repeatedly, like it's being drawn in — on solid black on
// top of the WebView (added after it in the view hierarchy, not replacing it — the page underneath
// keeps loading normally) until the web app itself calls back through AppReadyPlugin
// (window.Capacitor.Plugins.AppReady.ready(), fired from lib/mobile.js's notifyNativeReady() once
// real content — Home or Login — has actually rendered).
//
// The reveal is a plain ValueAnimator driving View.setClipBounds — not an AnimatedVectorDrawable
// (SplashActivity's own approach for its real stroke-by-stroke draw-in): that exact class reliably
// failed to ever render when added to this same overlay across several attempts (confirmed on a
// real device, cause not pinned down without one to debug on directly — see git log).
// ValueAnimator+clipBounds is a completely different, much simpler code path (a plain Choreographer
// callback updating one property) with none of that machinery to go wrong the same way — it can't
// replicate the web's actual stroke-path reveal, but a growing reveal of the same mark reads as
// "drawing itself in" too.
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

        final ImageView logo = overlay.findViewById(R.id.loading_logo);
        // Needs the view's real laid-out size for the clip rect, which isn't known yet on this
        // same pass (just added to the tree) — a one-shot listener for the layout that follows.
        logo.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                logo.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                startDrawReveal(logo);
            }
        });
    }

    private void startDrawReveal(ImageView logo) {
        final int w = logo.getWidth();
        final int h = logo.getHeight();
        if (w <= 0 || h <= 0) return;
        ValueAnimator anim = ValueAnimator.ofInt(0, w);
        anim.setDuration(900);
        anim.setInterpolator(new LinearInterpolator());
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setStartDelay(150);
        anim.addUpdateListener(a -> logo.setClipBounds(new Rect(0, 0, (int) a.getAnimatedValue(), h)));
        anim.start();
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
