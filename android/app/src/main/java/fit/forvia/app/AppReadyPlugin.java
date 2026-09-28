package fit.forvia.app;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

// The web app's one-way signal that it has real content on screen (Home or Login — see
// forvia's lib/mobile.js notifyNativeReady(), the only caller of window.Capacitor.Plugins
// .AppReady.ready()). MainActivity uses this to know when it can stop covering the WebView
// with the looping loading overlay; see its own comment for the rest of that mechanism.
@CapacitorPlugin(name = "AppReady")
public class AppReadyPlugin extends Plugin {
    @PluginMethod
    public void ready(PluginCall call) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) activity.runOnUiThread(activity::onWebAppReady);
        call.resolve();
    }
}
