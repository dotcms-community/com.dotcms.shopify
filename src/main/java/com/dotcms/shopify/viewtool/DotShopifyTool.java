package com.dotcms.shopify.viewtool;

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

    @Override
    public void init(Object initData) {
        this.request = ((ViewContext) initData).getRequest();
        this.host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(this.request);
    }

}
