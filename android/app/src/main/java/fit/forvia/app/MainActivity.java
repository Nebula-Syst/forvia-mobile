package fit.forvia.app;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.CookieManager;
import com.getcapacitor.BridgeActivity;

// The loading-screen job used to live here too (an overlay on top of the WebView, animated,
// waiting for a signal from the page) — pulled after several attempts that never actually
// rendered right on a real device and couldn't be debugged further without one. That job now
// belongs entirely to the web app's own boot screen (Forvia's App.jsx/EntranceHeader), which
// already has a proven-working animation and already uses the app's real theme background —
// forvia-mobile's job is just to get out of its way: a black WebView background (no white flash
// before the page's own CSS loads) and a flushed cookie jar (below), nothing more.
public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getBridge().getWebView().setBackgroundColor(Color.BLACK);
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
}
