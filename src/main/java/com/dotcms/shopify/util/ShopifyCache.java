package com.dotcms.shopify.util;

import com.dotcms.cache.DynamicTTLCache;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotmarketing.util.Config;
import io.vavr.Lazy;


public class ShopifyCache {
  private static Lazy<ShopifyCache> lazyInstance = Lazy.of(ShopifyCache::new);

  static public ShopifyCache getInstance() {
      return lazyInstance.get();
  }

  ShopifyCache(){

  }


  // cache things 1m
  static long CACHE_DEFAULT_TTL = Config.getLongProperty("SHOPIFY_CACHE_TTL",60*1000);

  // cache up to 10000 objects
  static long CACHE_DEFAULT_SIZE = Config.getLongProperty("SHOPIFY_CACHE_SIZE",10000);


  public enum CacheType {
    PRODUCTS,
    COLLECTIONS,
    GRAPHQL,
    CONFIG
  }

  static DynamicTTLCache<String, Object> ttlCache = new DynamicTTLCache<>(CACHE_DEFAULT_SIZE,CACHE_DEFAULT_TTL); // cache for a minute


  public Object get(CacheType type, String key){
    return ttlCache.getIfPresent(type.name() + key);
  }

  public void put(CacheType type, String key, Object value, long ttl){
    ttlCache.put(type.name() + key, value,ttl);
  }
  public void put(CacheType type, String key, Object value){
    this.put(type, key, value,CACHE_DEFAULT_TTL);
  }

  public void remove(CacheType type, String key) {
    ttlCache.invalidate(type.name() + key);
  }

  public void flushAll(){
    ttlCache.invalidateAll();
  }

  public void flush(CacheType type){
    ttlCache.asMap().keySet().stream().filter(k -> k.startsWith(type.name())).forEach(ttlCache::invalidate);
  }

  public void flushAsync(CacheType type){
    DotConcurrentFactory.getInstance().getSubmitter().submit(() -> flush(type));
  }


}
