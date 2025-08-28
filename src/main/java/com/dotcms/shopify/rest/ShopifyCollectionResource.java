package com.dotcms.shopify.rest;

import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.shopify.api.ProductSearchParams;
import com.dotcms.shopify.api.ShopifyAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/shopify/collection")
public class ShopifyCollectionResource implements Serializable {

    private static final long serialVersionUID = 204840922704940654L;

    @GET
    @Path("/")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response byId(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @BeanParam final ProductSearchParams searchParams) {

        checkUser(request, response);

        if (UtilMethods.isEmpty(searchParams.id)) {
            return Response.status(404).build();
        }

        Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        ShopifyAPI api = ShopifyAPI.api(host);

        return Response.ok(api.collectionById(searchParams.id)).build();
    }

    @GET
    @Path("/_search")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response searchCollections(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @BeanParam final ProductSearchParams searchParams) {

        checkUser(request, response);

        if (UtilMethods.isEmpty(searchParams.query)) {
            return Response.status(500, "Invalid argument").build();
        }

        Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        ShopifyAPI api = ShopifyAPI.api(host);
        return Response
                .ok(api.searchCollections(searchParams.toProductSearcher()))
                .build();

    }

    @GET
    @Path("/_redirect")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response redirectToShopifyCollection(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response, @BeanParam final ProductSearchParams searchParams)
            throws URISyntaxException {

        checkUser(request, response);

        if (UtilMethods.isEmpty(searchParams.id)) {
            throw new DotRuntimeException("missing collection Id");
        }

        Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        String redirectUrl = ShopifyAPI.api(host).linkToShopifyCollection(searchParams.id);

        return Response.status(Response.Status.FOUND).location(new URI(redirectUrl)).build();

    }

    private void checkUser(final HttpServletRequest request, final HttpServletResponse response) {
        new WebResource.InitBuilder(request, response)
                .rejectWhenNoUser(true)
                .requiredFrontendUser(true)
                .requiredBackendUser(true)
                .init()
                .getUser();

    }

}
