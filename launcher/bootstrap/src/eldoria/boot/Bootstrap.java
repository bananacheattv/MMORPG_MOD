package eldoria.boot;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

/**
 * Amorce du launcher Eldoria : telecharge le coeur du launcher (launcher-core.jar) publie a chaque push
 * sur GitHub, le met a jour si besoin puis le lance. L'amorce elle-meme ne change (presque) jamais.
 */
public final class Bootstrap {
    public static final int VERSION = 1;
    public static final String MANIFEST_URL =
            System.getProperty("eldoria.manifest", "https://github.com/bananacheattv/MMORPG_MOD/releases/download/latest/manifest.json");

    private Bootstrap() {}

    public static void main(String[] args) {
        try {
            run();
        } catch (Throwable t) {
            t.printStackTrace();
            if (!java.awt.GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(null, "Impossible de demarrer le launcher Eldoria :\n" + t,
                    "Eldoria", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    static Path home() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String appdata = System.getenv("APPDATA");
        if (os.contains("win") && appdata != null) return Path.of(appdata, ".eldoria");
        return Path.of(System.getProperty("user.home"), ".eldoria");
    }

    private static void run() throws Exception {
        Path home = home();
        Path dir = home.resolve("launcher");
        Files.createDirectories(dir);
        Path core = dir.resolve("launcher-core.jar");
        HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(10)).build();

        String manifest = null;
        try {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(MANIFEST_URL + "?t=" + System.currentTimeMillis()))
                    .timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (r.statusCode() == 200) manifest = r.body();
        } catch (Exception e) {
            System.err.println("Manifeste indisponible : " + e);
        }

        if (manifest != null) {
            Files.writeString(dir.resolve("manifest.json"), manifest);
            String url = field(manifest, "launcherCoreUrl");
            String sha = field(manifest, "launcherCoreSha256");
            if (url != null && sha != null && !(Files.exists(core) && sha.equalsIgnoreCase(sha256(core)))) {
                Path tmp = dir.resolve("launcher-core.jar.part");
                try (InputStream in = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
                        HttpResponse.BodyHandlers.ofInputStream()).body()) {
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                }
                if (!sha.equalsIgnoreCase(sha256(tmp))) throw new IllegalStateException("Somme de controle du launcher invalide");
                Files.move(tmp, core, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (!Files.exists(core)) throw new IllegalStateException("Pas de connexion et aucun launcher en cache.\n" + MANIFEST_URL);

        URLClassLoader cl = new URLClassLoader(new URL[]{core.toUri().toURL()}, Bootstrap.class.getClassLoader());
        Class<?> c = Class.forName("eldoria.launcher.Launcher", true, cl);
        Method m = c.getMethod("start", String.class, Path.class, int.class);
        m.invoke(null, manifest, home, VERSION);
    }

    private static String field(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    static String sha256(Path p) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(p)) {
            byte[] buf = new byte[1 << 16];
            for (int n; (n = in.read(buf)) > 0; ) md.update(buf, 0, n);
        }
        return HexFormat.of().formatHex(md.digest());
    }
}
