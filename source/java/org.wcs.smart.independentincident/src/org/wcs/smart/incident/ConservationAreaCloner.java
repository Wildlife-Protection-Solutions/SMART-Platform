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
package org.wcs.smart.incident;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.model.IncidentType;


/**
 * Clones incident template details
 * 
 * @since 8.1.0
 *
 */
public class ConservationAreaCloner implements IConservationAreaTemplateCloner{

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
		
		SubMonitor.convert(monitor, Messages.ConservationAreaCloner_taskanem, 1);
		cloneIncidentType(engine);
			
	}

	private void cloneIncidentType(ConservationAreaClonerEngine engine) throws Exception{
		List<IncidentType> mappings = QueryFactory.buildQuery(
				engine.getSession(), IncidentType.class, 
				"conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$
		
		for (IncidentType current : mappings){
			IncidentType clone = new IncidentType();
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(current.getKeyId());
			clone.setOptions(current.getOptions());
			clone.setIsActive(current.getIsActive());
			engine.copyLabels(current, clone);
			engine.getSession().persist(clone);
			
			engine.addConservationItemMapping(current, clone);
		}
		engine.getSession().flush();
	}
	
}
