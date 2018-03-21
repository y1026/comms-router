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

package com.softavail.api.test;

import com.softavail.commsrouter.test.api.Agent;
import com.softavail.commsrouter.test.api.CommsRouterResource;
import com.softavail.commsrouter.test.api.Plan;
import com.softavail.commsrouter.test.api.Router;
import com.softavail.commsrouter.test.api.Task;
import com.softavail.commsrouter.test.api.Queue;

import org.junit.*;

import java.net.MalformedURLException;
import java.net.URL;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;

import java.util.Arrays;
import java.util.Collections;

import com.softavail.commsrouter.api.dto.arg.CreateAgentArg;
import com.softavail.commsrouter.api.dto.arg.CreatePlanArg;
import com.softavail.commsrouter.api.dto.arg.CreateRouterArg;
import com.softavail.commsrouter.api.dto.arg.CreateTaskArg;
import com.softavail.commsrouter.api.dto.arg.CreateQueueArg;
import com.softavail.commsrouter.api.dto.model.attribute.AttributeGroupDto;
import com.softavail.commsrouter.api.dto.model.ApiObjectRef;
import com.softavail.commsrouter.api.dto.model.AgentDto;
import com.softavail.commsrouter.api.dto.model.QueueDto;
import com.softavail.commsrouter.api.dto.model.PlanDto;
import com.softavail.commsrouter.api.dto.model.RouteDto;
import com.softavail.commsrouter.api.dto.model.RuleDto;
import com.softavail.commsrouter.api.dto.model.RouterDto;
import com.softavail.commsrouter.api.dto.model.TaskDto;

import java.util.HashMap;

/** Unit test for simple App. */
public class BaseRouterTest extends BaseTest{

  @Test
  public void createRouter() {
    // best case
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    String description = "Router description";
    String name = "router-name";
    CreateRouterArg routerArg = new CreateRouterArg();
    routerArg.setDescription(description);
    routerArg.setName(name);
    ApiObjectRef ref = r.create(routerArg);
    RouterDto router = r.get();
    assertThat(router.getName(), is(name));
    assertThat(router.getDescription(), is(description));
    r.delete();
  }

  @Test
  public void newRouterWithSpecifiedId() {
    // create router using specified id
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    String description = "Router description";
    String name = "router-name";
    String routerId = "newRouterId";
    CreateRouterArg routerArg = new CreateRouterArg();
    routerArg.setDescription(description);
    routerArg.setName(name);
    state.put(CommsRouterResource.ROUTER, routerId);
    ApiObjectRef ref = r.replace(routerArg);

    RouterDto router = r.get();
    assertThat(router.getName(), is(name));
    assertThat(router.getDescription(), is(description));
    assertThat(router.getRef(), is(routerId));

    r.delete();
  }

  @Test
  public void replaceRouter() {
    // replace existing router
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    String description = "Router description";
    String name = "router-name";
    CreateRouterArg routerArg = new CreateRouterArg();
    routerArg.setDescription(description);
    routerArg.setName(name);
    ApiObjectRef ref = r.create(routerArg);
    RouterDto router = r.get();
    assertThat(router.getName(), is(name));
    assertThat(router.getDescription(), is(description));

    ApiObjectRef ref1 = r.replace(new CreateRouterArg()); // replace with null values
    assertThat(ref.getRef(), is(ref1.getRef()));
    router = r.get();
    assertThat(router.getName(), nullValue());
    assertThat(router.getDescription(), nullValue());

    r.delete();
  }

  // @SuppressWarnings("unchecked")
  @Test
  public void replaceRouterRouterResourcesAreThere() {
    // replace existing router and check that Queue is here after the replacement
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    String description = "Router description";
    String name = "router-name";
    CreateRouterArg routerArg = new CreateRouterArg();
    routerArg.setDescription(description);
    routerArg.setName(name);
    ApiObjectRef ref = r.create(routerArg);
    RouterDto router = r.get();
    assertThat(router.getName(), is(name));
    assertThat(router.getDescription(), is(description));

    Queue q = new Queue(state);
    ApiObjectRef qRef = q.create(new CreateQueueArg.Builder().predicate("1==1").build());

    // replace with null values
    r.replaceResponse(new CreateRouterArg())
        .statusCode(500)
        .body(
            "error.description",
            equalTo(
                "Cannot delete or update 'router' as there is record in 'queue' that refer to it."));

    // check that queue is still there
    QueueDto queue = q.get();
    assertThat(queue.getDescription(), nullValue());
    assertThat(q.list(), hasItems(hasProperty("ref", is(qRef.getRef()))));

    q.delete();
    r.delete();
  }

