package com.dotcms.shopify.api;

import com.dotcms.security.apps.AppSecrets;
import com.dotcms.shopify.util.DotShopifyApp;
import com.dotcms.shopify.util.AppUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;
import com.google.gson.JsonObject;
import io.vavr.control.Try;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;

public class ShopifyAPIImpl implements ShopifyAPI {

    private final Host host;

    /**
     * Method to load the app secrets into the variables.
     * 
     * @param host if is not sent will throw IllegalArgumentException, if sent will
     *             try to
     *             find the secrets for it, if there is no secrets for the host will
     *             use the ones for the System_Host
     *
     */
    public ShopifyAPIImpl(final Host host) {

        this.host = host;
    }

    @Override
    public Map<String, Object> productById(String id) {
        Logger.info(this.getClass(), "Reloading Shopify configuration for host: " + host.getHostname());
        return null;
    }

    @Override
    public Map<String, Object> collectionById(String id) {
        Logger.info(this.getClass(), "Reloading Shopify configuration for host: " + host.getHostname());
        return null;
    }

    @Override
    public List<Map<String, Object>> productSearch(String query, int limit, int page) {
        Logger.info(this.getClass(), "Reloading Shopify configuration for host: " + host.getHostname());
        return null;
    }

    @Override
    public List<Map<String, Object>> collectionSearch(String query, int limit, int page) {
        Logger.info(this.getClass(), "Reloading Shopify configuration for host: " + host.getHostname());
        return null;
    }

}
