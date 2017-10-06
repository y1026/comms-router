package com.softavail.commsrouter.client;

import com.softavail.commsrouter.api.dto.misc.PaginatedList;
import com.softavail.commsrouter.api.dto.model.ApiObjectId;
import com.softavail.commsrouter.api.dto.model.RouterObjectId;
import com.softavail.commsrouter.api.interfaces.RouterObjectService;

import java.net.URI;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Created by @author mapuo on 04.09.17.
 */
public abstract class ServiceClientBase<T extends ApiObjectId, R extends ApiObjectId> {

  private final Class<T> responseType;
  private final Class<R> createResponseType;

  public ServiceClientBase(Class<T> responseType, Class<R> createResponseType) {
    this.responseType = responseType;
    this.createResponseType = createResponseType;
  }

  // POST over container creates. Returns object
  protected R post(Object obj) {
    URI uri = getApiUrl().clone()
        .build();

    return getClient()
        .target(uri)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(obj, MediaType.APPLICATION_JSON_TYPE), createResponseType);
  }

  // POST over container creates. Returns object
  protected R post(Object obj, String containerId) {
    URI uri = getApiUrl().clone()
        .build(containerId);

    return getClient()
        .target(uri)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(obj, MediaType.APPLICATION_JSON_TYPE), createResponseType);
  }

  // POST over resource updates. Returns void
  protected void post(Object obj, ApiObjectId id) {
    URI uri = getApiUrl().clone()
        .path("{resourceId}")
        .build(id.getId());

    getClient()
        .target(uri)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(obj, MediaType.APPLICATION_JSON_TYPE));
  }

  // POST over resource updates. Returns void
  protected void post(Object obj, RouterObjectId id) {
    URI uri = getApiUrl().clone()
        .path("{resourceId}")
        .build(id.getRouterId(), id.getId());

    getClient()
        .target(uri)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(obj, MediaType.APPLICATION_JSON_TYPE));
  }

  protected T put(Object obj, String id) {
    URI uri = getApiUrl().clone().build(id);
    return getClient()
        .target(uri)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .put(Entity.entity(obj, MediaType.APPLICATION_JSON_TYPE), responseType);
  }

  protected R put(Object obj, RouterObjectId id) {
    URI uri = getApiUrl().clone()
        .path("{resourceId}")
        .build(id.getRouterId(), id.getId());

    return getClient()
        .target(uri)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .put(Entity.entity(obj, MediaType.APPLICATION_JSON_TYPE), createResponseType);
  }

  protected T getItem(ApiObjectId id) {
    URI uri = getApiUrl().clone().path(id.getId()).build();

    return getClient()
        .target(uri)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get(responseType);
  }

  protected T getItem(RouterObjectId id) {
    URI uri = getApiUrl().clone()
        .path("{resourceId}")
        .build(id.getRouterId(), id.getId());

    return getClient()
        .target(uri)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get(responseType);
  }

  protected List<T> getList() {
    return getClient()
        .target(getApiUrl().clone())
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get(new GenericType<List<T>>() {});
  }

  protected List<T> getList(String routerId) {
    URI uri = getApiUrl().clone()
        .build(routerId);

    return getClient()
        .target(uri)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get(new GenericType<List<T>>() {});
  }

  protected PaginatedList<T> getList(String routerId, int page, int perPage) {
    URI uri = getApiUrl().clone()
        .queryParam(RouterObjectService.PAGE_NUMBER_PARAM, page)
        .queryParam(RouterObjectService.ITEMS_PER_PAGE_PARAM, perPage)
        .build(routerId);

    Response response = getClient()
        .target(uri)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get();

    List<T> list = response.readEntity(new GenericType<List<T>>() {});
    Long totalCount = Long.valueOf(
        response.getHeaderString(RouterObjectService.TOTAL_COUNT_HEADER));

    return new PaginatedList<>(list, page, perPage, totalCount);
  }

  protected void deleteRequest(ApiObjectId id) {
    URI uri = getApiUrl().clone().path(id.getId()).build();

    getClient()
        .target(uri)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .delete();
  }

  protected void deleteRequest(RouterObjectId id) {
    URI uri = getApiUrl().clone()
        .path("{resourceId}")
        .build(id.getRouterId(), id.getId());

    getClient()
        .target(uri)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .delete();
  }

  abstract UriBuilder getApiUrl();

  abstract Client getClient();
}
