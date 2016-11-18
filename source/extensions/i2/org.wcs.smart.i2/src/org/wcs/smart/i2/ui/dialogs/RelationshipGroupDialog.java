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
package org.wcs.smart.i2.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RelationshipTypeManager;
import org.wcs.smart.i2.model.IntelRelationshipGroup;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for editing relationship types.
 * 
 * @author Emily
 *
 */
public class RelationshipGroupDialog extends TitleAreaDialog {

	@Inject
	private IEclipseContext context;
	
	private NameKeyComposite nameKeyInfo;
	private IntelRelationshipGroup group;
	private List<IntelRelationshipGroup> groupSiblings;
	private TableViewer lstTypes;
	
	public RelationshipGroupDialog(Shell parentShell, IntelRelationshipGroup group) {
		super(parentShell);
		this.group = group;
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,(int)(p.y*1.4));
	}
	
	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	protected void okPressed() {
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.saveOrUpdate(group);
			s.getTransaction().commit();
			
		}catch (Exception ex){
			if (s.getTransaction().isActive())s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Unable to save changes: " +ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
		initFields();
	}
	
	
	private void modified(){
		
		
		boolean isError = false;
		if (nameKeyInfo.validate()){
			isError = true;
		}	
		
		getButton(IDialogConstants.OK_ID).setEnabled(!isError);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		nameKeyInfo = new NameKeyComposite();
		nameKeyInfo.createControls(parent, true, group.getUuid() == null, new IChangeListener() {
			@Override
			public void itemModified() {
				if (!nameKeyInfo.validate()){
					nameKeyInfo.updateFields(group);
				}
				modified();
			}
		});
		
		Label l = new Label(parent, SWT.NONE);
		l.setText("Types:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		
		lstTypes = new TableViewer(parent);
		lstTypes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,2,1));
		lstTypes.setContentProvider(ArrayContentProvider.getInstance());
		lstTypes.setLabelProvider(new RelationshipTypeLabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof IntelRelationshipType){
					return ((IntelRelationshipType) element).getName();
				}
				return super.getText(element);
			}
		});
		lstTypes.setInput(new String[]{DialogConstants.LOADING_TEXT});

		setTitle("Relationship Group");
		getShell().setText("Relationship Group");
		setMessage("Configure relationship group");
		
		return parent;
	}
	
	
	private void initFields(){
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true,false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					Session s = HibernateManager.openSession();
					try{
						if (group.getUuid() != null){
							group = (IntelRelationshipGroup) s.get(IntelRelationshipGroup.class, group.getUuid());
							group.getNames().size();
							if (group.getRelationshipTypes() != null){
								group.getRelationshipTypes().forEach(g->g.getNames().size());
							}
						}
						groupSiblings = RelationshipTypeManager.INSTANCE.getRelationshipGroups(s, SmartDB.getCurrentConservationArea());
						groupSiblings.remove(group);
						
						if(group.getUuid() != null){
							group = (IntelRelationshipGroup) s.merge(group);
							group.getRelationshipTypes().forEach(e -> e.getName());
						}
					}finally{
						s.close();
					}
					
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							nameKeyInfo.initFields(group, groupSiblings, SmartDB.getCurrentConservationArea().getDefaultLanguage());
							lstTypes.setInput(group.getRelationshipTypes());
							getButton(IDialogConstants.OK_ID).setEnabled(group.getUuid() == null);
						}
					});
					
				}
			});
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(MessageFormat.format("Error loading relationship group: {0}", ex.getMessage()), ex);
		}
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}