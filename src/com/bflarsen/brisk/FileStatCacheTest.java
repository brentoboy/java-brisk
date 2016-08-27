package com.bflarsen.brisk;

import org.junit.After;
import org.junit.Before;

public class FileStatCacheTest extends junit.framework.TestCase {

    public FileStatCache Cache;

    @Before
    public void setUp() {
        Cache = new FileStatCache();
    }

    @After
    public void tearDown() {
        if (Cache != null) {
            // Cache.close();
            Cache = null;
        }
    }

    public void test_it_caches_stats() throws Exception {
        FileStatCache.FileStat originalPath = Cache.get("C:\\stuff");
        Thread.sleep(1); // give it some time to be a different reading
        FileStatCache.FileStat differentPath = Cache.get("c:\\code\\temp");
        FileStatCache.FileStat samePath = Cache.get("C:\\stuff");
        assertEquals(originalPath.whenLastChecked, samePath.whenLastChecked);
        assertNotSame(originalPath.whenLastChecked, differentPath.whenLastChecked);
    }

    public void test_streaming() throws Exception {
        Cache.streamTo("C:\\code\\storyable-things.txt", System.out);
        Cache.streamTo("C:\\code\\storyable-things.txt", System.out);
        Cache.discardCachedContent("C:\\code\\storyable-things.txt");
        Cache.streamTo("C:\\code\\storyable-things.txt", System.out);
    }
}
