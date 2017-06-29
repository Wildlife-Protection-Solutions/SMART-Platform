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

import java.util.Date;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.qa.model.QaRoutine;

/**
 * A validate tasks that includes the details about the data
 * to be validated.
 * 
 * @author Emily
 *
 */
public class ValidationTask{

	private QaRoutine routine;
	private IQaDataProvider provider;
	private Date startDate;
	private Date endDate;
	private ConservationArea ca;
	
	/**
	 * Creates a new validation task
	 * @param routine the validation routine
	 * @param provider the data provider
	 * @param startDate the start date
	 * @param endDate the end date 
	 * @param ca the conservation area 
	 */
	public ValidationTask(QaRoutine routine, IQaDataProvider provider, Date startDate, Date endDate, ConservationArea ca){
		this.routine = routine;
		this.provider = provider;
		this.startDate = startDate;
		this.endDate = endDate;
		this.ca = ca;
	}

	public void setQaRoutine(QaRoutine routine){
		this.routine = routine;
	}
	public QaRoutine getRoutine() {
		return routine;
	}

	public IQaDataProvider getDataProvider() {
		return provider;
	}

	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public ConservationArea getConservationArea() {
		return ca;
	}
}
