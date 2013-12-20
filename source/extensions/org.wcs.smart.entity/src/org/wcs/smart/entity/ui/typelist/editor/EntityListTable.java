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
package org.wcs.smart.entity.ui.typelist.editor;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;

/**
 * A composite that contains a table for displaying the
 * list of entities associated with a given
 * entity type.
 *  
 * @author Emily
 *
 */
public class EntityListTable extends Composite {

	private TableViewer entityTable;
	private EntityTableViewerComparator tableSorter = new EntityTableViewerComparator();

	/**
	 * Creates a new table
	 * @param parent
	 */
	public EntityListTable(Composite parent) {
		super(parent, SWT.NONE);

		setLayout(new GridLayout());
		createTable();
	}

	/**
	 * 
	 * @return the underlying table viewer
	 */
	public TableViewer getViewer() {
		return entityTable;
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
		// --- attribute table list
		entityTable = new TableViewer(this, SWT.FULL_SELECTION | SWT.MULTI
				| SWT.BORDER | SWT.V_SCROLL);
		entityTable.getTable().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		entityTable.setContentProvider(ArrayContentProvider.getInstance());
		entityTable.getTable().setHeaderVisible(true);
		entityTable.getTable().setLinesVisible(true);

		entityTable.setComparator(tableSorter);
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

		createTableColumn(Entity.ID_FIELD_NAME, new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Entity) {
					return ((Entity) element).getId();
				}
				return super.getText(element);
			}
		});
		
		createTableColumn(Entity.STATUS_FIELD_NAME, new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Entity) {
					return ((Entity) element).getStatus().getGuiName();
				}
				return super.getText(element);
			}
		});
		
		if (entityType.getAttributes() == null){
			return;
		}
		
		for (final EntityAttribute ea : entityType.getAttributes()) {
			if (ea.getIsPrimary()) {
				// only show primary columns in this table
				createTableColumn(ea.getName(), new ColumnLabelProvider() {
						public String getText(Object element) {
							if (element instanceof Entity) {
								Entity e = (Entity) element;
								EntityAttributeValue value = e
										.findAttribute(ea);
								if (value != null) {
									return value.getValueAsString();
								}
								return ""; //$NON-NLS-1$
							}
							return super.getText(element);
						}
				});
				
			}
		}
	}
	
	/*
	 * Creates a new table viewer column.
	 */
	private TableViewerColumn createTableColumn(String name, final ColumnLabelProvider provider){
		final TableViewerColumn column = new TableViewerColumn(entityTable, SWT.NONE);
		column.getColumn().setText(name);
		column.getColumn().setWidth(160);
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
		return column;
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
