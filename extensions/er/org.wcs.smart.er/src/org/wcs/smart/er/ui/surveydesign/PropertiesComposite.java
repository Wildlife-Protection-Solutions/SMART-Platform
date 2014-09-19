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
package org.wcs.smart.er.ui.surveydesign;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignProperty;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * PropertiesComposite for Survey design
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class PropertiesComposite extends SurveyDesignComposite {

	private TableViewer tableViewer;
	private List<SurveyDesignProperty> input = new ArrayList<SurveyDesignProperty>();
	
	@Override
	public Control createControl(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(2, false));

		Composite tableCmp = new Composite(part, SWT.NONE);
		tableCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tableCmp.getLayoutData()).heightHint = 150;

		TableColumnLayout tableLayout = new TableColumnLayout();
		tableCmp.setLayout(tableLayout);
		
		tableViewer = new TableViewer(tableCmp, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);

		createColumns(tableViewer);

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(tableViewer, new FocusCellHighlighter(tableViewer){});
		
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(tableViewer) {
			protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		
		TableViewerEditor.create(tableViewer, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | ColumnViewerEditor.KEYBOARD_ACTIVATION);

		Composite btnCmp = new Composite(part, SWT.NONE);
		btnCmp.setLayout(new GridLayout(1, false));
		btnCmp.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));

		Button btnAdd = new Button(btnCmp, SWT.NONE);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addProperty();
			}

		});
		
		final Button btnDelete = new Button(btnCmp, SWT.NONE);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setEnabled(false);
		btnDelete.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				deleteProperty();
			}
		});
		
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnDelete.setEnabled(!tableViewer.getSelection().isEmpty());
			}
		});

		part.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		return part;
	}

	private void createColumns(TableViewer viewer) {
		final TableViewerColumn colName = createTableViewerColumn(viewer, "Name", 180);
		colName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof SurveyDesignProperty) {
					SurveyDesignProperty p = (SurveyDesignProperty) element;
					return p.getName();
				}
				return super.getText(element);
			}
		});
		colName.setEditingSupport(new PropertyEditingSupport(viewer) {
			@Override
			protected void set(SurveyDesignProperty p, String value) {
				p.setName(value);
			}
			
			@Override
			protected String get(SurveyDesignProperty p) {
				return p.getName();
			}
		});

		final TableViewerColumn colValue = createTableViewerColumn(viewer, "Value", 180);
		colValue.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof SurveyDesignProperty) {
					SurveyDesignProperty p = (SurveyDesignProperty) element;
					return p.getValue();
				}
				return super.getText(element);
			}
		});
		colValue.setEditingSupport(new PropertyEditingSupport(viewer) {
			@Override
			protected void set(SurveyDesignProperty p, String value) {
				p.setValue(value);
			}
			
			@Override
			protected String get(SurveyDesignProperty p) {
				return p.getValue();
			}
		});
	}

	private TableViewerColumn createTableViewerColumn(TableViewer viewer, String title, int weight) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setResizable(true);
		column.setMoveable(true);

		TableColumnLayout layout = (TableColumnLayout) viewer.getTable() .getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(weight, ColumnWeightData.MINIMUM_WIDTH, true));

		return viewerColumn;
	}

	protected void addProperty() {
		SurveyDesignProperty p = new SurveyDesignProperty();
		p.setName("Name");
		p.setValue("Value");
		input.add(p);
		tableViewer.refresh();
		fireChangeListeners();
	}

	protected void deleteProperty() {
		Object obj = ((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		input.remove(obj);
		tableViewer.refresh();
		fireChangeListeners();
	}


	@Override
	public void init(SurveyDesign design, Session session) {
		input.clear();
		input.addAll(design.getProperties());
		tableViewer.setInput(input);
	}

	@Override
	public void updateDesign(SurveyDesign design) {
		design.getProperties().clear();
		for (SurveyDesignProperty p : input) {
			p.setSurveyDesign(design);
			design.getProperties().add(p);
		}
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public String getTitle() {
		return "Survey Properties";
	}

	@Override
	public String getDescription() {
		return "Properties that will be associated with this survey design. Double click to edit.";
	}

	private abstract class PropertyEditingSupport extends EditingSupport {

		private TextCellEditor editor;
		
		public PropertyEditingSupport(TableViewer viewer) {
			super(viewer);
			this.editor = new TextCellEditor(viewer.getTable());
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (element instanceof SurveyDesignProperty && value instanceof String) {
				set((SurveyDesignProperty) element, (String) value);
			}
			tableViewer.refresh();
			fireChangeListeners();
		}

		protected abstract void set(SurveyDesignProperty p, String value);
		
		@Override
		protected Object getValue(Object element) {
			if (element instanceof SurveyDesignProperty) {
				String value = get((SurveyDesignProperty) element);
				if (value != null) {
					return value;
				}
			}
			return ""; //$NON-NLS-1$
		}

		protected abstract String get(SurveyDesignProperty p);
		
		@Override
		protected CellEditor getCellEditor(Object element) {
			return editor;
		}
		
		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
		
	}
}
