package fit.forvia.app;

import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.ImageView;
import com.getcapacitor.BridgeActivity;

// The WebView starts loading the live site the instant super.onCreate() below runs, but a cold
// process start (fresh WebView, JS parse, the network round trip for /api/me + state) can easily
// outlast SplashActivity's own brief animation — without this overlay, the gap between
// SplashActivity finishing and the page actually painting something would show whatever blank
// color the WebView defaults to. This overlays the *same* looping mark on solid black on top of
// the WebView (added after it in the view hierarchy, not replacing it — the page underneath keeps
// loading normally) until the web app itself calls back through AppReadyPlugin
// (window.Capacitor.Plugins.AppReady.ready(), fired from lib/mobile.js's notifyNativeReady()
// once real content — Home or Login — has actually rendered, whether or not the web app's own
// boot screen played first). Same drawable, same position as SplashActivity's screen, so the
// activity swap between them reads as one continuous animation, not a hard cut.
//
// Two floors, so it always reads as a deliberate loading screen rather than a flash: at least
// MIN_LOOPS full plays before it's ever allowed to hide, and — if the page genuinely never calls
// back (a crash, a network that never resolves) — a hard ceiling so a real failure doesn't trap
// someone behind a spinner that would otherwise never go away.
public class MainActivity extends BridgeActivity {
    private static final int MIN_LOOPS = 2;
    private static final long MAX_WAIT_MS = 8000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private View overlay;
    private AnimatedVectorDrawable overlayDrawable;
    private int loopsPlayed = 0;
    private boolean webReady = false;
    private boolean hidden = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AppReadyPlugin.class);
        super.onCreate(savedInstanceState);
        addLoadingOverlay();
        handler.postDelayed(this::hideOverlay, MAX_WAIT_MS);
    }

    private void addLoadingOverlay() {
        overlay = LayoutInflater.from(this).inflate(R.layout.overlay_app_loading, null);
        ViewGroup root = findViewById(android.R.id.content);
        root.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView logo = overlay.findViewById(R.id.loading_logo);
        Drawable drawable = logo.getDrawable();
        if (drawable instanceof AnimatedVectorDrawable) {
            overlayDrawable = (AnimatedVectorDrawable) drawable;
            overlayDrawable.registerAnimationCallback(new Animatable2.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable d) {
                    loopsPlayed++;
                    if (loopsPlayed >= MIN_LOOPS && webReady) hideOverlay();
                    else overlayDrawable.start();
                }
            });
            // Starting an AnimatedVectorDrawable synchronously in the same pass it's added to the
            // view tree (unlike SplashActivity's own identical drawable, started from a normal
            // setContentView — a real, established content view, not a view bolted on afterward)
            // can silently no-op before the view has actually been attached/measured. Reported
            // symptom this fixes: solid black overlay, logo never appears at all (fillAlpha's base
            // state is 0 = invisible), the animation simply never started rather than playing
            // badly. Calling start() both now and once more on the next frame (post) is belt and
            // braces — a second start() on an already-running drawable is a harmless no-op.
            overlayDrawable.start();
            overlay.post(overlayDrawable::start);
        }
    }

    // Called (on the UI thread, via AppReadyPlugin) once the web app has real content on screen.
    void onWebAppReady() {
        webReady = true;
        if (overlayDrawable == null || loopsPlayed >= MIN_LOOPS) hideOverlay();
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
        if (hidden || overlay == null) return;
        hidden = true;
        handler.removeCallbacksAndMessages(null);
        if (overlayDrawable != null) overlayDrawable.stop();
        final View toRemove = overlay;
        toRemove.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> ((ViewGroup) toRemove.getParent()).removeView(toRemove))
                .start();
    }
}
