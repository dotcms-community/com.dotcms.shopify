package com.dotcms.shopify.rest;

import java.io.Serializable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.liferay.portal.model.User;

@Path("/v1/dotshopify")
public class DotShopifyResource implements Serializable {

    private static final long serialVersionUID = 204840922704940654L;

    /**
     * This endpoint is to get the statistics from the CDN.
     * We get the stats of a range of dates.
     *
     * The hostId property is to get the AppSecret Configuration. If the hostId is
     * not send will
     * try to get it from the session.
     *
     * @param dateFromStr date from we should get the stats, format yyyy-MM-dd
     * @param dateToStr   date until we should get the stats, format: yyyy-MM-dd
     * @param hostId      Id of the host which App config we should get.
     * @return
     */
    @GET
    @Path("/product/{id}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getStats(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("id") String id) {

        final User user = new WebResource.InitBuilder(request, response).rejectWhenNoUser(true)
                .requiredFrontendUser(true).init().getUser();

                
        // final Lazy<Host> lazyCurrentHost = Lazy.of(() -> Try.of(() ->
        // Host.class.cast(request.getSession().getAttribute(
        // WebKeys.CURRENT_HOST))).getOrNull());
        // final Host host = Try.of(()-> APILocator.getHostAPI().find(hostId, user,
        // false)).getOrElse(lazyCurrentHost.get());

        return Response.ok().build();

    }

    @GET
    @Path("/product/_search")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response purgeCache(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response, final SearchForm searchForm) {

        final User user = new WebResource.InitBuilder(request, response).rejectWhenNoUser(true)
                .requiredBackendUser(true)
                .requiredPortlet("dotCDN").init().getUser();

        return Response.ok(new ResponseEntityView(Map.of("All Urls Sent Purged: ", ""))).build();

    }

}
