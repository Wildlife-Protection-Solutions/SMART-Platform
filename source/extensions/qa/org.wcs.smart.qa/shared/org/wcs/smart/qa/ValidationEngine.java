package org.wcs.smart.qa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.routine.ValidationTask;

public class ValidationEngine {

	private List<ValidationTask> tasks;
	
	
	public ValidationEngine(){
		tasks = new ArrayList<>();
	}
	
	public void addValidationTask(ValidationTask task){
		this.tasks.add(task);
	}
	
	public Collection<QaError> validate( Session session, IProgressMonitor monitor ){
		SubMonitor  m = SubMonitor.convert(monitor, "Validating Data", tasks.size());
		
		Collection<QaError> errors = new ArrayList<QaError>();
		for (ValidationTask t : tasks){
			try{
				QaRoutine r = (QaRoutine) session.get(QaRoutine.class, t.getRoutine().getUuid());
				t.setQaRoutine(r);
				
				errors.addAll(r.getRoutineType().validateData(t, session, m.newChild(1)));
				
			}catch (Exception ex){
				//TODO: DO SOMETHING WITH THIS ERROR
				ex.printStackTrace();
			}
		}
		monitor.done();
		return errors;
	}
	
	
}
