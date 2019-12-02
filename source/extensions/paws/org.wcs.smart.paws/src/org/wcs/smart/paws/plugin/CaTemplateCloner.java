/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsParameter;
import org.wcs.smart.paws.model.PawsQueryClass;
import org.wcs.smart.paws.model.PawsSimpleClass;
import org.wcs.smart.util.UuidUtils;

/**
 * 
 * Functionality to clone Conservation Area configuration into
 * a new Conservation area. 
 * 
 * Clones PAWS Configurations.  Does not clone the area layers or the boundaries for 
 * these configurations.  Users will have to redefine these before running the
 * configuration.
 * 
 * @author Emily
 *
 */
public class CaTemplateCloner implements IConservationAreaTemplateCloner {

	public static final String ID = "org.wcs.smart.paws.caclone"; //$NON-NLS-1$
	
	public CaTemplateCloner() {
	}

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {		
		Session session = engine.getSession();
		
		List<PawsConfiguration> configs = QueryFactory.buildQuery(session, PawsConfiguration.class,
				new Object[]{"conservationArea", engine.getTemplateCa()}).getResultList();
		for (PawsConfiguration c : configs){
			PawsConfiguration clone = new PawsConfiguration();
			clone.setConservationArea(engine.getNewCa());
			clone.setName(c.getName());
			clone.setParameters(new ArrayList<>());
			
			for (PawsParameter pp : c.getParameters()){
				if (pp.getValue() == null || pp.getValue().isBlank()) continue;
				PawsParameter cpp = new PawsParameter();
				cpp.setConfiguration(clone);
				cpp.setKey(pp.getKey());
				
				if (pp.getKey().equals(PawsParameter.FixedParameter.GRID_CRS.name())){
					UUID uuid = UuidUtils.stringToUuid(pp.getValue().split(":")[0]);
					String def = pp.getValue().substring(pp.getValue().indexOf(':')+1);
					UuidItem prj = engine.getNewConservationItem(uuid);
					if (prj == null){
						pp.setValue(":" + def);
					}else{
						pp.setValue(UuidUtils.uuidToString(prj.getUuid()) + ":" + def);
					}
					clone.getParameters().add(cpp);
				}else if (pp.getKey().equals(PawsParameter.FixedParameter.GRID_SIZE.name())){
					cpp.setValue(pp.getValue());
					clone.getParameters().add(cpp);
				}else if (pp.getKey().equals(PawsParameter.FixedParameter.TRAINING_RES.name())) {
					cpp.setValue(pp.getValue());
					clone.getParameters().add(cpp);
				}else if (pp.getKey().equals(PawsParameter.FixedParameter.CLASSIFIER_MODEL.name())) {
					cpp.setValue(pp.getValue());
					clone.getParameters().add(cpp);
				}else if (pp.getKey().equals(PawsParameter.FixedParameter.LYR_BOUNDARY.name()) ||
						pp.getKey().equals(PawsParameter.FixedParameter.LYR_OTHER.name())){
					//will need to be redefined by user
				}
			}
			session.save(clone);
			
			List<PawsSimpleClass> simpleMappings = QueryFactory.buildQuery(session, PawsSimpleClass.class,
					new Object[]{"configuration", c}).getResultList();
			for (PawsSimpleClass pw : simpleMappings){
				PawsSimpleClass pwclone = new PawsSimpleClass();
				pwclone.setCategoryHkey(pw.getCategoryHkey());
				pwclone.setAttributeKey(pw.getAttributeKey());
				pwclone.setAttributeListItemKey(pw.getAttributeListItemKey());
				pwclone.setAttributeTreeNodeHkey(pw.getAttributeTreeNodeHkey());
				pwclone.setClassification(pw.getClassification());
				pwclone.setConfiguration(clone);
				session.save(pwclone);
			}
			
			List<PawsQueryClass> queryMappings = QueryFactory.buildQuery(session, PawsQueryClass.class,
					new Object[]{"configuration", c}).getResultList();
			for (PawsQueryClass pw : queryMappings){
				UUID quuid = engine.getNewConservationItem(pw.getQueryUuid()).getUuid();
				if (quuid == null) continue;
				PawsQueryClass pwclone = new PawsQueryClass();
				pwclone.setQueryType(pw.getQueryType());
				pwclone.setQueryUuid(quuid);
				pwclone.setClassification(pw.getClassification());
				pwclone.setConfiguration(clone);
				session.save(pwclone);
			}
		}
	}
}