  @Test
  public void updateRouterFields() {
    // update values of the existing router
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    String description = "Router description";
    String name = "router-name";
    CreateRouterArg routerArg = new CreateRouterArg();
    routerArg.setDescription(description);
    routerArg.setName(name);
    ApiObjectRef ref = r.create(routerArg);
    RouterDto router = r.get();
    assertThat(router.getName(), is(name));
    assertThat(router.getDescription(), is(description));

    r.update(new CreateRouterArg()); // should not change values
    router = r.get();
    assertThat(router.getName(), is(name));
    assertThat(router.getDescription(), is(description));

    routerArg.setDescription("changed");
    routerArg.setName(null);
    r.update(routerArg); // should change only description
    router = r.get();
    assertThat(router.getName(), is(name));
    assertThat(router.getDescription(), is("changed"));

    routerArg.setDescription(null);
    routerArg.setName("changedName");
    r.update(routerArg); // should change only description
    router = r.get();
    assertThat(router.getName(), is("changedName"));
    assertThat(router.getDescription(), is("changed"));

    r.delete();
  }

  @Test
  public void doubleDeleteRouter() {
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    String description = "Router description";
    String name = "router-name";
    CreateRouterArg routerArg = new CreateRouterArg();
    routerArg.setDescription(description);
    routerArg.setName(name);
    ApiObjectRef ref = r.create(routerArg);
    RouterDto router = r.get();
    assertThat(router.getName(), is(name));
    assertThat(router.getDescription(), is(description));
    r.delete();
    r.delete();
  }
  @Test
  public void twoRoutersWithEqualQueueIds() {
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    ApiObjectRef router1 = r.create(new CreateRouterArg());
    ApiObjectRef router2 = r.create(new CreateRouterArg());
    Queue q = new Queue(state);

    String description1 = "queue1 description";
    String predicate1 = "1==1";
    String description2 = "queue2 description";
    String predicate2 = "2==2";
    String queueRef = "Queue-ref";

    state.put(CommsRouterResource.QUEUE, queueRef);

    state.put(CommsRouterResource.ROUTER, router1.getRef());
    q.replace(new CreateQueueArg.Builder()
              .description(description1)
              .predicate(predicate1)
              .build());

    state.put(CommsRouterResource.ROUTER, router2.getRef());
    q.replace(new CreateQueueArg.Builder()
              .description(description2)
              .predicate(predicate2)
              .build());

    state.put(CommsRouterResource.ROUTER, router1.getRef());
    QueueDto queue = q.get();
    assertThat(queue.getPredicate(), is(predicate1));
    assertThat(queue.getDescription(), is(description1));
    assertThat(queue.getRef(), is(queueRef));

    state.put(CommsRouterResource.ROUTER, router2.getRef());
    queue = q.get();
    assertThat(queue.getPredicate(), is(predicate2));
    assertThat(queue.getDescription(), is(description2));
    assertThat(queue.getRef(), is(queueRef));

  }

  @Test
  public void twoRoutersWithEqualAgentIds() {
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    ApiObjectRef router1 = r.create(new CreateRouterArg());
    ApiObjectRef router2 = r.create(new CreateRouterArg());
    Agent a = new Agent(state);

    String description1 = "agent1d";
    String name1 = "agent1n";
    String description2 = "agent2d";
    String name2 = "agent2n";
    String agentRef = "Agent-ref";

    state.put(CommsRouterResource.AGENT, agentRef);

    state.put(CommsRouterResource.ROUTER, router1.getRef());
    a.replace(new CreateAgentArg.Builder(name1)
              .description(description1)
              .build());

    state.put(CommsRouterResource.ROUTER, router2.getRef());
    a.replace(new CreateAgentArg.Builder(name2)
              .description(description2)
              .build());

    state.put(CommsRouterResource.ROUTER, router1.getRef());
    AgentDto agent = a.get();
    assertThat(agent.getName(), is(name1));
    assertThat(agent.getDescription(), is(description1));
    assertThat(agent.getRef(), is(agentRef));

    state.put(CommsRouterResource.ROUTER, router2.getRef());
    agent = a.get();
    assertThat(agent.getName(), is(name2));
    assertThat(agent.getDescription(), is(description2));
    assertThat(agent.getRef(), is(agentRef));
  }

