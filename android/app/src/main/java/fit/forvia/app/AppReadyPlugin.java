package fit.forvia.app;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

// The web app's one-way signal that it has real content on screen (Home or Login — see Forvia's
// lib/mobile.js notifyNativeReady(), the only caller of window.Capacitor.Plugins.AppReady.ready(),
// itself only wired up correctly via a JS-side registerPlugin('AppReady') call — a raw
// window.Capacitor.Plugins.AppReady lookup without that is always undefined, a past bug that made
// this a permanent no-op). MainActivity uses this to know when it can stop covering the WebView
// with the loading overlay.
@CapacitorPlugin(name = "AppReady")
public class AppReadyPlugin extends Plugin {
    @PluginMethod
    public void ready(PluginCall call) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) activity.runOnUiThread(activity::onWebAppReady);
        call.resolve();
    }
}
