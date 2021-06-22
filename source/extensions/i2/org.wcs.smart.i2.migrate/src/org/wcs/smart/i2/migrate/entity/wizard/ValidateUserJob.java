/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.i2.migrate.entity.wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.i2.migrate.UserValidationManager;
import org.wcs.smart.i2.migrate.entity.Entity6Database;
import org.wcs.smart.i2.migrate.entity.EntityTypeItem;
import org.wcs.smart.i2.migrate.entity.EntityTypeMappingRecord;
import org.wcs.smart.i2.migrate.internal.Messages;

/**
 * Job to validate the username and passwords for all conservation
 * areas selected (for both SMART6 and 7+ database);
 * 
 * @author Emily
 *
 */
public class ValidateUserJob implements IRunnableWithProgress {

	private Entity6Database smart6;
	private List<ConservationArea> toValidate;
	
	private Shell shell;
	
	private Map<ConservationArea, Employee> employees;
	private List<EntityTypeMappingRecord> records ;
	
	public ValidateUserJob(Entity6Database db, List<ConservationArea> toValidate, Shell shell) {
		this.smart6 = db;
		this.toValidate = toValidate;
		this.shell = shell;
	}
	
	private Shell getShell() {
		return this.shell;
	}
	
	public List<EntityTypeMappingRecord> getMappingRecords() {
		return this.records;
	}
	
	public  Map<ConservationArea, Employee> getEmployeeMapping() {
		return this.employees;
	}
	
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		this.records = null;
		
		SubMonitor task = SubMonitor.convert(monitor);
		task.beginTask(Messages.ValidateUserJob_taskname, 2);
		
		Display.getDefault().syncExec(()->{
			employees = UserValidationManager.INSTANCE.validate(toValidate, smart6, getShell(), task);
		});
		
		if (employees == null) return;
		//users are validated move on to next page
		List<EntityTypeMappingRecord> lrecords = new ArrayList<>();
		try {
			
			for (ConservationArea ca : toValidate) {
				Collection<EntityTypeItem> etypes = smart6.getTypes(ca);
			
				for (EntityTypeItem i : etypes) {
					EntityTypeMappingRecord record = new EntityTypeMappingRecord(ca);
					record.setEntitytype(i);
					lrecords.add(record);
				}
			}

		}catch (Exception ex) {
			throw new InvocationTargetException(ex);
		}
		this.records = lrecords;
	}

}
