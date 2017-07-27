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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
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
		SubMonitor progress = SubMonitor.convert(monitor, Messages.ObservationCloner_TaskName, 1);
		
		progress.subTask(Messages.ObservationCloner_ProgressName);
		cloneOptions(engine);
		progress.worked(1);
	}
	
	
	/*
	 * clones patrol options
	 */
	private void cloneOptions(ConservationAreaClonerEngine engine){
		ObservationOptions newOp = ObservationHibernateManager.createPatrolOption(engine.getNewCa(), engine.getSession());
		ObservationOptions ops = engine.getSession().get(ObservationOptions.class, engine.getTemplateCa().getUuid());
		if (ops != null){
			newOp.setEditTime(ops.getEditTime());
			newOp.setTrackDistanceDirection(ops.getTrackDistanceDirection());
			newOp.setTrackObserver(ops.getTrackObserver());
		}
		engine.getSession().save(newOp);
		engine.getSession().flush();
	}

}
