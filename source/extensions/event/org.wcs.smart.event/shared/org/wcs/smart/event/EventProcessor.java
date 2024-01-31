/*
 * Copyright (C) 2016 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.event;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.event.filter.ParsedFilter;
import org.wcs.smart.event.model.EActionEvent;
import org.wcs.smart.event.model.IActionTypeExecutor;
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.filter.BooleanFilter;
import org.wcs.smart.filter.BracketFilter;
import org.wcs.smart.filter.CategoryAttributeFilter;
import org.wcs.smart.filter.CategoryFilter;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.NotFilter;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

/**
 * 
 */
public enum EventProcessor {

	INSTANCE;
	
	private static final String OPEN_BRACKET = "("; //$NON-NLS-1$
	private static final String CLOSE_BRACKET = ")"; //$NON-NLS-1$

	public Object processEvent(EActionEvent event, WaypointObservation o, Employee employee, Session session) throws Exception{
		//lets first check the waypoint source filter
		ParsedFilter filter = event.getFilter().getParsedFilter();
		
		if (filter.getSources() != null) {
			boolean found = false;
			for (IWaypointSource src : filter.getSources()) {
				if (src.getKey().equalsIgnoreCase(o.getWaypoint().getSourceId())) {
					found = true;
					break;
				}
			}
			if (!found) {
				//does not match waypoint source filter so don't do anything else
				return null;
			}
		}
		boolean ok = true;
		if (filter.getFilter() != null) {
			List<Object> equation = new ArrayList<>();
			processFilter(filter.getFilter(), equation, o);
			StringBuilder sb = new StringBuilder();
			for (Object x : equation) {
				sb.append(" "); //$NON-NLS-1$
				sb.append( x.toString() );
				sb.append(" "); //$NON-NLS-1$
			}
			ok = evaluate(equation);
		}
		
		if (ok) {
			//execute action
			IActionTypeExecutor actionType = ActionExecutorManager.INSTANCE.getActionType(event.getAction().getActionTypeKey());
			return actionType.performAction(event.getAction(),event.getFilter(), o, Locale.getDefault(), employee, session);
		}
		return null;
		
	}

