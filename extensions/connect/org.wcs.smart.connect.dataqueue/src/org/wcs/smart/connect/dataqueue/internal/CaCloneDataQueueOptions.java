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
package org.wcs.smart.connect.dataqueue.internal;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.connect.dataqueue.model.DataQueueProcessingOption;

/**
 * Clones any data processing options configured for the conservation
 * area.
 * 
 * @author Emily
 *
 */
public class CaCloneDataQueueOptions implements IConservationAreaTemplateCloner {

	@SuppressWarnings("unchecked")
	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.CaCloneDataQueueOptions_task, 1);
		try{
			Session s = engine.getSession();
			
			List<DataQueueProcessingOption> options = s.createCriteria(DataQueueProcessingOption.class)
					.add(Restrictions.eq("id.conservationArea", engine.getTemplateCa().getUuid())) //$NON-NLS-1$
					.list();
			
			for (DataQueueProcessingOption op : options){
				DataQueueProcessingOption cloneop = new DataQueueProcessingOption();
				cloneop.setConservationArea(engine.getNewCa().getUuid());
				cloneop.setOptionKey(op.getOptionKey());
				cloneop.setValue(op.getValue());
				
				s.save(cloneop);
			}
			
			s.flush();
		}finally{
			monitor.done();
		}
	}

}