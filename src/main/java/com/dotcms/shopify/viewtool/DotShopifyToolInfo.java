package com.dotcms.shopify.viewtool;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

public class DotShopifyToolInfo extends ServletToolInfo {

    @Override
    public String getKey () {
        return "dotshopify";
    }

    @Override
    public String getScope () {
        return ViewContext.REQUEST;
    }

    @Override
    public String getClassname () {
        return DotShopifyTool.class.getName();
    }

    @Override
    public Object getInstance ( Object initData ) {

        DotShopifyTool viewTool = new DotShopifyTool();
        viewTool.init( initData );

        setScope( ViewContext.REQUEST );

        return viewTool;
    }

}
