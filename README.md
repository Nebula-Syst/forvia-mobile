# Forvia — mobile shell

A thin native wrapper (Capacitor) around [app.forvia.fit](https://app.forvia.fit). There is no
bundled build and no offline data — the app *is* the live site, loaded straight into a WebView,
so a Forvia release never has to wait on this repo and this repo never drifts from what the
website actually does.

`www/` holds only a placeholder page; `capacitor.config.ts`'s `server.url` overrides it at
runtime and points the WebView at the real deployment.

## Building

Every push to `main` builds a debug (unsigned) APK via GitHub Actions and publishes it as a
[Release](../../releases) — versioned `1.0.<run>`, newest first. That's what Forvia's own
Settings → Mobile app card links to.

To build locally:

```bash
npm install
npx cap sync android
npx cap open android   # opens Android Studio — Run on an emulator or device
```

Point a build at a different backend (e.g. `forvia-dev` on the local network) with:

```bash
CAP_SERVER_URL=http://192.168.1.x:9027 npx cap sync android
```

## iOS

Not set up yet — Apple doesn't allow installing outside the App Store without a paid developer
account, and Forvia doesn't publish there. Self-hosting instances remain reachable as a
full PWA from Safari in the meantime (add to home screen).

## License

AGPL-3.0-or-later, same as [Nebula-Syst/forvia](https://github.com/Nebula-Syst/forvia) — this
repo is just its native shell, no independent logic of its own.
