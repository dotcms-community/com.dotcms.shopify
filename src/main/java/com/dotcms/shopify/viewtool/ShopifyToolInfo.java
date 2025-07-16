package com.dotcms.shopify.viewtool;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

public class ShopifyToolInfo extends ServletToolInfo {

    @Override
    public String getKey () {
        return "shopify";
    }

    @Override
    public String getScope () {
        return ViewContext.REQUEST;
    }

    @Override
    public String getClassname () {
        return ShopifyTool.class.getName();
    }

    @Override
    public Object getInstance ( Object initData ) {

    	ShopifyTool viewTool = new ShopifyTool();
        viewTool.init( initData );

        setScope( ViewContext.REQUEST );

        return viewTool;
    }

}
