import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by vlad on 22/05/16.
 */
public class TestFileUtil {

    String base;
    FileUtil fileUtil;




    @Before
    public void prepareTest(){
        base = System.getProperty("user.dir");
        fileUtil = new FileUtil(base + "/server");
    }

    @Test
    public void testIsPassValid() throws IOException, NoSuchAlgorithmException {
        File htaccess = new File(base + "/server/safe_dir/.htaccess");
        File someFile = new File(base + "/server/text_test.txt");
        String authHash = Base64.getEncoder().encodeToString("webuser1:password1".getBytes());

        assertTrue(fileUtil.isPassValid(authHash, htaccess));
        assertFalse(fileUtil.isPassValid(authHash, someFile));
    }

    @Test
    public void testGetHTACCESS(){
        File safeDir = new File(base + "/server/safe_dir");
        File root = new File(base + "/server/");
        File dir1 = new File(base + "/server/dir1");
        File textFile = new File(base + "/server/text_test.txt");

        File htaccess = fileUtil.getHTACCESS(safeDir);
        assertTrue(htaccess != null);

        assertTrue(fileUtil.getHTACCESS(root) == null);

        assertTrue(fileUtil.getHTACCESS(dir1) == null);

        assertTrue(fileUtil.getHTACCESS(textFile) == null);
    }


    @Test
    public void testCreateDirs(){
        String query = "/test/some/dir/whatever.txt";
        fileUtil.createFolders(query);
        File test = new File(base + "/server/test");
        File some = new File(base + "/server/test/some");
        File dir = new File(base + "/server/test/some/dir");

        assertTrue(test.exists());
        assertTrue(some.exists());
        assertTrue(dir.exists());

        test.delete();
        some.delete();
        dir.delete();
    }


    @Test
    public void testMD5Hash() throws NoSuchAlgorithmException {
        String storedPass = "7c6a180b36896a0a8c02787eeafb0e4c";
        String pass = fileUtil.md5Hash("password1");

        assertTrue(pass.equals(storedPass));
    }
}
