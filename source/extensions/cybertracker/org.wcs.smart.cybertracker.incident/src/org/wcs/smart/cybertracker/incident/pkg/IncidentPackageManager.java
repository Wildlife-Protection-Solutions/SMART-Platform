package org.wcs.smart.cybertracker.incident.pkg;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackageConfigurator;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackageManager;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.PackageContributionManager;
import org.wcs.smart.cybertracker.export.data.DataModelWrapper;
import org.wcs.smart.cybertracker.incident.internal.Messages;
import org.wcs.smart.cybertracker.incident.model.IncidentCtPackage;
import org.wcs.smart.cybertracker.incident.pkg.ui.CtIncidentPackageConfigurator;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentPlugIn;

public class IncidentPackageManager implements ICtPackageManager {

	public IncidentPackageManager() {
	}
	
	@Override
	public String getTypeIdentifier() {
		return IncidentCtPackage.TYPE_NAME;
	}

	@Override
	public String getTypeName() {
		return Messages.IncidentPackageManager_IncidentPackageName;
	}

	@Override
	public Image getTypeImage() {
		return IncidentPlugIn.getDefault().getImageRegistry().get(IncidentPlugIn.INCIDENT32_ICON);
	}
	
	@Override
	public List<? extends ICtPackage> getPackages(Session session) {
		return QueryFactory.buildQuery(session, IncidentCtPackage.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}) //$NON-NLS-1$
			.getResultList();
	}

	@Override
	public ICtPackage createPackage() {
		IncidentCtPackage ctpackage = new IncidentCtPackage();
		ctpackage.setConservationArea(SmartDB.getCurrentConservationArea());
		ctpackage.setName(Messages.IncidentPackageManager_IncidentPackageDefaultName);
		ctpackage.setMetadataValues(new ArrayList<>());
		return ctpackage;
	}

	@Override
	public ICtPackageConfigurator createConfigurator() {
		return new CtIncidentPackageConfigurator();
	}

	@Override
	public void buildPackage(ICtPackage ctpackage, IEclipseContext context, Path output) throws IOException {
		if (Files.exists(output)) {
			Files.delete(output);
		}
		if (!Files.exists(output.getParent())) {
			Files.createDirectories(output.getParent());
		}
		
		final IncidentCtPackage ppackage = (IncidentCtPackage)ctpackage;
		
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						SubMonitor progress = SubMonitor.convert(monitor, Messages.IncidentPackageManager_ExportTask, ppackage.getConfigurableModel() == null ? 2 : 3);
						List<IPackageContribution> contributions = PackageContributionManager.INSTANCE.getContributionItems();
				
						//process contributions
						List<IPackageContribution.PackageContribution> updates = new ArrayList<>();
						SubMonitor work = progress.split(1);
						if (contributions != null) {
							for (IPackageContribution cc : contributions) {
								IPackageContribution.PackageContribution update = cc.packageFiles(ppackage, context, work);
								if (update != null) updates.add(update);
							}
						}
						progress.checkCanceled();
						
						ConfigurableModel toExport = null;
						if (ppackage.getConfigurableModel() == null) {
							//convert data model to configurable model
							monitor.subTask(Messages.IncidentPackageManager_Progress);
							try(Session session = HibernateManager.openSession()){
								toExport = (new DataModelWrapper()).buildConfigurableModel(session, progress.split(1));
								toExport.setConservationArea(SmartDB.getCurrentConservationArea());
							}
						}else {
							toExport = ppackage.getConfigurableModel();
						}
						ppackage.setConfigurableModel(toExport);
						progress.checkCanceled();
						IncidentPackageExporter.INSTANCE.exportPackage(ppackage, updates, output, context, progress.split(1));
					}catch(OperationCanceledException e) {
						Display.getDefault().syncExec(()->{
							MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.IncidentPackageManager_CancelTitle, Messages.IncidentPackageManager_CancelMessage);	
						});
						
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					}		
				}
			});
		} catch (Exception e) {
			if (e.getCause() != null && e.getCause() instanceof IOException) throw ((IOException)e.getCause());
			throw new IOException(e);
		}	
	}
}
