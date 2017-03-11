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
package org.wcs.smart.i2.ui.views;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.birt.core.framework.IConfigurationElement;
import org.eclipse.birt.report.designer.internal.ui.util.UIHelper;
import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.EntityExportReportJob;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;

/**
 * Dialog for exporting multiple entities
 * 
 * @author Emily
 *
 */
public class EntityExportDialog extends TitleAreaDialog {

	private static final String DIR_KEY = "org.wcs.smart.i2.entity.export.directory"; //$NON-NLS-1$
	private List<IntelEntity> toExport;
	
	private DateFilterDropDownComposite dateComp;
	private Text txtDirectory;
	private TableComboViewer cmbFormat;
	
	public EntityExportDialog(Shell parentShell, List<IntelEntity> toExport) {
		super(parentShell);
		this.toExport = toExport;
		
	}
	
	@Override
	public void okPressed(){
		String dir = txtDirectory.getText();
		
		Path p = Paths.get(dir);
		if (!Files.exists(p)){
			if (!MessageDialog.openQuestion(getShell(), Messages.EntityExportDialog_ExportDialogTitle, MessageFormat.format(Messages.EntityExportDialog_DirectoryMsg, p.toString()))){
				return;
			}
			try{
				Path tmpdir = Files.createDirectories(p);
				if (tmpdir == null) throw new Exception(MessageFormat.format(Messages.EntityExportDialog_DirectoryNotCreated, p.toString()));
			}catch (Exception ex){
				Intelligence2PlugIn.displayLog(Messages.EntityExportDialog_ErrorCreatingDirectory, ex);
				return;
			}
		}
		try {
			if (Files.list(p).count() > 0){
				if (!MessageDialog.openQuestion(getShell(), Messages.EntityExportDialog_OverwriteTitle, MessageFormat.format(Messages.EntityExportDialog_OverwriteMsg, p.toString()))){
					return;
				}
			}
		} catch (IOException ex) {
			Intelligence2PlugIn.displayLog(Messages.EntityExportDialog_ErrorReadingOutputDir, ex);
			return;
		}
		DateFilter df = dateComp.getDateFilter();
		EntityExportReportJob job = new EntityExportReportJob(toExport, new Date[]{df.getStartDate(), df.getEndDate()},(EmitterInfo) ((IStructuredSelection)cmbFormat.getSelection()).getFirstElement(),p);
		job.schedule();
		
		Intelligence2PlugIn.getDefault().getDialogSettings().put(DIR_KEY, p.toString());
		
		super.okPressed();
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite)super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.EntityExportDialog_FormatLabel);
		
		cmbFormat = new TableComboViewer(main);
		cmbFormat.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbFormat.setContentProvider(ArrayContentProvider.getInstance());
		cmbFormat.setLabelProvider(new LabelProvider(){
			private HashMap<EmitterInfo, Image> images = new HashMap<>();
			public String getText(Object element){
				if (element instanceof EmitterInfo){
					return ((EmitterInfo) element).getFormat();
				}
				return super.getText(element);
			}
		
			
			@Override
			public Image getImage(Object element){
				Image x = images.get(element);
				if (x != null) return x;
				
				if (element instanceof EmitterInfo){
					IConfigurationElement confElem = ((EmitterInfo)element).getEmitter();
					if ( confElem != null && ((EmitterInfo)element).getIcon() != null){
						String pluginId = confElem.getDeclaringExtension( ).getNamespace( );
						Bundle bundle = Platform.getBundle( pluginId );
						x = UIHelper.getImage( bundle, ((EmitterInfo)element).getIcon(), false );
						images.put((EmitterInfo)element, x);
						return x;
					}
				}
				return null;
			}
		});
		EmitterInfo[] emitters = ReportEngineManager.getBirtReportEngine().getEmitterInfo();
		cmbFormat.setInput(emitters);
		if (emitters.length > 0) cmbFormat.setSelection(new StructuredSelection(emitters[0]));
		
		l = new Label(main, SWT.NONE);
		l.setText(Messages.EntityExportDialog_LocationLabel);
		
		txtDirectory = new Text(main, SWT.BORDER);
		txtDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button btnBrowse = new Button(main, SWT.PUSH);
		btnBrowse.setText("..."); //$NON-NLS-1$
		btnBrowse.addListener(SWT.MouseUp, e->{
			DirectoryDialog dd = new DirectoryDialog(getShell());
			dd.setFilterPath(txtDirectory.getText());
			dd.setMessage(Messages.EntityExportDialog_DDMessage);
			dd.setText(Messages.EntityExportDialog_DDText);
			String d = dd.open();
			if (d != null) txtDirectory.setText(d);
		});
		String file = Intelligence2PlugIn.getDefault().getDialogSettings().get(DIR_KEY);
		if (file != null){
			txtDirectory.setText(file);
		}
		
		
		l = new Label(main, SWT.NONE);
		l.setText(Messages.EntityExportDialog_DateRangeLabel);
		l.setToolTipText(Messages.EntityExportDialog_DateRangeTooltip);
		
		DateFilterComposite.DateFilter[] defaultFilters = new DateFilter[]{
				DateFilter.LAST_30_DAYS,
				DateFilter.LAST_60_DAYS,
				DateFilter.LAST_YEAR,
				DateFilter.LAST_5_YEARS,
				DateFilter.ALL,
				DateFilter.CUSTOM
		};
		dateComp = new DateFilterDropDownComposite(main, defaultFilters, DateFilter.LAST_YEAR);
		dateComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
    
		setTitle(Messages.EntityExportDialog_Title);
		setMessage(MessageFormat.format(Messages.EntityExportDialog_Message, toExport.size()));
		getShell().setText(Messages.EntityExportDialog_Title);
		return parent;
	}

	
	
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
}
