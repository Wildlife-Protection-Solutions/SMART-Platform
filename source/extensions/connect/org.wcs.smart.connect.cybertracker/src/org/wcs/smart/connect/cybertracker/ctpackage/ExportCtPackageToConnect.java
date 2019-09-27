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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerNavigationProxy;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerPackageProxy;
import org.wcs.smart.connect.internal.server.FileUploaderJob;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtExportAction;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.NavigationLayer;
import org.wcs.smart.cybertracker.navigation.ExportNavigationManager;

/**
 * Export CyberTracker package to connect action.
 * @author Emily
 *
 */
public class ExportCtPackageToConnect implements ICtExportAction {

	public ExportCtPackageToConnect() {
	}

	private List<Job> jobs = new ArrayList<>();
	private int ok;
	private int total;
	
	
	@Override
	public void doAction(List<ICtPackage> ctpackages, List<NavigationLayer> navlayers, IEclipseContext context) {
		try {
			SmartConnect connect = context.get(SmartConnect.class);
			if (connect == null) {
				ConnectDialog cd = new ConnectDialog(context.get(Shell.class), true){
					@Override
					protected Control createDialogArea(Composite parent) {
						setTitle(Messages.ExportCtPackageToConnect_Title);
						getShell().setText(Messages.ExportCtPackageToConnect_ShellTitle);
						setMessage(Messages.ExportCtPackageToConnect_Message);	
						return super.createDialogArea(parent);
					}	
				};
				if (cd.open() != Window.OK) {
					return;
				}
				
				connect = cd.getConnection();
				context.set(SmartConnect.class, connect);
			}
			
			ResteasyClient client = connect.getClient();
			ResteasyWebTarget target = client.target(connect.getServer().getServerUrl() + SmartConnect.API_URL);
			CtConnectClient simple = target.proxy(CtConnectClient.class);

			
			IJobChangeListener jobListener = new IJobChangeListener() {
				
				@Override
				public void sleeping(IJobChangeEvent event) {}
				
				@Override
				public void scheduled(IJobChangeEvent event) {}
				
				@Override
				public void running(IJobChangeEvent event) {}
				
				@Override
				public void done(IJobChangeEvent event) { 
					jobs.remove(event.getJob());
					checkdone(context.get(Shell.class));
				}
				
				@Override
				public void awake(IJobChangeEvent event) {}
				
				@Override
				public void aboutToRun(IJobChangeEvent event) {}
			};
			
			total = ctpackages.size() + navlayers.size();
			
			for (ICtPackage ctpackage: ctpackages) {			
				Path file = ctpackage.getLocalFile();
	
				String version = file.getFileName().toString();
				int index = version.lastIndexOf('.');
				version = version.substring(version.indexOf('.') + 1, index);
				
				CyberTrackerPackageProxy proxy = new CyberTrackerPackageProxy();
				proxy.setCaUuid(ctpackage.getConservationArea().getUuid());
				proxy.setName(ctpackage.getName());
				proxy.setVersion(version);
				
				Response response = simple.uploadCtPackage(Files.size(file), ctpackage.getUuid().toString(), proxy);
				if (response.getStatus() != Response.Status.OK.getStatusCode()) {
					throw new Exception(response.getStatusInfo().getReasonPhrase());
				}
				String location = response.getHeaderString(HttpHeaders.LOCATION);
				if (location == null) {
					throw new Exception(Messages.ExportCtPackageToConnect_NoUrl);
				}
				
				FileUploaderJob job = new FileUploaderJob(location, file, connect, Messages.ExportCtPackageToConnect_UploadJobName) {
	
					@Override
					protected void onUploadComplete(WorkItemStatus status) {
					}
	
					@Override
					protected void onProcessingComplete(WorkItemStatus status) {
						ok++;
					}
	
					@Override
					protected void onError(String errorMessage) {
						ConnectPlugIn.log(Messages.ExportCtPackageToConnect_UploadError + errorMessage, null);
					}
	
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							super.uploadFile(monitor);
						} catch (Exception e) {
							ConnectPlugIn.log(Messages.ExportCtPackageToConnect_UploadError +e.getMessage(), e);
						}
						return Status.OK_STATUS;
					}
					
				};
				jobs.add(job);
				job.addJobChangeListener(jobListener);
				job.schedule();
				
			}
			
			
			for (NavigationLayer nlayer : navlayers) {
				
				Path tempFile = Files.createTempFile("smartnav", "zip"); //$NON-NLS-1$ //$NON-NLS-2$
				ExportNavigationManager.INSTANCE.exportNavigationLayer(nlayer, tempFile);
				
				CyberTrackerNavigationProxy proxy = new CyberTrackerNavigationProxy();
				proxy.setCaLabel(nlayer.getConservationArea().getName());
				proxy.setCaUuid(nlayer.getConservationArea().getUuid());
				proxy.setName(nlayer.getName());
							
				Response response = simple.uploadNavigationLayer(Files.size(tempFile), nlayer.getUuid().toString(), proxy);
				if (response.getStatus() != Response.Status.OK.getStatusCode()) {
					throw new Exception(response.getStatusInfo().getReasonPhrase());
				}
				String location = response.getHeaderString(HttpHeaders.LOCATION);
				if (location == null) {
					throw new Exception(Messages.ExportCtPackageToConnect_NoUrl);
				}
				
				FileUploaderJob job = new FileUploaderJob(location, tempFile, connect, Messages.ExportCtPackageToConnect_UploadJobName) {
	
					@Override
					protected void onUploadComplete(WorkItemStatus status) {
						removeFile();
					}
	
					@Override
					protected void onProcessingComplete(WorkItemStatus status) {
						ok++;
					}
	
					@Override
					protected void onError(String errorMessage) {
						ConnectPlugIn.log(Messages.ExportCtPackageToConnect_UploadError + errorMessage, null);
						removeFile();
					}
					
					private void removeFile() {
						try {
							Files.deleteIfExists(tempFile);
						} catch (IOException e) {
							ConnectPlugIn.log(e.getMessage(), e);
						}
					}
	
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							super.uploadFile(monitor);
						} catch (Exception e) {
							ConnectPlugIn.log(Messages.ExportCtPackageToConnect_UploadError +e.getMessage(), e);
						}
						return Status.OK_STATUS;
					}
					
				};
				jobs.add(job);
				job.addJobChangeListener(jobListener);
				job.schedule();
				
			}
		} catch (Exception e) {
			ConnectPlugIn.displayLog(Messages.ExportCtPackageToConnect_NavUploadError + e.getMessage(), e);
		}
	}
	
	private void checkdone(Shell shell) {
		if (!jobs.isEmpty()) return;
		
		shell.getDisplay().syncExec(()->{
			if (ok == total) {
				MessageDialog.openInformation(shell, Messages.ExportCtPackageToConnect_CompleteTitle, MessageFormat.format(Messages.ExportCtPackageToConnect_UplodeCompleteMsg, ok, total));
			}else {
				MessageDialog.openInformation(shell, Messages.ExportCtPackageToConnect_CompleteTitle, MessageFormat.format(Messages.ExportCtPackageToConnect_UploadCompleteMsg2, ok, total));
			}
		});
	}

	@Override
	public String getName() {
		return Messages.ExportCtPackageToConnect_Name;
	}

	@Override
	public Image getIcon() {
		return ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER32_ICON);
	}

}
