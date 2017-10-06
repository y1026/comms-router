package com.softavail.commsrouter.api.interfaces;

import com.softavail.commsrouter.api.dto.misc.PaginatedList;
import com.softavail.commsrouter.api.dto.model.RouterObjectId;
import com.softavail.commsrouter.api.exception.CommsRouterException;

import java.util.List;

/**
 * Created by @author mapuo on 04.09.17.
 */
public interface RouterObjectService<ENTITYT extends RouterObjectId> {

  ENTITYT get(RouterObjectId routerObjectId) throws CommsRouterException;

  List<ENTITYT> list(String routerId) throws CommsRouterException;

  PaginatedList<ENTITYT> listPage(String routerId, int page, int perPage)
      throws CommsRouterException;

  void delete(RouterObjectId routerObjectId) throws CommsRouterException;

}