	private boolean evaluate(List<Object> equation) throws Exception {
		Stack<Object> valueStack = new Stack<>();
		Stack<Object> opStack = new Stack<>();
		for (Object x : equation) {
			if (x instanceof Boolean) valueStack.push(x);
			if (x == OPEN_BRACKET) {
				opStack.push(OPEN_BRACKET);
			}else if (x == CLOSE_BRACKET) {
				while(opStack.peek() != OPEN_BRACKET) {
					Object operator = opStack.pop();
					Boolean v1 = (Boolean) valueStack.pop();
					Boolean v2 = (Boolean) valueStack.pop();
					if (operator == Operator.AND) {
						valueStack.push(v1 && v2);
					}else if (operator == Operator.OR) {
						valueStack.push(v1 || v2);
					}else {
						throw new Exception("Invalid operator: " + operator.toString());
					}
				}
				opStack.pop();
			}else if (x instanceof Operator) {
				opStack.push(x);
			}
		}
		while(!opStack.isEmpty()) {
			Object operator = opStack.pop();
			Boolean v1 = (Boolean) valueStack.pop();
			Boolean v2 = (Boolean) valueStack.pop();
			if (operator == Operator.AND) {
				valueStack.push(v1 && v2);
			}else if (operator == Operator.OR) {
				valueStack.push(v1 || v2);
			}else {
				throw new Exception("Invalid operator: " + operator.toString());
			}
		}
		if (valueStack.size() != 1) throw new Exception("Evaluation error");
		Boolean isok = (Boolean)valueStack.pop();
		return isok;
	}
	
	
	private void processFilter(IFilter filter, List<Object> equation, WaypointObservation obs) throws Exception {
		if (filter instanceof BooleanFilter) {
			processFilter(((BooleanFilter) filter).getFilter1(), equation, obs);
			equation.add(((BooleanFilter) filter).getOperator());
			processFilter(((BooleanFilter) filter).getFilter2(), equation, obs);
			
		}else if (filter instanceof NotFilter) {
			equation.add(Operator.NOT);
			processFilter(((NotFilter) filter).getFilter(), equation, obs);
			
		}else if (filter instanceof BracketFilter) {
			equation.add(OPEN_BRACKET);
			processFilter(((BracketFilter) filter).getFilter(), equation, obs);
			equation.add(CLOSE_BRACKET);
			
		}else if (filter instanceof AttributeFilter) {
			AttributeFilter afilter = (AttributeFilter)filter;
			String attributeKey = afilter.getAttributeKey();
			WaypointObservationAttribute attributeValue = null;
			for (WaypointObservationAttribute att : obs.getAttributes()) {
				if (att.getAttribute().getKeyId().equalsIgnoreCase(attributeKey.trim())) {
					attributeValue = att;
					break;
				}
			}
			if (attributeValue != null) {
				switch(attributeValue.getAttribute().getType()) {
				case BOOLEAN:
					equation.add (attributeValue.getNumberValue() >= 0.5) ;
					break;
					
				case DATE:
					DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //$NON-NLS-1$
					LocalDate date1 = LocalDate.parse(afilter.getValue().toString(), format );
					LocalDate date2 = LocalDate.parse(afilter.getValue2().toString(), format );
					LocalDate dvalue = attributeValue.getDateValue();
					boolean isbetween = ( date1.isEqual(dvalue) || dvalue.isAfter(date1)) && (date2.isEqual(dvalue) || dvalue.isBefore(date2));
					if (afilter.getOperator() == Operator.NOT_BETWEEN) {
						isbetween = !isbetween;
					}
					equation.add(isbetween);
					break;
					
				case LIST:
					String listItemKey = afilter.getValue().toString();
					if (attributeValue.getAttributeListItem() != null && attributeValue.getAttributeListItem().getKeyId().equalsIgnoreCase(listItemKey.trim())) {
						equation.add(Boolean.TRUE);
					}else {
						equation.add(Boolean.FALSE);
					}
					break;
				case MLIST:
					String[] listItemKeys = afilter.getValue().toString().split(AttributeFilter.MLIST_SEPERATOR);
					Set<String> keys = new HashSet<>();
					for(String s : listItemKeys) keys.add(s);
					
					Set<String> values = attributeValue.getAttributeListItems().stream().map(e->e.getAttributeListItem().getKeyId()).collect(Collectors.toSet());
					
					boolean lvalue = false;
					if (afilter.getOperator() == Operator.OR) {
						//must find one of listitemkeys
						for (String k : values) {
							if (keys.contains(k)) {
								lvalue = true;
								break;
							}
						}
					}else if (afilter.getOperator() == Operator.EXACT) {
						if (keys.size() == values.size()) {
							lvalue = true;
							for (String x : values) {
								if (!keys.contains(x)) {
									lvalue = false;
									break;
								}
							}
						}
					}else if (afilter.getOperator() == Operator.AND) {
						//every key must be in value but we don't care if we have more 
						//values
						lvalue = true;
						for (String x : keys) {
							if (!values.contains(x)) {
								lvalue = false;
								break;
							}
						}
					}
					equation.add(lvalue);
					break;
				case NUMERIC:
					boolean nvalue = false;
					double filterValue = (Double) afilter.getValue();
					double observationValue = attributeValue.getNumberValue();
					if (afilter.getOperator() == Operator.EQUALS) {
						nvalue = observationValue == filterValue;
					}else if (afilter.getOperator() == Operator.NOTEQUALS) {
						nvalue = observationValue != filterValue;
					}else if (afilter.getOperator() == Operator.LESSTHAN) {
						nvalue = observationValue < filterValue;
					}else if (afilter.getOperator() == Operator.LESSTHANEQUALS) {
						nvalue = observationValue <= filterValue;
					}else if (afilter.getOperator() == Operator.GREATERTHAN) {
						nvalue = observationValue > filterValue;
					}else if (afilter.getOperator() == Operator.GREATERTHANEQUALS) {
						nvalue = observationValue >= filterValue;
					}
					equation.add(nvalue);
					break;
					
				case TEXT:
					boolean value = false;
					String strvalue = afilter.getValue().toString();
					if (afilter.getOperator() == Operator.STR_EQUALS) {
						value = strvalue.equalsIgnoreCase(attributeValue.getStringValue().trim());
					}else if (afilter.getOperator() == Operator.STR_CONTAINS) {
						Pattern p = Pattern.compile( ".*" + Pattern.quote(strvalue) + ".*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE); //$NON-NLS-1$ //$NON-NLS-2$
						value = p.matcher(attributeValue.getStringValue()).matches();
					}else if (afilter.getOperator() == Operator.STR_NOTCONTAINS) {
						Pattern p = Pattern.compile( ".*" + Pattern.quote(strvalue) + ".*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE); //$NON-NLS-1$ //$NON-NLS-2$
						value = !p.matcher(attributeValue.getStringValue()).matches();
					}
					equation.add(value);
					break;
					
				case TREE:
					String treeNodeKey = afilter.getValue().toString();
					if (attributeValue.getAttributeTreeNode() != null && attributeValue.getAttributeTreeNode().getHkey().equalsIgnoreCase(treeNodeKey.trim())) {
						equation.add(Boolean.TRUE);
					}else {
						equation.add(Boolean.FALSE);
					}
					break;
				case POLYGON:
				case LINE:
					double obsvalue = attributeValue.getNumberValue();					
					if (afilter.getGeometryProperty() == AttributeFilter.GeometryProperty.AREA) {
						obsvalue = attributeValue.getNumberValue2();
					}
					boolean gvalue = false;
					Double gFilterValue = (Double) afilter.getValue();
					if (afilter.getOperator() == Operator.EQUALS) {
						gvalue = obsvalue == gFilterValue;
					}else if (afilter.getOperator() == Operator.NOTEQUALS) {
						gvalue = obsvalue != gFilterValue;
					}else if (afilter.getOperator() == Operator.LESSTHAN) {
						gvalue = obsvalue < gFilterValue;
					}else if (afilter.getOperator() == Operator.LESSTHANEQUALS) {
						gvalue = obsvalue <= gFilterValue;
					}else if (afilter.getOperator() == Operator.GREATERTHAN) {
						gvalue = obsvalue > gFilterValue;
					}else if (afilter.getOperator() == Operator.GREATERTHANEQUALS) {
						gvalue = obsvalue >= gFilterValue;
					}
					equation.add(gvalue);
					break;
				}
				
			}else {
				equation.add(Boolean.FALSE);
			}
			
		}else if (filter instanceof CategoryAttributeFilter) {
			equation.add(OPEN_BRACKET);
			processFilter(((CategoryAttributeFilter) filter).getCategoryFilter(), equation, obs);
			equation.add(Operator.AND);
			processFilter(((CategoryAttributeFilter) filter).getAttributeFilter(), equation, obs);
			equation.add(CLOSE_BRACKET);
			
		}else if (filter instanceof CategoryFilter) {
			String filterCategory = ((CategoryFilter) filter).getCategoryKey().toLowerCase();
			String obsCategory = obs.getCategory().getHkey().toLowerCase();
			if (obsCategory.matches(filterCategory + ".*")) { //$NON-NLS-1$
				equation.add(Boolean.TRUE);
			}else {
				equation.add(Boolean.FALSE);
			}		
		}
	}


}
