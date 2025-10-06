package com.dotcms.shopify.rest;

import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.shopify.api.ShopifyAPI;
import com.dotcms.shopify.api.ProductSearcher;
import com.dotcms.shopify.api.ShopifySearchParams;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/shopify/product")
public class ShopifyProductResource implements Serializable {

    private static final long serialVersionUID = 204840922704940654L;

    /**
     * Searches for Shopify products the product identified by the provided ID.
     *
     * @param request the HTTP servlet request containing user and session information
     * @param response the HTTP servlet response for sending the result
     * @param searchParams the search parameters for querying Shopify products
     * @return a Response object containing the search results in JSON format or an error message if the input is invalid
     */
    @GET
    @Path("/")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response byId(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @BeanParam final ShopifySearchParams searchParams) {
        if (UtilMethods.isEmpty(searchParams.id)) {
            return Response.status(404).build();
        }
        return searchProducts(request, response, searchParams);
    }

    /**
     * Searches for Shopify products based on the provided search parameters.
     *
     * @param request the HTTP servlet request containing user and session information
     * @param response the HTTP servlet response for sending the result
     * @param searchParams the search parameters for querying Shopify products
     * @return a Response object containing the search results in JSON format or an error message if the input is invalid
     */
    @GET
    @Path("/_search")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response searchProducts(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @BeanParam final ShopifySearchParams searchParams) {

        final User user = new WebResource.InitBuilder(request, response)
                .rejectWhenNoUser(true)
                .requiredFrontendUser(true)
                .requiredBackendUser(true)
                .init()
                .getUser();

        if (searchParams.query == null && UtilMethods.isEmpty(searchParams.id)) {
            return Response.ok(Map.of("errors", "No searchTerm or id passed in")).build();
        }

        Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        Host host = searchParams.hostName != null
                ? Try.of(() -> APILocator.getHostAPI().resolveHostName(searchParams.hostName, user, true))
                        .getOrElse(currentHost)
                : currentHost;

        ShopifyAPI api = ShopifyAPI.api(host);
        ProductSearcher productSearcher = searchParams.toProductSearcher();

        return searchProducts(api, productSearcher);

    }

    /**
     * Searches for Shopify products based on the provided search parameters.
     *
     * @param request the HTTP servlet request containing user and session information
     * @param response the HTTP servlet response for sending the result
     * @param searchParams the search parameters for querying Shopify products
     * @return a Response object containing the search results in JSON format or an error message if the input is invalid
     */
    /**
     * Executes a raw GraphQL query against the Shopify API.
     *
     * @param request the HTTP servlet request containing user and session information
     * @param response the HTTP servlet response for sending the result
     * @param jsonBody the JSON body containing the GraphQL query and optional variables
     * @return a Response object containing the GraphQL query results in JSON format
     */
    @POST
    @Path("/_gql")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public final Response executeGraphQLQuery(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final String jsonBody) {

        final User user = new WebResource.InitBuilder(request, response)
                .rejectWhenNoUser(true)
                .requiredFrontendUser(true)
                .requiredBackendUser(true)
                .init()
                .getUser();

        if (UtilMethods.isEmpty(jsonBody)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("errors", "JSON body with GraphQL query is required"))
                    .build();
        }

        Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        ShopifyAPI api = ShopifyAPI.api(currentHost);

        try {
            Map<String, Object> result = api.rawQuery(jsonBody);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("errors", "Invalid GraphQL query: " + e.getMessage()))
                    .build();
        }
    }


    private Response searchProducts(ShopifyAPI api, ProductSearcher productSearcher) {
        if (UtilMethods.isSet(productSearcher.id)) {
            return Response.ok(api.productById(productSearcher.id)).build();
        }
        if (UtilMethods.isEmpty(productSearcher.cursor)) {
            return Response.ok(api.searchProducts(productSearcher.query, productSearcher.limit)).build();
        }

        return Response.ok(
                api.searchProducts(productSearcher)).build();

    }

    /**
     * Tests the connection to Shopify
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/test")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response testConnection(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {

        final User user = new WebResource.InitBuilder(request, response).rejectWhenNoUser(true)
                .requiredBackendUser(true)
                .init().getUser();

        Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        Map<String, Object> responseMap = ShopifyAPI.api(host).testConnection();

        return Response.ok(responseMap).build();

    }


    /**
     * Redirects to a Shopify product URL based on the provided product ID.
     *
     * @param request the HTTP servlet request object containing user and request details
     * @param response the HTTP servlet response object for returning the result
     * @param searchParams the search parameters containing details such as the product ID
     * @return a Response object with an HTTP 302 Found status and the location set to the Shopify product URL
     * @throws URISyntaxException if the generated Shopify product URL is not valid
     */
    @GET
    @Path("/_redirect")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response redirectToShopifyProduct(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response, @BeanParam final ShopifySearchParams searchParams)
            throws URISyntaxException {

        final User user = new WebResource.InitBuilder(request, response).rejectWhenNoUser(true)
                .requiredBackendUser(true)
                .init().getUser();

        if (UtilMethods.isEmpty(searchParams.id)) {
            throw new DotRuntimeException("missing product Id");
        }

        Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        String redirectUrl = ShopifyAPI.api(host).linkToShopifyProduct(searchParams.id);

        return Response.status(Response.Status.FOUND).location(new URI(redirectUrl)).build();

    }

}
