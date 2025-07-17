package com.dotcms.shopify.osgi;

import com.dotcms.filters.interceptor.FilterWebInterceptorProvider;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.filters.interceptor.WebInterceptorDelegate;
import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.shopify.listener.ShopifyAppListener;
import com.dotcms.shopify.listener.ShopifyContentListener;
import com.dotcms.shopify.rest.ShopifyInterceptor;
import com.dotcms.shopify.util.DotShopifyApp;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.filters.InterceptorFilter;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.osgi.framework.BundleContext;


public class Activator extends GenericBundleActivator {

  final WebInterceptorDelegate delegate =
      FilterWebInterceptorProvider.getInstance(Config.CONTEXT).getDelegate(
          InterceptorFilter.class);
  final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
  private final WebInterceptor[] webInterceptors = {new ShopifyInterceptor()};
  private final ShopifyAppListener shopifyAppListener = new ShopifyAppListener();
  private final ShopifyContentListener shopifyContentListener = new ShopifyContentListener();

  public void start(final org.osgi.framework.BundleContext context) throws Exception {

    Logger.info(Activator.class.getName(), "Starting Shopify Plugin");

    ActivatorUtil activatorUtil = new ActivatorUtil();
    this.initializeServices(context);

    //Adding APP yaml
    //Logger.info(Activator.class.getName(), "Copying dotCDN APP");
    activatorUtil.copyAppYml();

    //Register Receiver PP listener events.
    localSystemEventsAPI.subscribe(shopifyContentListener);
    localSystemEventsAPI.subscribe(AppSecretSavedEvent.class, shopifyAppListener);

    activatorUtil.moveJarFilestoFileAssets("graphql", DotShopifyApp.GRAPHQL_QUERY_PATH);
    activatorUtil.moveJarFilestoFileAssets("vtl", "/application/shopify/vtl");
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
    new ActivatorUtil().deleteYml();

    localSystemEventsAPI.unsubscribe(shopifyContentListener);
    localSystemEventsAPI.unsubscribe(shopifyAppListener);

    Logger.info(Activator.class.getName(), "Stopping Shopify Plugin");
  }


}
