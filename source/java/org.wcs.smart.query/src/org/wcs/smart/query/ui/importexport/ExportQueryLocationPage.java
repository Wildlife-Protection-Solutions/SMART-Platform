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
package org.wcs.smart.query.ui.importexport;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.locationtech.udig.catalog.URLUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.export.dialog.DelimiterCombo;
import org.wcs.smart.query.importexport.ICsvQueryExporter;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.ui.ProjectionLabelProvider;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Query wizard page to select the output file format.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ExportQueryLocationPage extends WizardPage {

	private Text txtFile = null;

	private Label lblDelimiter;
	private DelimiterCombo cmbDelimiter;
	private Label lblSpacer;
	
	private Label lblCharset;
	private ComboViewer cmbCharset;
	private Label lblSpacer2;
	
	private ComboViewer cmbProjection;
	private Label lblProjection;
	private Label lblSpacer3;
	
	private Composite main;
	
	/**
	 * Creates a new query wizard page.
	 */
	protected ExportQueryLocationPage() {
		super(Messages.ExportQueryLocationPage_PageName);
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
		if (wizard.getQuery().getId() != null){
			initFile = initFile + "_" + wizard.getQuery().getId(); //$NON-NLS-1$
		}
		initFile = location + File.separator + URLUtils.cleanFilename(initFile) + "." ; //$NON-NLS-1$
		
		if (exporter == null){
			initFile += ".txt"; //$NON-NLS-1$
		}else{
			if (exporter.getDefaultExtension() != null){
				initFile += exporter.getDefaultExtension();
			}else{
				initFile = location;
			}
		}
		txtFile.setText( initFile );

		boolean isDelimiter =  ((ExportQueryWizard)getWizard()).getQueryExporter() instanceof ICsvQueryExporter;
		
		Object[] ctrs = new Object[]{lblDelimiter, cmbDelimiter, lblSpacer, lblSpacer3, lblProjection, cmbProjection, lblSpacer2, cmbCharset, lblCharset};
		
		for (Object c : ctrs){
			if (c == null) continue;
			if (c instanceof Control) ((Control) c).dispose();
			if (c instanceof ComboViewer) ((ComboViewer)c).getControl().dispose();
			if (c instanceof DelimiterCombo) ((DelimiterCombo)c).getControl().dispose();
		}
		
		lblDelimiter = null;
		cmbDelimiter = null;
		lblSpacer = null;
		lblProjection = null;
		cmbProjection = null;
		lblCharset = null;
		lblSpacer2 = null;
		lblSpacer3 = null;
		cmbCharset = null;
		
		if (isDelimiter){
			createDelimiterOption();	
		}
		
		if (exporter != null && exporter.supportsProjection()){
			createProjectionOption();
		}
		
		if (exporter != null && exporter.supportsCharEncodings()){
			createCharsetOption();
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
		lbl.setText(Messages.ExportQueryLocationPage_FileLabel);
		
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
		btnBrowse.setText(Messages.ExportQueryLocationPage_BrowseButton);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String ext = ((ExportQueryWizard)getWizard()).getQueryExporter().getDefaultExtension();
				String name= ((ExportQueryWizard)getWizard()).getQueryExporter().getName();
				if (ext != null){
					FileDialog fd = new FileDialog(ExportQueryLocationPage.this.getShell(), SWT.SAVE);
					
					String[] extensions = new String[]{"*." + ext, "*.*"}; //$NON-NLS-1$ //$NON-NLS-2$
					String[] names = new String[]{name + " (*." + ext + ")", Messages.ExportQueryLocationPage_AllFilesFilterName}; //$NON-NLS-1$ //$NON-NLS-2$
					
					fd.setFilterExtensions(extensions);
					fd.setFilterNames(names);
					
					fd.setFilterPath(txtFile.getText());
					fd.setFileName(txtFile.getText());
					
					String f = fd.open();
					if (f != null) {
						txtFile.setText(f);
					}
				}else{
					DirectoryDialog fd = new DirectoryDialog(ExportQueryLocationPage.this.getShell());
					fd.setFilterPath(txtFile.getText());
					
					String f = fd.open();
					if (f != null){
						txtFile.setText(f);
					}
				}
			}
		});
		
		setTitle(Messages.ExportQueryLocationPage_PageTitle + ": " + ((ExportQueryWizard)getWizard()).getQuery().getName()); //$NON-NLS-1$
		setMessage(Messages.ExportQueryLocationPage_DialogMessage);
		setPageComplete(false);
		setControl(main);
	}

	private void createCharsetOption() {
		lblCharset = new Label(main, SWT.NONE);
		lblCharset.setText(Messages.ExportQueryLocationPage_CharsetLabel);
		lblCharset.setToolTipText(Messages.ExportQueryLocationPage_Charsettooltip);
		lblCharset.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		cmbCharset = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbCharset.setContentProvider(ArrayContentProvider.getInstance());
		cmbCharset.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((Charset)element).displayName();
			}
		});
		cmbCharset.setInput( Charset.availableCharsets().values() );
		cmbCharset.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbCharset.setSelection(new StructuredSelection(StandardCharsets.UTF_8));
		cmbCharset.getControl().setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		lblSpacer2 = new Label(main, SWT.NONE);
		lblSpacer2.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
	}
	
	private void createDelimiterOption(){
		lblDelimiter = new Label(main, SWT.NONE);
		lblDelimiter.setText(Messages.ExportQueryLocationPage_delimiterLabel);
		lblDelimiter.setToolTipText(Messages.ExportQueryLocationPage_delimiterTooltip);
		lblDelimiter.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		cmbDelimiter = new DelimiterCombo(main,  SWT.DROP_DOWN);
		cmbDelimiter.getControl().setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		lblSpacer = new Label(main, SWT.NONE);
		lblSpacer.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
	}
	
	private void createProjectionOption(){

		ExportQueryWizard wizard = (ExportQueryWizard) getWizard();
		
		lblProjection = new Label(main, SWT.NONE);
		lblProjection.setText(Messages.ExportQueryLocationPage_ProjectionLbl);
		lblProjection.setToolTipText(Messages.ExportQueryLocationPage_ProjectionTooltip);
		lblProjection.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
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
		cmbProjection.getControl().setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		lblSpacer3 = new Label(main, SWT.NONE);
		lblSpacer3.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
	}
	
	public Projection getProjection(){
		if (cmbProjection == null) return null;
		return (Projection)((StructuredSelection)cmbProjection.getSelection()).getFirstElement();
	}
	/**
	 * @return the selected file
	 */
	public File getFile(){
		return new File(txtFile.getText());
	}
	
	public HashMap<String, Object> getOptions() throws Exception{
		HashMap<String, Object> ops = new HashMap<String, Object>();
		if (cmbDelimiter != null){
			ops.put(ICsvQueryExporter.DELIMITER_KEY, cmbDelimiter.getDelimiter());
		}
		if (cmbProjection != null){
			CoordinateReferenceSystem crs = getProjection().getParsedCoordinateReferenceSystem();
			if (crs == null){
				crs = ReprojectUtils.stringToCrs(getProjection().getDefinition());
				getProjection().setParsedCoordinateReferenceSystem(crs);
			}
			ops.put(IQueryExporter.PROJECTION_PARAM_KEY, getProjection());
		}
		if (cmbCharset != null) {
			ops.put(IQueryExporter.ENCODING_KEY, cmbCharset.getStructuredSelection().getFirstElement());
		}
		
		if (ops.isEmpty()) return null;
		return ops;
		
	}
	 public IWizardPage getNextPage() {
		 return null;
	 }
	 
	 
	
}
