package com.softavail.commsrouter.webservice.mappers;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

/**
 * Created by @author mapuo on 03.09.17.
 */
@Provider
public class IllegalArgumentMapper extends BaseExceptionMapper<IllegalArgumentException> {

  public IllegalArgumentMapper() {
    super(Status.BAD_REQUEST);
  }

}
