package eldoria.launcher;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.LongConsumer;

/** Acces reseau : JSON, formulaires et telechargements verifies (sha1 / sha256 / sha512). */
final class Net {
    static final HttpClient HTTP = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(20)).build();

    private Net() {}

    static String get(String url) throws Exception {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60))
                .header("User-Agent", "EldoriaLauncher").build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (r.statusCode() != 200) throw new IllegalStateException("HTTP " + r.statusCode() + " : " + url);
        return r.body();
    }

    static Map<String, Object> getJson(String url) throws Exception { return Json.obj(Json.parse(get(url))); }

    static Map<String, Object> getJson(String url, String bearer) throws Exception {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + bearer).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (r.statusCode() != 200) throw new IllegalStateException("HTTP " + r.statusCode() + " : " + url + "\n" + r.body());
        return Json.obj(Json.parse(r.body()));
    }

    /** POST ; renvoie [code, corps]. */
    static Object[] post(String url, String contentType, String body) throws Exception {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", contentType).header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Object[]{r.statusCode(), r.body()};
    }

    static Map<String, Object> postJson(String url, Object json) throws Exception {
        Object[] r = post(url, "application/json", Json.write(json));
        if ((int) r[0] != 200) throw new IllegalStateException("HTTP " + r[0] + " : " + url + "\n" + r[1]);
        return Json.obj(Json.parse((String) r[1]));
    }

    static String form(Map<String, String> m) {
        StringJoiner j = new StringJoiner("&");
        m.forEach((k, v) -> j.add(URLEncoder.encode(k, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8)));
        return j.toString();
    }

    static String hash(Path p, String algo) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algo);
        try (InputStream in = Files.newInputStream(p)) {
            byte[] buf = new byte[1 << 16];
            for (int n; (n = in.read(buf)) > 0; ) md.update(buf, 0, n);
        }
        return HexFormat.of().formatHex(md.digest());
    }

    /** Vrai si le fichier existe et correspond au hash donne (algo "SHA-1", "SHA-256"...), ou a la taille si pas de hash. */
    static boolean valid(Path p, String algo, String expected, long size) throws Exception {
        if (!Files.isRegularFile(p)) return false;
        if (expected != null && !expected.isEmpty()) return expected.equalsIgnoreCase(hash(p, algo));
        return size <= 0 || Files.size(p) == size;
    }

    /** Comme {@link #valid} mais sans re-hasher un fichier dont taille + date n'ont pas change depuis la derniere verification. */
    static boolean validCached(Path p, String algo, String expected, Map<String, Object> cache) throws Exception {
        if (!Files.isRegularFile(p)) return false;
        String key = p.getFileName().toString();
        String stamp = Files.size(p) + ":" + Files.getLastModifiedTime(p).toMillis() + ":" + expected;
        if (stamp.equals(cache.get(key))) return true;
        if (!valid(p, algo, expected, 0)) return false;
        cache.put(key, stamp);
        return true;
    }

    static void remember(Path p, String expected, Map<String, Object> cache) throws Exception {
        cache.put(p.getFileName().toString(), Files.size(p) + ":" + Files.getLastModifiedTime(p).toMillis() + ":" + expected);
    }

    /** Mesure le debit vers une URL (jusqu'a 6 Mo, 15 s max) : "X.X Mo/s, connexion Y ms" ou le message d'erreur. */
    static String speedTest(String url) {
        long t0 = System.nanoTime();
        try {
            HttpResponse<InputStream> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "EldoriaLauncher").header("Range", "bytes=0-6291455").build(), HttpResponse.BodyHandlers.ofInputStream());
            long first = -1, got = 0;
            try (InputStream in = r.body()) {
                byte[] buf = new byte[1 << 16];
                for (int n; (n = in.read(buf)) > 0; ) {
                    if (first < 0) first = System.nanoTime();
                    got += n;
                    if (System.nanoTime() - t0 > 12_000_000_000L) break;
                }
            }
            if (first < 0) return "HTTP " + r.statusCode() + ", aucune donnee";
            double sec = Math.max(0.05, (System.nanoTime() - first) / 1e9);
            return String.format("%.1f Mo/s (%d Ko en %.1f s), reponse en %d ms, HTTP %d", got / sec / 1048576, got >> 10, sec,
                    (first - t0) / 1_000_000, r.statusCode());
        } catch (Exception e) {
            return "echec : " + e;
        }
    }

    static void download(String url, Path target, String algo, String expected, LongConsumer progress) throws Exception {
        Exception last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                downloadOnce(url, target, algo, expected, progress);
                return;
            } catch (Exception e) {
                last = e;
                Thread.sleep(1000L << attempt);
            }
        }
        throw last;
    }

    private static void downloadOnce(String url, Path target, String algo, String expected, LongConsumer progress) throws Exception {
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".part");
        if (!Files.exists(tmp) && parallel(url, tmp, progress)) {
            verifyAndMove(tmp, target, algo, expected);
            return;
        }
        long have = Files.isRegularFile(tmp) ? Files.size(tmp) : 0;
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(5)).header("User-Agent", "EldoriaLauncher");
        if (have > 0) rb.header("Range", "bytes=" + have + "-");
        HttpResponse<InputStream> r = HTTP.send(rb.build(), HttpResponse.BodyHandlers.ofInputStream());
        boolean resumed = r.statusCode() == 206 && have > 0;
        if (r.statusCode() != 200 && !resumed) {
            r.body().close();
            if (r.statusCode() == 416) Files.deleteIfExists(tmp);
            throw new IllegalStateException("HTTP " + r.statusCode() + " : " + url);
        }
        try (InputStream in = r.body(); var out = resumed
                ? Files.newOutputStream(tmp, java.nio.file.StandardOpenOption.APPEND) : Files.newOutputStream(tmp)) {
            byte[] buf = new byte[1 << 16];
            long done = resumed ? have : 0;
            for (int n; (n = in.read(buf)) > 0; ) {
                out.write(buf, 0, n);
                done += n;
                if (progress != null) progress.accept(done);
            }
        }
        verifyAndMove(tmp, target, algo, expected);
    }

    private static void verifyAndMove(Path tmp, Path target, String algo, String expected) throws Exception {
        if (expected != null && !expected.isEmpty() && !expected.equalsIgnoreCase(hash(tmp, algo))) {
            Files.deleteIfExists(tmp);
            throw new IllegalStateException("Fichier corrompu : " + target.getFileName());
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Gros fichier : 6 connexions en parallele (Range). Beaucoup de reseaux limitent le debit par connexion. Faux si non supporte. */
    private static boolean parallel(String url, Path tmp, LongConsumer progress) throws Exception {
        HttpResponse<InputStream> probe = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("User-Agent", "EldoriaLauncher").header("Range", "bytes=0-0").build(), HttpResponse.BodyHandlers.ofInputStream());
        probe.body().close();
        String cr = probe.headers().firstValue("Content-Range").orElse("");
        if (probe.statusCode() != 206 || !cr.contains("/")) return false;
        long total = Long.parseLong(cr.substring(cr.indexOf('/') + 1).trim());
        if (total < (8 << 20)) return false;
        int n = 6;
        long chunk = (total + n - 1) / n;
        java.util.concurrent.atomic.AtomicLong done = new java.util.concurrent.atomic.AtomicLong();
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(tmp.toFile(), "rw")) {
            raf.setLength(total);
        }
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(n);
        try {
            List<java.util.concurrent.Future<?>> fs = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) {
                long from = i * chunk, to = Math.min(total, from + chunk) - 1;
                if (from > to) break;
                fs.add(pool.submit(() -> {
                    for (int attempt = 0; ; attempt++) {
                        long pos = from, wrote = 0;
                        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(tmp.toFile(), "rw")) {
                            HttpResponse<InputStream> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(10))
                                    .header("User-Agent", "EldoriaLauncher").header("Range", "bytes=" + from + "-" + to).build(),
                                    HttpResponse.BodyHandlers.ofInputStream());
                            if (r.statusCode() != 206) throw new IllegalStateException("HTTP " + r.statusCode());
                            try (InputStream in = r.body()) {
                                raf.seek(pos);
                                byte[] buf = new byte[1 << 16];
                                for (int k; (k = in.read(buf)) > 0; ) {
                                    raf.write(buf, 0, k);
                                    wrote += k;
                                    long d = done.addAndGet(k);
                                    if (progress != null) progress.accept(d);
                                }
                            }
                            return null;
                        } catch (Exception e) {
                            if (attempt >= 3) throw e;
                            done.addAndGet(-wrote);        // on recommence ce morceau : la progression ne compte pas deux fois ces octets
                            Thread.sleep(1000L << attempt);
                        }
                    }
                }));
            }
            for (var f : fs) f.get();
        } catch (Exception e) {
            Files.deleteIfExists(tmp);
            throw e;
        } finally {
            pool.shutdownNow();
        }
        return true;
    }
}
