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
package org.wcs.smart.i2.migrate.intelligence.wizard;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.migrate.MigratePlugin;
import org.wcs.smart.i2.migrate.intelligence.IntelMappingRecord;
import org.wcs.smart.i2.migrate.intelligence.IntelligenceSource;
import org.wcs.smart.i2.migrate.intelligence.Smart6Database;
import org.wcs.smart.i2.migrate.internal.Messages;
import org.wcs.smart.i2.security.IntelAdminUserLevel;
import org.wcs.smart.ui.UserNamePasswordDialog;

/**
 * Job to validate the username and passwords for all conservation
 * areas selected (for both SMART6 and 7+ database);
 * 
 * @author Emily
 *
 */
public class ValidateUserJob implements IRunnableWithProgress {

	private Smart6Database smart6;
	private List<ConservationArea> toValidate;
	private Shell shell;
	
	private List<IntelMappingRecord> records ;
	
	public ValidateUserJob(Smart6Database db, List<ConservationArea> toValidate, Shell shell) {
		this.smart6 = db;
		this.toValidate = toValidate;
		this.shell = shell;
	}
	
	private Shell getShell() {
		return this.shell;
	}
	
	public List<IntelMappingRecord> getMappingRecords() {
		return this.records;
	}
	
	private boolean ok = false;
	
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		this.records = null;
		
		SubMonitor task = SubMonitor.convert(monitor);
		task.beginTask(Messages.ValidateUserJob_taskname, 2);
		
		ok = true;
		
		getShell().getDisplay().syncExec(()->{
			task.split(1).beginTask(Messages.ValidateUserJob_smart6subtask, 1);

			UserNamePasswordDialog udialog = new UserNamePasswordDialog(getShell(), Messages.ValidateUserJob_DialogTitle, Messages.ValidateUserJob_AllCaMsg, IDialogConstants.OK_LABEL);
			if (udialog.open() != Window.OK) {
				ok = false;
				return;
			}
			
			String username = udialog.getUserName();
			String password = udialog.getPassword();
			
			List<ConservationArea> smart6error = new ArrayList<>();
			for (ConservationArea ca : toValidate) {
				try {
					if (!smart6.validateUser(ca, username, password)) {
						smart6error.add(ca);
					}
				} catch (SQLException e) {
					MigratePlugin.log(e.getMessage(), e);
					smart6error.add(ca);
				}
			}
			while(!smart6error.isEmpty()) {
				ConservationArea ca = smart6error.remove(0);
				udialog = new UserNamePasswordDialog(getShell(), 
						Messages.ValidateUserJob_DialogTitle, 
						MessageFormat.format(Messages.ValidateUserJob_Invalid6Ca, ca.getNameLabel()),
						IDialogConstants.OK_LABEL);
				
				if (udialog.open() != Window.OK) {
					ok = false;
					return;
				}
				try {
					if (!smart6.validateUser(ca,  udialog.getUserName(), udialog.getPassword())) {
						smart6error.add(0, ca);
					}
				} catch (SQLException e) {
					MigratePlugin.log(e.getMessage(), e);
					smart6error.add(0, ca);
				}
			}
			
			//valid smart7
			task.split(1).beginTask(Messages.ValidateUserJob_smart7subtask, 1);
			List<ConservationArea> smart7error = new ArrayList<>();
			for (ConservationArea ca : toValidate) {
				Employee emp = HibernateManager.validateUser(username, password, ca);
				if ( emp == null) {
					smart7error.add(ca);
				}else {
					if (!emp.getSmartUserLevels().contains(IntelAdminUserLevel.INSTANCE.getKey())){
						//must be an intel-admin user
						smart7error.add(ca);
					}
				}
			}
			while(!smart7error.isEmpty()) {
				ConservationArea ca = smart7error.remove(0);
				udialog = new UserNamePasswordDialog(getShell(), 
						Messages.ValidateUserJob_DialogTitle, 
						MessageFormat.format(Messages.ValidateUserJob_Invalid7Ca, ca.getNameLabel(), IntelAdminUserLevel.INSTANCE.getKey()),
						IDialogConstants.OK_LABEL);
				
				if (udialog.open() != Window.OK) {
					ok = false;
					return;
				}
				Employee emp = HibernateManager.validateUser(udialog.getUserName(), udialog.getPassword(), ca);
				if ( emp == null) {
					smart7error.add(0, ca);
				}else {
					if (!emp.getSmartUserLevels().contains(IntelAdminUserLevel.INSTANCE.getKey())){
						//must be an intel-admin user
						smart7error.add(0, ca);
					}
				}
			}
		});
		
		if (!ok) return;
		//users are validated move on to next page
		List<IntelMappingRecord> lrecords = new ArrayList<>();
		try {
			List<IntelligenceSource> sources = smart6.getSources(toValidate);
			HashMap<UUID, ConservationArea> map = new HashMap<>();
			for (ConservationArea ca : toValidate) map.put(ca.getUuid(), ca);
			for (IntelligenceSource source : sources) {
				
				IntelMappingRecord record = new IntelMappingRecord(map.get(source.getConservationArea().getUuid()), source);
				lrecords.add(record);
			}
		}catch (Exception ex) {
			throw new InvocationTargetException(ex);
		}
		this.records = lrecords;
	}

}
