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
package org.wcs.smart.entity.ui.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.Entity.Status;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * A composite that contains a table for displaying the
 * list of entities associated with a given
 * entity type.
 *  
 * @author Emily
 *
 */
public class EntityListTable extends Composite {

	private HashMap<TableViewerColumn, ColumnLabelProvider> tableLabelProviders;
	private TableViewer entityTable;
	private EntityTableViewerComparator tableSorter = new EntityTableViewerComparator();

	
	private ComboViewer filterFieldViewer;
	private FilterComposite txtFilter;
	private List<Object> filterColumns;
	
	private EntityTableFilter tableFilter;
	private EntityActiveFilter activeFilter;
	
	/**
	 * Creates a new table
	 * @param parent
	 */
	public EntityListTable(Composite parent) {
		super(parent, SWT.NONE);

		setLayout(new GridLayout());
		createTable();
	}

	@Override
	public boolean setFocus() {
		return entityTable.getTable().setFocus();
	}
	
	/**
	 * Sets the entity table input.
	 * 
	 * @param input
	 */
	public void setInput(Object input){
		entityTable.setSelection(new StructuredSelection());
		
		//we remove any filters here so the data is
		//all refreshed while a hibernate session is open
		ViewerFilter[] currentFilters = entityTable.getFilters();
		for (ViewerFilter f : currentFilters){
			entityTable.removeFilter(f);
		}
		entityTable.setInput(input);

		//add back any removed filters
		for (ViewerFilter f : currentFilters){
			entityTable.addFilter(f);
		}
	}
	
	/**
	 * Adds a double click listener to the table
	 * @param listener
	 */
	public void addDoubleClickListener(IDoubleClickListener listener){
		entityTable.addDoubleClickListener(listener);
	}
	
	/**
	 * Adds a selection changed listener to the table
	 * @param listener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener){
		entityTable.addSelectionChangedListener(listener);
	}
	
	/**
	 * Sets the selection
	 * 
	 * @param newSelection
	 */
	public void setSelection(ISelection newSelection, boolean show){
		this.entityTable.setSelection(newSelection, show);
	}
	/**
	 * 
	 * @return the current selection
	 */
	public ISelection getSelection() {
		return this.entityTable.getSelection();
	}

