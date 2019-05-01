/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.ui.server.ConnectDialog;

/**
 * Job for loading support alert types from Connect server
 * @author Emily
 *
 */
public abstract class LoadAlertTypesJob extends Job {

	private IEclipseContext context;
	private boolean forceReload = false;
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
		this(context, false);
	}
	public LoadAlertTypesJob(IEclipseContext context, boolean forceReload) {
		super(Messages.LoadAlertTypesJob_LoadingAlertType);
		this.context = context;
		setRule(MutexRule.INSTANCE);
		this.forceReload = forceReload;
	}

	public abstract void typesLoaded(List<AlertType> types);
	
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<AlertType> loadedTypes = (List<AlertType>) context.get(AlertType.class.toString());
		if (loadedTypes == null || forceReload) {
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
							setTitle(Messages.LoadAlertTypesJob_DialogTitle);
							getShell().setText(Messages.LoadAlertTypesJob_DialogTitle);
							setMessage(Messages.LoadAlertTypesJob_DialogMessage);	
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
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.LoadAlertTypesJob_WarningTitle, Messages.LoadAlertTypesJob_WarningMessage);
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
