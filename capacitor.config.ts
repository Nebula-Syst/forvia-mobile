import type { CapacitorConfig } from '@capacitor/cli';

// CAP_SERVER_URL lets a test build point at a different host, e.g.
// CAP_SERVER_URL=http://192.168.1.50:9027 to test against forvia-dev on the local
// network. Production (the default) points at the real deployed instance — same
// pattern as Nebula-Syst/nebula-mobile-client.
const serverUrl = process.env.CAP_SERVER_URL || 'https://app.forvia.fit';

const config: CapacitorConfig = {
  appId: 'fit.forvia.app',
  appName: 'Forvia',
  webDir: 'www',
  server: {
    url: serverUrl,
    cleartext: serverUrl.startsWith('http://'),
    allowNavigation: ['forvia.fit', '*.forvia.fit'],
  },
};

export default config;
