/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.dialogs.query;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.locationtech.udig.catalog.URLUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.export.dialog.DelimiterCombo;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.query.export.IQueryExporter;
import org.wcs.smart.i2.query.export.IQueryExporter.ExportOption;
import org.wcs.smart.ui.ProjectionLabelProvider;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Export query wizard page for selection query format options
 * @author Emily
 *
 */
public class QueryFormatOptionPage extends WizardPage {

	private Text txtFile = null;

	private Label lblDelimiter;
	private DelimiterCombo cmbDelimiter;
	private Label lblSpacer;
	
	private ComboViewer cmbProjection;
	private Label lblProjection;
	
	private Composite main;
	
	/**
	 * Creates a new query wizard page.
	 */
	protected QueryFormatOptionPage() {
		super("Export_Options"); //$NON-NLS-1$
	}

	/**
	 * Initializes the values in the query wizard
	 */
	public void initValues(){
		String location = getWizard().getDialogSettings() != null ? getWizard().getDialogSettings().get(ExportQueryWizard.LAST_DIR_KEY) : null;
		if (location == null){
			location = System.getProperty("user.home"); //$NON-NLS-1$
		}
		
		ExportQueryWizard wizard = (ExportQueryWizard) getWizard();
		IQueryExporter exporter = wizard.getQueryExporter();
		
		String initFile = wizard.getQuery().getName();
		initFile = location + File.separator + URLUtils.cleanFilename(initFile) + "." ; //$NON-NLS-1$
		initFile += exporter.getExtension();
		txtFile.setText( initFile );

		boolean isDelimiter = exporter.supportsOption(ExportOption.DELIMITER);
		boolean isProjection = exporter.supportsOption(ExportOption.PROJECTION);
		
		Control[] ctrs = new Control[]{lblDelimiter, cmbDelimiter == null ? null : cmbDelimiter.getControl(), lblSpacer, lblProjection, cmbProjection == null ? null : cmbProjection.getControl()};
		for (Control c : ctrs){
			if (c != null) c.dispose();
		}
		lblDelimiter = null;
		cmbDelimiter = null;
		lblSpacer = null;
		lblProjection = null;
		cmbProjection = null;
		
		if (isDelimiter){
			createDelimiterOption();
		}
		
		if (isProjection){
			createProjectionOption();
		}
		
		main.layout(true);
		setPageComplete(false);
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.QueryFormatOptionPage_LocationTitle);
		
		txtFile = new Text(main, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		txtFile.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (txtFile.getText().length() > 0) {
					setPageComplete(true);
				}
			}
		});		
		
		Button btnBrowse = new Button(main, SWT.NONE);
		btnBrowse.setText("..."); //$NON-NLS-1$
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String ext = ((ExportQueryWizard)getWizard()).getQueryExporter().getExtension();
				String name= ((ExportQueryWizard)getWizard()).getQueryExporter().getName(Locale.getDefault());

				FileDialog fd = new FileDialog(QueryFormatOptionPage.this.getShell(), SWT.SAVE);
					
				String[] extensions = new String[]{"*." + ext, "*.*"}; //$NON-NLS-1$ //$NON-NLS-2$
				String[] names = new String[]{name + " (*." + ext + ")",Messages.QueryFormatOptionPage_AllFileDialogOp}; //$NON-NLS-1$ //$NON-NLS-2$
					
				fd.setFilterExtensions(extensions);
				fd.setFilterNames(names);
					
				fd.setFilterPath(txtFile.getText());
				fd.setFileName(txtFile.getText());
					
				String f = fd.open();
				if (f != null) {
					txtFile.setText(f);
				}
			}
		});
		

		
		setTitle(MessageFormat.format(Messages.QueryFormatOptionPage_Title, ((ExportQueryWizard)getWizard()).getQuery().getName())); 
		setMessage(Messages.QueryFormatOptionPage_Message);
		setPageComplete(false);
		setControl(main);
	}

	private void createDelimiterOption(){
		lblDelimiter = new Label(main, SWT.NONE);
		lblDelimiter.setText(Messages.QueryFormatOptionPage_DelimiterOp);
		lblDelimiter.setToolTipText(Messages.QueryFormatOptionPage_DelimiterTooltip);
		
		cmbDelimiter = new DelimiterCombo(main,  SWT.DROP_DOWN);
	
		lblSpacer = new Label(main, SWT.NONE);
	}
	
	private void createProjectionOption(){

		ExportQueryWizard wizard = (ExportQueryWizard) getWizard();
		
		lblProjection = new Label(main, SWT.NONE);
		lblProjection.setText(Messages.QueryFormatOptionPage_ProjectionOp);
		lblProjection.setToolTipText(Messages.QueryFormatOptionPage_ProjectionTooltip);
		
		cmbProjection = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbProjection.setContentProvider(ArrayContentProvider.getInstance());
		cmbProjection.setLabelProvider(ProjectionLabelProvider.getInstance());
		cmbProjection.setInput( wizard.getSupportedProjections()  );
		cmbProjection.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (wizard.getDefaultProjection() != null){
			cmbProjection.setSelection(new StructuredSelection(wizard.getDefaultProjection()));
		}else{
			cmbProjection.setSelection(new StructuredSelection(wizard.getSupportedProjections().get(0)));
		}
	}
	
	public Projection getProjection(){
		if (cmbProjection == null) return null;
		return (Projection)((StructuredSelection)cmbProjection.getSelection()).getFirstElement();
	}
	
	/**
	 * @return the selected file
	 */
	public Path getFile(){
		return Paths.get(txtFile.getText());
	}
	
	public HashMap<IQueryExporter.ExportOption, Object> getOptions() throws Exception{
		HashMap<IQueryExporter.ExportOption, Object> ops = new HashMap<>();
		if (cmbDelimiter != null){
			ops.put(IQueryExporter.ExportOption.DELIMITER, new Character(cmbDelimiter.getDelimiter()));
		}
		if (cmbProjection != null){
			Projection p = getProjection();
			CoordinateReferenceSystem crs = p.getParsedCoordinateReferenceSystem();
			if (crs == null){
				crs = ReprojectUtils.stringToCrs(getProjection().getDefinition());
				p.setParsedCoordinateReferenceSystem(crs);
			}
			ops.put(IQueryExporter.ExportOption.PROJECTION, p);
		}
		return ops;
	}
	
}
