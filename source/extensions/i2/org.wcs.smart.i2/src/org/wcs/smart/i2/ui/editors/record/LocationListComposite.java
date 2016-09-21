package org.wcs.smart.i2.ui.editors.record;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.ui.properties.DialogConstants;

public class LocationListComposite extends Composite{

	private TableViewer tblObservations;
	private RecordEditor editor;
	
	
	public LocationListComposite(Composite parent, FormToolkit toolkit, RecordEditor editor){
		super(parent, SWT.NONE);
		this.editor = editor;
		toolkit.adapt(this);
		
		
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginHeight = 0;
		((GridLayout)getLayout()).marginWidth = 0;
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblObservations = new TableViewer(this);
		tblObservations.getTable().setLinesVisible(true);
		tblObservations.getTable().setHeaderVisible(true);
		tblObservations.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblObservations.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn idColumn = new TableViewerColumn(tblObservations, SWT.BORDER | SWT.V_SCROLL);
		idColumn.getColumn().setText("ID");
		idColumn.getColumn().setWidth(100);
		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelEntityRecord){
					return ((IntelEntityRecord) element).getEntity().getIdAttributeAsText();
				}
				return super.getText(element);
			}
		});
		TableViewerColumn obsColumn = new TableViewerColumn(tblObservations, SWT.BORDER | SWT.V_SCROLL);
		obsColumn.getColumn().setText("Observation");
		obsColumn.getColumn().setWidth(200);
		obsColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return super.getText(element);
			}
		});
		TableViewerColumn entityListColumn = new TableViewerColumn(tblObservations, SWT.BORDER | SWT.V_SCROLL);
		entityListColumn.getColumn().setText("Entities");
		entityListColumn.getColumn().setWidth(200);
		entityListColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return super.getText(element);
			}
		});
		
	}
	
	public void init(){
		tblObservations.setInput(editor.getRecord().getLocations());
	}
}
