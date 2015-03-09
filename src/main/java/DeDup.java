import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.output.NullOutputStream;
import org.bongiorno.misc.collections.ImprovedList;
import org.bongiorno.misc.utils.OtherUtils;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.bongiorno.misc.collections.ImprovedCollection.improve;


/**
 * @author chribong
 */
public class DeDup {

    @Option(name = "--root", aliases = "-r", usage = "Where to search from. Default is '.'")
    private ImprovedList<File> roots = ImprovedList.of(new File("."));

    @Option(name = "--type", aliases = "-t", usage = "File type to include in the hash. Default is '.jpg' and '.gif'")
    private ImprovedList<String> fileTypes = ImprovedList.of(".jpg", ".gif");

    @Option(name = "--algo", aliases = "-a", usage = "The algorithm to hash with. Default is MD5")
    private String hashAlgo = "MD5";

    private Map<String, ImprovedList<File>> hashes;
    private Map<Long, ImprovedList<File>> sizes;
    private Map<Boolean, ImprovedList<File>>  deleted;

    public static void main(String[] args) throws Exception {

        DeDup app = new DeDup();
        CmdLineParser cmdLineParser = new CmdLineParser(app);
        cmdLineParser.parseArgument(args);
//        System.out.println();
        app.execute();
        System.out.println("Files found to process: " + app.hashes.values().parallelStream().mapToInt(List::size).sum());
        System.out.println("Files deleted: " + app.deleted.getOrDefault(Boolean.TRUE, new ImprovedList<>()).size());

    }

    public DeDup() {
    }

    public DeDup(List<File> roots, String hashAlgo, List<String> fileTypes) {
        this.roots = new ImprovedList<>(roots);
        this.hashAlgo = hashAlgo;
        this.fileTypes = new ImprovedList<>(fileTypes);
    }

    public Map<Boolean, ImprovedList<File>> execute() throws Exception {


        FileFilter filter = new OrFileFilter(Arrays.asList(DirectoryFileFilter.INSTANCE, new SuffixFileFilter(fileTypes)));

        ImprovedList<File> files = new ImprovedList<>(new LinkedList<>());
        roots.forEach(f -> getFiles(f, filter, files));
        sizes = files.filter(File::isFile).groupingBy(File::length);
        // we could say don't hash if there is only 1 file, but that would make debugging harder as it wouldn't show up here
        hashes = improve(sizes.values()).flatMap(Collection::parallelStream).groupingBy(this::hash);


        this.deleted = improve(hashes.values()).map(l -> l.subList(1)).flatMap(Collection::parallelStream).groupingBy(this::delete);


        return this.deleted;
    }


    private boolean delete(File f) {
        try {
            return Files.deleteIfExists(f.toPath());
        } catch (IOException e) {
            System.err.println(e.toString());
            return false;
        }

    }

    protected static Collection<File> getFiles(File start, FileFilter filter, Collection<File> results) {
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

    private String hash(File f) throws RuntimeException {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(hashAlgo);
            FileInputStream input = new FileInputStream(f);
            IOUtils.copy(input, new DigestOutputStream(new NullOutputStream(), digest));
            input.close();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
        return OtherUtils.hexFormat(digest.digest());
    }

    public Map<Long, ImprovedList<File>> getSizes() {
        return sizes;
    }

    public List<File> getRoots() {
        return roots;
    }

    public Map<String, ImprovedList<File>> getHashes() {
        return hashes;
    }

    public String getHashAlgo() {
        return hashAlgo;
    }

    public List<String> getFileTypes() {
        return fileTypes;
    }

}
