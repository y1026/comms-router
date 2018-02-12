/*
 * Copyright 2017 SoftAvail, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.softavail.api.test;

import com.softavail.commsrouter.test.api.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.*;
import static io.restassured.RestAssured.*;
import io.restassured.response.ValidatableResponse;

import com.softavail.commsrouter.api.dto.arg.*;
import com.softavail.commsrouter.api.dto.model.*;
import com.softavail.commsrouter.test.api.*;


import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/** Unit test for simple App. */
public class QueryParamsTest extends BaseTest {

  @Test
  public void crudRouter() {
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    ApiObjectRef ref = r.create(new CreateRouterArg());
    ApiRouter api_r = new ApiRouter(state);
    
    String last = api_r.list("").extract().headers().get("Link").toString(); // it looks like Link=<routers/routers?page_num=36>; rel="last"
    String lastPage = last.substring("Link=<routers/routers?page_num=".length(),last.length() - ">; rel=\"last\"".length());
    assertThat( api_r.list("page_num="+lastPage).extract().asString(), containsString(ref.getRef()));

    r.delete();
    r.delete();
  }

  /*  @Test
  public void crdQueue() {
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    r.create(new CreateRouterArg());

    Queue q = new Queue(state);
    ApiObjectRef ref = q.create(new CreateQueueArg.Builder().predicate("1==1").build());
    QueueDto queue = q.get();
    assertThat(queue.getDescription(), nullValue());

    ApiQueue api_q = new ApiQueue(state);
    String last = api_q.list(state.get(CommsRouterResource.ROUTER)).extract().headers().get("Link").toString(); // it looks like Link=<routers/routers?page_num=36>; rel="last"
    String lastPage = api_q.getLastPage(last);
    assertThat( api_q.list(state.get(CommsRouterResource.ROUTER), "page_num="+lastPage).extract().asString(), containsString(ref.getRef()));

    q.delete();
    r.delete();
  }

  @Test
  public void crdPlan() {
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    Plan p = new Plan(state);
    ApiObjectRef ref = r.create(new CreateRouterArg());
    Queue q = new Queue(state);
    ApiObjectRef queueRef = q.create(new CreateQueueArg.Builder().predicate("true").build());
    CreatePlanArg arg = new CreatePlanArg();
    RouteDto defaultRoute = new RouteDto();
    defaultRoute.setQueueRef(queueRef.getRef());
    arg.setDefaultRoute(defaultRoute);
    ref = p.create(arg);

    ApiPlan api_p = new ApiPlan(state);
    String last = api_p.list(state.get(CommsRouterResource.ROUTER)).extract().headers().toString(); // it looks like Link=<routers/routers?page_num=36>; rel="last"
    
    String lastPage = api_p.getLastPage(last);
    assertThat( api_p.list(state.get(CommsRouterResource.ROUTER), "page_num="+lastPage).extract().asString(), containsString(ref.getRef()));

    p.deleteResponse().statusCode(204);
    r.deleteResponse()
      .statusCode(500)
      .body(
            "error.description",
            equalTo(
                    "Cannot delete or update 'router' as there is record in 'queue' that refer to it."));
  }

  @Test
  public void crdAgent() {
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    ApiObjectRef ref = r.create(new CreateRouterArg());
    Agent a = new Agent(state);
    CreateAgentArg arg = new CreateAgentArg();
    ref = a.create(arg);
    AgentDto resource = a.get();
    assertThat(resource.getCapabilities(), nullValue());

    ApiAgent api_a = new ApiAgent(state);
    String last = api_a.list(state.get(CommsRouterResource.ROUTER)).extract().headers().get("Link").toString(); // it looks like Link=<routers/routers?page_num=36>; rel="last"
    String lastPage = api_a.getLastPage(last);
    assertThat( api_a.list(state.get(CommsRouterResource.ROUTER),"page_num="+lastPage).extract().asString(), containsString(ref.getRef()));

    a.delete();
    r.delete();
  }

  @Test
  public void crdTask() throws MalformedURLException {
    HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
    Router r = new Router(state);
    ApiObjectRef ref = r.create(new CreateRouterArg());
    Queue q = new Queue(state);
    ApiObjectRef queueRef = q.create(new CreateQueueArg.Builder().predicate("1==1").build());
    Task t = new Task(state);
    CreateTaskArg arg = new CreateTaskArg();
    arg.setCallbackUrl(new URL("http://example.com"));
    arg.setQueueRef(queueRef.getRef());
    ref = t.create(arg);
    TaskDto resource = t.get();
    assertThat(resource.getRequirements(), nullValue());

    ApiTask api_t = new ApiTask(state);
    String last = api_t.list(state.get(CommsRouterResource.ROUTER)).extract().headers().get("Link").toString(); // it looks like Link=<routers/routers?page_num=36>; rel="last"
    String lastPage = api_t.getLastPage(last);
    assertThat( api_t.list(state.get(CommsRouterResource.ROUTER),"page_num="+lastPage).extract().asString(), containsString(ref.getRef()));

    t.delete();
    q.delete();
    r.delete();
  }
  */
}