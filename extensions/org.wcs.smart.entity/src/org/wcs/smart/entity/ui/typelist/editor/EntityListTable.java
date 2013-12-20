package org.wcs.smart.entity.ui.typelist.editor;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
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

public class EntityListTable extends Composite {

	private TableViewer entityTable;
	private EntityTableViewerComparator tableSorter = new EntityTableViewerComparator();

	public EntityListTable(Composite parent) {
		super(parent, SWT.NONE);

		setLayout(new GridLayout());
		createTable();
	}

	public TableViewer getViewer() {
		return entityTable;
	}

	public ISelection getSelection() {
		return this.entityTable.getSelection();
	}

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
								return "";
							}
							return super.getText(element);
						}
				});
				
			}
		}
	}
	
	private TableViewerColumn createTableColumn(String name, final ColumnLabelProvider provider){
		final TableViewerColumn column = new TableViewerColumn(entityTable, SWT.NONE);
		column.getColumn().setText(name);
		column.getColumn().setWidth(160);
		column.setLabelProvider(provider);
		column.getColumn().addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
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
