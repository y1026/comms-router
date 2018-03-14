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

import com.softavail.commsrouter.api.exception.EvaluatorException;
import com.softavail.commsrouter.domain.Attribute;
import com.softavail.commsrouter.domain.AttributeGroup;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;


/**
 *
 * @author Vladislav Todorov
 * @param <Boolean>
 * @param <String>
 */
public class EvalRSQLVisitor implements RSQLVisitor<Boolean, AttributeGroup> {

    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(EvalRSQLVisitor.class);

    @Override
    public Boolean visit(AndNode andNode, AttributeGroup attributeGroup) {

        Boolean result = true;
        Iterator<Node> nodes = andNode.iterator();

        while (result && nodes.hasNext()) {
            result = result && nodes.next().accept(this, attributeGroup);
        }

        return result;
    }

    @Override
    public Boolean visit(OrNode orNode, AttributeGroup attributeGroup) {

        Boolean result = false;
        Iterator<Node> nodes = orNode.iterator();

        while (!result && nodes.hasNext()) {
            result = result || nodes.next().accept(this, attributeGroup);
        }

        return result;
    }

    @Override
    public Boolean visit(ComparisonNode comparisonNode, AttributeGroup attributeGroup) {

        Boolean result = null;
        try {
            result = comapre(comparisonNode.getSelector(), comparisonNode.getOperator(), comparisonNode.getArguments(), attributeGroup);
        } catch (EvaluatorException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }

        return result;
    }

    private Boolean comapre(String selector, ComparisonOperator comparisonOperator, List<String> arguments, AttributeGroup attributeGroup) throws EvaluatorException {

        List<Attribute> attributes = attributeGroup.getAttributes(selector);
        String operator = comparisonOperator.getSymbol();

        if (attributes.isEmpty()) {
          switch (operator) {
            case "==":
            case "=gt=":
            case ">":
            case "=ge=":
            case ">=":
            case "=lt=":
            case "<":
            case "=le=":
            case "<=":
                assertSingleArgument(operator, arguments);
                return false;
            case "!=":
                assertSingleArgument(operator, arguments);
                return true;
            case "=in=":
                return false;
            case "=out=":
                return true;
            default:
                throw new EvaluatorException("Unsupported operator: " + comparisonOperator.getSymbol());
          }
        }

        Attribute.Type type = attributes.get(0).getType();

        switch (operator) {
            case "==":
                return getValues(attributes).contains(parseSingleArgument(operator, arguments, type));
            case "!=":
                return !getValues(attributes).contains(parseSingleArgument(operator, arguments, type));
            case "=gt=":
            case ">":
                return compareSingleElement(attributes, arguments, operator) > 0;
            case "=ge=":
            case ">=":
                return compareSingleElement(attributes, arguments, operator) >= 0;
            case "=lt=":
            case "<":
                return compareSingleElement(attributes, arguments, operator) < 0;
            case "=le=":
            case "<=":
                return compareSingleElement(attributes, arguments, operator) <= 0;
            case "=in=":
                assertSingleAttribute(operator, attributes);
                return parseArguments(arguments, type).contains(attributes.get(0).getValue());
            case "=out=":
                assertSingleAttribute(operator, attributes);
                return !parseArguments(arguments, type).contains(attributes.get(0).getValue());
            default:
                throw new EvaluatorException("Unsupported operator: " + comparisonOperator.getSymbol());
        }
    }

    private List<Object> getValues(List<Attribute> attributes) {
      return attributes.stream().map(a -> a.getValue()).collect(Collectors.toList());
    }

    private Object parseArgument(String argument, Attribute.Type type) {
        switch (type) {
            case STRING:
                return argument;
            case DOUBLE:
                return Double.parseDouble(argument);
            case BOOLEAN:
                return Boolean.parseBoolean(argument);
        }
        throw new RuntimeException("Unexpected argument type");
    }

    private Object parseSingleArgument(String operator, List<String> arguments, Attribute.Type type) throws EvaluatorException {
      assertSingleArgument(operator, arguments);
      return parseArgument(arguments.get(0), type);
    }

    private List<Object> parseArguments(List<String> arguments, Attribute.Type type) {
        return arguments.stream()
                .sequential()
                .map(argument -> parseArgument(argument, type))
                .collect(Collectors.toList());
    }

    private int compareSingleElement(List<Attribute> attributes, List<String> arguments, String operator) throws EvaluatorException {
      assertSingleArgument(operator, arguments);
      assertSingleAttribute(operator, attributes);
      return compare(attributes.get(0), arguments.get(0));
    }

    private int compare(Attribute attribute, String argument) {
        switch (attribute.getType()) {
            case STRING:
                return attribute.getStringValue().compareTo(argument);
            case DOUBLE:
                return attribute.getDoubleValue().compareTo(Double.parseDouble(argument));
            case BOOLEAN:
                return attribute.getBooleanValue().compareTo(Boolean.parseBoolean(argument));
            default:
                throw new RuntimeException("Unexpected attribute type " + attribute.getType() + " for " + attribute.getName()
                  + "in " + attribute.getAttributeGroup().getId());
        }
    }

    private void assertSingleArgument(String operator, List<String> arguments) throws EvaluatorException {
        if (arguments.size() != 1) {
            throw new EvaluatorException("Invalid arguments number for operator '" + operator + "'. Expected 1 but found " + arguments.size());
        }
    }

    private void assertSingleAttribute(String operator, List<Attribute> attributes) throws EvaluatorException {
        if (attributes.size() != 1) {
            throw new EvaluatorException("Invalid attributes number for operator '" + operator + "'. Expected no more than 1 but found " + attributes.size());
        }
    }
}
