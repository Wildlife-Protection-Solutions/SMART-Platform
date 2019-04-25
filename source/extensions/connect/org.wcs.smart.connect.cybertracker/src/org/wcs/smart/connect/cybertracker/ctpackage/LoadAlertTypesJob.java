package org.wcs.smart.connect.cybertracker.ctpackage;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.AlertType;
import org.wcs.smart.connect.ui.server.ConnectDialog;

public abstract class LoadAlertTypesJob extends Job {

	private IEclipseContext context;
	
	private static class MutexRule implements ISchedulingRule {
		private static final MutexRule INSTANCE = new MutexRule();

		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this;
		}

		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}
	}
	
	public LoadAlertTypesJob(IEclipseContext context) {
		super("load alert types");
		this.context = context;
		setRule(MutexRule.INSTANCE);
	}

	public abstract void typesLoaded(List<AlertType> types);
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<AlertType> loadedTypes = (List<AlertType>) context.get(AlertType.class.toString());
		if (loadedTypes == null) {
			if (context != null && context.get(SmartConnect.class) != null) {
				//try to load alert types from connect
				try {
					loadedTypes = context.get(SmartConnect.class).getAlertTypes();
				}catch (Exception ex) {					
				}

			}
			if (loadedTypes == null) {
				
				
				Display.getDefault().syncExec(()->{
					ConnectDialog cd = new ConnectDialog(Display.getCurrent().getActiveShell(), true) {
						@Override
						protected Control createDialogArea(Composite parent) {
							setTitle("Connect Alerts");
							getShell().setText("Connect Alerts");
							setMessage("Load alert types from connect");	
							return super.createDialogArea(parent);
						}	
					};
					
					if (cd.open() == Window.OK) {
						SmartConnect connect = cd.getConnection();
						if (context != null) context.set(SmartConnect.class, connect);
					}
				});
				SmartConnect connect = context.get(SmartConnect.class);
				if (connect != null) {
					try {
						loadedTypes = connect.getAlertTypes();
						AlertUtils.cacheAlertTypes(loadedTypes);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			if (loadedTypes == null) {
				loadedTypes = AlertUtils.getCachedAlertTypes();
				if (loadedTypes != null) {
					Display.getDefault().syncExec(()->{
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Warning", "Alert types could not be refreshed from the Connect server.  Cached values will be used instead.");
					});
				}
			}
		}
		if (loadedTypes == null) {
			loadedTypes = new ArrayList<>();
		}else {
			context.set(AlertType.class.toString(), loadedTypes);
		}
		typesLoaded(loadedTypes);
		
		return Status.OK_STATUS;
	}

}
