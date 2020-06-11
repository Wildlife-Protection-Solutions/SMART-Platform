/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
import org.wcs.smart.smartcollect.internal.Messages;
import org.wcs.smart.smartcollect.model.SmartCollectPackage;
import org.wcs.smart.smartcollect.ui.SmartCollectPackageConfigurator;

/**
 * Package manager fo SMARTCollect packages
 * 
 * @author Emily
 *
 */
public class SmartCollectPackageManager implements ICtPackageManager {
	
	/**
	 * Mobile value identifying the waypoints as a smartcollect waypoint
	 */
	public static final String SMARTCOLLECT_RESOURCE_ID = "smartcollect"; //$NON-NLS-1$
	
	/**
	 * Mobile device setting key for option to collect incidents by group
	 */
	public static final String INCIDENT_GROUPUI_KEY = "INCIDENT_GROUP_UI"; //$NON-NLS-1$
	
	/**
	 * SMARTCollect package metadata key for username field
	 */
	public static final String USERNAMEMETADATA_KEY = "SMART_CollectUser"; //$NON-NLS-1$

	/**
	 * SMART Metadata package field value for option to collect incidents
	 * by group 
	 */
	public static final String COLLECT_GROUPS_FIELDKEY = "CollectGroupsKey"; //$NON-NLS-1$

	
	@Override
	public String getTypeIdentifier() {
		return SmartCollectPackage.PACKAGE_TYPENAME;
	}

	@Override
	public String getTypeName() {
		return Messages.SmartCollectPackageManager_PackageTypeName;
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
		ctpackage.setName(Messages.SmartCollectPackageManager_DefaultPackageName);
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
						SubMonitor progress = SubMonitor.convert(monitor, Messages.SmartCollectPackageManager_ExportTaskName, ppackage.getConfigurableModel() == null ? 2 : 3);
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
							monitor.subTask(Messages.SmartCollectPackageManager_CovertTaskName);
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
							MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.SmartCollectPackageManager_CancelledTitle, Messages.SmartCollectPackageManager_CancelledMessage);	
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
