
package com.dotcms.shopify.util;

import com.dotmarketing.exception.DotRuntimeException;

public enum AppKey {
  DOT_SHOPIFY_APP_KEY("dot-shopify"), // the name of the app/yaml file
  API_KEY("apiKey"), // shopify api key
  STORE_NAME("storeName"), // shopify store id/name (in the shopify url)
  DEBUG_GRAPHQL("debugGraphQL"), // debug graphql queries
  API_VERSION("apiVersion"); // shopify api version to use, e.g. "2025-07"


  public final String appValue;

  AppKey(String appValue) {
    this.appValue = appValue;
  }

  public static AppKey fromString(String appValue) {
    for (AppKey appKey : AppKey.values()) {
      if (appKey.appValue.equalsIgnoreCase(appValue)) {
        return appKey;
      }
      if (appKey.name().equalsIgnoreCase(appValue)) {
        return appKey;
      }
    }
    throw new DotRuntimeException("Unknown app key: " + appValue);
  }

}
