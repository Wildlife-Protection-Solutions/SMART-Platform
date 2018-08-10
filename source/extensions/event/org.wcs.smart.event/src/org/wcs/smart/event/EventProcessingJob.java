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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.event.filter.AttributeFilter;
import org.wcs.smart.event.filter.BooleanFilter;
import org.wcs.smart.event.filter.BracketFilter;
import org.wcs.smart.event.filter.CategoryAttributeFilter;
import org.wcs.smart.event.filter.CategoryFilter;
import org.wcs.smart.event.filter.IFilter;
import org.wcs.smart.event.filter.NotFilter;
import org.wcs.smart.event.filter.Operator;
import org.wcs.smart.event.filter.ParsedFilter;
import org.wcs.smart.event.internal.Messages;
import org.wcs.smart.event.model.EActionEvent;
import org.wcs.smart.event.model.IActionType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.util.SharedUtils;

/**
 * Job for processing observation events. There is a single job
 * accessed through the getInstance() function.
 * 
 * @author Emily
 *
 */
public class EventProcessingJob extends Job {

	private static EventProcessingJob INSTANCE = null;
	
	public static synchronized EventProcessingJob getInstance() {
		if (INSTANCE == null) INSTANCE = new EventProcessingJob();
		return INSTANCE;
	}
	
	private static final String OPEN_BRACKET = "("; //$NON-NLS-1$
	private static final String CLOSE_BRACKET = ")"; //$NON-NLS-1$
	
	private List<EActionEvent> cachedEvents = null;
	private List<WaypointObservation> observations = Collections.synchronizedList(new ArrayList<>());
	
	private EventProcessingJob() {
		super(Messages.EventProcessingJob_JobName);
	}

	public void addObservation(WaypointObservation observation) {
		synchronized(observations) {
			boolean schedule = observations.isEmpty();
			observations.add(observation);
			if (schedule) schedule();
		}
	}
		
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		while(!observations.isEmpty()) {
			monitor.setTaskName(Messages.EventProcessingJob_RemainingTaskLabel + observations.size());
			WaypointObservation o = observations.remove(0);
			for(EActionEvent event : getEventActions()) {
				if (!event.isEnabled()) continue;
				try {
					processEvent(event, o);
				}catch (Exception ex) {
					EventPlugIn.displayLog(ex.getMessage(), ex);
				}
			}
		}
		return Status.OK_STATUS;
	}
	
	private void processEvent(EActionEvent event, WaypointObservation o) throws Exception{
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
				return;
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
			IActionType actionType = ActionTypeManager.INSTANCE.getActionType(event.getAction().getActionTypeKey());
			actionType.performAction(event.getAction(),event.getFilter(), o, Locale.getDefault());
		}
		
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
						throw new Exception(Messages.EventProcessingJob_InvalidOpException + operator.toString());
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
				throw new Exception(Messages.EventProcessingJob_InvalidOpException + operator.toString());
			}
		}
		if (valueStack.size() != 1) throw new Exception(Messages.EventProcessingJob_FilterEvalError);
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
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
					Date date1 = format.parse( afilter.getValue().toString() );
					Date date2 = format.parse( afilter.getValue2().toString() );
					Date dvalue = attributeValue.getDateValue();
					boolean isbetween = ( (SharedUtils.isSameDate(date1, dvalue) || dvalue.after(date1)) && (SharedUtils.isSameDate(date2, dvalue) || dvalue.before(date2)));
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
						value = attributeValue.getStringValue().trim().toUpperCase().matches(".*"+ strvalue.toUpperCase() + ".*"); //$NON-NLS-1$ //$NON-NLS-2$
					}else if (afilter.getOperator() == Operator.STR_NOTCONTAINS) {
						value = !attributeValue.getStringValue().trim().toUpperCase().matches(".*"+ strvalue.toUpperCase() + ".*");  //$NON-NLS-1$ //$NON-NLS-2$
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
	
	/**
	 * Clears the trigger cache
	 */
	public synchronized void reset(){
		cachedEvents = null;
	}
	
	private synchronized List<EActionEvent> getEventActions(){
		if (cachedEvents != null) return cachedEvents;
		
		cachedEvents = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			cachedEvents.addAll( QueryFactory.buildQuery(session, EActionEvent.class, 
					new Object[] {"action.conservationArea", SmartDB.getCurrentConservationArea()}).list() ); //$NON-NLS-1$
			cachedEvents.forEach(evt->{
				evt.getAction().getParameters().forEach(p->p.getId().getParameterKey());
				evt.getFilter().getFilterString();
			});
		}
		return cachedEvents;
	}

}
