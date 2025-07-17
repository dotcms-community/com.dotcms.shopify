package com.dotcms.shopify.viewtool;

import com.dotcms.shopify.api.ShopifyAPI;
import io.vavr.Lazy;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.liferay.portal.model.User;

public class DotShopifyTool implements ViewTool {

    private HttpServletRequest request;
    private Host host;
    private User user;

    private Lazy<ShopifyAPI> api;
    @Override
    public void init(Object initData) {
        this.request = ((ViewContext) initData).getRequest();
        this.host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(this.request);
        this.api = Lazy.of(() -> ShopifyAPI.api(host));
    }

    public Map<String, Object> getProduct(long productId) {
        return this.getProduct(String.valueOf(productId));


    }

    public Map<String, Object> getProduct(String productId) {
       return api.get().productById(productId);


    }


}
