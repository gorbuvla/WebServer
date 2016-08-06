import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Created by vlad on 22/05/16.
 */
public class CacheMap<V> {

    private static CacheMap<String> INSTANCE = null;

    public static CacheMap<String> getDirCache(){
        if(INSTANCE == null){
            INSTANCE = new CacheMap<>(32);
        }
        return INSTANCE;
    }

    private HashMap<String, SoftReference<V>> cacheMap;
    private Logger logger = Logger.getLogger("cache_map");


    private CacheMap(int size){
        cacheMap = new HashMap<>(size);
        logger.info("new cache initialized");
    }


    public void put(String key, V value){
        synchronized (cacheMap){
            SoftReference<V> softValue = new SoftReference<V>(value);
            cacheMap.put(key, softValue);
            logger.info("Put cache value under key: " + key);
        }
    }


    public V get(String key){
        synchronized (cacheMap){

            if(cacheMap.containsKey(key)){
                SoftReference<V> softValue = cacheMap.get(key);
                V value = softValue.get();
                if(value != null){
                    logger.info("Cached value retrieved under key: " + key);
                    return value;
                }
            }
        }
        return null;
    }

    public void remove(String key){
        synchronized (cacheMap){
            cacheMap.remove(key);
        }
    }


    public void clean(){
        synchronized (cacheMap){
            cacheMap.clear();
            logger.info("cache cleaned");
        }

    }



}
