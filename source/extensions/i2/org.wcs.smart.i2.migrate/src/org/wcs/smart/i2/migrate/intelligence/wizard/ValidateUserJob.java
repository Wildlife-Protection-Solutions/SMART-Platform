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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.migrate.MigratePlugin;
import org.wcs.smart.i2.migrate.intelligence.IntelMappingRecord;
import org.wcs.smart.i2.migrate.intelligence.IntelligenceSource;
import org.wcs.smart.i2.migrate.intelligence.Smart6Database;
import org.wcs.smart.ui.UserNamePasswordDialog;


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
		task.beginTask("validating users", 2);
		
		ok = true;
		
		getShell().getDisplay().syncExec(()->{
			task.split(1).beginTask("validating SMART 6 users.", 1);

			UserNamePasswordDialog udialog = new UserNamePasswordDialog(getShell(), "Validate Users", "Enter username and password for the Conservation Areas", IDialogConstants.OK_LABEL);
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
						"Validate Users", 
						MessageFormat.format("Invalid username/password for SMART 6 Conservation Area {0}. Please try again.", ca.getNameLabel()),
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
			task.split(1).beginTask("validating SMART users.", 1);
			List<ConservationArea> smart7error = new ArrayList<>();
			for (ConservationArea ca : toValidate) {
				if (HibernateManager.validateUser(username, password, ca) == null) {
					smart7error.add(ca);
				}
			}
			while(!smart7error.isEmpty()) {
				ConservationArea ca = smart7error.remove(0);
				udialog = new UserNamePasswordDialog(getShell(), 
						"Validate Users", 
						MessageFormat.format("Invalid username/password for SMART Conservation Area {0}. Please try again.", ca.getNameLabel()),
						IDialogConstants.OK_LABEL);
				
				if (udialog.open() != Window.OK) {
					ok = false;
					return;
				}
				if (HibernateManager.validateUser(udialog.getUserName(), udialog.getPassword(), ca) == null) {
					smart7error.add(0, ca);
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
