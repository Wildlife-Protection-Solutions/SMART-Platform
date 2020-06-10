package org.wcs.smart.smartcollect.pkg;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
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
import org.wcs.smart.connect.cybertracker.ctpackage.ConnectAlertContribution;
import org.wcs.smart.connect.cybertracker.ctpackage.ConnectDataContribution;
import org.wcs.smart.connect.cybertracker.ctpackage.ConnectUrlContribution;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackageConfigurator;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackageManager;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.PackageContributionManager;
import org.wcs.smart.cybertracker.export.data.DataModelWrapper;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.smartcollect.SmartCollectPlugIn;
import org.wcs.smart.smartcollect.connect.SmartCollectConnectAlertContribution;
import org.wcs.smart.smartcollect.connect.SmartCollectConnectDataContribution;
import org.wcs.smart.smartcollect.connect.SmartCollectConnectUrlContribution;
import org.wcs.smart.smartcollect.model.SmartCollectPackage;
import org.wcs.smart.smartcollect.ui.SmartCollectPackageConfigurator;

public class SmartCollectPackageManager implements ICtPackageManager {
	

	@Override
	public String getTypeIdentifier() {
		return SmartCollectPackage.PACKAGE_TYPENAME;
	}

	@Override
	public String getTypeName() {
		return "SMART Collect Package";
	}

	@Override
	public Image getTypeImage() {
		return SmartCollectPlugIn.getDefault().getImageRegistry().get(SmartCollectPlugIn.SMARTCOLLECT32_ICON);
	}
	
	@Override
	public List<? extends ICtPackage> getPackages(Session session) {
		return QueryFactory.buildQuery(session, SmartCollectPackage.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}) //$NON-NLS-1$
			.getResultList();
	}

	@Override
	public ICtPackage createPackage() {
		SmartCollectPackage ctpackage = new SmartCollectPackage();
		ctpackage.setConservationArea(SmartDB.getCurrentConservationArea());
		ctpackage.setName("SMART Collect");
		return ctpackage;
	}

	@Override
	public ICtPackageConfigurator createConfigurator() {
		return new SmartCollectPackageConfigurator();
	}

	@Override
	public void buildPackage(ICtPackage ctpackage, IEclipseContext context, Path output) throws IOException {
		if (Files.exists(output)) {
			Files.delete(output);
		}
		if (!Files.exists(output.getParent())) {
			Files.createDirectories(output.getParent());
		}
		
		final SmartCollectPackage ppackage = (SmartCollectPackage)ctpackage;
		
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						SubMonitor progress = SubMonitor.convert(monitor, "Exporting SMART Collect Package", ppackage.getConfigurableModel() == null ? 2 : 3);
						List<IPackageContribution> contributions = PackageContributionManager.INSTANCE.getContributionItems();
						contributions.add(new SmartCollectConnectDataContribution());
						contributions.add(new SmartCollectConnectUrlContribution());
						contributions.add(new SmartCollectConnectAlertContribution());
						
						//remove the connect 
						for (Iterator<IPackageContribution> iterator = contributions.iterator(); iterator.hasNext();) {
							IPackageContribution iPackageContribution = (IPackageContribution) iterator.next();
							if (iPackageContribution.getClass().equals(ConnectDataContribution.class)) iterator.remove();
							if (iPackageContribution.getClass().equals(ConnectUrlContribution.class)) iterator.remove();
							if (iPackageContribution.getClass().equals(ConnectAlertContribution.class)) iterator.remove();
						}	
						
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
							monitor.subTask("Converting Data Model");
							try(Session session = HibernateManager.openSession()){
								toExport = (new DataModelWrapper()).buildConfigurableModel(session, progress.split(1));
								toExport.setConservationArea(SmartDB.getCurrentConservationArea());
							}
						}else {
							toExport = ppackage.getConfigurableModel();
						}
						ppackage.setConfigurableModel(toExport);
						progress.checkCanceled();
						SmartCollectPackageExporter.INSTANCE.exportPackage(ppackage, updates, output, context, progress.split(1));
					}catch(OperationCanceledException e) {
						Display.getDefault().syncExec(()->{
							MessageDialog.openError(Display.getCurrent().getActiveShell(), "Cancelled", "User cancelled export process.");	
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
