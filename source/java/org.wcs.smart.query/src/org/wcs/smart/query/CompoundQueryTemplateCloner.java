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
package org.wcs.smart.query;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.common.model.CompoundMapQueryLayer;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

/**
 * Clones compound queries.  Configured to run after all other cloners so we have 
 * access to all the queries cloned.
 * 
 * @author Emily
 *
 */
public class CompoundQueryTemplateCloner implements
		IConservationAreaTemplateCloner {

	@SuppressWarnings("unchecked")
	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {

		List<CompoundMapQuery> queries = engine.getSession().createCriteria(CompoundMapQuery.class)
			.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
			.add(Restrictions.eq("isShared", true)) //$NON-NLS-1$
			.list();  
		
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		
		for (CompoundMapQuery toclone : queries){
			
			CompoundMapQuery clone = new CompoundMapQuery();
			clone.setConservationArea(engine.getNewCa());
			clone.setConservationAreaFilter( (new ConservationAreaFilter(true, engine.getNewCa())).asString());

			if (toclone.getFolder() != null){
				clone.setFolder((QueryFolder)engine.getNewConservationItem(toclone.getFolder()));
			}
			clone.setId(toclone.getId());
			clone.setIsShared(toclone.getIsShared());
			
			engine.copyLabels(toclone, clone);
			clone.setOwner(newEmployee);
			
			clone.setLayers(new ArrayList<CompoundMapQueryLayer>());
			
			for (CompoundMapQueryLayer layer : toclone.getLayers()){
				
				UuidItem newQuery = engine.getNewConservationItem(layer.getQueryUuid());
				if (newQuery == null ) continue; //query not found
				
				CompoundMapQueryLayer layerclone = new CompoundMapQueryLayer();
				layerclone.setDateFilter(layer.getDateFilter());
				layerclone.setOrder(layer.getOrder());
				layerclone.setQueryStyle(layerclone.getQueryStyle());
				layerclone.setQueryType(layer.getQueryType());
				layerclone.setQueryUuid(newQuery.getUuid());
				
				layerclone.setMapQuery(clone);
				clone.getLayers().add(layerclone);
			}
			
			engine.addConservationItemMapping(toclone, clone);
			engine.getSession().save(clone);
		}
		engine.getSession().flush();
	}

}
