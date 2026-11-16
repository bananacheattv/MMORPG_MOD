'use strict';
// Démarrage : npm start (lit .env). Écoute en local (127.0.0.1) ; l'exposer en HTTPS via un tunnel ou un proxy inverse.
const http = require('node:http');
const { loadConfig } = require('./config');
const { createApp } = require('./app');

let cfg;
try {
  cfg = loadConfig();
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
const app = createApp(cfg);
const server = http.createServer((req, res) => app.handle(req, res));
server.requestTimeout = 20000;
server.headersTimeout = 10000;
server.listen(cfg.port, cfg.host, () => {
  console.info(`Aéloria back-end sur http://${cfg.host}:${cfg.port} pour ${cfg.siteUrl}`);
  console.info(`Boutique : ${cfg.stripe ? (cfg.stripe.live ? 'PAIEMENTS RÉELS' : 'mode test Stripe') : 'fermée (pas de clé Stripe)'} · livraison : ${cfg.bridge ? cfg.bridge.url : 'désactivée'}`);
});
const timer = setInterval(() => app.deliverDue(), 30000);
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    clearInterval(timer);
    server.close(() => {
      app.close();
      process.exit(0);
    });
  });
}
