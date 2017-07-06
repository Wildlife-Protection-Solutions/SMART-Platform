/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.auto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.InternalExtensionManager;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.ValidationEngine;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.routine.IQaDataProvider;
import org.wcs.smart.qa.routine.ValidationTask;

/**
 * Auto validation job/manager
 * 
 * @author Emily
 *
 */
public class AutoValidateJob extends Job{

	public static final AutoValidateJob INSTANCE = new AutoValidateJob();
	
	private List<ValidationTask> tasks = Collections.synchronizedList(new ArrayList<>());
		
	private AutoValidateJob() {
		super("Executing QA Routines");
	}

	/**
	 * Register a new data provider for validations.  
	 * Data providers must provide data when passed null date filters 
	 */
	public void addTask(IQaDataProvider provider){
		for (QaRoutine r : InternalExtensionManager.INSTANCE.getAutoRoutines()){
			tasks.add(new ValidationTask(r,provider, null, null, SmartDB.getCurrentConservationArea()));
		}
		if (getState() == Job.WAITING || 
				getState() == Job.NONE){
			//cancel & run now
			cancel();
			schedule();
		}
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Validating QA Data", IProgressMonitor.UNKNOWN);
		while(!tasks.isEmpty()){
			ValidationEngine engine = new ValidationEngine(Locale.getDefault());
			synchronized (tasks) {
				for (ValidationTask t : tasks){
					engine.addValidationTask(t);
				}
				tasks.clear();
			}
			
			Session session = HibernateManager.openSession();
			try{
				Collection<QaError> errors = engine.validate(session, new SubProgressMonitor(monitor, -1));
				session.beginTransaction();
				for (QaError error : errors){
					session.save(error);
				}
				session.getTransaction().commit();
			}catch(Exception ex){
				QaPlugIn.displayLog("Error executing auto validation routines on new data. These routines may need to modified to prevent these errors in the future. " + ex.getMessage(), ex);
			}finally{
				session.close();
			}
			if (monitor.isCanceled()) break;
		}
		synchronized (tasks) {
			if (!tasks.isEmpty()) schedule();
		}
		return Status.OK_STATUS;
	}

}
