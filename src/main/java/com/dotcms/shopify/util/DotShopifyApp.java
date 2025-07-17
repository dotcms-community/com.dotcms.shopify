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

    static DynamicTTLCache<String,Lazy<Map<Object,String>>> ttlCache = new DynamicTTLCache<>(1000,60*1000); // cache for a minute

    public static String GRAPHQL_QUERY_PATH = "/graphql/shopify/";
    public final String appValue;

    DotShopifyApp(String appValue) {
        this.appValue = appValue;

    }


    public Lazy<Map<Object,String>> instance(Host host){
        Lazy<Map<Object,String>> config = ttlCache.getIfPresent(host.getHostname());
        if(config == null){
            config = loadAppConfigNoCache(host);
            ttlCache.put(host.getHostname(),config);
        }
        
        return config;



    }

    private Lazy<Map<Object, String>> loadAppConfigNoCache(Host host) {
        return Lazy.of(() -> {
            Optional<AppSecrets> secrets = Try.of(
                    () -> APILocator.getAppsAPI().getSecrets(DOT_SHOPIFY_APP_KEY.appValue, host, APILocator.systemUser()))
                    .get();


            if (secrets.isEmpty()) {
                return Map.of();
            }
            Map<Object,String> values = new HashMap<>();
            for(DotShopifyApp appKey :DotShopifyApp.values()){
                String value = Try.of(()->secrets.get().getSecrets().get(appKey.appValue).getString().trim()).getOrNull();
                if(UtilMethods.isSet(value)){
                    values.put(appKey.appValue, value);
                    values.put(API_KEY.name(), value);
                    values.put(API_KEY, value);
                }
            }
            values.put(STORE_NAME.appValue, parseStoreName(values.get(STORE_NAME.appValue)));
            values.put(STORE_NAME.name(), parseStoreName(values.get(STORE_NAME.appValue)));
            values.put(STORE_NAME, parseStoreName(values.get(STORE_NAME.appValue)));
            return values;
                

                    
        });
    }

    String parseStoreName(String storeName) {
        if (UtilMethods.isEmpty(storeName)) {
            return null;
        }

        return storeName.trim()
            .replaceAll("https://", "")
            .replaceAll("http://", "")
            .replaceAll(".myshopify.com", "")
            .replaceAll(".myshopify.com/", "");


    }

}
