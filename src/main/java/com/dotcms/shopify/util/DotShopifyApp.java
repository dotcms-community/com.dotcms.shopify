package com.dotcms.shopify.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


import com.dotcms.cache.DynamicTTLCache;
import com.dotcms.security.apps.AppSecrets;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;

import io.vavr.Lazy;
import io.vavr.control.Try;

public enum DotShopifyApp {


    
    DOT_SHOPIFY_APP_KEY("dot-shopify"),     // the name of the app/yaml file
    API_KEY("apiKey"),                      // shopify api key
    STORE_NAME("storeName"),                // shopify store id/name (in the shopify url)
    API_VERSION("apiVersion");              // shopify api version to use, e.g. "2025-07"

    static DynamicTTLCache<String,Lazy<Map<String,String>>> ttlCache = new DynamicTTLCache<>(1000,60*1000); // cache for a minute

    
    final String appValue;

    DotShopifyApp(String appValue) {
        this.appValue = appValue;

    }

    String getAppValue(Host host) {
        return appValue;
    }


    public Lazy<Map<String,String>> instance(Host host){
        Lazy<Map<String,String>> config = ttlCache.getIfPresent(host.getHostname());
        if(config == null){
            config = loadAppConfigNoCache(host);
            ttlCache.put(host.getHostname(),config);
        }
        
        return config;



    }

    private Lazy<Map<String, String>> loadAppConfigNoCache(Host host) {
        return Lazy.of(() -> {
            Optional<AppSecrets> secrets = Try.of(
                    () -> APILocator.getAppsAPI().getSecrets(DOT_SHOPIFY_APP_KEY.name(), host, APILocator.systemUser()))
                    .get();

            // return Map.of();
            if (secrets.isEmpty()) {
                return Map.of();
            }
            Map<String,String> values = new HashMap<>();
            for(DotShopifyApp appKey :DotShopifyApp.values()){
                if(UtilMethods.isSet(()->secrets.get().getSecrets().get(appKey.name()).getString())){
                    values.put(appKey.name(), secrets.get().getSecrets().get(appKey.name()).getString());
                }
            }
            return values;
                

                    
        });
    }

}
