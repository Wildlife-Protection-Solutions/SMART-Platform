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
package org.wcs.smart.intelligence;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligenceSource;
import org.wcs.smart.intelligence.report.ReportIntelligence;

/**
 * Cloner that clones intelligence source options.
 * 
 * @author Emily
 *
 */
public class IntelligenceCaCloner implements IConservationAreaTemplateCloner {

	public IntelligenceCaCloner() {
	}

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.IntelligenceCaCloner_ProgressMessage, 2);
		cloneSources(engine);
		monitor.worked(1);
		clonePrintTemplate(engine);
		monitor.worked(1);		
		monitor.done();
	}

	
	/*
	 * clone patrol mandates
	 */
	private void cloneSources(ConservationAreaClonerEngine engine){
		List<IntelligenceSource> sources = IntelligenceHibernateManager.getSourceTypes(engine.getTemplateCa(), engine.getSession());
		for (IntelligenceSource s : sources){
			IntelligenceSource clone = new IntelligenceSource();
			clone.setConservationArea(engine.getNewCa());
			clone.setIsActive(s.getIsActive());
			clone.setKeyId(s.getKeyId());
			engine.copyLabels(s, clone);
			
			engine.getSession().save(clone);
			engine.addConservationItemMapping(s,clone);
		}
		engine.getSession().flush();
	}
	
	/*
	 * clone the printing template
	 */
	private void clonePrintTemplate(ConservationAreaClonerEngine engine){
		//need to clone: the plan template
		File f = new File(engine.getTemplateCa().getFileDataStoreLocation(),
				Intelligence.INTELLIGENCE_DIR);
		File templateFile = new File(f, ReportIntelligence.INTELLIGENCE_TEMPLATE);
		
		if (templateFile.exists()){
			//copy intelligence template
			f = new File(engine.getNewCa().getFileDataStoreLocation(),
					Intelligence.INTELLIGENCE_DIR);
			File newFile = new File(f, ReportIntelligence.INTELLIGENCE_TEMPLATE);
			try {
				FileUtils.copyFile(templateFile, newFile);
			} catch (IOException e) {
				IntelligencePlugIn.log("Error clonging intelligence template.", e); //$NON-NLS-1$
			}
		}
	}
}
