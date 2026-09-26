package fit.forvia.app;

import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

// The real launcher activity now (see AndroidManifest.xml) — plays the same "draw the mark,
// then resolve to solid" reveal as the web app's own boot screen (App.jsx's EntranceHeader,
// index.css's .logo-mark-svg.draw), natively, before the WebView (MainActivity) ever starts —
// this repo stays a thin wrapper (no bundled build, no offline data, per the README), so this
// is the one piece of real native code it owns: everything else still is the live site.
//
// SplashScreen.installSplashScreen() (androidx.core:core-splashscreen, already a dependency)
// matters on API 31+ specifically: without calling it, Android's OWN SplashScreen system takes
// the very first frame regardless of this Activity's theme, using its own default — the
// launcher icon boxed in a round/square mask on a plain grey background, not anything this repo
// controls (that's what a real device was showing before this — a genuine Android 12+ platform
// behaviour, not a leftover static asset). Calling it hands control of that first frame to
// values-v31/styles.xml's own windowSplashScreenAnimatedIcon/Background instead. On API <31 this
// call is a no-op shim; those OS versions never had this behaviour to begin with, so
// activity_splash.xml's own layout (below) is already the very first thing shown there.
//
// 2400ms below matches ic_forvia_logo_animated.xml's own total duration (its last path's
// fillAlpha animator ends at 1900+500ms) — change one, change the other.
public class SplashActivity extends AppCompatActivity {
    private static final long ANIMATION_DURATION_MS = 2400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        Drawable drawable = logo.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }

        new Handler(Looper.getMainLooper()).postDelayed(this::goToMain, ANIMATION_DURATION_MS);
    }

    private void goToMain() {
        if (isFinishing()) return;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
