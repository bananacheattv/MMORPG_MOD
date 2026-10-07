package eldoria.launcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Installation et lancement direct de Minecraft + NeoForge (sans le launcher officiel) :
 * Java de Mojang, client vanilla, NeoForge, bibliotheques, ressources, puis ligne de commande.
 */
final class Game {
    private static final String VERSIONS = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String RUNTIMES =
            "https://piston-meta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";

    /** Progression : (texte, pour-mille ou -1). */
    interface Progress { void update(String text, int permille); }

    private final Path dir;        // donnees partagees : versions, libraries, assets, runtime
    private final Path gameDir;    // instance : mods, config, saves
    private final Consumer<String> log;
    private final Progress progress;
    private Map<String, Object> version;
    private Path java;

    Game(Path dir, Path gameDir, Consumer<String> log, Progress progress) {
        this.dir = dir;
        this.gameDir = gameDir;
        this.log = log;
        this.progress = progress;
    }

    // ------------------------------------------------------------------ OS

    static String osName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "windows" : os.contains("mac") ? "osx" : "linux";
    }

    static boolean arm() { return System.getProperty("os.arch", "").toLowerCase().matches("aarch64|arm64"); }

    private static String runtimePlatform() {
        return switch (osName()) {
            case "windows" -> arm() ? "windows-arm64" : "windows-x64";
            case "osx" -> arm() ? "mac-os-arm64" : "mac-os";
            default -> "linux";
        };
    }

    static boolean rulesAllow(Object rules, Map<String, Boolean> features) {
        if (rules == null) return true;
        boolean allow = false;
        for (Object o : Json.arr(rules)) {
            Map<String, Object> rule = Json.obj(o);
            boolean match = true;
            Map<String, Object> os = Json.obj(rule.get("os"));
            if (os.containsKey("name") && !osName().equals(os.get("name"))) match = false;
            if (os.containsKey("arch")) {
                String a = System.getProperty("os.arch", "");
                boolean x86 = a.equals("x86") || a.equals("i386");
                if ("x86".equals(os.get("arch")) != x86) match = false;
            }
            Map<String, Object> range = Json.obj(os.get("versionRange"));
            if (!range.isEmpty()) {
                double v = osVersion();
                if (range.containsKey("min") && v < versionNum(Json.str(range.get("min"), "0"))) match = false;
                if (range.containsKey("max") && v >= versionNum(Json.str(range.get("max"), "0"))) match = false;
            }
            for (Map.Entry<String, Object> f : Json.obj(rule.get("features")).entrySet())
                if (!f.getValue().equals(features.getOrDefault(f.getKey(), false))) match = false;
            if (match) allow = "allow".equals(rule.get("action"));
        }
        return allow;
    }

    private static double osVersion() { return versionNum(System.getProperty("os.version", "0")); }

    /** "10.0.17134" -> 10.000017134 (comparaison approximative mais monotone). */
    private static double versionNum(String s) {
        String[] p = s.split("\\.");
        double v = 0, scale = 1;
        for (String part : p) {
            try {
                v += Integer.parseInt(part.replaceAll("\\D.*", "")) * scale;
            } catch (NumberFormatException ignored) {}
            scale /= 100000;
        }
        return v;
    }

    // ------------------------------------------------------------------ installation

    void prepare(String mcVersion, String neoforge) throws Exception {
        Path vanillaJson = dir.resolve("versions").resolve(mcVersion).resolve(mcVersion + ".json");
        if (!Files.exists(vanillaJson)) {
            progress.update("Recherche de Minecraft " + mcVersion + "...", -1);
            String url = null;
            for (Object o : Json.arr(Net.getJson(VERSIONS).get("versions"))) {
                Map<String, Object> v = Json.obj(o);
                if (mcVersion.equals(v.get("id"))) url = Json.str(v.get("url"), null);
            }
            if (url == null) throw new IllegalStateException("Version Minecraft introuvable : " + mcVersion);
            Net.download(url, vanillaJson, "SHA-1", null, null);
        }
        Map<String, Object> vanilla = Json.obj(Json.parse(Files.readString(vanillaJson)));
        java = ensureRuntime(Json.obj(vanilla.get("javaVersion")));

        String id = "neoforge-" + neoforge;
        Path neoJson = dir.resolve("versions").resolve(id).resolve(id + ".json");
        if (!Files.exists(neoJson)) installNeoForge(neoforge);
        version = resolve(id);

        downloadLibraries();
        downloadClientAndAssets();
    }

    private Path ensureRuntime(Map<String, Object> jv) throws Exception {
        String component = Json.str(jv.get("component"), "java-runtime-delta");
        Path root = dir.resolve("runtime").resolve(component);
        Path exe = osName().equals("osx") ? root.resolve("jre.bundle/Contents/Home/bin/java")
                : root.resolve("bin").resolve(osName().equals("windows") ? "javaw.exe" : "java");
        Path marker = root.resolve(".eldoria-ok");
        if (Files.exists(marker) && Files.exists(exe)) return exe;

        progress.update("Telechargement de Java (" + component + ")...", -1);
        Map<String, Object> all = Net.getJson(RUNTIMES);
        List<Object> entries = Json.arr(Json.obj(all.get(runtimePlatform())).get(component));
        if (entries.isEmpty()) throw new IllegalStateException("Java " + component + " indisponible pour " + runtimePlatform());
        String url = Json.str(Json.obj(Json.obj(entries.get(0)).get("manifest")).get("url"), "");
        Map<String, Object> files = Json.obj(Net.getJson(url).get("files"));
        List<Runnable> jobs = new ArrayList<>();
        List<Path> executables = new ArrayList<>();
        for (Map.Entry<String, Object> e : files.entrySet()) {
            Map<String, Object> f = Json.obj(e.getValue());
            Path p = root.resolve(e.getKey());
            switch (Json.str(f.get("type"), "")) {
                case "directory" -> Files.createDirectories(p);
                case "file" -> {
                    Map<String, Object> raw = Json.obj(Json.obj(f.get("downloads")).get("raw"));
                    String sha1 = Json.str(raw.get("sha1"), null);
                    if (Boolean.TRUE.equals(f.get("executable"))) executables.add(p);
                    jobs.add(() -> fetch(Json.str(raw.get("url"), ""), p, sha1));
                }
                case "link" -> jobs.add(() -> {
                    try {
                        Files.createDirectories(p.getParent());
                        Files.deleteIfExists(p);
                        Files.createSymbolicLink(p, Path.of(Json.str(f.get("target"), "")));
                    } catch (Exception ignored) {}
                });
                default -> {}
            }
        }
        runAll(jobs, "Java");
        for (Path p : executables) p.toFile().setExecutable(true);
        Files.writeString(marker, "ok");
        return exe;
    }

    private void installNeoForge(String neo) throws Exception {
        Files.createDirectories(dir);
        Path profiles = dir.resolve("launcher_profiles.json");
        if (!Files.exists(profiles)) Files.writeString(profiles, "{\n  \"profiles\" : {}\n}");
        Path inst = dir.resolve("neoforge-" + neo + "-installer.jar");
        log.accept("Telechargement de l'installeur NeoForge " + neo + "...");
        Net.download("https://maven.neoforged.net/releases/net/neoforged/neoforge/" + neo + "/neoforge-" + neo + "-installer.jar",
                inst, "SHA-1", null, null);
        progress.update("Installation de NeoForge (quelques minutes)...", -1);
        Path javaCli = java.resolveSibling(osName().equals("windows") ? "java.exe" : "java");
        Process p = new ProcessBuilder(javaCli.toString(), "-jar", inst.toString(), "--install-client", dir.toString())
                .directory(dir.toFile()).redirectErrorStream(true).start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) log.accept("  [neoforge] " + line);
        }
        if (p.waitFor() != 0) throw new IllegalStateException("Echec de l'installation de NeoForge");
        Files.deleteIfExists(inst);
        Files.deleteIfExists(dir.resolve(inst.getFileName() + ".log"));
    }

    /** Charge une version et fusionne ses parents (inheritsFrom). */
    private Map<String, Object> resolve(String id) throws Exception {
        Map<String, Object> v = Json.obj(Json.parse(Files.readString(dir.resolve("versions").resolve(id).resolve(id + ".json"))));
        String parent = Json.str(v.get("inheritsFrom"), null);
        if (parent == null) return v;
        Map<String, Object> merged = new LinkedHashMap<>(resolve(parent));
        List<Object> libs = new ArrayList<>(Json.arr(v.get("libraries")));
        libs.addAll(Json.arr(merged.get("libraries")));
        Map<String, Object> args = new LinkedHashMap<>();
        for (String k : new String[]{"game", "jvm"}) {
            List<Object> l = new ArrayList<>(Json.arr(Json.obj(merged.get("arguments")).get(k)));
            l.addAll(Json.arr(Json.obj(v.get("arguments")).get(k)));
            args.put(k, l);
        }
        args.put("default-user-jvm", Json.obj(merged.get("arguments")).get("default-user-jvm"));
        for (Map.Entry<String, Object> e : v.entrySet())
            if (!e.getKey().equals("inheritsFrom")) merged.put(e.getKey(), e.getValue());
        merged.put("libraries", libs);
        merged.put("arguments", args);
        merged.putIfAbsent("jarId", parent);
        return merged;
    }

    /** Bibliotheques actives, dedoublonnees (la version enfant gagne). */
    private List<Map<String, Object>> libraries() {
        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Object o : Json.arr(version.get("libraries"))) {
            Map<String, Object> lib = Json.obj(o);
            if (!rulesAllow(lib.get("rules"), Map.of())) continue;
            String[] n = Json.str(lib.get("name"), "").split(":");
            String key = n.length > 3 ? n[0] + ":" + n[1] + ":" + n[3] : n.length > 1 ? n[0] + ":" + n[1] : n[0];
            if (seen.add(key)) out.add(lib);
        }
        return out;
    }

    private Path libPath(Map<String, Object> lib) {
        Map<String, Object> art = Json.obj(Json.obj(lib.get("downloads")).get("artifact"));
        String path = Json.str(art.get("path"), null);
        if (path == null) {
            String[] n = Json.str(lib.get("name"), "").split(":");
            String file = n[1] + "-" + n[2] + (n.length > 3 ? "-" + n[3] : "") + ".jar";
            path = n[0].replace('.', '/') + "/" + n[1] + "/" + n[2] + "/" + file;
        }
        return dir.resolve("libraries").resolve(path);
    }

    private void downloadLibraries() throws Exception {
        List<Runnable> jobs = new ArrayList<>();
        for (Map<String, Object> lib : libraries()) {
            Map<String, Object> art = Json.obj(Json.obj(lib.get("downloads")).get("artifact"));
            String url = Json.str(art.get("url"), "");
            Path p = libPath(lib);
            if (url.isEmpty()) continue; // genere par l'installeur NeoForge
            long size = (long) Double.parseDouble(Json.str(art.get("size"), "0"));
            if (!Net.valid(p, "SHA-1", null, size)) jobs.add(() -> fetch(url, p, Json.str(art.get("sha1"), null)));
        }
        runAll(jobs, "bibliotheques");
    }

    private void downloadClientAndAssets() throws Exception {
        String jarId = Json.str(version.get("jarId"), Json.str(version.get("id"), ""));
        Map<String, Object> client = Json.obj(Json.obj(version.get("downloads")).get("client"));
        Path jar = dir.resolve("versions").resolve(jarId).resolve(jarId + ".jar");
        if (!client.isEmpty() && !Net.valid(jar, "SHA-1", null, (long) Double.parseDouble(Json.str(client.get("size"), "0")))) {
            progress.update("Telechargement du client Minecraft...", -1);
            Net.download(Json.str(client.get("url"), ""), jar, "SHA-1", Json.str(client.get("sha1"), null), null);
        }
        Map<String, Object> ai = Json.obj(version.get("assetIndex"));
        Path index = dir.resolve("assets/indexes").resolve(Json.str(ai.get("id"), "") + ".json");
        if (!Net.valid(index, "SHA-1", Json.str(ai.get("sha1"), null), 0))
            Net.download(Json.str(ai.get("url"), ""), index, "SHA-1", Json.str(ai.get("sha1"), null), null);
        Map<String, Object> objects = Json.obj(Json.obj(Json.parse(Files.readString(index))).get("objects"));
        List<Runnable> jobs = new ArrayList<>();
        Set<String> hashes = new LinkedHashSet<>();
        for (Object o : objects.values()) {
            Map<String, Object> obj = Json.obj(o);
            String h = Json.str(obj.get("hash"), "");
            long size = (long) Double.parseDouble(Json.str(obj.get("size"), "0"));
            Path p = dir.resolve("assets/objects").resolve(h.substring(0, 2)).resolve(h);
            if (hashes.add(h) && !(Files.isRegularFile(p) && Files.size(p) == size))
                jobs.add(() -> fetch("https://resources.download.minecraft.net/" + h.substring(0, 2) + "/" + h, p, h));
        }
        runAll(jobs, "ressources");
    }

    private static void fetch(String url, Path p, String sha1) {
        try {
            Net.download(url, p, "SHA-1", sha1, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void runAll(List<Runnable> jobs, String what) throws Exception {
        if (jobs.isEmpty()) return;
        log.accept("Telechargement : " + jobs.size() + " fichiers (" + what + ")");
        ExecutorService pool = Executors.newFixedThreadPool(8);
        AtomicInteger done = new AtomicInteger();
        try {
            List<Future<?>> fs = new ArrayList<>();
            for (Runnable r : jobs) fs.add(pool.submit(() -> {
                r.run();
                int d = done.incrementAndGet();
                progress.update("Telechargement des " + what + " (" + d + "/" + jobs.size() + ")", d * 1000 / jobs.size());
            }));
            for (Future<?> f : fs) f.get();
        } finally {
            pool.shutdownNow();
        }
    }

    // ------------------------------------------------------------------ lancement

    Process launch(MsAuth.Account acc, List<String> extraJvm) throws Exception {
        String sep = osName().equals("windows") ? ";" : ":";
        Path natives = dir.resolve("natives");
        Files.createDirectories(natives);
        Files.createDirectories(gameDir);
        List<String> cp = new ArrayList<>();
        for (Map<String, Object> lib : libraries()) {
            Path p = libPath(lib);
            if (Files.exists(p)) cp.add(p.toString());
        }
        String jarId = Json.str(version.get("jarId"), Json.str(version.get("id"), ""));
        cp.add(dir.resolve("versions").resolve(jarId).resolve(jarId + ".jar").toString());

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("auth_player_name", acc.name());
        vars.put("version_name", Json.str(version.get("id"), ""));
        vars.put("game_directory", gameDir.toString());
        vars.put("assets_root", dir.resolve("assets").toString());
        vars.put("assets_index_name", Json.str(Json.obj(version.get("assetIndex")).get("id"), ""));
        vars.put("auth_uuid", acc.uuid());
        vars.put("auth_access_token", acc.token());
        vars.put("clientid", "0");
        vars.put("auth_xuid", acc.xuid());
        vars.put("user_type", acc.type());
        vars.put("version_type", "Eldoria");
        vars.put("natives_directory", natives.toString());
        vars.put("launcher_name", "EldoriaLauncher");
        vars.put("launcher_version", "1");
        vars.put("classpath", String.join(sep, cp));
        vars.put("library_directory", dir.resolve("libraries").toString());
        vars.put("classpath_separator", sep);

        Map<String, Object> args = Json.obj(version.get("arguments"));
        List<String> cmd = new ArrayList<>();
        cmd.add(java.toString());
        addArgs(cmd, args.get("default-user-jvm"), vars);
        cmd.addAll(extraJvm);
        addArgs(cmd, args.get("jvm"), vars);
        cmd.add(Json.str(version.get("mainClass"), ""));
        addArgs(cmd, args.get("game"), vars);
        log.accept("Lancement de Minecraft (" + vars.get("version_name") + ") en tant que " + acc.name());
        return new ProcessBuilder(cmd).directory(gameDir.toFile()).redirectErrorStream(true).start();
    }

    private static void addArgs(List<String> cmd, Object list, Map<String, String> vars) {
        for (Object o : Json.arr(list)) {
            if (o instanceof String s) {
                cmd.add(sub(s, vars));
                continue;
            }
            Map<String, Object> a = Json.obj(o);
            if (!rulesAllow(a.get("rules"), Map.of())) continue;
            Object val = a.get("value");
            if (val instanceof String s) cmd.add(sub(s, vars));
            else for (Object x : Json.arr(val)) cmd.add(sub(String.valueOf(x), vars));
        }
    }

    private static String sub(String s, Map<String, String> vars) {
        for (Map.Entry<String, String> e : vars.entrySet()) s = s.replace("${" + e.getKey() + "}", e.getValue());
        return s;
    }
}
