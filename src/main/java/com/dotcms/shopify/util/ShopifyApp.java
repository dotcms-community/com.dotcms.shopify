package com.dotcms.shopify.util;

import com.dotcms.security.apps.AppSecrets;
import com.dotcms.shopify.util.ShopifyCache.CacheType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;

public class ShopifyApp {




    public static Map<String, String> instance(@Nonnull Host host) {
        Map<String, String> config = (Map<String, String>) ShopifyCache.getInstance().get(CacheType.CONFIG,
                host.getIdentifier());
        if (config == null) {
            config = loadAppConfigNoCache(host);
            ShopifyCache.getInstance().put(CacheType.CONFIG, host.getIdentifier(), config, 60 * 60 * 1000);
        }

        return config;

    }

    private static Map<String, String> loadAppConfigNoCache(@Nonnull Host host) {

        Optional<AppSecrets> secrets = Try.of(
                () -> APILocator.getAppsAPI()
                        .getSecrets(AppKey.DOT_SHOPIFY_APP_KEY.appValue, true, host, APILocator.systemUser()))
                .get();

        if (secrets.isEmpty()) {
            return Map.of();
        }
        Map<String, String> values = new HashMap<>();
        for (AppKey appKey : AppKey.values()) {
            String value = Try.of(() -> secrets.get().getSecrets().get(appKey.appValue).getString().trim()).getOrNull();
            if (UtilMethods.isSet(value)) {
                values.put(appKey.appValue, value);
                values.put(appKey.name(), value);
            }
        }
        values.put(AppKey.STORE_NAME.appValue, parseStoreName(values.get(AppKey.STORE_NAME.appValue)));
        values.put(AppKey.STORE_NAME.name(), parseStoreName(values.get(AppKey.STORE_NAME.appValue)));
        return values;

    }

    static String parseStoreName(String storeName) {
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
