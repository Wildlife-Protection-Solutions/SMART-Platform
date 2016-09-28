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
package org.wcs.smart.i2.ui.editors.record;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.ui.DateCellEditor;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for displaying all locations in a list.
 * @author Emily
 *
 */
public class LocationListComposite extends Composite{
	
	public enum Column{ID,COMMENT,DATE,TIME};
	
	private TableViewer tblObservations;
	private RecordEditor editor;
	
	private LocationDetailsShell detailsShell = null; 
	private boolean doHover = true;
	
	private MenuItem deleteItem;
	private MenuItem linkItem;
	
	
	public LocationListComposite(Composite parent, FormToolkit toolkit, RecordEditor editor){
		super(parent, SWT.NONE);
		this.editor = editor;
		toolkit.adapt(this);
		
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginHeight = 0;
		((GridLayout)getLayout()).marginWidth = 0;
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblObservations = new TableViewer(this, SWT.FULL_SELECTION | SWT.BORDER);
		tblObservations.getTable().setLinesVisible(true);
		tblObservations.getTable().setHeaderVisible(true);
		tblObservations.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblObservations.setContentProvider(ArrayContentProvider.getInstance());
		
		
		
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(tblObservations, new FocusCellHighlighter(tblObservations){});
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(tblObservations) {
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		
		TableViewerEditor.create(tblObservations, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | ColumnViewerEditor.KEYBOARD_ACTIVATION);

		
		TableViewerColumn idColumn = new TableViewerColumn(tblObservations, SWT.BORDER | SWT.V_SCROLL);
		idColumn.getColumn().setText("ID");
		idColumn.getColumn().setWidth(100);
		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelLocation){
					return ((IntelLocation) element).getId();
				}
				return super.getText(element);
			}
		});
		idColumn.setEditingSupport(new TextTableEditor(tblObservations, Column.ID));
		
		TableViewerColumn dateColumn = new TableViewerColumn(tblObservations, SWT.BORDER | SWT.V_SCROLL);
		dateColumn.getColumn().setText("Date");
		dateColumn.getColumn().setWidth(100);
		dateColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelLocation){
					return DateFormat.getDateInstance().format(((IntelLocation) element).getDateTime());
				}
				return super.getText(element);
			}
		});
		dateColumn.setEditingSupport(new TextTableEditor(tblObservations, Column.DATE));
		
		TableViewerColumn timeColumn = new TableViewerColumn(tblObservations, SWT.BORDER | SWT.V_SCROLL);
		timeColumn.getColumn().setText("Time");
		timeColumn.getColumn().setWidth(100);
		timeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelLocation){
					return DateFormat.getTimeInstance().format(((IntelLocation) element).getDateTime());
				}
				return super.getText(element);
			}
		});
		timeColumn.setEditingSupport(new TextTableEditor(tblObservations, Column.TIME));
		
		TableViewerColumn commentColumn = new TableViewerColumn(tblObservations, SWT.BORDER | SWT.V_SCROLL);
		commentColumn.getColumn().setText("Comment");
		commentColumn.getColumn().setWidth(200);
		commentColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelLocation){
					return ((IntelLocation) element).getComment();
				}
				return super.getText(element);
			}
		});
		commentColumn.setEditingSupport(new TextTableEditor(tblObservations, Column.COMMENT));
		
		
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
				if (element instanceof IntelLocation){
					StringBuilder sb = new StringBuilder();
					for (IntelEntityLocation l : editor.getEntityLocationLinks()){
						if (l.getLocation().equals(element)){
							sb.append(l.getEntity().getIdAttributeAsText());
							sb.append("; ");
						}
					}
					if (sb.length() > 0){
						return sb.substring(0, sb.length() - 2);
					}
					return "";
				}
				return super.getText(element);
			}
		});
		
		Menu linkEntities = new Menu(tblObservations.getTable());
		tblObservations.getTable().setMenu(linkEntities);
		
		SelectionListener linkEntityListener = new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object selection = ((IStructuredSelection)tblObservations.getSelection()).getFirstElement();
				if (!(selection instanceof IntelLocation)) return;
				
				IntelLocation location = (IntelLocation)selection;
				Object data = ((MenuItem)e.widget).getData();
				List<IntelEntity> toLink = new ArrayList<IntelEntity>();
				if (data == null){
					for (IntelEntityRecord r : editor.getRecord().getEntities()){
						toLink.add(r.getEntity());
					}
				}else if (data instanceof IntelEntity){
					toLink.add((IntelEntity) data);
				}
				for (IntelEntity entity : toLink){
					editor.linkEntityToLocation(location, entity);
				}
			}
		};
		
		linkEntities.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				if (!editor.getEditMode()){
					if (deleteItem != null){
						deleteItem.dispose();
						deleteItem = null;
					}
					if (linkItem != null){
						linkItem.dispose();
						linkItem = null;
					}
				}else{
					if (deleteItem == null){
						deleteItem = new MenuItem(linkEntities, SWT.PUSH);
						deleteItem.setText(DialogConstants.DELETE_BUTTON_TEXT);
						deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
						deleteItem.addSelectionListener(new SelectionAdapter() {
							
							@Override
							public void widgetSelected(SelectionEvent e) {
								Object selection = ((IStructuredSelection)tblObservations.getSelection()).getFirstElement();
								if (selection instanceof IntelLocation){
									editor.deleteLocation(((IntelLocation)selection));
								}
							}
						});
					}
					
					if (linkItem == null){
						linkItem = new MenuItem(linkEntities, SWT.CASCADE);
						linkItem.setText("Link Entities");
					}
					
					Menu linkSubMenu = new Menu(linkItem);
					linkItem.setMenu(linkSubMenu);
						
					for (MenuItem item : linkSubMenu.getItems()){
						item.dispose();
					}
					Object selection = ((IStructuredSelection)tblObservations.getSelection()).getFirstElement();
					if (!(selection instanceof IntelLocation)) return;
					IntelLocation location = (IntelLocation)selection;
						
					List<IntelEntityRecord> allEntities = editor.getRecord().getEntities();
					List<IntelEntity> toAdd = new ArrayList<IntelEntity>();
					for (IntelEntityRecord record : allEntities){
						boolean add = true;
						for (IntelEntityLocation elocation: editor.getEntityLocationLinks()){
							if (elocation.getLocation().equals(location)){
								if (elocation.getEntity().equals(record.getEntity())){
									add = false;
									break;
								}
							}
						}
						if (add) toAdd.add(record.getEntity());
					}
					if (toAdd.size() > 0){
						MenuItem linkToAll = new MenuItem(linkSubMenu, SWT.PUSH);
						linkToAll.setText("All");
						linkToAll.addSelectionListener(linkEntityListener);
						new MenuItem(linkSubMenu, SWT.SEPARATOR);
						for (IntelEntity entity : toAdd){
							MenuItem linkTo = new MenuItem(linkSubMenu, SWT.PUSH);
							linkTo.setText(entity.getIdAttributeAsText());
							linkTo.setImage(EntityTypeLabelProvider.INSTANCE.getImage(entity.getEntityType()));
							linkTo.setData(entity);
							linkTo.addSelectionListener(linkEntityListener);
						}
					}else{
						MenuItem noMore = new MenuItem(linkSubMenu, SWT.PUSH);
						noMore.setEnabled(false);
						noMore.setText("No Options");
					}
					
				}
			}
			
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		
		
		Listener tableListener = new Listener(){

			@Override
			public void handleEvent(Event event) {
				switch(event.type){
					case SWT.MouseDoubleClick:
					case SWT.MouseDown:
					case SWT.MouseUp:
						doHover = false;
						break;
					case SWT.MouseMove:
						doHover= true;
						break;
					case SWT.MouseHover:
						if (doHover){
							doHover(event.x,event.y);
						}
						break;
				}
					
			}
			private void doHover(int x, int y){
				
				ViewerCell cell = tblObservations.getCell(new Point(x, y));
				if (cell != null && cell.getElement() instanceof IntelLocation){
					IntelLocation location = (IntelLocation) cell.getElement();
					if (detailsShell == null || detailsShell.isDisposed() || !detailsShell.getLocationRecord().equals(location)){
						detailsShell = new LocationDetailsShell(tblObservations.getTable().getDisplay(),(IntelLocation)cell.getElement(), editor.getEntityLocationLinks());
					
						int height = detailsShell.getSize().y;
						Point p  = tblObservations.getTable().toDisplay(x, y);
						detailsShell.open(new Point(p.x, p.y - height));
					}
				}
			}
			
		};
		tblObservations.getTable().addListener(SWT.MouseDoubleClick, tableListener);
		tblObservations.getTable().addListener(SWT.MouseDown, tableListener);
		tblObservations.getTable().addListener(SWT.MouseUp, tableListener);
		tblObservations.getTable().addListener(SWT.MouseMove, tableListener);
		tblObservations.getTable().addListener(SWT.MouseHover, tableListener);

		
	}
	
	public void init(){
		tblObservations.setInput(editor.getRecord().getLocations());
	}
	
	public void refreshTable(){
		tblObservations.refresh();
	}
	
	private class TextTableEditor extends EditingSupport {
		
		private TableViewer viewer;
		private CellEditor editor;
		private Column col;
		
		TextTableEditor(TableViewer viewer, Column col) {
			super(viewer);
			this.viewer = viewer;
			this.col = col;
			if (col == Column.DATE ){
				this.editor = new DateCellEditor(viewer.getTable(), SWT.DATE | SWT.DROP_DOWN);
			}else if (col == Column.TIME){
				this.editor = new DateCellEditor(viewer.getTable(), SWT.TIME | SWT.DROP_DOWN);
			}else{
				this.editor = new TextCellEditor(viewer.getTable());
			}
		}

		@Override
		protected void setValue(Object element, Object value) {
			String error = validate(value);
			if (error == null){
				if (col == Column.ID){
					((IntelLocation)element).setId(((String)value).trim());
				}else if (col == Column.COMMENT){
					if (((String)value).isEmpty()){
						((IntelLocation)element).setComment(null);
					}else{
						((IntelLocation)element).setComment(((String)value).trim());
					}
				}else if (col == Column.DATE){
					Date time = ((IntelLocation)element).getDateTime();
					Date date = (Date)value;
					((IntelLocation)element).setDateTime(SmartUtils.combineDateTime(date, time));
				}else if (col == Column.TIME){
					Date time = (Date)value;
					Date date = ((IntelLocation)element).getDateTime();
					((IntelLocation)element).setDateTime(SmartUtils.combineDateTime(date, time));
				}
				//Fire changes
				LocationListComposite.this.editor.locationsUpdated();
			}else{
				MessageDialog.openError(getShell(), "Error", error);
			}
		}

		@Override
		protected Object getValue(Object element) {
			if(col == Column.ID){
				return ((IntelLocation)element).getId();
			}else if (col == Column.COMMENT){
				String value = ((IntelLocation)element).getComment();
				if (value == null) return "";
				return value;
			}else if (col == Column.DATE || col == Column.TIME){
				return ((IntelLocation)element).getDateTime();
			}
			return null;
		}

		private String validate(Object value){
			if (col == Column.ID | col == Column.COMMENT){
				String vv = value == null? null:((String)value).trim();
				if (col == Column.ID){
					if (vv == null||vv.isEmpty()){
						return "ID is required";
					}else if (vv != null && vv.length() > IntelLocation.ID_MAX_LENGTH){
						return MessageFormat.format("ID is must be fewer than {0} characters.", IntelLocation.ID_MAX_LENGTH);
					}
				}else if (col == Column.COMMENT){
					if (vv != null && vv.length() > IntelLocation.COMMENT_MAX_LENGTH){
						return MessageFormat.format("Comment is must be fewer than {0} characters.", IntelLocation.COMMENT_MAX_LENGTH);
					}
				}
			}
			return null;
		}
		
		@Override
		protected CellEditor getCellEditor(final Object element) {
			this.editor.setValidator(new ICellEditorValidator() {
				@Override
				public String isValid(Object value) {
					return validate(value);
				}
			});
			return editor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}
}
