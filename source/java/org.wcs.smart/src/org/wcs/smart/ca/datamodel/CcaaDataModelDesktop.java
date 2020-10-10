/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.ca.datamodel;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.ConservationAreaConfiguration;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Ccaa data model representing the current desktop configuration
 * 
 * @author Emily
 *
 */
public class CcaaDataModelDesktop extends CcaaDataModel{
	
	private static CcaaDataModelDesktop INSTANCE = null;

	
	public static synchronized CcaaDataModelDesktop getInstance() {
		if (INSTANCE == null) INSTANCE = new CcaaDataModelDesktop();
		return INSTANCE;
	}
	
	protected CcaaDataModelDesktop() {
		super(SmartDB.getConservationAreaConfiguration().getMainConservationArea(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
		DataModelManager.INSTANCE.addChangeListener(()->clearDataModel());
	}
	
	@Override
	public void clearDataModel(){
		this.core = SmartDB.getConservationAreaConfiguration().getMainConservationArea();
		this.cas = SmartDB.getConservationAreaConfiguration().getConservationAreas().toArray(new ConservationArea[SmartDB.getConservationAreaConfiguration().getConservationAreas().size()]);

		super.clearDataModel();
	}
	
	
	protected void getDataModelInternal() {
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				ProgressMonitorDialog dialog = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
				try {
					dialog.run(true, false, loadAndMergeDataModelJob);
				} catch (Exception e) {
					//SmartPlugIn.displayLog("Error merging data model: " + e.getLocalizedMessage(), e);
					//TODO:
					e.printStackTrace();
				}		
		}});
	}
	
	private IRunnableWithProgress loadAndMergeDataModelJob = new IRunnableWithProgress() {
		
		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException,
				InterruptedException {
			try(Session session = HibernateManager.openSession()){
				session.beginTransaction();
				try{
					ConservationAreaConfiguration config = SmartDB.getConservationAreaConfiguration();
					DataModelMerger merger = new DataModelMerger();
					SimpleDataModel simple = merger.mergeDataModels(
							config.getConservationAreas().toArray(new ConservationArea[config.getCaCount()]),
							config.getMainConservationArea(), 
							session, Locale.getDefault(), monitor);
					dm = new DataModel(simple.getConservationArea(), simple.getCategories(), simple.getAttributes());
							
				}finally{
					session.getTransaction().rollback();
				}
			}

			
		}
	};	

}
