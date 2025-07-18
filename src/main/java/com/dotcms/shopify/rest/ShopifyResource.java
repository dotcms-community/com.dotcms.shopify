package com.dotcms.shopify.rest;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.shopify.api.ShopifyAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.liferay.portal.model.User;
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

@Path("/v1/shopify")
public class ShopifyResource implements Serializable {

  private static final long serialVersionUID = 204840922704940654L;


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
        .init().getUser();

    return Response.ok(new ResponseEntityView(Map.of("All Urls Sent Purged: ", ""))).build();

  }

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


}
