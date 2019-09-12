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
package org.wcs.smart.cybertracker.patrol.ui;

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
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.patrol.export.PatrolPackageExporter;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.PatrolCtPackage;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.SmartPatrolPlugIn;

/**
 * Package manager for patrol cybertracker packages.
 * 
 * @author Emily
 *
 */
public class PatrolCtPackageManager implements ICtPackageManager {

	public PatrolCtPackageManager() {
	}
	
	@Override
	public String getTypeIdentifier() {
		return PatrolCtPackage.TYPE_NAME;
	}

	@Override
	public String getTypeName() {
		return Messages.PatrolCtPackageManager_PatrolType;
	}
	
	@Override
	public Image getTypeImage() {
		return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL32_ICON);
	}
	
	@Override
	public List<? extends ICtPackage> getPackages(Session session) {
		return QueryFactory.buildQuery(session, PatrolCtPackage.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}) //$NON-NLS-1$
			.getResultList();
	}

	@Override
	public ICtPackage createPackage() {
		PatrolCtPackage ctpackage = new PatrolCtPackage();
		ctpackage.setConservationArea(SmartDB.getCurrentConservationArea());
		ctpackage.setName(Messages.PatrolCtPackageManager_DefaultName);
		return ctpackage;
	}
	
	
	@Override
	public void buildPackage(ICtPackage ctpackage, IEclipseContext context, Path output) throws IOException {
		if (Files.exists(output)) {
			Files.delete(output);
		}
		if (!Files.exists(output.getParent())) {
			Files.createDirectories(output.getParent());
		}
		
		final PatrolCtPackage ppackage = (PatrolCtPackage)ctpackage;
		
		final boolean[] iscancel = new boolean[] {false};
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						SubMonitor progress = SubMonitor.convert(monitor, Messages.PatrolCTPackageDialog_TaskName, ppackage.getConfigurableModel() == null ? 2 : 3);
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
							monitor.subTask(Messages.PatrolCTPackageDialog_DmToCmTaskName);
							try(Session session = HibernateManager.openSession()){
								toExport = (new DataModelWrapper()).buildConfigurableModel(session, progress.split(1));
								toExport.setConservationArea(SmartDB.getCurrentConservationArea());
							}
						}else {
							toExport = ppackage.getConfigurableModel();
						}
						ppackage.setConfigurableModel(toExport);
						progress.checkCanceled();
						PatrolPackageExporter.INSTANCE.exportPackage(ppackage, updates, output, context, progress.split(1));
					}catch(OperationCanceledException e) {
						iscancel[0] = true;
						Display.getDefault().syncExec(()->{
							MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.PatrolCTPackageDialog_CancelledTitle, Messages.PatrolCTPackageDialog_CancelledMsg);	
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

	@Override
	public ICtPackageConfigurator createConfigurator() {
		return new CtPatrolPackageConfigurator();
	}

}
