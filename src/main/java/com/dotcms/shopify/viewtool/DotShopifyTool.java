package com.dotcms.shopify.viewtool;

import com.dotcms.shopify.api.ProductSearcher;
import com.dotcms.shopify.api.ShopifyAPI;
import com.dotcms.shopify.api.ShopifyAPI.BEFORE_AFTER;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

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

    public Map<String, Object> searchProducts(String searchTerm, int limit) {
        return api.get().searchProducts(searchTerm, limit);
    }

    public Map<String, Object> searchProducts(String searchTerm, int limit, String cursor, String beforeOrAfter) {

        ProductSearcher.Builder builder = new ProductSearcher.Builder().query(searchTerm).limit(limit).cursor(cursor);

        if ("BEFORE".equalsIgnoreCase(beforeOrAfter)) {

            builder.before(BEFORE_AFTER.BEFORE);

        }

        return api.get().searchProducts(builder.build());

    }

    public Map<String, Object> getCollectionById(String id) {
        if (UtilMethods.isEmpty(id)) {
            return null;
        }
        return api.get().collectionById(id);

    }

   public Map<String, Object> getCollectionById(String id, int productLimit, String cursor, String beforeOrAfter, String sortKey) {
      if (UtilMethods.isEmpty(id)) {
         return null;
      }
      return api.get().collectionById(id);

   }




}