	/*
	 * Create a table.
	 */
	private void createTable() {
		
		FormToolkit toolkit = new FormToolkit(getDisplay());
		// ---- FILTER FIELD -----
		Composite filtercomp = toolkit.createComposite(this);
		GridLayout gl = new GridLayout(3, false);
		gl.marginWidth =  0;
		filtercomp.setLayout(gl);
		filtercomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Label l =toolkit.createLabel(filtercomp,Messages.EntityListTable_FilterFieldLabel);
		l.setToolTipText(Messages.EntityListTable_FilterFieldTooltip);
		
		filterFieldViewer = new ComboViewer(filtercomp, SWT.READ_ONLY | SWT.DROP_DOWN);
		toolkit.adapt(filterFieldViewer.getCombo());
		
		filterFieldViewer.setContentProvider(ArrayContentProvider.getInstance());
		filterFieldViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof TableViewerColumn){
					return ((TableViewerColumn) element).getColumn().getText();
				}
				return super.getText(element);
			}
		});
		filterFieldViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		//((GridData)filterFieldViewer.getControl().getLayoutData()).widthHint = 50;
		filterFieldViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)filterFieldViewer.getSelection()).getFirstElement();
				if (x instanceof TableViewerColumn){
					tableFilter.setColumn((TableViewerColumn) x);
				}else{
					tableFilter.setColumn(null);
				}
				entityTable.refresh();	
			}
		});
		
		txtFilter = new FilterComposite(filtercomp, SWT.NONE);
		txtFilter.addChangeListener(new ChangeListener() {	
			@Override
			public void stateChanged(ChangeEvent e) {
				tableFilter.setText(txtFilter.getPatternFilter());
				entityTable.refresh();	
			}
		});
		txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)txtFilter.getLayoutData()).widthHint = 250;

		// --- ACTIVE FILTER ---
		final Button chActive = toolkit.createButton(this, Messages.EntityListTable_IncludeInactiveLabel, SWT.CHECK);
		chActive.setSelection(false);
		chActive.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 2, 1));
		chActive.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (chActive.getSelection()){
					entityTable.removeFilter(activeFilter);
				}else{
					entityTable.addFilter(activeFilter);
				}
			}
		});
		
		// --- ENTITY TABLE LIST ---
		entityTable = new TableViewer(this, SWT.FULL_SELECTION | SWT.MULTI
				| SWT.BORDER | SWT.V_SCROLL);
		entityTable.getTable().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		entityTable.setContentProvider(ArrayContentProvider.getInstance());
		entityTable.getTable().setHeaderVisible(true);
		entityTable.getTable().setLinesVisible(true);

		entityTable.setComparator(tableSorter);
		tableFilter = new EntityTableFilter();
		activeFilter = new EntityActiveFilter();
		entityTable.setFilters(new ViewerFilter[]{tableFilter, activeFilter});
	}

	/**
	 * Sets the entity type for the table.  This disposes
	 * of all existing columns and re-creates them based
	 * on the new entity type.
	 * Only primary attribute columns are displayed. 
	 *  
	 * @param entityType
	 */
	public void setEntityType(EntityType entityType) {
		// create entity table columns
		for (TableColumn c : entityTable.getTable().getColumns()) {
			c.dispose();
		}
		
		tableLabelProviders = new HashMap<TableViewerColumn, ColumnLabelProvider>();
		
		filterColumns = new ArrayList<Object>();
		filterColumns.add(Messages.EntityListTable_AnyLabel);
		
		TableViewerColumn col = createTableColumn(Entity.STATUS_FIELD_NAME,60, new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Entity) {
					return ((Entity) element).getStatus().getGuiName();
				}
				return super.getText(element);
			}
		});
		
		col = createTableColumn(Entity.ID_FIELD_NAME,null, new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Entity) {
					return ((Entity) element).getId();
				}
				return super.getText(element);
			}
		});
		filterColumns.add(col);
		
		if (SmartDB.isMultipleAnalysis()){
			col = createTableColumn(Entity.CA_FIELD_NAME, null, new ColumnLabelProvider() {
				public String getText(Object element) {
					if (element instanceof Entity) {
						return ((Entity) element).getEntityType().getConservationArea().getId();
					}
					return super.getText(element);
				}
			});
			filterColumns.add(col);	
		}
		
		
		if (entityType.getAttributes() != null){
			for (final EntityAttribute ea : entityType.getAttributes()) {
				if (ea.getIsPrimary()) {
					// only show primary columns in this table
					col = createTableColumn(ea.getName(),null, new ColumnLabelProvider() {
						public String getText(Object element) {
							if (element instanceof Entity) {
								Entity e = (Entity) element;
								EntityAttributeValue value = e.findAttribute(ea.getKeyId());
								if (value != null) {
									return value.getValueAsString();
								}
								return ""; //$NON-NLS-1$
							}
							return super.getText(element);
						}
					});
					if (ea.getDmAttribute().getType() == Attribute.AttributeType.LIST || 
						ea.getDmAttribute().getType() == Attribute.AttributeType.TREE ||
						ea.getDmAttribute().getType() == Attribute.AttributeType.TEXT ){
						filterColumns.add(col);
					}
				}
			}
		}
		
		filterFieldViewer.setInput(filterColumns);
		filterFieldViewer.setSelection(new StructuredSelection(filterColumns.get(0)));
		filterFieldViewer.refresh();
		filterFieldViewer.getControl().getParent().layout(true,  true);
		
	}
	
	
	/*
	 * Creates a new table viewer column.
	 */
	private TableViewerColumn createTableColumn(String name, Integer width, final ColumnLabelProvider provider){
		
		final TableViewerColumn column = new TableViewerColumn(entityTable, SWT.NONE);
		column.getColumn().setText(name);
		column.getColumn().setWidth(width == null ? 150 : width);
		column.setLabelProvider(provider);
		column.getColumn().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				entityTable.getTable().setSortDirection(tableSorter.getDirection());
				entityTable.getTable().setSortColumn(column.getColumn());
				tableSorter.setColumn(provider);
				entityTable.refresh();
			}

		});
		tableLabelProviders.put(column, provider);
		return column;
	}
	
	
	private class EntityTableFilter extends ViewerFilter{
		private String filterText = null;
		private TableViewerColumn filterColumn = null;
		
		
		
		public void setText(String text){
			this.filterText = text;
		}
		
		public void setColumn(TableViewerColumn filter){
			this.filterColumn = filter;
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			
			if (filterText == null || filterText.trim().length() == 0){
				return true;
			}
			
			String filter = Pattern.quote(filterText.toLowerCase()) + ".*"; //$NON-NLS-1$
			
			if (filterColumn != null){
				return tableLabelProviders.get(filterColumn).getText(element).toLowerCase().matches(filter);
			}else{
				//search all columns				
				for (Object x : filterColumns){
					if (x instanceof TableViewerColumn){
						if (tableLabelProviders.get(x).getText(element).toLowerCase().matches(filter)){
							return true;
						}
					}
				}
				
			}
			
			return false;
		}
	}
	
	/**
	 * A filter that filters based on if an entity is active
	 * or not.
	 *
	 */
	private class EntityActiveFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			Entity p = (Entity) element;
			return p.getStatus() == Status.ACTIVE;
		}
	}
	
	private class EntityTableViewerComparator extends ViewerComparator {
		  private ColumnLabelProvider sortColumn;
		  private static final int DESCENDING = 1;
		  private int direction = DESCENDING;

		  public EntityTableViewerComparator() {
		    this.sortColumn = null;
		    direction = DESCENDING;
		  }

		  public int getDirection() {
		    return direction == 1 ? SWT.DOWN : SWT.UP;
		  }

		  public void setColumn(ColumnLabelProvider column) {
		    if (column == this.sortColumn) {
		      // Same column as last sort; toggle the direction
		      direction = 1 - direction;
		    } else {
		      // New column; do an ascending sort
		      this.sortColumn = column;
		      direction = DESCENDING;
		    }
		  }

		  @Override
		  public int compare(Viewer viewer, Object e1, Object e2) {
			  if (sortColumn == null){
				  return 0;
			  }
			  String l1 = sortColumn.getText(e1);
			  String l2 = sortColumn.getText(e2);
			  int rc = l1.compareTo(l2);
			  // If descending order, flip the direction
			  if (direction == DESCENDING) {
				  rc = -rc;
			  }
			  return rc;
		  }

		} 
}
