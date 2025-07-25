package com.dotcms.shopify.viewtool;

import com.dotcms.shopify.api.ShopifyAPI;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;
import java.util.List;
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

    public Map<String, Object> getProduct(String productId) {
        return api.get().productById(productId);
    }

    public List<Map<String, Object>> searchProducts(String searchTerm, int limit) {
        return api.get().searchProducts(searchTerm, limit);
    }

    public List<Map<String, Object>> searchProducts(String searchTerm, int limit, String cursor, String beforeOrAfter) {
        if(UtilMethods.isEmpty(cursor)){
            return api.get().searchProducts(searchTerm, limit);
        }

        if("BEFORE".equalsIgnoreCase(beforeOrAfter)){
            return api.get().searchProducts(searchTerm, limit,cursor,ShopifyAPI.BEFORE_AFTER.BEFORE);
        }else{
            return api.get().searchProducts(searchTerm, limit,cursor,ShopifyAPI.BEFORE_AFTER.AFTER);
        }

    }



    public Map<String, Object> getProductByHandle(String handle) {
       return api.get().productById(handle);


    }


}
