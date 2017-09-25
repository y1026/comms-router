/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */

package com.softavail.commsrouter.jpa;

import com.softavail.commsrouter.domain.Agent;
import com.softavail.commsrouter.domain.Plan;
import com.softavail.commsrouter.domain.Queue;
import com.softavail.commsrouter.domain.Router;
import com.softavail.commsrouter.domain.Task;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author ikrustev
 */
public class JpaDbFacade {

  public final JpaTransactionManager transactionManager;

  public final GenericRepository<Router> router;
  public final RouterObjectRepository<Queue> queue;
  public final RouterObjectRepository<Plan> plan;
  public final RouterObjectRepository<Agent> agent;
  public final RouterObjectRepository<Task> task;

  public JpaDbFacade() {

    EntityManagerFactory emf =
        Persistence.createEntityManagerFactory("com.softavail.comms-router.core-pu");

    transactionManager = new JpaTransactionManager(emf);

    this.router = new RouterRepository(transactionManager);
    this.queue = new QueueRepository(transactionManager);
    this.plan = new PlanRepository(transactionManager);
    this.agent = new AgentRepository(transactionManager);
    this.task = new TaskRepository(transactionManager);

  }

  public void close() {
    transactionManager.close();
  }

}
