package com.dotcms.shopify.rest;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.util.Logger;

public class ShopifyInterceptor  implements WebInterceptor {

    private static final long serialVersionUID = 1L;

    @Override
    public String[] getFilters() {
        return new String[] {
                "/shopify/*"};
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response)  {

        Logger.info(this.getClass().getName(),"  ShopifyInterceptor : " + request.getRequestURI());



        return Result.NEXT;

    }

}
