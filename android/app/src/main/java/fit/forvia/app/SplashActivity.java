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

// The real launcher activity now (see AndroidManifest.xml) — plays the same "draw the mark,
// then resolve to solid" reveal as the web app's own boot screen (App.jsx's EntranceHeader,
// index.css's .logo-mark-svg.draw), natively, before the WebView (MainActivity) ever starts —
// this repo stays a thin wrapper (no bundled build, no offline data, per the README), so this
// is the one piece of real native code it owns: everything else still is the live site.
//
// 2400ms below matches ic_forvia_logo_animated.xml's own total duration (its last path's
// fillAlpha animator ends at 1900+500ms) — change one, change the other.
public class SplashActivity extends AppCompatActivity {
    private static final long ANIMATION_DURATION_MS = 2400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
