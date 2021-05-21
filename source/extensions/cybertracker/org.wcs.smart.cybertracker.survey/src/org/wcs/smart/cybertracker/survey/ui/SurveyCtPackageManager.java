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
package org.wcs.smart.cybertracker.survey.ui;

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
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.survey.export.SurveyPackageExporter;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.cybertracker.survey.model.SurveyCtPackage;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Package manager for survey cybertracker packages.
 * 
 * @author Emily
 *
 */
public class SurveyCtPackageManager implements ICtPackageManager {

	public SurveyCtPackageManager() {
	}
	
	@Override
	public String getTypeIdentifier() {
		return SurveyCtPackage.TYPE_NAME;
	}

	@Override
	public String getTypeName() {
		return Messages.SurveyCtPackageManager_PackageType;
	}
	
	@Override
	public Image getTypeImage() {
		return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY32_ICON);
	}
	
	@Override
	public List<? extends ICtPackage> getPackages(Session session) {
		List<SurveyCtPackage> items = QueryFactory.buildQuery(session, SurveyCtPackage.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}) //$NON-NLS-1$
			.getResultList();
		items.forEach(e->e.isDataModel());
		return items;
	}

	@Override
	public ICtPackage createPackage() {
		SurveyCtPackage ctpackage = new SurveyCtPackage();
		ctpackage.setConservationArea(SmartDB.getCurrentConservationArea());
		ctpackage.setName(Messages.SurveyCtPackageManager_DefaultPackageName);
		ctpackage.setMetadataValues(new ArrayList<>());

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
		
		final SurveyCtPackage ppackage = (SurveyCtPackage)ctpackage;
		
		final boolean[] iscancel = new boolean[] {false};
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					SubMonitor progress = SubMonitor.convert(monitor, Messages.SurveyCTPackageDialog_ExportTaskName, 2);
					
					try {
						//process contributions
						List<IPackageContribution> contributions = PackageContributionManager.INSTANCE.getContributionItems();
						List<IPackageContribution.PackageContribution> updates = new ArrayList<>();
						SubMonitor work = progress.split(1);
						if (contributions != null) {
							for (IPackageContribution cc : PackageContributionManager.INSTANCE.getContributionItems()) {
								IPackageContribution.PackageContribution update = cc.packageFiles(ppackage, context, work);
								if (update != null) updates.add(update);
							}
						}
						
						SurveyPackageExporter.INSTANCE.exportPackage(ppackage, output, updates, context, progress.split(1));
					}catch(OperationCanceledException e) {
						iscancel[0] = true;
						Display.getDefault().syncExec(()->{
							MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.SurveyCTPackageDialog_CancelledTitle, Messages.SurveyCTPackageDialog_CancelledMsg);	
						});
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					}				
				}
			});
		} catch (Exception e) {
			throw new IOException(e.getCause() != null ? e.getCause() : e);
		}	
	}

	@Override
	public ICtPackageConfigurator createConfigurator() {
		return new CtSurveyPackageConfigurator();
	}

}
