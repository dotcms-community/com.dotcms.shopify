package com.dotcms.shopify.osgi;

import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.shopify.listener.ShopifyAppListener;
import com.dotcms.shopify.listener.ShopifyContentListener;
import com.dotcms.shopify.rest.ShopifyInterceptor;
import com.dotcms.shopify.util.AppUtil;
import com.dotcms.shopify.workflow.ShopifyActionlet;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.filters.InterceptorFilter;

import org.osgi.framework.BundleContext;

import com.dotcms.filters.interceptor.FilterWebInterceptorProvider;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.filters.interceptor.WebInterceptorDelegate;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;


public class Activator extends GenericBundleActivator {

    private final WebInterceptor[] webInterceptors = {new ShopifyInterceptor()};

    private final ShopifyAppListener shopifyAppListener = new ShopifyAppListener();
    private final ShopifyContentListener shopifyContentListener = new ShopifyContentListener();
    final WebInterceptorDelegate delegate =
                    FilterWebInterceptorProvider.getInstance(Config.CONTEXT).getDelegate(
                            InterceptorFilter.class);

    final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();


    public void start(final org.osgi.framework.BundleContext context) throws Exception {

        Logger.info(Activator.class.getName(), "Starting Shopify Plugin");

        this.initializeServices(context);


        //Adding APP yaml
        //Logger.info(Activator.class.getName(), "Copying dotCDN APP");
        new AppUtil().copyAppYml();

        //Register Actionlet
        this.registerActionlet(context, new ShopifyActionlet());

        //Register Receiver PP listener events.
        localSystemEventsAPI.subscribe(shopifyContentListener);
        localSystemEventsAPI.subscribe(AppSecretSavedEvent.class, shopifyAppListener);

    }



    @Override
    public void stop(BundleContext context) throws Exception {

        // remove portlet, viewtool, actionlet
        this.unregisterServices(context);

        Logger.info(Activator.class.getName(), "Stopping Interceptor");
        for (WebInterceptor webIn : webInterceptors) {
            Logger.info(Activator.class.getName(), "Removing the " + webIn.getClass().getName());
            delegate.remove(webIn.getName(), true);
        }



        final FilterWebInterceptorProvider filterWebInterceptorProvider =
                        FilterWebInterceptorProvider.getInstance(Config.CONTEXT);

        filterWebInterceptorProvider.getDelegate(InterceptorFilter.class);

        Logger.info(Activator.class.getName(), "Removing Shopify APP");
        new AppUtil().deleteYml();

        localSystemEventsAPI.unsubscribe(shopifyContentListener);
        localSystemEventsAPI.unsubscribe(shopifyAppListener);

        Logger.info(Activator.class.getName(), "Stopping Shopify Plugin");
    }








}
