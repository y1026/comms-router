/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */

package com.softavail.commsrouter.jpa;

import com.softavail.commsrouter.domain.Agent;

/**
 * @author ikrustev
 */
public class AgentRepository extends RouterObjectRepository<Agent> {

  public AgentRepository(JpaTransactionManager transactionManager) {
    super(transactionManager);
  }

}
