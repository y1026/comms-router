/*
 * Copyright 2017 SoftAvail Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.softavail.commsrouter.api.service;

import com.softavail.commsrouter.api.dto.arg.CreateAgentArg;
import com.softavail.commsrouter.api.dto.arg.UpdateAgentArg;
import com.softavail.commsrouter.api.dto.model.AgentDto;
import com.softavail.commsrouter.api.dto.model.AgentState;
import com.softavail.commsrouter.api.dto.model.ApiObjectRef;
import com.softavail.commsrouter.api.dto.model.RouterObjectRef;
import com.softavail.commsrouter.api.dto.model.attribute.AttributeGroupDto;
import com.softavail.commsrouter.api.exception.BadValueException;
import com.softavail.commsrouter.api.exception.CommsRouterException;
import com.softavail.commsrouter.api.exception.InternalErrorException;
import com.softavail.commsrouter.api.exception.InvalidStateException;
import com.softavail.commsrouter.api.interfaces.AgentService;
import com.softavail.commsrouter.app.AppContext;
import com.softavail.commsrouter.domain.Agent;
import com.softavail.commsrouter.domain.AgentQueueMapping;
import com.softavail.commsrouter.domain.Queue;
import com.softavail.commsrouter.domain.Router;
import com.softavail.commsrouter.eval.CommsRouterEvaluator;
import com.softavail.commsrouter.util.Fields;
import com.softavail.commsrouter.util.Uuid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import javax.persistence.EntityManager;

/**
 * Created by @author mapuo on 04.09.17.
 */
