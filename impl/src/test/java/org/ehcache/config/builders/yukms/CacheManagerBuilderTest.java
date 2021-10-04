package org.ehcache.config.builders.yukms;

import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.junit.Test;

public class CacheManagerBuilderTest {
  @Test
  public void test_build() {
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
  }
}
