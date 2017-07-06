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
package org.wcs.smart.qa;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.routine.ValidationTask;

/**
 * Engine to perform qa validation
 * 
 * @author Emily
 *
 */
public class ValidationEngine {

	private List<ValidationTask> tasks;
	private List<Exception> exceptions;
	private Locale l;
	
	/**
	 * Creates a new engine with no tasks
	 */
	public ValidationEngine(Locale l){
		tasks = new ArrayList<>();
		this.l = l;
	}
	
	/**
	 * Adds a validation task
	 * @param task
	 */
	public void addValidationTask(ValidationTask task){
		this.tasks.add(task);
	}
	
	/**
	 * 
	 * @return collection of exceptions that occurred while validating data
	 * 
	 */
	public List<Exception> getExceptions(){
		return this.exceptions;
	}
	
	/**
	 * Runs all the validation tasks and returns all associated validation errors 
	 * 
	 * @param session
	 * @param monitor
	 * @return
	 */
	public Collection<QaError> validate( Session session, IProgressMonitor monitor ){
		SubMonitor  m = SubMonitor.convert(monitor, "Validating Data", tasks.size());
		exceptions = new ArrayList<>();
		Collection<QaError> errors = new ArrayList<QaError>();
		for (ValidationTask t : tasks){
			m.setTaskName(MessageFormat.format("Validating {0} ({1})", t.getDataProvider().getName(l), t.getRoutine().getName()));
			try{
				QaRoutine r = (QaRoutine) session.get(QaRoutine.class, t.getRoutine().getUuid());
				t.setQaRoutine(r);
				
				Collection<QaError> localErrors = r.getRoutineType().validateData(t, session, m.newChild(1));
				
				for(QaError newError : localErrors){
					for (QaError existing : errors){
						if (newError.getDataProviderId().equals(existing.getDataProviderId()) && newError.getSourceId().equals(existing.getSourceId())){
							newError.addLink(existing);
							existing.addLink(newError);
						}
					}
					errors.add(newError);
				}
			}catch (Exception ex){
				exceptions.add(ex);
			}
		}
		monitor.done();
		return errors;
	}
	
	
}
