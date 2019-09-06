package org.wcs.smart.connect.cybertracker.navigation;

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
import org.wcs.smart.connect.cybertracker.ctpackage.CtConnectClient;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerNavigationProxy;
import org.wcs.smart.connect.internal.server.FileUploaderJob;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.cybertracker.model.NavigationLayer;
import org.wcs.smart.cybertracker.navigation.ExportNavigationManager;
import org.wcs.smart.cybertracker.navigation.INavigationExportAction;

public class ExportNavigationLayerToConnect implements INavigationExportAction {

	private List<Job> jobs = new ArrayList<>();
	private int ok;
	private int total;

	@Override
	public void doAction(List<NavigationLayer> layers, IEclipseContext context) {
		try {
			SmartConnect connect = context.get(SmartConnect.class);
			if (connect == null) {
				ConnectDialog cd = new ConnectDialog(context.get(Shell.class), true) {
					@Override
					protected Control createDialogArea(Composite parent) {
						setTitle("Export Navigation Layer To Connect");
						getShell().setText("Export Navigation Layer To Connect");
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
				public void sleeping(IJobChangeEvent event) {
				}

				@Override
				public void scheduled(IJobChangeEvent event) {
				}

				@Override
				public void running(IJobChangeEvent event) {
				}

				@Override
				public void done(IJobChangeEvent event) {
					jobs.remove(event.getJob());
					checkdone(context.get(Shell.class));
				}

				@Override
				public void awake(IJobChangeEvent event) {
				}

				@Override
				public void aboutToRun(IJobChangeEvent event) {
				}
			};

			total = layers.size();

			Path tempDir = Files.createTempDirectory("smartnav");
			System.out.println(tempDir);
			for (NavigationLayer navigationlayer : layers) {
				
				Path tempFile = tempDir.resolve(ExportNavigationManager.INSTANCE.getExportFileName(navigationlayer));
				ExportNavigationManager.INSTANCE.exportNavigationLayer(navigationlayer, tempFile);

				CyberTrackerNavigationProxy proxy = new CyberTrackerNavigationProxy();
				proxy.setCaUuid(navigationlayer.getConservationArea().getUuid());
				proxy.setName(navigationlayer.getName());
				
				Response response = simple.uploadNavigationLayer(Files.size(tempFile), 
						navigationlayer.getUuid().toString(), proxy);
				
				if (response.getStatus() != Response.Status.OK.getStatusCode()) {
					throw new Exception(response.getStatusInfo().getReasonPhrase());
				}
				String location = response.getHeaderString(HttpHeaders.LOCATION);
				if (location == null) {
					throw new Exception("Response does not contain upload url");
				}

				FileUploaderJob job = new FileUploaderJob(location, tempFile, connect, "Upload SMART Navigation Layer") {
					@Override
					protected void onUploadComplete(WorkItemStatus status) {
						delete();
					}

					@Override
					protected void onProcessingComplete(WorkItemStatus status) {
						ok++;
					}

					private void delete() {
						super.deleteLocalFile();
						try {
							if (Files.list(tempDir).count() == 0) Files.delete(tempDir);
						}catch (Exception ex) {
							ConnectPlugIn.log(ex.getMessage(), ex);
						}
					}
					
					@Override
					protected void onError(String errorMessage) {
						delete();
						ConnectPlugIn.displayLog("Error uploading Navigation Layer to Connect: " + errorMessage, null);
					}

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							super.uploadFile(monitor);
						} catch (Exception e) {
							ConnectPlugIn.displayLog("Error uploading Navigation Layer to Connect: " + e.getMessage(), e);
						}
						return Status.OK_STATUS;
					}

				};
				jobs.add(job);
				job.addJobChangeListener(jobListener);
				job.schedule();

			}
		} catch (Exception e) {
			ConnectPlugIn.displayLog("Error uploading Navigation Layer to Connect: " + e.getMessage(), e);
		}
	}

	private void checkdone(Shell shell) {
		if (!jobs.isEmpty())
			return;

		shell.getDisplay().syncExec(() -> {
			if (ok == total) {
				MessageDialog.openInformation(shell, "Upload Complete",
						MessageFormat.format("Uploaded {0} of {1} package(s) to connect.", ok, total));
			} else {
				MessageDialog.openInformation(shell, "Upload Complete",
						MessageFormat.format("Uploaded {0} of {1} package(s) to connect. See error logs for error details.", ok, total));
			}
		});
	}

	@Override
	public String getName() {
		return "Export To Connect";
	}

	@Override
	public Image getIcon() {
		return ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER32_ICON);
	}
}
