/*
 * Copyright 2018 SoftAvail Inc.
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

package com.softavail.commsrouter.eval;

import com.softavail.commsrouter.api.exception.ExpressionException;
import cz.jirutka.rsql.parser.ast.Node;

/**
 *
 * @author vladislav
 */
public interface RsqlValidator {
  void validate(Node node, String routerRef) throws ExpressionException;
}
