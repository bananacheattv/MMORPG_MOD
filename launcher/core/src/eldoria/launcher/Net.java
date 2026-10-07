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
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.LongConsumer;

/** Acces reseau : JSON, formulaires et telechargements verifies (sha1 / sha256 / sha512). */
final class Net {
    static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS)
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
        HttpResponse<InputStream> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(5))
                .header("User-Agent", "EldoriaLauncher").build(), HttpResponse.BodyHandlers.ofInputStream());
        if (r.statusCode() != 200) {
            r.body().close();
            throw new IllegalStateException("HTTP " + r.statusCode() + " : " + url);
        }
        try (InputStream in = r.body(); var out = Files.newOutputStream(tmp)) {
            byte[] buf = new byte[1 << 16];
            long done = 0;
            for (int n; (n = in.read(buf)) > 0; ) {
                out.write(buf, 0, n);
                done += n;
                if (progress != null) progress.accept(done);
            }
        }
        if (expected != null && !expected.isEmpty() && !expected.equalsIgnoreCase(hash(tmp, algo))) {
            Files.deleteIfExists(tmp);
            throw new IllegalStateException("Fichier corrompu : " + target.getFileName());
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
