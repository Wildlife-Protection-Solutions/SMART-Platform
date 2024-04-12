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
package org.wcs.smart.entity.ui;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.entity.EntityCsvExporter;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType.Type;
import org.wcs.smart.export.dialog.AbstractCsvDialog;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.ProjectionLabelProvider;
import org.wcs.smart.util.SmartUtils;

/**
 * Export entities dialog.
 * 
 * @author Emily
 *
 */
public class ExportEntityDialog extends AbstractCsvDialog {

	private EntityCsvExporter config;
	private Button chIncludeAll;
	private ComboViewer lstProjection = null;
	private boolean activeOnly = true;
	private Projection selectedPrj = null;
	
	public static final String EXPORT_ENTITIES_DIRKEY = "org.wcs.smart.export.entities.directory"; //$NON-NLS-1$
	
	/**
	 * @param parentShell
	 * @param config
	 */
	public ExportEntityDialog(Shell parentShell, EntityCsvExporter config) {
		super(parentShell, config.getDialogConfiguration());
		this.config = config;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			activeOnly = !chIncludeAll.getSelection();
			if (lstProjection != null){
				selectedPrj = (Projection)((StructuredSelection)lstProjection.getSelection()).getFirstElement();
			}else{
				selectedPrj = null;
			}
			EntityPlugIn.getDefault().getDialogSettings().put(EXPORT_ENTITIES_DIRKEY, (Paths.get(csvComposite.getFileText())).getParent().toString()); 
		}
		super.buttonPressed(buttonId);
	}
	
	@Override
	protected boolean performAction(Path file, char delimiter, boolean headers, Charset cs,
			IProgressMonitor monitor, Session session) throws Exception {
		
		config.setActiveOnly(activeOnly);
		config.setProjection(selectedPrj);
		
		return config.getDialogConfiguration().getExporter().exportCsvFile(file, delimiter, SmartDB.getCurrentConservationArea(), 
				headers, cs, monitor, session);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(csvComposite.getFileText().length() > 0);
	}
	
	@Override
	public Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		Composite fileComp = super.createFileComposite(comp, true);
		fileComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		if (config.getEntityType().getType() == Type.FIXED){
			//include projection for location
			//spacer
			new Label(fileComp, SWT.NONE);
			Label lblProj = new Label(fileComp, SWT.NONE);
			lblProj.setText(Messages.ExportEntityDialog_ProjectionLabel);
			
			lstProjection = new ComboViewer(fileComp, SWT.DROP_DOWN | SWT.READ_ONLY);
			lstProjection.getControl().setLayoutData(new GridData(SWT.FILL,  SWT.FILL,  true, false));
			lstProjection.setContentProvider(ArrayContentProvider.getInstance());
			lstProjection.setLabelProvider(ProjectionLabelProvider.getInstance());
			lstProjection.setInput(config.getProjectionOptions());
			lstProjection.setSelection(new StructuredSelection(config.getCurrentProjection()));
		}
		
		Composite custom = new Composite(comp, SWT.NONE);
		custom.setLayout(new GridLayout());
		chIncludeAll = new Button(custom, SWT.CHECK);
		chIncludeAll.setText(Messages.ExportEntityDialog_IncludeInactive);
		
		String file = EntityPlugIn.getDefault().getDialogSettings().get(EXPORT_ENTITIES_DIRKEY);
		if (file == null){
			file = System.getProperty("user.home"); //$NON-NLS-1$
		}
		Path init = Paths.get(file).resolve(URLUtils.cleanFilename(config.getEntityType().getName()) + ".csv"); //$NON-NLS-1$
		super.csvComposite.setFileText(init.toString());
		
		return comp;
	}
	
	/**
	 * ensure the export location exists and can be overwritten
	 * @param fileName
	 * @return
	 */
	@Override
	protected boolean validateFilename(String fileName){
		Path f = Paths.get(fileName);
		if (Files.exists(f)){
			boolean ok = MessageDialog.openQuestion(getShell(), Messages.ExportEntityDialog_ExportDialogTitle, MessageFormat.format(Messages.ExportEntityDialog_FileExists, new Object[]{f.toAbsolutePath().toString()}));
			if (!ok){
				return false;
			}
		}
		if (!Files.exists(f.getParent())){
			boolean ok = MessageDialog.openQuestion(getShell(), Messages.ExportEntityDialog_ExportDialogTitle, MessageFormat.format(Messages.ExportEntityDialog_DirectoryNotFound, new Object[]{f.getParent()}));
			if (ok){
				if (!SmartUtils.createDirectory(f.getParent())){
					return false;
				}
			}else{
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected List<String> getWarnings(){
		return null;
	}
}
