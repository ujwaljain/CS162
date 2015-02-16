package kvstore;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import kvstore.xml.KVCacheEntry;
import kvstore.xml.KVCacheType;
import kvstore.xml.KVSetType;
import kvstore.xml.ObjectFactory;


/**
 * A set-associate cache which has a fixed maximum number of sets (numSets).
 * Each set has a maximum number of elements (MAX_ELEMS_PER_SET).
 * If a set is full and another entry is added, an entry is dropped based on
 * the eviction policy.
 */
public class KVCache implements KeyValueInterface {
    private List<CacheSet> cache;
    private List<Lock> setLocks;
    private int numSets, maxElemsPerSet;
    /**
     * Constructs a second-chance-replacement cache.
     *
     * @param numSets the number of sets this cache will have
     * @param maxElemsPerSet the size of each set
     */
    @SuppressWarnings("unchecked")
    public KVCache(int numSets, int maxElemsPerSet) {
        this.numSets = numSets;
        this.maxElemsPerSet = maxElemsPerSet;
        cache = new ArrayList<CacheSet>(numSets);
        setLocks = new ArrayList<Lock>(numSets);
        for (int i = 0; i < numSets; i++) {
            CacheSet cacheSet = new CacheSet(maxElemsPerSet);
            cache.add(cacheSet);
            setLocks.add(new ReentrantLock());
        }
    }

    /**
     * Retrieves an entry from the cache.
     * Assumes access to the corresponding set has already been locked by the
     * caller of this method.
     *
     * @param  key the key whose associated value is to be returned.
     * @return the value associated to this key or null if no value is
     *         associated with this key in the cache
     */
    @Override
    public String get(String key) {
        int s = getCacheSetForKey(key);
        CacheEntry e = cache.get(s).getCacheEntryForKey(key);
        return (e != null) ? e.value : null;
    }

    /**
     * Adds an entry to this cache.
     * If an entry with the specified key already exists in the cache, it is
     * replaced by the new entry. When an entry is replaced, its reference bit
     * will be set to True. If the set is full, an entry is removed from
     * the cache based on the eviction policy. If the set is not full, the entry
     * will be inserted behind all existing entries. For this policy, we suggest
     * using a LinkedList over an array to keep track of entries in a set since
     * deleting an entry in an array will leave a gap in the array, likely not
     * at the end. More details and explanations in the spec. Assumes access to
     * the corresponding set has already been locked by the caller of this
     * method.
     *
     * @param key the key with which the specified value is to be associated
     * @param value a value to be associated with the specified key
     */
    @Override
    public void put(String key, String value) {
        CacheSet set = cache.get(getCacheSetForKey(key));
        set.putCacheEntryForKey(key, value);
    }

    /**
     * Removes an entry from this cache.
     * Assumes access to the corresponding set has already been locked by the
     * caller of this method. Does nothing if called on a key not in the cache.
     *
     * @param key key with which the specified value is to be associated
     */
    @Override
    public void del(String key) {
        CacheSet set = cache.get(getCacheSetForKey(key));
        set.deleteCacheEntryForKey(key);
    }

    /**
     * Get a lock for the set corresponding to a given key.
     * The lock should be used by the caller of the get/put/del methods
     * so that different sets can be #{modified|changed} in parallel.
     *
     * @param  key key to determine the lock to return
     * @return lock for the set that contains the key
     */

    public Lock getLock(String key) {
        int setIndex = getCacheSetForKey(key);
        return setLocks.get(setIndex);
    }

    /**
     * Get the size of a given set in the cache.
     * @param cacheSet Which set.
     * @return Size of the cache set.
     */
    int getCacheSetSize(int cacheSet) {
        return cache.get(cacheSet).getSetSize();
    }

    private void marshalTo(OutputStream os) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(KVCacheType.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty("com.sun.xml.internal.bind.xmlHeaders", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        marshaller.marshal(getXMLRoot(), os);
    }

    private JAXBElement<KVCacheType> getXMLRoot() throws JAXBException {
        ObjectFactory factory = new ObjectFactory();
        KVCacheType xmlCache = factory.createKVCacheType();
        for (int i = 0; i < numSets; i++) {
            KVSetType setType = new KVSetType();
            CacheSet cs = cache.get(i);
            for (int a = 0; a < getCacheSetSize(i); a++) {
                KVCacheEntry e = new KVCacheEntry();
                CacheEntry ce = cs.cacheSet.get(a);
                e.setKey(ce.key);
                e.setValue(ce.value);
                e.setIsReferenced(ce.isUsed ? "True" : "False");
                setType.getCacheEntry().add(e);
            }
            xmlCache.getSet().add(setType);
        }
        return factory.createKVCache(xmlCache);
    }

    /**
     * Serialize this store to XML. See spec for details on output format.
     */
    public String toXML() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            marshalTo(os);
        } catch (JAXBException e) {
            // ignore
        }
        return os.toString();
    }
    @Override
    public String toString() {
        return this.toXML();
    }

    // Utility methods
    private int getCacheSetForKey(String key) {
        int hash = Math.abs(key.hashCode());
        return hash % numSets;
    }

    private class CacheEntry {
        public String key;
        public String value;
        public boolean isUsed;

        public CacheEntry() {
            key = null; value = null; isUsed = false;
        }

        public CacheEntry(String key, String value) {
            this.key = key;
            this.value = value;
            this.isUsed = false;
        }
    }

    private class CacheSet {
        public List<CacheEntry> cacheSet;
        private int maxElemsPerSet;

        public CacheSet(int maxElemsPerSet)
        {
            cacheSet = new ArrayList<CacheEntry>(maxElemsPerSet);
            this.maxElemsPerSet = maxElemsPerSet;
        }

        private int getCacheEntryIndex(String key) {
            for (int i = 0; i < getSetSize(); i++) {
                String k = cacheSet.get(i).key;
                if (key.equals(k))
                    return i;
            }
            return -1;
        }

        public CacheEntry getCacheEntryForKey(String key) {
            int idx = getCacheEntryIndex(key);
            if (idx != -1) {
                // mark it used.
                CacheEntry ce = cacheSet.get(idx);
                ce.isUsed = true;
                return ce;
            }
            return null;
        }

        public void deleteCacheEntryForKey(String key) {
            int idx = getCacheEntryIndex(key);
            if (idx != -1) {
                cacheSet.remove(idx);
            }
        }

        public void putCacheEntryForKey(String key, String value) {
            // check if key already exists
            CacheEntry centry = getCacheEntryForKey(key);
            if (centry != null) {
                centry.value = value;
                return;
            }

            // if set not full, just add it at the back
            if (!isSetFull()) {
                CacheEntry e = new CacheEntry(key, value);
                cacheSet.add(e);
                return;
            }

            // cycle all cache entries for first non-used entry and
            // in the worst case you give everyone second chance and
            // choose the first element.
            for (int i = 0; i <= getSetSize(); i++) {
                // always get first entry, as we are gonna traverse
                // setSize times.
                CacheEntry ce = cacheSet.get(0);
                if (ce.isUsed) {
                    // give it second chance, and add it to the last.
                    ce.isUsed = false;
                    cacheSet.remove(0);
                    cacheSet.add(ce);
                } else {
                    // evict this entry.
                    cacheSet.remove(0);
                    ce = null;  // set null for gc
                    CacheEntry e = new CacheEntry(key, value);
                    cacheSet.add(e);
                    break;
                }
            }
            return;
        }

        public int getSetSize() {
            return cacheSet.size();
        }

        private boolean isSetFull() {
            return (getSetSize() == maxElemsPerSet);
        }

    }
}
