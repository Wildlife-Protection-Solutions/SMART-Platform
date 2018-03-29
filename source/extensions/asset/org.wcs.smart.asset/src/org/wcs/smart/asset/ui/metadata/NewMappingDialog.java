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
package org.wcs.smart.asset.ui.metadata;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.AssetMetadataMapping.MetadataType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * New dialog for mapping image/file metadata to data model elements 
 * 
 * @author Emily
 *
 */
public class NewMappingDialog extends TitleAreaDialog {

	private Button btnExif;
	private Button btnXmp;
	
	private List<AssetMetadataMapping> mappings;
	
	private NewMappingExif exifPanel;
	private NewMappingXmp xmpPanel; 
	
	private DataModel cachedDm;
	
	private AssetMetadataMapping toEdit = null;
	
	public NewMappingDialog(Shell parentShell) {
		super(parentShell);
	}

	public NewMappingDialog(Shell parentShell, AssetMetadataMapping toEdit) {
		super(parentShell);
		this.toEdit = toEdit;
	}
	
	public AssetMetadataMapping getEditItem() {
		return this.toEdit;
	}
	
	public List<AssetMetadataMapping> getMappings() {
		return mappings;
	}

	public DataModel getCachedDataModel() {
		if (cachedDm == null) {
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			try {
				pmd.run(true, false,  new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask(Messages.NewMappingDialog_LoadingTaskName, 2);
						
						try(Session session = HibernateManager.openSession()){
							List<Category> cats = QueryFactory.buildQuery(session, Category.class, 
									new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
									new Object[] {"parent", null}).list(); //$NON-NLS-1$
							
							List<Category> toVisit = new ArrayList<>();
							toVisit.addAll(cats);
							while(!toVisit.isEmpty()) {
								Category c = toVisit.remove(0);
								c.getName();
								c.getAttributes().forEach(e->e.getAttribute().getName());
								toVisit.addAll(c.getChildren());
							}
							monitor.worked(1);
							
							List<Attribute> atts = QueryFactory.buildQuery(session, Attribute.class,
									new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
							List<AttributeTreeNode> toVisit2 = new ArrayList<>();
							for (Attribute a : atts) {
								a.getName();
								if (a.getAttributeList() != null) a.getAttributeList().forEach(b->b.getName());
								if (a.getTree() != null) toVisit2.addAll(a.getTree());
							}
							while(!toVisit2.isEmpty()) {
								AttributeTreeNode n = toVisit2.remove(0);
								n.getName();
								toVisit2.addAll(n.getChildren());
							}
							monitor.worked(1);
							cachedDm = new DataModel(SmartDB.getCurrentConservationArea(), cats, atts);
						}
					}
				});
			}catch (Exception ex) {
				AssetPlugIn.displayLog(ex.getMessage(), ex);
			}
		}
		return this.cachedDm;
	}
	
	@Override
	protected void okPressed() {
		if (btnExif.getSelection()) {
			mappings = exifPanel.getMappings();
		}else if (btnXmp.getSelection()) {
			mappings = xmpPanel.getMappings();
		}
		super.okPressed();
	}
	
	@Override
	protected void cancelPressed(){
		super.cancelPressed();
	}

	protected void createButtonsForButtonBar(Composite parent) {
		Button btnOk = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		btnOk.setEnabled(false);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite header = new Composite(main, SWT.BORDER);
		header.setLayout(new GridLayout(3, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(header, SWT.NONE);
		l.setText(Messages.NewMappingDialog_TypeLabel);
		
		btnExif = new Button(header, SWT.RADIO);
		btnExif.setText(Messages.NewMappingDialog_exifOp);
		btnExif.setSelection(true);
		
		btnXmp = new Button(header, SWT.RADIO);
		btnXmp.setText(Messages.NewMappingDialog_xmlOp);
			
		Composite stackPanel = new Composite(main, SWT.BORDER);
		stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stackPanel.setLayout(new StackLayout());
		
		exifPanel = new NewMappingExif(this);
		Composite exifPanelComposite = exifPanel.createPanel(stackPanel);
		
		Composite xmpPanel = createXmpPanel(stackPanel);
		
		btnExif.addListener(SWT.Selection, e->{
			((StackLayout)stackPanel.getLayout()).topControl = exifPanelComposite;
			stackPanel.layout();
			modified();
		});
		btnXmp.addListener(SWT.Selection, e->{
			((StackLayout)stackPanel.getLayout()).topControl = xmpPanel;
			stackPanel.layout();
			modified();
		});
		
		((StackLayout)stackPanel.getLayout()).topControl = exifPanelComposite;
		stackPanel.layout();
		
		
		if (toEdit != null) {
			if (toEdit.getMetadataType() == MetadataType.EXIF) {
				((StackLayout)stackPanel.getLayout()).topControl = exifPanelComposite;
				btnExif.setSelection(true);
				btnXmp.setSelection(false);
			}else if (toEdit.getMetadataType() == MetadataType.XMP) {
				((StackLayout)stackPanel.getLayout()).topControl = xmpPanel;
				btnExif.setSelection(false);
				btnXmp.setSelection(true);
			}
			stackPanel.layout();
			btnExif.setEnabled(false);
			btnXmp.setEnabled(false);
		}
		
		modified();
		
		setTitle(Messages.NewMappingDialog_Title);
		getShell().setText(Messages.NewMappingDialog_Title);
		setMessage(Messages.NewMappingDialog_Message);
		return parent;
	}
	
	private Composite createXmpPanel(Composite parent) {
		xmpPanel = new NewMappingXmp(this);
		return xmpPanel.createPanel(parent);
	}
	
	
	public void modified() {
		String message = null;
		if (btnExif.getSelection()) {
			message = exifPanel.validate();
		}else if (btnXmp.getSelection()) {
			message = xmpPanel.validate();
		}
		setErrorMessage(message);
		if (getButton(IDialogConstants.OK_ID) != null) getButton(IDialogConstants.OK_ID).setEnabled(message == null);
	}
	
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
}