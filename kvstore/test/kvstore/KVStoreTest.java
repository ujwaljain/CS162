package kvstore;

import static autograder.TestUtils.kTimeoutDefault;
import static autograder.TestUtils.kTimeoutQuick;
import static kvstore.KVConstants.ERROR_NO_SUCH_KEY;
import static kvstore.KVConstants.RESP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import autograder.AGCategories.AGTestDetails;
import autograder.AGCategories.AG_PROJ3_CODE;

public class KVStoreTest {

    KVStore store;

    @Before
    public void setupStore() {
        store = new KVStore();
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Verify get returns value just put into store")
    public void putAndGetOneKey() throws KVException {
        String key = "this is the key.";
        String val = "this is the value.";
        store.put(key, val);
        assertEquals(val, store.get(key));
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Verify delete")
    public void verifyDelete() {
        String key = "this is the key";
        String val = "this is the value";
        try {
            store.put(key, val);
            store.del(key);
            val = store.get(key);
        } catch (KVException e) {
            assertEquals(e.getKVMessage().getMessage(), ERROR_NO_SUCH_KEY);
        }
    }

    @Test(timeout = kTimeoutDefault)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Verify dump and restore")
    public void verifyDumpAndRestore() {
        String key1 = "key1", key2 = "key2";
        String val1 = "val1", val2 = "val2";
        String filename = "temp.txt";

        store.put(key1, val1);
        store.put(key2, val2);
        store.dumpToFile(filename);
        store.restoreFromFile(filename);

        try {
            assertEquals(store.get(key1), val1);
            assertEquals(store.get(key2), val2);
        } catch (KVException e) {
            fail("Can't retrieve keys from restored store");
        } finally {
            File f = new File(filename);
            f.delete();
        }
    }
}
