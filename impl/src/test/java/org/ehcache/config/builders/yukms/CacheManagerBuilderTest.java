package org.ehcache.config.builders.yukms;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.CachePersistenceException;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.EhcacheManager;
import org.ehcache.core.internal.resilience.ThrowingResilienceStrategy;
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
    return "k:\\ehcache_test" + UUID.randomUUID();
  }

  @Test
  public void test_cache() {
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().withDefaultSizeOfMaxObjectGraph(111)
      .with(CacheManagerBuilder.persistence(getPath())).build(true);
    cacheManager.createCache("cache1",
      CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(2, MemoryUnit.MB).offheap(4, MemoryUnit.MB)
          .disk(8, MemoryUnit.MB, true)));
    cacheManager.getCache("cache1", String.class, String.class);
    cacheManager.removeCache("cache1");
  }

  @Test
  public void test_put() throws InterruptedException, FileNotFoundException {
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()//
      .withDefaultSizeOfMaxObjectGraph(111)//
      .with(CacheManagerBuilder.persistence(getPath()))//
      .build(true);
    Cache<String, String> cache = cacheManager.createCache("cache_test",

      CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
        ResourcePoolsBuilder.newResourcePoolsBuilder()//
          .heap(1, MemoryUnit.B)//
          .offheap(1, MemoryUnit.MB)//
          .disk(2, MemoryUnit.MB, true)//
      )//
        .withResilienceStrategy(new ThrowingResilienceStrategy<>())//
      .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.of(1, ChronoUnit.SECONDS)))//
    );
    String key = "cache_test_key";
    for (int i = 1; i <= 10_000_000; i++) {
      //BufferedReader reader = new LineNumberReader(new FileReader("C:\\Users\\yukms\\Desktop\\value.txt"));
      //String s1 = reader.lines().reduce((s, s2) -> s + s2).get();
      try {
        //cache.put("cache_test_key" + i, s1);
        cache.put("cache_test_key" + i, "cache_test_key" + i);
      } catch (Throwable e) {
        e.fillInStackTrace();
        throw e;
      }
    }
    Thread.sleep(1000);
    Assert.assertNull(cache.get(key));
    Assert.assertNull(cache.get(key));
    cache.remove(key);
  }

  @Test
  public void test_put1() {
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()//
      .withDefaultSizeOfMaxObjectGraph(111)//
      .with(CacheManagerBuilder.persistence(getPath()))//
      .build(true);
    Cache<String, String> cache = cacheManager.createCache("cache_test",

      CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
          ResourcePoolsBuilder.newResourcePoolsBuilder()//
            //.heap(1, MemoryUnit.B)//
          .offheap(1, MemoryUnit.MB)//
          //.disk(1, MemoryUnit.MB, true)//
        )//
        .withResilienceStrategy(new ThrowingResilienceStrategy<>())//
    );
    String key = "cache_test_key";
    cache.put(key, "cache_test_value");
    cache.put(key, "cache_test_value1");
    cache.get(key);
  }
}
