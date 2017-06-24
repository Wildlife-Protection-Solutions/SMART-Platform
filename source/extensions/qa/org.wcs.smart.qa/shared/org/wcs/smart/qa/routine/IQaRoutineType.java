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
package org.wcs.smart.qa.routine;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaRoutine;

/**
 * An interface for implementing a QA Routine type.  These
 * define routines that users can implement with custom
 * parameters for validating data.
 * 
 * @author Emily
 *
 */
public interface IQaRoutineType {

	/**
	 * 
	 * @return the identifier of the QA routine
	 */
	public String getId();
	
	/**
	 * The property QA routine name for the given locale
	 * 
	 * @return
	 */
	public String getName(Locale l);
	
	
	/**
	 * A description of what the QA routine validates
	 * 
	 * @return
	 */
	public String getDescription(Locale l);

	
	/**
	 * Validates data returns a set of errors.
	 * 
	 * @return
	 */
//	public Collection<QaError> validateData(IQaDataProvider dataProvider, QaRoutine routine);
	public Collection<QaError> validateData(ValidationTask task, Session session, IProgressMonitor monitor) throws Exception;
		
	/**
	 * Returns a summary of the parameter values from the qa routine for display to the user
	 * @param routine
	 * @return
	 */
	public String getParameterSummary(QaRoutine routine);
}
