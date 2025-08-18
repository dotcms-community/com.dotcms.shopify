package com.dotcms.shopify.rest;

import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.shopify.api.ShopifyAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/shopify")
public class ShopifyCollectionResource implements Serializable {

    private static final long serialVersionUID = 204840922704940654L;

    @GET
    @Path("/collection/")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response byId(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("id") String id) {

        return searchCollections(request, response, null, null, 1, null, id, null);
    }

    @GET
    @Path("/collection/_search")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response searchCollections(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("searchTerm") String searchTerm,
            @QueryParam("hostName") String hostName,
            @QueryParam("limit") int limit,
            @QueryParam("cursor") String cursor,
            @QueryParam("id") String id,
            @QueryParam("beforeAfter") String beforeAfter) {

        final User user = new WebResource.InitBuilder(request, response)
                .rejectWhenNoUser(true)
                .requiredFrontendUser(true)
                .requiredBackendUser(true)
                .init()
                .getUser();

        SearchForm searchForm = new SearchForm.Builder()
                .searchTerm(searchTerm)
                .hostName(hostName)
                .limit(limit)
                .cursor(cursor)
                .id(id)
                .beforeAfter(beforeAfter)
                .build();

        if (searchForm.searchTerm == null && UtilMethods.isEmpty(searchForm.id)) {
            return Response.ok(Map.of("errors", "No searchTerm or id passed in")).build();
        }

        Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        Host host = searchForm.hostName != null
                ? Try.of(() -> APILocator.getHostAPI().resolveHostName(searchForm.hostName, user, true))
                        .getOrElse(currentHost)
                : currentHost;

        ShopifyAPI api = ShopifyAPI.api(host);

        if (UtilMethods.isSet(searchForm.id)) {
            return Response.ok(api.productById(searchForm.id)).build();
        }

        if (UtilMethods.isEmpty(cursor)) {
            return Response.ok(api.searchCollections(searchForm.searchTerm, searchForm.limit)).build();
        }

        return Response
                .ok()
                .build();

    }

}
