/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.ui.ca.datamodel;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelToXmlConverter;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelToXmlConverter.IconOption;
import org.wcs.smart.internal.ca.datamodel.xml.v12.DataModelSmartToXmlConverter;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Wizard to create a data model expor.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ExportDataModelWizard extends Wizard implements IPageChangingListener {

	public static final String FILE_KEY = ExportDataModelWizard.class.getCanonicalName() + ".file"; //$NON-NLS-1$
	
	private boolean canFinish = false;
	
	private ExportDataModelPage1 page1;
	private ExportDataModelPage2 page2;
	
	private DataModel dataModel;
	/**
	 * Creates a new wizard.
	 */
	public ExportDataModelWizard(DataModel dataModel) {
		this.dataModel = dataModel;
		setWindowTitle(Messages.ExportDataModelWizard_ExportDm);
	}

	/**
	 * Sets if the wizard can finish
	 * 
	 * @param canFinish
	 *            if the wizard can finish
	 */
	public void setCanFinish(boolean canFinish) {
		this.canFinish = canFinish;
		getContainer().updateButtons();
	}

	/**
	 * Closes the active session
	 */
	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public boolean canFinish() {
		return super.canFinish() && this.canFinish && getContainer().getCurrentPage() == page2;
	}
	
	public IconOption getIconOption() {
		return page1.getOption();
	}

	@Override
	public void addPages() {
		page1 = new ExportDataModelPage1();
		page2 = new ExportDataModelPage2();
		super.addPage(page1);
		super.addPage(page2);
		((WizardDialog) getContainer()).addPageChangingListener(this);
	}
	
	/**
	 * Creates the patrol leg days then saved the patrol to the database.
	 */
	@Override
	public boolean performFinish() {
	
		final IconOption op = getIconOption();
		Path f = Paths.get(page2.getFile());
		
		SmartPlugIn.getDefault().getPreferenceStore().setValue(FILE_KEY, f.toString());
		
		if (!f.getFileName().toString().endsWith(op.getFileType())) {
			//add correct extension
			f = f.getParent().resolve(f.getFileName().toString() + "." + op.getFileType()); //$NON-NLS-1$
		}
		if (Files.exists(f)){
			if (!MessageDialog.openQuestion(getShell(), Messages.DataModelPropertyPage_OverwriteFile_DialogTitle, 
					MessageFormat.format(Messages.DataModelPropertyPage_OverwriteFile_DialogMessage, new Object[]{ f.getFileName()}))){
				return false;
			}
		}
		if (!Files.exists(f.getParent())) {
			try {
				Files.createDirectories(f.getParent());
			} catch (IOException e) {
				SmartPlugIn.displayLog(e.getMessage(), e);
				return false;
			}
		}
		
		final Path outFile = f;
		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor pmonitor)
						throws InvocationTargetException, InterruptedException {
					SubMonitor monitor = SubMonitor.convert(pmonitor);
					try{
						monitor.beginTask(Messages.DataModelPropertyPage_Progress_ExportingXml, op == IconOption.NONE ? 2 : 3);
						
						monitor.subTask(Messages.DataModelPropertyPage_Progress_ConvertingXml);
						DataModelSmartToXmlConverter converter = new DataModelSmartToXmlConverter(op, monitor.split(1));
						org.wcs.smart.internal.ca.datamodel.xml.generate.v12.DataModel xml = converter.convert(dataModel);
						
						Path outputFile = outFile;
						if (op != IconOption.NONE) {
							Path dir = Files.createTempDirectory("smart_dmexport"); //$NON-NLS-1$
							outputFile = dir.resolve("datamodel.xml"); //$NON-NLS-1$
						}
						
						monitor.subTask(Messages.DataModelPropertyPage_Progress_WritingXml);
						try(OutputStream fout = Files.newOutputStream(outputFile)){
							DataModelToXmlConverter.writeDataModel(xml,fout);
						}
						monitor.worked(1);
						
						if (op != IconOption.NONE) {
							Set<Path> files = new HashSet<>(converter.getIconFiles());
							files.add(outputFile);
							ZipUtil.createZip(files, outFile, monitor.split(1));
							
							//delete temp files
							SmartUtils.deleteDirectory(outputFile.getParent());
						}
					
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								MessageDialog.openInformation(pmd.getShell(), 
										Messages.DataModelPropertyPage_ExportSuccess_DialogTitle, 
										MessageFormat.format(Messages.ExportDataModelWizard_DmExported, outFile.toString()));
							}});
						
					}catch (final Exception ex){
						throw new InvocationTargetException(ex);
					}
				}
			});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.DataModelPropertyPage_Error_XmlExport, ex);
			return false;
		}
		
		return true;
	}

	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getTargetPage() == page2) {
			page2.updateFileName();
		}
	}

}
