package com.dotcms.shopify.util;

import com.dotcms.cache.CacheValue;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Config;
import io.vavr.Lazy;


public class ShopifyCache implements Cachable {

  // cache things 1m
  static long CACHE_DEFAULT_TTL = Config.getLongProperty("SHOPIFY_CACHE_TTL", 60 * 1000);

  // cache up to 10000 objects
  static long CACHE_DEFAULT_SIZE = Config.getLongProperty("SHOPIFY_CACHE_SIZE", 10000);
  private static Lazy<ShopifyCache> lazyInstance = Lazy.of(ShopifyCache::new);
  final DotCacheAdministrator cache;


  ShopifyCache() {
    cache = CacheLocator.getCacheAdministrator();
  }

  static public ShopifyCache getInstance() {
    return lazyInstance.get();
  }

  @Override
  public String getPrimaryGroup() {
    return "ShopifyCache";
  }

  @Override
  public String[] getGroups() {
    return new String[]{getPrimaryGroup()};
  }

  @Override
  public void clearCache() {
    cache.flushAll();
  }

  public Object get(CacheType type, String key) {
    Object cacheObject = (Object) cache.getNoThrow(type.name() + key, getPrimaryGroup());
    return cacheObject != null && cacheObject instanceof CacheValue ? ((CacheValue) cacheObject).value : cacheObject;
  }

  public void put(CacheType type, String key, Object valueIn, long ttl) {
    CacheValue cacheObject = new CacheValue(valueIn, ttl);
    cache.put(type.name() + key, cacheObject, getPrimaryGroup());
  }

  public void put(CacheType type, String key, Object valueIn) {

    this.put(type, key, valueIn, CACHE_DEFAULT_TTL);
  }

  public void remove(CacheType type, String key) {
    cache.remove(type.name() + key, getPrimaryGroup());
  }

  public void flushAll() {
    cache.flushGroup(getPrimaryGroup());
  }

  public enum CacheType {
    PRODUCTS,
    COLLECTIONS,
    GRAPHQL,
    CONFIG
  }


}
