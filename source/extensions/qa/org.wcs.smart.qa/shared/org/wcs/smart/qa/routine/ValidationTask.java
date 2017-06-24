package org.wcs.smart.qa.routine;

import java.util.Date;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.qa.model.QaRoutine;

public class ValidationTask{

	private QaRoutine routine;
	private IQaDataProvider provider;
	private Date startDate;
	private Date endDate;
	private ConservationArea ca;
	
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
