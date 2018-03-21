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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionEvent;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.model.EFilter;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Clones event data
 * @author Emily
 *
 */
public class ConservationAreaCloner implements IConservationAreaTemplateCloner {

	public ConservationAreaCloner() {
	}

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, 3);
		
		//clone filters
		progress.beginTask("Cloning event data",  3);
		
		HashMap<UUID, EFilter> oldToCloneFilter = new HashMap<>();
		progress.subTask("cloning filters");
		List<EFilter> filters = QueryFactory.buildQuery(engine.getSession(), EFilter.class, new Object[] {"conservationArea", engine.getTemplateCa()}).list();
		for (EFilter filter : filters) {
			EFilter clone = new EFilter();
			clone.setConservationArea(engine.getTemplateCa());
			clone.setId(filter.getId());
			clone.setFilterString(clone.getFilterString());
		
			oldToCloneFilter.put(filter.getUuid(), clone);
			engine.getSession().save(clone);
		}
		progress.worked(1);
		
		//clone actions
		progress.subTask("cloning actions");
		HashMap<UUID, EAction> oldToCloneAction = new HashMap<>();
		List<EAction> actions = QueryFactory.buildQuery(engine.getSession(), EAction.class, new Object[] {"conservationArea", engine.getTemplateCa()}).list();
		for (EAction action : actions) {
			EAction clone = new EAction();
			clone.setConservationArea(engine.getTemplateCa());
			clone.setId(action.getId());
			clone.setActionTypeKey(action.getActionTypeKey());
			clone.setParameters(new ArrayList<>());
			
			for (EActionParameterValue param : action.getParameters()) {
				EActionParameterValue cloneParam = new EActionParameterValue();
				cloneParam.getId().setAction(clone);
				cloneParam.getId().setParameterKey(param.getId().getParameterKey());
				cloneParam.setParameterValue(param.getParameterValue());
				
				clone.getParameters().add(cloneParam);
			}
			
			oldToCloneAction.put(action.getUuid(), clone);
			engine.getSession().save(clone);
		}
		progress.worked(1);
		
		//clone events
		progress.subTask("cloning events");
		List<EActionEvent> events = QueryFactory.buildQuery(engine.getSession(), EActionEvent.class, new Object[] {"action.conservationArea", engine.getTemplateCa()}).list();
		for (EActionEvent event : events) {
			EActionEvent clone = new EActionEvent();
			clone.setAction(oldToCloneAction.get(event.getAction().getUuid()));
			clone.setFilter(oldToCloneFilter.get(event.getFilter().getUuid()));
			
			engine.getSession().save(clone);
		}
		progress.worked(1);
		
		engine.getSession().flush();
	}

}
