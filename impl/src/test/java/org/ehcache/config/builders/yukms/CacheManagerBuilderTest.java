package org.ehcache.config.builders.yukms;

import java.util.UUID;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.CachePersistenceException;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.EhcacheManager;
import org.junit.Assert;
import org.junit.Test;

public class CacheManagerBuilderTest {
  @Test
  public void test_build() {
    CacheManagerBuilder.newCacheManagerBuilder().build(true);
  }

  @Test
  public void test_close() {
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
    cacheManager.close();
  }

  @Test
  public void test_destroy() throws CachePersistenceException {
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
    EhcacheManager ehcacheManager = (EhcacheManager) cacheManager;
    cacheManager.close();
    // yukms TODO: close后才可调用destroy
    ehcacheManager.destroy();
  }

  @Test
  public void test_destroyCache() throws CachePersistenceException {
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
    EhcacheManager ehcacheManager = (EhcacheManager) cacheManager;
    cacheManager.createCache("cache1",
      CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(2, MemoryUnit.MB)));
    ehcacheManager.destroyCache("cache1");
  }

  @Test
  public void test_disk_persistent() throws CachePersistenceException {
    String path = getPath();
    System.out.println(path);
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().with(CacheManagerBuilder.persistence(path))
      .build(true);
    Cache<String, String> cache = cacheManager.createCache("cache1",
      CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(2, MemoryUnit.MB).disk(4, MemoryUnit.MB, true)));
    cache.put("1", "1");
    cacheManager.close();

    cacheManager = CacheManagerBuilder.newCacheManagerBuilder().with(CacheManagerBuilder.persistence(path)).build(true);
    cache = cacheManager.createCache("cache1",
      CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(2, MemoryUnit.MB).disk(4, MemoryUnit.MB, true)));
    Assert.assertEquals("1", cache.get("1"));
    cacheManager.close();
    EhcacheManager ehcacheManager = (EhcacheManager) cacheManager;
    ehcacheManager.destroy();
  }

  private String getPath() {
    return "c:\\ehcache_test" + UUID.randomUUID();
  }

  @Test
  public void test_cache() {
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
      .with(CacheManagerBuilder.persistence(getPath())).build(true);
    cacheManager.createCache("cache1",
      CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(2, MemoryUnit.MB).disk(4, MemoryUnit.MB, true)));
    cacheManager.getCache("cache1", String.class, String.class);
    cacheManager.removeCache("cache1");
  }
}
