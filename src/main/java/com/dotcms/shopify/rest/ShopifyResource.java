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
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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


    return Response.ok().build();

  }

  @GET
  @Path("/product/_search")
  @NoCache
  @Produces(MediaType.APPLICATION_JSON)
  public final Response purgeCache(@Context final HttpServletRequest request,
      @Context final HttpServletResponse response,
      @QueryParam("searchTerm") String searchTerm,
      @QueryParam("hostName") String hostName,
      @QueryParam("limit") int limit,
      @QueryParam("cursor") String cursor,
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
        .beforeAfter(beforeAfter)
        .build();



    if (searchForm.searchTerm == null) {
      return Response.ok(Map.of("errors", "No search term passed in")).build();
    }

    Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

    Host host = searchForm.hostName !=null
        ?  Try.of(() -> APILocator.getHostAPI().resolveHostName(searchForm.hostName, user, true)).getOrElse(currentHost)
        : currentHost;

    ShopifyAPI api = ShopifyAPI.api(host);


    if(UtilMethods.isEmpty(cursor)){
      return Response.ok(api.searchProducts(searchForm.searchTerm, searchForm.limit)).build();
    }


    return Response.ok(api.searchProducts(searchForm.searchTerm,searchForm.limit,cursor,searchForm.getBeforeAfter())).build();



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
