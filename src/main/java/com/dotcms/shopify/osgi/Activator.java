package com.dotcms.shopify.osgi;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.filters.interceptor.FilterWebInterceptorProvider;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.filters.interceptor.WebInterceptorDelegate;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.shopify.api.ShopifyAPI;
import com.dotcms.shopify.listener.ShopifyAppListener;
import com.dotcms.shopify.listener.ShopifyContentListener;
import com.dotcms.shopify.rest.ShopifyInterceptor;
import com.dotcms.shopify.rest.ShopifyProductResource;
import com.dotcms.shopify.util.ShopifyApp;
import com.dotcms.shopify.viewtool.DotShopifyToolInfo;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.filters.InterceptorFilter;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.util.concurrent.TimeUnit;
import org.osgi.framework.BundleContext;

public class Activator extends GenericBundleActivator {

    final WebInterceptorDelegate delegate = FilterWebInterceptorProvider.getInstance(Config.CONTEXT).getDelegate(
            InterceptorFilter.class);
    final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();

    private final WebInterceptor[] webInterceptors = { new ShopifyInterceptor() };

    private final ShopifyAppListener shopifyAppListener = new ShopifyAppListener();

    private final ShopifyContentListener shopifyContentListener = new ShopifyContentListener();

    public void start(final org.osgi.framework.BundleContext context) throws Exception {

        Logger.info(Activator.class.getName(), "Starting Shopify Plugin");

        ActivatorUtil activatorUtil = new ActivatorUtil();
        this.initializeServices(context);

        // Adding APP yaml
        // Logger.info(Activator.class.getName(), "Copying dotCDN APP");
        activatorUtil.copyAppYml();

        registerViewToolService(context, new DotShopifyToolInfo());

        // Register Receiver PP listener events.
        localSystemEventsAPI.subscribe(shopifyContentListener);
        localSystemEventsAPI.subscribe(AppSecretSavedEvent.class, new ShopifyAppListener());

        activatorUtil.moveJarFilestoFileAssets("gql", ShopifyApp.GRAPHQL_QUERY_FILES_PATH);
        activatorUtil.moveJarFilestoFileAssets("vtl", ShopifyApp.VTL_FILES_PATH);

        activatorUtil.createShopifyExampleContentType();

        // this should be done last in case the bundle fails to start
        RestServiceUtil.addResource(ShopifyProductResource.class);

        DotConcurrentFactory.getInstance().getSubmitter().submit(() -> ShopifyAPI.api(APILocator.systemHost()).reload(),
                10,
                TimeUnit.SECONDS);
    }

    @Override
    public void stop(BundleContext context) throws Exception {

        // remove portlet, viewtool, actionlet
        this.unregisterServices(context);
        this.unregisterViewToolServices();
        Logger.info(Activator.class.getName(), "Stopping Interceptor");
        for (WebInterceptor webIn : webInterceptors) {
            Logger.info(Activator.class.getName(), "Removing the " + webIn.getClass().getName());
            delegate.remove(webIn.getName(), true);
        }

        final FilterWebInterceptorProvider filterWebInterceptorProvider = FilterWebInterceptorProvider
                .getInstance(Config.CONTEXT);

        filterWebInterceptorProvider.getDelegate(InterceptorFilter.class);

        Logger.info(Activator.class.getName(), "Removing Shopify APP");
        new ActivatorUtil().deleteYml();

        localSystemEventsAPI.unsubscribe(shopifyContentListener);
        localSystemEventsAPI.unsubscribe(shopifyAppListener);

        RestServiceUtil.removeResource(ShopifyProductResource.class);

        Logger.info(Activator.class.getName(), "Stopping Shopify Plugin");

    }

}
