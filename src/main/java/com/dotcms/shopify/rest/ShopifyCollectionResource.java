package com.dotcms.shopify.rest;

import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.shopify.api.ShopifySearchParams;
import com.dotcms.shopify.api.ShopifyAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
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


    /**
     * Retrieves a Shopify collection by its identifier.
     *
     * Checks if the user is authenticated and authorized to access the resource.
     * If the `id` parameter is not provided in the request, returns a HTTP 404 response.
     * Otherwise, fetches the Shopify collection data using the provided collection ID
     * and returns it in the response.
     *
     * @param request the HTTP servlet request containing the client-specific context
     * @param response the HTTP servlet response to send data to the client
     * @param searchParams the parameters for searching collections, which must include the collection ID (`id`)
     * @return a Response containing the Shopify collection data if the ID is valid,
     *         or a 404 HTTP response if the ID is missing or invalid.
     */
    @GET
    @Path("/")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response byId(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @BeanParam final ShopifySearchParams searchParams) {

        checkUser(request, response);

        if (UtilMethods.isEmpty(searchParams.id)) {
            return Response.status(404).build();
        }

        Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        ShopifyAPI api = ShopifyAPI.api(host);

        return Response.ok(api.collectionById(searchParams.id)).build();
    }

    /**
     * Searches Shopify collections based on the provided search parameters.
     * Validates user access to ensure the request is authenticated and authorized.
     * If the search parameter `query` is empty, returns an HTTP 500 response indicating an invalid argument.
     * Otherwise, performs the collection search using the Shopify API and returns the results.
     *
     * @param request the HTTP servlet request containing the client-specific context, such as headers
     * @param response the HTTP servlet response to send data back to the client
     * @param searchParams the parameters used to search Shopify collections, including query, limit, sort options, etc.
     * @return a Response object containing the search results if successful, or an HTTP error response if the query is invalid
     */
    @GET
    @Path("/_search")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response searchCollections(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @BeanParam final ShopifySearchParams searchParams) {

        checkUser(request, response);

        if (UtilMethods.isEmpty(searchParams.query)) {
            return Response.status(500, "Invalid argument").build();
        }

        Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        ShopifyAPI api = ShopifyAPI.api(host);

        Map<String,Object> json = api.searchCollections(searchParams.toProductSearcher());


        return Response
                .ok(json)
                .build();

    }

    /**
     * Redirects to a Shopify collection URL based on the provided collection ID.
     * @param request
     * @param response
     * @param searchParams
     * @return
     * @throws URISyntaxException
     */
    @GET
    @Path("/_redirect")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response redirectToShopifyCollection(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response, @BeanParam final ShopifySearchParams searchParams)
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
