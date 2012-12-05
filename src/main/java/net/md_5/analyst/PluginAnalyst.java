package net.md_5.analyst;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class PluginAnalyst {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final ExecutorService pool = Executors.newFixedThreadPool(12);
    private static final File dlDir = new File("temp");
    private static final File dumpDir = new File("dump");
    private static final File outDir = new File("out");
    private static final Map<Integer, String> javaVersion = new HashMap<Integer, String>() {
        {
            put(45, "JDK 1.1");
            put(46, "JDK 1.2");
            put(47, "JDK 1.3");
            put(48, "JDK 1.4");
            put(49, "JDK 1.5");
            put(50, "JDK 1.6");
            put(51, "JDK 1.7");
        }
    };
    /*========================================================================*/
    private static int dlCount;

    public static void main(String[] args) throws Exception {
        dlDir.mkdir();
        dumpDir.mkdir();
        outDir.mkdir();

        visitDirectory(dumpDir);

        // doDownload();
    }

    private static void visitDirectory(File dir) throws IOException {
        String filter = "org/bukkit/(?!craftbukkit).*";
        Map<Ownable, Integer> tally = new HashMap<>();

        for (File file : dir.listFiles()) {
            JarInspector inspection = visitFile(file);

            for (Ownable o : inspection.referenceData.methods.keySet()) {
                ReferenceData.add(tally, o);
            }
        }

        // Precheck all classes and climb the tree
        Iterator<Map.Entry<Ownable, Integer>> iter = tally.entrySet().iterator();
        tally = new HashMap<>();
        while (iter.hasNext()) {
            Map.Entry<Ownable, Integer> o = iter.next();
            if (!o.getKey().owner.matches(filter)) {
                continue;
            }
            Ownable declarer = findDeclarer(o.getKey());

            if (declarer != null) {
                System.out.println("Remapping: " + o.getKey() + " to " + declarer);
            }
            ReferenceData.add(tally, declarer == null ? o.getKey() : declarer, o.getValue());
        }


        tally = MapSorter.valueSortedMap(tally);

        try (PrintWriter fr = new PrintWriter("methods.log")) {
            for (Map.Entry<Ownable, Integer> e : tally.entrySet()) {
                if (e.getKey().toString().matches(filter)) {
                    fr.println(e.getValue() + " " + e.getKey());
                }
            }
        }
    }

    private static Ownable findDeclarer(Ownable o) {
        Ownable mapped;
        try {
            ClassReader cr = new ClassReader(o.owner); // Read the class holding this ownable
            ClassNode node = new ClassNode();
            cr.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            if (node.methods != null) {
                for (MethodNode m : (List<MethodNode>) node.methods) {
                    if (m.name.equals(o.name) && m.desc.equals(o.descriptor)) {
                        return new Ownable(node.name, m.name, m.desc);
                    }
                }
            }

            if (node.interfaces != null) {
                for (String iface : (List<String>) node.interfaces) {
                    mapped = findDeclarer(new Ownable(iface, o.name, o.descriptor));
                    if (mapped != null) {
                        return mapped;
                    }
                }
            }
            if (node.superName != null) {
                return findDeclarer(new Ownable(node.superName, o.name, o.descriptor));
            }
        } catch (IOException ex) {
        }
        return null;
    }

    private static JarInspector visitFile(File file) throws IOException {
        JarInspector inspector = new JarInspector();

        try (FileInputStream in = new FileInputStream(file)) {
            Map<String, byte[]> entries = getAllEntries(in);
            for (Map.Entry<String, byte[]> clazz : entries.entrySet()) {
                try {
                    ClassReader cr = new ClassReader(clazz.getValue());
                    cr.accept(inspector, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                } catch (Exception ex) {
                    System.err.println("Warning: " + clazz.getKey() + " may be corrupted!");
                }
            }
            // ease some gc pressure
            entries.clear();
        }

        // try (FileWriter statWriter = new FileWriter(new File(outDir, file.getName()))) {
        //     gson.toJson(comparer.referenceData, statWriter);
        // }

        return inspector;
    }

    private static Map<String, byte[]> getAllEntries(InputStream in) throws IOException {
        ZipInputStream zip = new ZipInputStream(in);
        Map<String, byte[]> ret = new HashMap<>();

        while (true) {
            try {
                ZipEntry entry = zip.getNextEntry();
                if (entry == null) {
                    break;
                }

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int n;
                byte[] data = new byte[1 << 15]; // Max class file size
                while ((n = zip.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, n);
                }
                buffer.flush();

                // Zip-ception
                if (entry.getName().endsWith(".jar") || entry.getName().endsWith(".zip")) {
                    Map<String, byte[]> children = getAllEntries(new ByteArrayInputStream(buffer.toByteArray()));
                    ret.putAll(children);
                } else if (entry.getName().endsWith(".class")) {
                    ret.put(entry.getName(), buffer.toByteArray());
                }
            } catch (Exception ex) {
                if (ex.getMessage().equals("MALFORMED") || ex.getMessage().equals("encrypted ZIP entry not supported") || ex.getMessage().startsWith("invalid entry CRC")) {
                    System.out.println("Unpacking Error: " + ex.getMessage());
                    continue;
                } else {
                    throw new ZipException(ex.getMessage());
                }
            }
        }
        return ret;
    }

    /*========================================================================*/
    public static void doDownload() throws IOException {
        List<String> slugs = getAllSlugs();
        dlDir.mkdir();
        System.out.println("Found " + slugs.size() + " slugs");

        for (String slug : slugs) {
            pool.submit(new Dowloader(slug));
        }
    }

    public static List<String> getAllSlugs() throws IOException {
        return download("http://api.bukget.org/api/plugins", List.class);
    }

    public static Plugin getPlugin(String slug) throws IOException {
        return download("http://api.bukget.org/api/plugin/" + slug, Plugin.class);
    }

    public static <T> T download(String url, Type type) throws IOException {
        URLConnection con = new URL(url).openConnection();
        try (Reader rd = new InputStreamReader(con.getInputStream())) {
            return gson.fromJson(rd, type);
        }
    }

    public static class Dowloader implements Runnable {

        private final String slug;

        public Dowloader(String slug) {
            this.slug = slug;
        }

        @Override
        public void run() {
            try {
                Plugin plugin = getPlugin(slug);
                if (plugin.versions.size() <= 0) {
                    return;
                }

                Document dboPage = Jsoup.connect(plugin.versions.get(0).dl_link).timeout(15000).ignoreHttpErrors(true).get();
                Element el = dboPage.getElementsByClass("user-action-download").first();
                if (el == null) {
                    return;
                }

                String download = el.getElementsByTag("a").first().absUrl("href");
                File file = new File(dlDir, slug + ".dbo");

                URL website = new URL(download);
                try (ReadableByteChannel rbc = Channels.newChannel(website.openStream()); FileOutputStream fos = new FileOutputStream(file)) {
                    fos.getChannel().transferFrom(rbc, 0, 1 << 24);
                }
                System.out.println("Downloaded: " + file + " - #" + ++dlCount);
            } catch (Exception ex) {
                System.err.println("\t" + slug + " " + ex.getClass().getName() + ":" + ex.getMessage());
                run();
            }
        }
    }

    public static class Plugin {

        private List<Version> versions;

        public static class Version {

            private String dl_link;
        }
    }
}
