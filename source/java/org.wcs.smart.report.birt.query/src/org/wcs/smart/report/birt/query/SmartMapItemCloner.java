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
package org.wcs.smart.report.birt.query;

import java.io.File;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.report.model.api.DesignConfig;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.DesignEngine;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.birt.report.model.core.DesignElement;
import org.eclipse.birt.report.model.elements.ExtendedItem;
import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.birt.BirtResourceLocator;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.data.oda.smart.internal.Messages;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.util.UuidUtils;

import com.ibm.icu.util.ULocale;

/**
 * This template cloner fixes up the query references
 * in the smart map extension item.  
 * 
 * @author Emily
 *
 */
public class SmartMapItemCloner implements IConservationAreaTemplateCloner {

	@SuppressWarnings("unchecked")
	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
		
		//the report files have already been cloned
		//here we just need to update the 
		//query text in the BIRT smart
		//report item
		List<Report> newReports = engine.getSession().createCriteria(Report.class).add(Restrictions.eq("conservationArea", engine.getNewCa())).list(); //$NON-NLS-1$
		monitor.beginTask(Messages.SmartMapItemCloner_CloningReportItem, newReports.size());
		for (Report r : newReports){
			File reportFile = new File(new File(engine.getNewCa().getFileDataStoreLocation(), Report.REPORT_DIR), r.getFilename());
			updateReportFile(r, reportFile, engine);
			monitor.worked(1);
		}
		monitor.done();
	}
	
	/**
	 * 
	 * @param report
	 * @param dest
	 * @param engine
	 * @throws Exception
	 */
	private void updateReportFile(Report report, File dest, ConservationAreaClonerEngine engine) throws Exception{
		DesignConfig config = new DesignConfig();
		config.setResourceLocator(BirtResourceLocator.INSTANCE);
		SessionHandle session = new DesignEngine( config ).newSessionHandle( ULocale.getDefault( ) );
		ReportDesignHandle rdh = session.openDesign(dest.getAbsolutePath());
		
		rdh.getRoot().getComponents();
		
		for (Iterator<?> iterator = rdh.getBody().iterator(); iterator.hasNext();) {
			DesignElementHandle type = (DesignElementHandle) iterator.next();
			DesignElement element = type.getElement();
			if (element instanceof ExtendedItem){
				if (((ExtendedItem)element).getExtDefn().getName().equals("org.wcs.smart.report.birt.SmartMap")){ //$NON-NLS-1$
					ExtendedItem it = (ExtendedItem)element;
					@SuppressWarnings("unchecked")
					List<Object> mapLayerQueries = (List<Object>) it.getProperty(it.getRoot(), "org.wcs.smart.birt.map.layers"); //$NON-NLS-1$
					if (mapLayerQueries != null){
						for (int i = 0; i < mapLayerQueries.size(); i ++){
							String queryText = (String)mapLayerQueries.get(i);
							String bits[] = queryText.split(":"); //$NON-NLS-1$
							String queryUuid = bits[1];
							try{
								UuidItem newQueryReferences = engine.getNewConservationItem(UuidUtils.stringToUuid(queryUuid));
							
								if (newQueryReferences != null){
									mapLayerQueries.set(i, (Object)(bits[0] + ":" + UuidUtils.uuidToString(newQueryReferences.getUuid()))); //$NON-NLS-1$
								}else{
									//new query reference cannot be found; 
									throw new Exception(MessageFormat.format(Messages.SmartMapItemCloner_CloneError, new Object[]{report.getName()}));
								}
							}catch (Exception ex){
								//map query is not a uuid item so we can skip it
							}
						}
					}
				}
			}
			
		}
		
		
		rdh.save();
		rdh.close();
	}

}
