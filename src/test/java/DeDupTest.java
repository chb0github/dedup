import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.bongiorno.misc.collections.ImprovedList;
import org.bongiorno.misc.utils.OtherUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DeDupTest {


    private String copy(InputStream in, int count, String prefix, List<File> capture) throws IOException, NoSuchAlgorithmException {


        MessageDigest digest = MessageDigest.getInstance("MD5");
        in.mark(1024*1024);
        IOUtils.copy(in, new DigestOutputStream(new NullOutputStream(), digest));
        in.reset();
        String result = OtherUtils.hexFormat(digest.digest());
        for (int i = 0; i < count; i++) {
            File temp = File.createTempFile(prefix, null);
            FileOutputStream out = new FileOutputStream(temp);

            in.mark(1024*1024);
            IOUtils.copy(in, out);
            in.reset();

            capture.add(temp);
            out.flush();
            out.close();
        }
        return result;
    }

    @Test
    public void testMain() throws Exception {
        Class<? extends DeDupTest> aClass = this.getClass();
        List<File> input = new LinkedList<>();

        String hDesert  = copy(aClass.getResourceAsStream("/original/Desert.jpg"), 3, "Desert", input);
        String hJFish = copy(aClass.getResourceAsStream("/original/Jellyfish.jpg"), 5, "Jellyfish", input);
        String hFlower = copy(aClass.getResourceAsStream("/original/Chrysanthemum.jpg"), 10, "Chrysanthemum", input);

        File koala = new File(aClass.getResource("/original/Koala.jpg").getFile());

        input.add(koala);
        // 19 total files
        assertEquals(19, input.size());

        long now = System.currentTimeMillis();
        DeDup dedup = new DeDup(input, "MD5", Arrays.asList(".gif", ".jpg"));
        Map<Boolean, ImprovedList<File>> results = dedup.execute();
        System.out.println(System.currentTimeMillis() - now);

        ImprovedList<File> errors = results.get(Boolean.FALSE);
        assertNull(errors);

        ImprovedList<File> deleted = results.get(Boolean.TRUE);
        // 1 is spared in every duplicate set of files. 4 different files 19 -4
        assertEquals(15, deleted.size());


        Map<String, ImprovedList<File>> hashes = dedup.getHashes();

        ImprovedList<File> desert = hashes.get(hDesert);
        assertEquals(3,desert.size());
        assertFalse(deleted.containsAll(desert));

        // assert list contains 2 deserts deleted
        desert = desert.subList(1);
        assertTrue(deleted.containsAll(desert));

        ImprovedList<File> jelly = hashes.get(hJFish);
        assertEquals(5,jelly.size());
        assertFalse(deleted.containsAll(jelly));

        // assert list contains 4 fish deleted
        jelly = jelly.subList(1);
        assertTrue(deleted.containsAll(jelly));


        ImprovedList<File> flowers = hashes.get(hFlower);
        assertEquals(10,flowers.size());
        assertFalse(deleted.containsAll(flowers));

        // assert list contains 9 flowers deleted
        flowers = flowers.subList(1);
        assertTrue(deleted.containsAll(flowers));

        // assert no koalas
        assertFalse(deleted.contains(koala));


    }
}