/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.observation;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.ObservationOptions;
/**
 * For cloning observation options
 * 
 * @author Emily
 *
 */
public class ObservationCloner implements IConservationAreaTemplateCloner {

	public ObservationCloner() {
	}

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.ObservationCloner_TaskName, 1);
		try{
			monitor.subTask(Messages.ObservationCloner_ProgressName);
			cloneOptions(engine);
			monitor.worked(1);
		}finally{
			monitor.done();
		}
	}
	
	
	/*
	 * clones patrol options
	 */
	private void cloneOptions(ConservationAreaClonerEngine engine){
		@SuppressWarnings("unchecked")
		List<ObservationOptions> ops = engine.getSession().createCriteria(ObservationOptions.class).add(Restrictions.eq("uuid", engine.getTemplateCa().getUuid())).list(); //$NON-NLS-1$

		ObservationOptions newOp = ObservationHibernateManager.createPatrolOption(engine.getNewCa(), engine.getSession());
		if (ops.size() > 0){
			ObservationOptions tempOp = ops.get(0);
			newOp.setEditTime(tempOp.getEditTime());
			newOp.setTrackDistanceDirection(tempOp.getTrackDistanceDirection());
			newOp.setTrackObserver(tempOp.getTrackObserver());
			newOp.setViewProjection((Projection)engine.getNewConservationItem(tempOp.getViewProjection()));
		}
		engine.getSession().save(newOp);
		engine.getSession().flush();
	}

}
