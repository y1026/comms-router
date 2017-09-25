/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */

package com.softavail.commsrouter.api.service;

import com.softavail.commsrouter.api.dto.arg.CreatePlanArg;
import com.softavail.commsrouter.api.dto.arg.UpdatePlanArg;
import com.softavail.commsrouter.api.dto.model.PlanDto;
import com.softavail.commsrouter.api.exception.CommsRouterException;
import com.softavail.commsrouter.api.interfaces.PlanService;
import com.softavail.commsrouter.app.AppContext;
import com.softavail.commsrouter.domain.Plan;
import com.softavail.commsrouter.domain.Rule;
import com.softavail.commsrouter.util.Fields;

import java.util.List;


/**
 * @author ikrustev
 */
public class CorePlanService extends CoreRouterObjectService<PlanDto, Plan> implements PlanService {

  public CorePlanService(AppContext app) {
    super(app, app.db.plan, app.entityMapper.plan);
  }

  @Override
  public PlanDto create(CreatePlanArg createArg) throws CommsRouterException {

    return app.db.transactionManager.execute((em) -> {
      Plan plan = new Plan(ensureIdPresent(createArg));
      plan.setRules(app.entityMapper.plan.toJpaRules(plan, createArg.getRules()));
      em.persist(plan);
      return entityMapper.toDto(plan);
    });
  }

  @Override
  public void update(UpdatePlanArg updateArg) throws CommsRouterException {

    app.db.transactionManager.executeVoid((em) -> {
      Plan plan = app.db.plan.get(em, updateArg);
      // List<Rule> rules = app.entityMapper.plan.toJpaRules(plan, updateArg.getRules());
      // Fields.update(plan::setRules, plan.getRules(), rules);
      Fields.update(plan::setDescription, plan.getDescription(), updateArg.getDescription());
    });
  }

}
