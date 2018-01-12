package org.wcs.smart.asset.ui.views.map;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.asset.map.engine.IFilter;
import org.wcs.smart.asset.map.engine.parser.Parser;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.ColumnType;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.TreeEditorField;

public class CombinedColumnComposite extends Composite {
	

	private Text txtAttributeFilters;
	private ListViewer columnOptions;
	
	private CombinedOverviewColumn newColumn = null;
	private CombinedOverviewColumn toUpdate = null;
	
	private CategoryColumnDialog dialog;


	/**
	 * Creates a new dialog for editing an existing column
	 * @param parentShell
	 * @param toUpdate can be null if we are creating a new one
	 */
	public CombinedColumnComposite(Composite parent, CategoryColumnDialog dialog,  IOverviewTableColumn toUpdate) {
		super(parent, SWT.NONE);
		this.toUpdate = (CombinedOverviewColumn) toUpdate;
		this.dialog = dialog;
		
		createComposite();
	}
	
	/**
	 * 
	 * @return the new column created; this will return null if we are updating a column
	 */
	public CombinedOverviewColumn getNewColumn() {
		return newColumn;
	}
	
	
	public boolean validate() {
		String text = txtAttributeFilters.getText().trim();
		if (!text.isEmpty()) {
			
			try(InputStream is = new ByteArrayInputStream(text.getBytes())){
				Parser parser = new Parser(is);
				IFilter f = parser.CombinedExpression();
			} catch (Exception e) {
				e.printStackTrace();
				dialog.setErrorMessage(e.getMessage());
				return false;
			}
		}
		dialog.setErrorMessage(null);
		return true;
	}
	
	public void cancelPressed() {
		newColumn = null;
	}
	
	public void okPressed() {		
		if (toUpdate == null) {
			newColumn = new CombinedOverviewColumn(dialog.getName().trim(), txtAttributeFilters.getText());
		}else {
			toUpdate.updateValues(dialog.getName().trim(), txtAttributeFilters.getText());
		}
	}
	
	
	protected Control createComposite() {
		Composite parent = this;
		parent.setLayout(new GridLayout(2, true));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		Label l = new Label(parent, SWT.NONE);
		l.setText("Definition:");
		
		l = new Label(parent, SWT.NONE);
		l.setText("Columns:");
		
		txtAttributeFilters = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		txtAttributeFilters.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtAttributeFilters.addListener(SWT.Modify, e->dialog.validate());
		
		columnOptions = new ListViewer(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
		columnOptions.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		columnOptions.setContentProvider(ArrayContentProvider.getInstance());
		columnOptions.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IOverviewTableColumn) return ((IOverviewTableColumn) element).getName();
				return super.getText(element);
			}
		});
		columnOptions.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addSelection();
			}
		});
		List<IOverviewTableColumn> columns = new ArrayList<>();
		for (IOverviewTableColumn c : dialog.getAllColumns()) {
			if (!(c instanceof CombinedOverviewColumn) && (c.getType() == ColumnType.INTEGER || c.getType() == ColumnType.NUMBER)){
				columns.add(c);
			}
		}
		columnOptions.setInput(columns);

		if (toUpdate != null) {	
			txtAttributeFilters.setText(toUpdate.getDefinition());
		}
		
		return parent;
	}
	
	
	private void addSelection() {
		Object option = columnOptions.getStructuredSelection().getFirstElement();
		String part = null;
		
		if (option instanceof IOverviewTableColumn) {
			IOverviewTableColumn item = (IOverviewTableColumn) option;
			part = "[" + item.getKey() + "]";
		}
		if (part != null) {
			if (txtAttributeFilters.getText().isEmpty()) {
				txtAttributeFilters.setText(part);
			}else {
				txtAttributeFilters.setText(txtAttributeFilters.getText() + " < + | - | * | / > " + part);
			}
		}
	}
	
}