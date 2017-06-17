package org.bongiorno;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;


public class DeDup {

    private Collection<File> roots = new LinkedList<>();

    private Collection<String> fileTypes = new HashSet<>(Arrays.asList(".jpg", ".gif"));

    private String hashAlgo = "MD5";

    private Map<String, List<File>> hashes;
    private Map<Long, List<File>> sizes;
    private Map<Boolean, List<File>> deleted;

    private static final Map<String, Function<DeDup, Consumer<String>>> CMDS = new HashMap<>();

    private final Predicate<File> isChild = file -> file != null && (roots.contains(file) || this.isChild.test(file.getParentFile()));


    static {
        CMDS.put("--root", d -> f -> Optional.of(f).map(File::new).filter(d.isChild.negate()).ifPresent(d.roots::add));
        CMDS.put("--type", d -> type -> d.fileTypes.add(type));
        CMDS.put("--hash", d -> hash -> d.hashAlgo = hash);
        CMDS.put("--help", d -> d::exitHelp);
    }


    public static void main(String[] args) throws Exception {


        DeDup app = new DeDup();
        for(int i = 0; i < args.length;i++) {
            CMDS.computeIfAbsent(args[i++], s -> d -> string -> {}).apply(app).accept(args[i]);
        }

        app.execute();
        System.out.println("Files found to process: " + app.hashes.values().parallelStream().mapToInt(List::size).sum());
        System.out.println("Files deleted: " + app.deleted.getOrDefault(Boolean.TRUE, new LinkedList<>()).size());

    }

    public DeDup() {
    }


    public DeDup(Set<File> roots, String hashAlgo, Set<String> fileTypes) {
        this.roots = roots;
        this.hashAlgo = hashAlgo;
        this.fileTypes = fileTypes;
    }

    public Map<Boolean, List<File>> execute() throws Exception {


        FileFilter filter = (file) -> file.isDirectory() || fileTypes.stream().anyMatch(t -> file.getName().endsWith(t));

        List<File> files = new LinkedList<>();
        roots.forEach(f -> getFiles(f, filter, files));
        sizes = files.stream().filter(File::isFile).collect(groupingBy(File::length));
        // we could say don't hash if there is only 1 file, but that would make debugging harder as it wouldn't show up here
        hashes = sizes.values().stream().flatMap(Collection::parallelStream).collect(groupingBy(this::hash));


        this.deleted = hashes.values().stream().map(l -> l.subList(1, l.size()))
                .flatMap(Collection::parallelStream).collect(groupingBy(this::delete));


        return this.deleted;
    }

    private void exitHelp(String ignored) {
        System.out.format("usage: ");
        System.out.format("       DeDup --root /home/cbongiorno --hash MD5 --type .jpg --type .gif");
        System.out.format("--root <path> a path to the root directory to search. You may supply multiples. Redundant paths are removed%n");
        System.out.format("--hash the hash algorithm to apply%n");
        System.out.format("--type the file extension to look for. May supply more than one%n");
        System.exit(0);
    }

    private boolean delete(File f) {
        try {
            return Files.deleteIfExists(f.toPath());
        } catch (IOException e) {
            System.err.println(e.toString());
            return false;
        }

    }

    private static Collection<File> getFiles(File start, FileFilter filter, Collection<File> results) {
        if (start.isDirectory()) {
            File[] files = start.listFiles(filter);
            if (files != null) {
                for (File file : files) {
                    getFiles(file, filter, results);
                }
            }
        } else
            results.add(start);
        return results;
    }

    static String hash(InputStream input, String algo) {

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(algo);
            byte[] buffer = new byte[1024];
            for (long l = input.read(buffer); l > -1; l = input.read(buffer))
                digest.update(buffer, 0, (int) l);

            input.close();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }

        Formatter formatter = new Formatter();

        // Yup, there is a stream for all other types except byte[]

        for (byte b : digest.digest())
            formatter.format("%02x", b);

        return formatter.toString();
    }
    String hash(File f)  {
        try {
            return hash(new FileInputStream(f),this.hashAlgo);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Long, List<File>> getSizes() {
        return sizes;
    }

    public Collection<File> getRoots() {
        return roots;
    }

    public Map<String, List<File>> getHashes() {
        return hashes;
    }

    public String getHashAlgo() {
        return hashAlgo;
    }

    public Collection<String> getFileTypes() {
        return fileTypes;
    }

}
