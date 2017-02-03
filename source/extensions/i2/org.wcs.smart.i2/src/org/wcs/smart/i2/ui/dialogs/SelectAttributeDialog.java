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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.i2.ui.NamedItemViewerFilter;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * Dialog for selecting a list of attributes and optionally add a new attribute
 * @author Emily
 *
 */
public class SelectAttributeDialog extends TitleAreaDialog{
	
	@Inject
	private IEclipseContext context;
	
	private CheckboxTableViewer attributeList;
	private NamedItemViewerFilter filter;
		
	private List<IntelAttribute> selection = null;
	private String message;
	
	private Job loadAttributes = new LoadAttributesJob(){
		@Override
		public void afterLoad() {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					attributeList.setInput(attributes);
				}					
			});
		}
		
	};
	public SelectAttributeDialog(Shell parentShell, String message) {
		super(parentShell);
		this.message = message;
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,(int)(p.y*1.4));
	}

	public List<IntelAttribute> getSelectedAttributes(){
		return this.selection;
	}
	
	protected void okPressed(IntelAttribute newAttribute) {
		selection = new ArrayList<IntelAttribute>();
		for (Object lselection : attributeList.getCheckedElements()){
			if (lselection instanceof IntelAttribute){
				selection.add((IntelAttribute)lselection);
			}
		}
		selection.add(newAttribute);
		super.okPressed();
	}
	
	
	protected void okPressed() {
		selection = new ArrayList<IntelAttribute>();
		for (Object lselection : attributeList.getCheckedElements()){
			if (lselection instanceof IntelAttribute){
				selection.add((IntelAttribute)lselection);
			}
		}
		super.okPressed();
	}
	
	@Override
	protected void cancelPressed(){
		selection = null;
		super.cancelPressed();
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		FilterComposite typeFilter = new FilterComposite(parent, SWT.NONE);
		typeFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		typeFilter.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				filter.setFilterString(typeFilter.getPatternFilter());
			}
		});
		
		attributeList = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.MULTI);
		attributeList.setContentProvider(ArrayContentProvider.getInstance());
		attributeList.setLabelProvider(new AttributeLabelProvider());
		attributeList.setInput(new String[]{DialogConstants.LOADING_TEXT});
		attributeList.getControl().setFocus();
		attributeList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributeList.getTable().addKeyListener(new KeyAdapter() {
			//spacebar check
			@Override
			public void keyPressed(KeyEvent e) {
				if (attributeList.getSelection().isEmpty()){
					return;
				}
				if (e.keyCode == SWT.SPACE){
					IStructuredSelection selection = ((IStructuredSelection)attributeList.getSelection());
					selection.getFirstElement();
					boolean value = attributeList.getChecked(selection.getFirstElement() );
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						attributeList.setChecked(tp, !value);
					}
					e.doit = false;
							
				}
				
			}
		});
		filter = new NamedItemViewerFilter(attributeList);
		attributeList.setFilters(new ViewerFilter[]{filter});
		attributeList.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				attributeList.setChecked( ((IStructuredSelection)attributeList.getSelection()).getFirstElement(), true );
				okPressed();
				
			}
		});
		Button btnNew = new Button(parent, SWT.PUSH);
		btnNew.setText("Create New Attribute");
		btnNew.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				IntelAttribute newAttribute = newAttribute();
				if (newAttribute != null){
					okPressed(newAttribute);
				}
			}
		});
		setTitle("Select Attributes");
		getShell().setText("Select Attributes");
		setMessage(message);
		
		loadAttributes.setSystem(true);
		loadAttributes.schedule(0);
		
		return parent;
	}
	
	private IntelAttribute newAttribute(){
		IntelAttribute attribute = new IntelAttribute();
		attribute.setConservationArea(SmartDB.getCurrentConservationArea());
		attribute.setAttributeList(new ArrayList<IntelAttributeListItem>());
		
		AttributeDialog.showAttributeDialog(getShell(), attribute, context);
		if (attribute.getUuid() != null){
			return attribute;
		}
		return null;
		
	}
	
	
	@Override
	public boolean isResizable(){
		return true;
	}	
}