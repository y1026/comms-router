/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */

package com.softavail.commsrouter.api.dto.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 *
 * @author ikrustev
 */
@MappedSuperclass
public class RouterObject extends ApiObject {

  @Column(name = "router_id")
  private String routerId;

  public RouterObject() {}

  public RouterObject(RouterObject rhs) {
    super(rhs);
    this.routerId = rhs.routerId;
  }

  public RouterObject(String id, String routerId) {
    super(id);
    this.routerId = routerId;
  }

  public String getRouterId() {
    return routerId;
  }

  public void setRouterId(String routerId) {
    this.routerId = routerId;
  }

  @Override
  public String toString() {
    return "" + getRouterId() + ":" + getId();
  }

  public static class Builder {

    private String id;
    private String routerId;

    public Builder() {}

    public Builder setId(String id) {
      this.id = id;
      return this;
    }

    public Builder setRouterId(String routerId) {
      this.routerId = routerId;
      return this;
    }

    public RouterObject build() {
      return new RouterObject(id, routerId);
    }

  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object object) {
    boolean equals = super.equals(object);
    if (equals) {
      RouterObject routerObject = (RouterObject) object;
      return Objects.equals(getRouterId(), routerObject.getRouterId());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getRouterId(), getVersion(), getClass());
  }

}
