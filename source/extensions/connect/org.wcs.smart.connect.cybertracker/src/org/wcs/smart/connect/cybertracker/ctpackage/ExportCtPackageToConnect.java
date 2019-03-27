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
import org.wcs.smart.connect.cybertracker.model.CyberTrackerPackageProxy;
import org.wcs.smart.connect.internal.server.FileUploaderJob;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtExportAction;
import org.wcs.smart.cybertracker.model.ICtPackage;

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
	public void doAction(List<ICtPackage> ctpackages, IEclipseContext context) {
		try {
			SmartConnect connect = context.get(SmartConnect.class);
			if (connect == null) {
				ConnectDialog cd = new ConnectDialog(context.get(Shell.class), true){
					@Override
					protected Control createDialogArea(Composite parent) {
						setTitle("Export Package To Connect");
						getShell().setText("Export CyberTracker Package To Connect");
						setMessage("Configure SMART Connect details for export");	
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
			
			total = ctpackages.size();
			
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
					throw new Exception("Response does not contain upload url");
				}
				
				FileUploaderJob job = new FileUploaderJob(location, file, connect, "Upload Cybertracker Package") {
	
					@Override
					protected void onUploadComplete(WorkItemStatus status) {
					}
	
					@Override
					protected void onProcessingComplete(WorkItemStatus status) {
						ok++;
					}
	
					@Override
					protected void onError(String errorMessage) {
						ConnectPlugIn.log("Error uploading package to connect: " + errorMessage, null);
					}
	
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							super.uploadFile(monitor);
						} catch (Exception e) {
							ConnectPlugIn.log("Error uploading package to Connect: " +e.getMessage(), e);
						}
						return Status.OK_STATUS;
					}
					
				};
				jobs.add(job);
				job.addJobChangeListener(jobListener);
				job.schedule();
				
			}
		} catch (Exception e) {
			ConnectPlugIn.displayLog("Error uploading package to Connect: " + e.getMessage(), e);
		}
	}
	
	private void checkdone(Shell shell) {
		if (!jobs.isEmpty()) return;
		
		shell.getDisplay().syncExec(()->{
			if (ok == total) {
				MessageDialog.openInformation(shell, "Upload Complete", MessageFormat.format("Uploaded {0} of {1} package(s) to connect.", ok, total));
			}else {
				MessageDialog.openInformation(shell, "Upload Complete", MessageFormat.format("Uploaded {0} of {1} package(s) to connect. See error logs for error details.", ok, total));
			}
		});
	}

	@Override
	public String getName() {
		return "Export To Connect";
	}

	@Override
	public Image getIcon() {
		return null;
	}

}