public class CoreAgentService extends CoreRouterObjectService<AgentDto, Agent>
    implements AgentService {

  private static final Logger LOGGER = LogManager.getLogger(CoreAgentService.class);

  public CoreAgentService(AppContext app) {
    super(app, app.db.agent, app.entityMapper.agent);
  }

  @Override
  public ApiObjectRef create(CreateAgentArg createArg, String routerRef)
      throws CommsRouterException {

    RouterObjectRef routerObjectRef =
        RouterObjectRef.builder().setRef(Uuid.get()).setRouterRef(routerRef).build();

    return app.db.transactionManager.execute((EntityManager em) -> {
      return doCreate(em, createArg, routerObjectRef);
    });
  }

  @Override
  public ApiObjectRef replace(CreateAgentArg createArg, RouterObjectRef objectRef)
      throws CommsRouterException {

    return app.db.transactionManager.execute((em) -> {
      Agent agent = repository.getNoThrow(em, objectRef);
      if (agent != null) {
        if (!agent.getState().isDeleteAllowed()) {
          throw new InvalidStateException(
              "Replacing agent in state " + agent.getState() + " not allowed");
        }
        em.remove(agent);
        em.flush();
      }
      return doCreate(em, createArg, objectRef);
    });
  }

  private ApiObjectRef doCreate(EntityManager em, CreateAgentArg createArg,
      RouterObjectRef objectRef)
      throws CommsRouterException {

    Router router = getRouter(em, objectRef);
    Agent agent = new Agent(objectRef);
    agent.setRouter(router);
    agent.setAddress(createArg.getAddress());
    agent.setCapabilities(app.entityMapper.attributes.fromDto(createArg.getCapabilities()));
    agent.setState(AgentState.offline);
    em.persist(agent);
    attachQueues(em, agent, createArg.getCapabilities(), true);
    return agent.cloneApiObjectRef();
  }

  void attachQueues(EntityManager em, Agent agent, AttributeGroupDto capabilities,
      boolean isNewAgent) throws CommsRouterException {

    app.db.router.lockConfig(em, agent.getRouter().getId());

    LOGGER.info("Agent {}: attaching queues...", agent.getRef());

    int attachedQueuesCount = 0;
    CommsRouterEvaluator evaluator = app.evaluatorFactory.provide(null);
    for (Queue queue : app.db.queue.list(em, agent.getRouter().getRef())) {
      try {
        if (evaluator.init(queue.getPredicate()).evaluate(capabilities)) {

          LOGGER.info("Queue {} <=> Agent {}", queue.getRef(), agent.getRef());
          ++attachedQueuesCount;

          AgentQueueMapping mapping = new AgentQueueMapping(agent, queue);
          em.persist(mapping);

          if (isNewAgent || !queue.getAgentQueueMappings().contains(mapping)) {
            queue.getAgentQueueMappings().add(mapping);
          }
          agent.getAgentQueueMappings().add(mapping);
        } else if (!isNewAgent) {
          queue.getAgentQueueMappings().remove(new AgentQueueMapping(agent, queue));
        }
      } catch (CommsRouterException ex) {
        LOGGER.error("Agent {}: failure attaching queue {}: {}", agent.getRef(), queue.getRef(), ex,
            ex);
        throw ex;
      }
    }
    LOGGER.info("Agent {}: queues attached: {}", agent.getRef(), attachedQueuesCount);
  }

  private static class UpdateInfo {
    private static Long agentId;
    private static boolean becameReady;

    public UpdateInfo(Long agentId, boolean becameReady) {
      this.agentId = agentId;
      this.becameReady = becameReady;
    }

    Long getAgentId() {
      return agentId;
    }

    boolean agentBecameReady() {
      return becameReady;
    }

  }

  @Override
  public void update(UpdateAgentArg updateArg, RouterObjectRef objectRef)
      throws CommsRouterException {

    UpdateInfo updateInfo = updateAgent(updateArg, objectRef);
    if (updateInfo.agentBecameReady()) {
      app.taskDispatcher.dispatchAgent(updateInfo.getAgentId());
    }

  }

  private Boolean capabilitiesAreEqual(AttributeGroupDto newAttributes,
      AttributeGroupDto oldAttributes) {
    if (newAttributes == null && oldAttributes != null) {
      return false;
    }
    if (newAttributes != null && oldAttributes == null) {
      return false;
    }
    if (newAttributes == null && oldAttributes == null) {
      return true;
    }
    if (newAttributes.size() != oldAttributes.size()) {
      return false;
    }
    Set<String> keys = newAttributes.keySet();
    for (String key : keys) {
      if (!oldAttributes.containsKey(key)) {
        return false;
      }
    }
    return false; // !keys.isEmpty();
  }

  private UpdateInfo updateAgent(UpdateAgentArg updateArg, RouterObjectRef objectRef)
      throws CommsRouterException {

    if (updateArg.getState() == AgentState.busy
        || updateArg.getState() == AgentState.unavailable) {
      throw new BadValueException(
          "Setting agent state to '" + updateArg.getState() + "' not allowed");
    }

    return app.db.transactionManager.execute((em) -> {
      Agent agent = app.db.agent.get(em, objectRef);
      boolean agentBecameAvailable = updateState(agent, updateArg.getState());
      updateCapabilities(em, agent, updateArg.getCapabilities());
      Fields.update(agent::setAddress, agent.getAddress(), updateArg.getAddress());
      return new UpdateInfo(agent.getId(), agentBecameAvailable);
    });
  }

  private boolean updateState(Agent agent, AgentState newState)
      throws InvalidStateException, InternalErrorException {
    if (newState == null) {
      // no change requested
      return false;
    }
    final AgentState oldState = agent.getState();
    if (oldState == newState) {
      return false;
    }
    boolean agentBecameAvailable = false;
    switch (oldState) {
      case busy:
        throw new InvalidStateException(
            "Changing state of a busy agent is not implemented. Complete corresponding task.");
      case offline:
      case unavailable:
        // check once again just in case
        agentBecameAvailable = newState == AgentState.ready;
        break;
      case ready:
        agentBecameAvailable = false;
        break;
      default:
        throw new InternalErrorException("Unexpected agent state: " + oldState);
    }
    agent.setState(newState);
    return agentBecameAvailable;
  }

  private void updateCapabilities(EntityManager em, Agent agent, AttributeGroupDto newCapabilities)
      throws CommsRouterException {

    if (newCapabilities == null) {
      // no capabilities change requested
      return;
    }

    if (capabilitiesAreEqual(newCapabilities,
        app.entityMapper.attributes.toDto(agent.getCapabilities()))) {
      LOGGER.info("Agent {}: no capabilities change - will keep current queues", agent.getRef());
      return;
    }
    LOGGER.info("Agent {}: detaching all queues due to capabilities change", agent.getRef());

    agent.setCapabilities(app.entityMapper.attributes.fromDto(newCapabilities));
    agent.getAgentQueueMappings().clear();
    attachQueues(em, agent, newCapabilities, false);
  }

  @Override
  public void delete(RouterObjectRef routerObjectRef) throws CommsRouterException {
    app.db.transactionManager.executeVoid((em) -> {
      Agent agent = app.db.agent.get(em, routerObjectRef);
      if (!agent.getState().isDeleteAllowed()) {
        throw new InvalidStateException(
            "Deleting agent in state " + agent.getState() + " not allowed");
      }
      em.remove(agent);
    });
  }

}