  @Test
  public void twoRoutersWithEqualTaskIds() throws MalformedURLException {
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    ApiObjectRef router1 = r.create(new CreateRouterArg());
    ApiObjectRef router2 = r.create(new CreateRouterArg());
    Task t = new Task(state);
    Queue q = new Queue(state);

    String description1 = "task1d";
    String name1 = "task1n";
    String description2 = "task2d";
    String name2 = "task2n";
    String taskRef = "Task-ref";

    state.put(CommsRouterResource.TASK, taskRef);

    state.put(CommsRouterResource.ROUTER, router1.getRef());
    String queueRef1 = q.create(new CreateQueueArg.Builder().description("queue description").predicate("1==1").build()).getRef();
      
    t.replace(new CreateTaskArg.Builder()
              .callback(new URL("http://localhost:8080"))
              .requirements(new AttributeGroupDto())
              .queue(queueRef1)
              .build());

    state.put(CommsRouterResource.ROUTER, router2.getRef());
    String queueRef2 = q.create(new CreateQueueArg.Builder().description("queue description").predicate("1==1").build()).getRef();
      
    t.replace(new CreateTaskArg.Builder()
              .callback(new URL("http://localhost:8080"))
              .requirements(new AttributeGroupDto())
              .queue(queueRef2)
              .build());

    state.put(CommsRouterResource.ROUTER, router1.getRef());
    TaskDto task = t.get();
    assertThat(task.getQueueRef(), is(queueRef1));


    state.put(CommsRouterResource.ROUTER, router2.getRef());
    task = t.get();
    assertThat(task.getQueueRef(), is(queueRef2));

  }

  @Test
  public void twoRoutersWithEqualPlanIds() throws MalformedURLException {
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    ApiObjectRef router1 = r.create(new CreateRouterArg());
    ApiObjectRef router2 = r.create(new CreateRouterArg());
    Plan p = new Plan(state);
    Queue q = new Queue(state);
    
    String description1 = "plan1d";
    String description2 = "plan2d";
    String planRef = "Plan-ref";

    state.put(CommsRouterResource.PLAN, planRef);

    state.put(CommsRouterResource.ROUTER, router1.getRef());
    String queueRef1 = q.create(new CreateQueueArg.Builder().description("queue description").predicate("1==1").build()).getRef();

    p.replace(new CreatePlanArg.Builder(description1)
              .rules(Collections.singletonList(new RuleDto.Builder("true")
                                               .routes(Arrays.asList(
                                                                     new RouteDto.Builder(queueRef1).timeout(1L).build(),
                                                                     new RouteDto.Builder(queueRef1).build()))
                                               .build()))
              .defaultRoute(new RouteDto.Builder(queueRef1).build())
              .build());

    state.put(CommsRouterResource.ROUTER, router2.getRef());
    String queueRef2 = q.create(new CreateQueueArg.Builder().description("queue description").predicate("1==1").build()).getRef();

    p.replace(new CreatePlanArg.Builder(description2)
              .rules(Collections.singletonList(new RuleDto.Builder("true")
                                               .routes(Arrays.asList(
                                                                     new RouteDto.Builder(queueRef2).timeout(1L).build(),
                                                                     new RouteDto.Builder(queueRef2).build()))
                                               .build()))
              .defaultRoute(new RouteDto.Builder(queueRef2).build())
              .build());

    state.put(CommsRouterResource.ROUTER, router1.getRef());
    PlanDto plan = p.get();
    assertThat(plan.getDescription(), is(description1));

    state.put(CommsRouterResource.ROUTER, router2.getRef());
    plan = p.get();
    assertThat(plan.getDescription(), is(description2));

  }
  
}
