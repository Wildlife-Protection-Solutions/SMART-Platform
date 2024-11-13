/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.query.ui;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.IQueryColumnProvider;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnConfiguration;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * @since 8.1.0
 */
public class QueryColumnConfigurationDialog extends SmartStyledTitleDialog{

	private QueryColumnConfiguration config;
	
	private Text txtName;
	
	private ComboViewer cmbQueryType;
	private TableViewer tblAvailableColumns;
	private TableViewer tblVisibleColumns;
	
	private List<QueryColumn> allColumns;
	private List<QueryColumn> selectedColumns;
	private List<QueryColumn> rawColumns;
	
	private Menu mnuAddColumns;
	private Menu mnuSortColumns;
	
	private boolean modified = false;
	
	protected QueryColumnConfigurationDialog(Shell parent, QueryColumnConfiguration config) {
		super(parent);
		this.config = config;
	}

	
	protected void createButtonsForButtonBar(Composite parent) {
		Button btnSave = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		btnSave.setEnabled(false);
		
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	public Point getInitialSize(){
		return new Point(800, 600);
	}
	
	public void okPressed() {
		save();
	}
	
	public void cancelPressed() {
		if (this.modified) {
			if (!MessageDialog.openQuestion(getShell(), "Changes", "There are unsaved changes. Are you sure you want to close?")) {
				return;
			}
		}
		super.cancelPressed();
	}
	
	private boolean save() {
		StringBuilder def = new StringBuilder();
		for (QueryColumn col : selectedColumns) {
			def.append(col.getKey());
			def.append(SimpleQuery.COLUMN_SPLITTER);
		}
		def.deleteCharAt(def.length() - 1);
		
		IQueryType qtype = (IQueryType) cmbQueryType.getStructuredSelection().getFirstElement();
		
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				
				QueryColumnConfiguration toUpdate = null;
				if (config.getUuid() != null) {
					toUpdate = session.get(QueryColumnConfiguration.class, config.getUuid());
					
					for (org.wcs.smart.ca.Label l : config.getNames()) {
						toUpdate.updateName(l.getLanguage(), l.getValue());
					}
					List<org.wcs.smart.ca.Label> delete = new ArrayList<>(); 
					for (org.wcs.smart.ca.Label l : toUpdate.getNames()) {
						boolean exists = false;
						for (org.wcs.smart.ca.Label l2 : config.getNames()) {
							if (l.getLanguage().equals(l2.getLanguage())) {
								exists = true;
								break;
							}
						}
						if (!exists) delete.add(l);
					}
					toUpdate.getNames().removeAll(delete);
					
				}else {
					toUpdate = config;
					toUpdate.setQueryTypeKey(qtype.getKey());
					
					session.persist(toUpdate);
				}
				toUpdate.setColumnConfiguration(def.toString());
				session.getTransaction().commit();
				this.modified = false;
				getButton(IDialogConstants.OK_ID).setEnabled(false);
				this.config = toUpdate;
			}catch (Exception ex) {
				QueryPlugIn.displayLog(ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		allColumns = new ArrayList<>();
		selectedColumns = new ArrayList<>();
		rawColumns = new ArrayList<>();
		
		parent = (Composite)super.createDialogArea(parent);
	
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));		
		
		Label lblName = new Label(container, SWT.NONE);
		lblName.setText("Name:");
		lblName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtName = new Text(container, SWT.BORDER);
		txtName.setText(config.getName());
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtName.addModifyListener(e->{
			config.setName(txtName.getText());
			config.updateName(SmartDB.getCurrentLanguage(), config.getName());
			modified();
		});
		
		Link lnkTranslate = new Link(container, SWT.NONE);
		lnkTranslate.setText("<a>" + "translate" + "</a>"); 
		lnkTranslate.addListener(SWT.Selection, e->{
			NamedItem toUpdate = new NamedItem();
			for(org.wcs.smart.ca.Label l : config.getNames()){
				toUpdate.updateName(l.getLanguage(), l.getValue());
				
			}
			TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(getShell(), toUpdate);
			if (dialog.open() == TranslateSimpleListItemDialog.OK){
				 txtName.setText(toUpdate.getName());
				 for (org.wcs.smart.ca.Label l : toUpdate.getNames()){
					 config.updateName(l.getLanguage(),l.getValue());	 
				 }
				modified();
			}
			
		});
		
		Label lblQueryType = new Label(container, SWT.NONE);
		lblQueryType.setText("Name:");
		lblQueryType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		cmbQueryType = new ComboViewer(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbQueryType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbQueryType.setContentProvider(ArrayContentProvider.getInstance());
		cmbQueryType.getControl().setEnabled(this.config.getUuid() == null);
		
		cmbQueryType.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object x) {
				if (x instanceof IQueryType t) return t.getGuiName();
				return super.getText(x);
			}
		});
		
		List<IQueryType> options = new ArrayList<>();
		for (IQueryType type: QueryTypeManager.INSTANCE.getAllQueryTypes()) {
			if (SimpleQuery.class.isAssignableFrom(type.getHibernateClass())) options.add(type);
		}
		cmbQueryType.setInput(options);
		if (this.config.getQueryTypeKey() != null) {
			IQueryType type = QueryTypeManager.INSTANCE.findQueryType(this.config.getQueryTypeKey());
			if (type != null) cmbQueryType.setSelection(new StructuredSelection(type));
			
			loadQueryColumns.schedule();
		}else {
			cmbQueryType.addSelectionChangedListener(e->{
				loadQueryColumns.schedule();
			});
		}
		
		
		Composite orderComp = new Composite(container, SWT.NONE);
		orderComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		orderComp.setLayout(new GridLayout(4, false));
		
		Label l = new Label(orderComp, SWT.NONE);
		l.setText("All query columns");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		l = new Label(orderComp, SWT.NONE);
		l.setText("Output query columns");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Composite tableComposite = new Composite(orderComp, SWT.NONE);
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableComposite.setLayout(tableColumnLayout);
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblAvailableColumns = new TableViewer(tableComposite, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		tblAvailableColumns.setContentProvider(ArrayContentProvider.getInstance());
		tblAvailableColumns.setInput(allColumns);
		
		TableViewerColumn col = new TableViewerColumn(tblAvailableColumns, SWT.NONE);
				col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof QueryColumn col) return col.getName();
				return super.getText(element);
			}
		});
		
		tableColumnLayout.setColumnData(col.getColumn(), new ColumnWeightData(1));
		tblAvailableColumns.addDoubleClickListener(e->{
			addSelected();
		});
	            
		
		Composite addButtonComp = new Composite(orderComp, SWT.NONE);
		addButtonComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		addButtonComp.setLayout(new GridLayout());
		
		ToolBar tbAdd = new ToolBar(addButtonComp, SWT.FLAT | SWT.VERTICAL);
		
		ToolItem tiMove = new ToolItem(tbAdd, SWT.PUSH);
		tiMove.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.BROWSER_FORWARD));
		tiMove.addListener(SWT.Selection, e->addSelected());
		tiMove.setToolTipText("add selected columns to query output");
		
		ToolItem tiRemove = new ToolItem(tbAdd, SWT.PUSH);
		tiRemove.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.BROWSER_BACKWARD));
		tiRemove.addListener(SWT.Selection, e->removeSelected());
		tiRemove.setToolTipText("remove selected columns from query output");
		
		ToolItem tiAdd = new ToolItem(tbAdd, SWT.DROP_DOWN);
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAdd.setToolTipText("add a subset of columns to the query output");
		
		mnuAddColumns = new Menu(tbAdd);
		MenuItem miAddAll= new MenuItem(mnuAddColumns, SWT.PUSH);
		miAddAll.setText("Add All");
		miAddAll.addListener(SWT.Selection, e->addAllColumns());
		
		MenuItem miAddNonAttribute= new MenuItem(mnuAddColumns, SWT.PUSH);
		miAddNonAttribute.setText("Add Non-Attribute Columns");
		miAddNonAttribute.addListener(SWT.Selection, e->addNonAttributeColumns());
		
		tiAdd.addListener(SWT.Selection, e->mnuAddColumns.setVisible(true));
	
		Menu mnuall = new Menu(tblAvailableColumns.getControl());
		MenuItem miAdd = new MenuItem(mnuall, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.addListener(SWT.Selection, e->addSelected());
		tblAvailableColumns.getControl().setMenu(mnuall);
		
		tableComposite = new Composite(orderComp, SWT.NONE);
		tableColumnLayout = new TableColumnLayout();
		tableComposite.setLayout(tableColumnLayout);
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblVisibleColumns = new TableViewer(tableComposite, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		tblVisibleColumns.setContentProvider(ArrayContentProvider.getInstance());
		
		col = new TableViewerColumn(tblVisibleColumns, SWT.NONE);
				col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof QueryColumn col) return col.getName();
				return super.getText(element);
			}
		});
		
		tableColumnLayout.setColumnData(col.getColumn(), new ColumnWeightData(1));
		
		tblVisibleColumns.setInput(selectedColumns);
		tblVisibleColumns.addDoubleClickListener(e->{
			removeSelected();
		});
		
		Composite orderButtonComp = new Composite(orderComp, SWT.NONE);
		orderButtonComp.setLayout(new GridLayout());
		orderButtonComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		ToolBar tbSort = new ToolBar(orderButtonComp, SWT.FLAT | SWT.VERTICAL);
		
		ToolItem tiSort = new ToolItem(tbSort, SWT.DROP_DOWN);
		tiSort.setText("Sort By");
		
		ToolItem tiMoveUp = new ToolItem(tbSort, SWT.PUSH);
		tiMoveUp.setText("Move Up");
		
		tiMoveUp.addListener(SWT.Selection, e->move(-1));
		
		ToolItem tiMoveDown = new ToolItem(tbSort, SWT.PUSH );
		tiMoveDown.setText("Move Down");
		tiMoveDown.addListener(SWT.Selection, e->move(1));
		
		mnuSortColumns = new Menu(tbSort);
		
		Menu mnuvisible = new Menu(tblVisibleColumns.getControl());
		MenuItem miDelete = new MenuItem(mnuvisible, SWT.PUSH);
		miDelete.setText("Remove");
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addListener(SWT.Selection, e->removeSelected());
		tblVisibleColumns.getControl().setMenu(mnuvisible);
		
		MenuItem miSortDefault = new MenuItem(mnuSortColumns, SWT.PUSH);
		miSortDefault.setText("Default");
		miSortDefault.addListener(SWT.Selection, e->sortRevertOrder());
		
		MenuItem miSortDefaultAA = new MenuItem(mnuSortColumns, SWT.PUSH);
		miSortDefaultAA.setText("Alphabetic");
		miSortDefaultAA.addListener(SWT.Selection, e->sortAlphabetic());
		
		tiSort.addListener(SWT.Selection, e->mnuSortColumns.setVisible(true));
		
		addDragAndDropSupport();
		
		loadConfigurableModelsJob.schedule();
		
		getShell().setText("Query Column Configuration");
		setTitle("Query Column Configuration");
		setMessage("Configure column visibility and order.");
		return container;
	}
	
	private void addDragAndDropSupport() {
		/* drag and drop support */
		int operations = DND.DROP_MOVE;
		Transfer[] transferTypes = new Transfer[]{LocalSelectionTransfer.getTransfer()};
		tblVisibleColumns.addDragSupport(operations, transferTypes, new DragSourceListener() {
			@Override
			public void dragStart(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(tblVisibleColumns.getSelection());
				event.doit = true;
				
			}
			
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (LocalSelectionTransfer.getTransfer()
						.isSupportedType(event.dataType)) {
					event.data = tblVisibleColumns.getSelection();
				}
			}
			
			@Override
			public void dragFinished(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(null);
				tblVisibleColumns.refresh();
			}
		});
		
		ViewerDropAdapter dropAdapter = new ViewerDropAdapter(tblVisibleColumns) {
			
			@Override
			public boolean validateDrop(Object target, int operation,
					TransferData transferType) {
				return (target instanceof QueryColumn);
			}
			
			@Override
			public boolean performDrop(Object data) {
				StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
				if (selection == null){
					return false;
				}
				
				List<QueryColumn> items = selection.stream().filter(e->e instanceof QueryColumn)
						.map(e->(QueryColumn)e)
						.collect(Collectors.toList());
				
				QueryColumn obj = items.get(0);
				
				QueryColumn target = (QueryColumn)getCurrentTarget();
				
				if (target.equals(obj)) return false;

				int index = selectedColumns.indexOf(obj);
				int toIndex = selectedColumns.indexOf(target);
				
				if (index == -1 || toIndex == -1) return false;
				
				selectedColumns.removeAll(items);
				toIndex = selectedColumns.indexOf(target);
				if (toIndex == -1) {
					toIndex = index;
				}
				
				if (getCurrentLocation() == LOCATION_AFTER) toIndex++;
					
				for (QueryColumn a : items.reversed()) {
					selectedColumns.add(toIndex, a);
				}
				refreshLists();
				modified();
				return true;
			}
		};
		tblVisibleColumns.addDropSupport(operations, transferTypes, dropAdapter);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private void refreshLists() {
		tblAvailableColumns.refresh();
		tblVisibleColumns.refresh();
	}
	
	private void addSelected() {
		IStructuredSelection selection = tblAvailableColumns.getStructuredSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object next = iterator.next();
			if (next instanceof QueryColumn qc) {
				selectedColumns.add(qc);
				allColumns.remove(qc);
			}
		}
		refreshLists();
		modified();
	}
	
	private void removeSelected() {
		IStructuredSelection selection = tblVisibleColumns.getStructuredSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object next = iterator.next();
			if (next instanceof QueryColumn qc) {
				selectedColumns.remove(qc);
				allColumns.add(qc);
			}
		}
		
		allColumns.sort((a,b)->Integer.compare(rawColumns.indexOf(a), rawColumns.indexOf(b)));
		refreshLists();
		modified();
	}
	
	private void sortRevertOrder() {
		selectedColumns.sort((a,b)->((Integer)rawColumns.indexOf(a)).compareTo(rawColumns.indexOf(b)));		
		refreshLists();
	}
	
	private void sortAlphabetic() {
		selectedColumns.sort((a,b)->Collator.getInstance().compare(a.getName(),  b.getName()));
		refreshLists();
		modified();
	}

	private void sortDefaultOtherCmAttribute(ConfigurableModel cm) {
		
		LoadColumnsAndExecute job = new LoadColumnsAndExecute(cm,
				keys->{
					selectedColumns.sort((a,b)->{
						boolean aatt = a instanceof AttributeQueryColumn;
						boolean batt = b instanceof AttributeQueryColumn;
						if (aatt && batt) {
							AttributeQueryColumn aq = (AttributeQueryColumn)a;
							AttributeQueryColumn bq = (AttributeQueryColumn)b;
							return ((Integer)keys.indexOf(aq.getAttributeId())).compareTo(keys.indexOf(bq.getAttributeId()));
						}else if (aatt && !batt) {
							return 1;
						}else if (!aatt&& batt) {
							return -1;
						}else {
							return ((Integer)rawColumns.indexOf(a)).compareTo(rawColumns.indexOf(b));
						}
					});
					Display.getDefault().syncExec(()->{
						refreshLists();
						modified();
					});
				});
		job.schedule();
	}
	

	private void addAllColumns() {
		selectedColumns.addAll(allColumns);
		allColumns.clear();
		refreshLists();
		modified();
	}
	
	private void addNonAttributeColumns() {
		List<QueryColumn> toadd = new ArrayList<>();
		for (QueryColumn qc : allColumns) {
			if (qc instanceof AttributeQueryColumn ) continue;
			toadd.add(qc);
		}
		
		selectedColumns.addAll(toadd);
		allColumns.removeAll(toadd);			
		
		refreshLists();
		modified();
	}

	private void addFromConfigurableModel(ConfigurableModel cm) {
		
		LoadColumnsAndExecute job = new LoadColumnsAndExecute(cm,
				keys->{
					HashMap<String,QueryColumn> attcols = new HashMap<>();
					for (QueryColumn qc : allColumns) {
						if (qc instanceof AttributeQueryColumn aqc) {
							attcols.put(aqc.getAttributeId(), aqc);
						}
					}
					for (String key : keys) {
						QueryColumn col = attcols.get(key);
						if (col == null) continue;
						if (selectedColumns.contains(col)) continue;
						selectedColumns.add(col);
						allColumns.remove(col);
					}
					Display.getDefault().syncExec(()->{
						refreshLists();
						modified();
					});
				});
		job.schedule();
	}
	
	
	private void move(int amount) {
		if (tblVisibleColumns.getStructuredSelection().isEmpty()) return;
		List<QueryColumn> items = tblVisibleColumns.getStructuredSelection()
				.stream()
				.filter(e->e instanceof QueryColumn)
				.map(e->(QueryColumn)e)				
				.collect(Collectors.toList());
		
		QueryColumn obj = items.get(0);
		
		int index = selectedColumns.indexOf(obj);
		if (index == -1) return;
		
		int toIndex = index += amount ;
		if (toIndex < 0) toIndex = 0;
		
		selectedColumns.removeAll(items);
		if (toIndex > selectedColumns.size()) toIndex = selectedColumns.size();

		for (QueryColumn a : items.reversed()) {
			selectedColumns.add(toIndex, a);
		}
		refreshLists();
		modified();
	}
	
	private void modified() {
		
		//name is required
		this.modified = true;
		String error = null;
		if (txtName.getText().isEmpty()) {
			error = "Name required";
		}else {
			boolean hasdefault = false;
			for (org.wcs.smart.ca.Label l : config.getNames()) {
				if (l.getLanguage().isDefault()) {
					hasdefault = true;
					if (l.getValue().isBlank()) {
						error = MessageFormat.format("Name required for {0}.", l.getLanguage().getDisplayName());
					}
				}
				if (l.getValue().length() > org.wcs.smart.ca.Label.MAX_LENGTH) {
					error = MessageFormat.format("Name too long (maximum length {0}): {1}.",org.wcs.smart.ca.Label.MAX_LENGTH, l.getValue() );
				}
			}
			if (!hasdefault) {
				error = MessageFormat.format("Name required for {0}.", config.getConservationArea().getDefaultLanguage().getDisplayName());
			}
		}
		
		setErrorMessage(error);
		
		
		getButton(IDialogConstants.OK_ID).setEnabled(this.modified && error == null);
	}
	
	private IQueryType getSelectedQueryType() {
		return (IQueryType) cmbQueryType.getStructuredSelection().getFirstElement();
	}
	
	private Job loadQueryColumns = new Job("loading query columns") { //$NON-NLS-1$
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			IQueryType[] type = new IQueryType[] {null};
			Display.getDefault().syncExec(()->{
				type[0] = getSelectedQueryType();
			});
			try {
				Query q = type[0].getHibernateClass().getDeclaredConstructor().newInstance();
				if (q instanceof SimpleQuery sq) {
					IQueryColumnProvider provider = SmartContext.INSTANCE.getClass(sq.getColumnProviderClass());
					try(Session session = HibernateManager.openSession()){
						allColumns.clear();
						selectedColumns.clear();
						for(QueryColumn c : provider.getQueryColumns(q, Locale.getDefault(), false, session)) {
							allColumns.add(c);
							rawColumns.add(c);
						}
					}
				}
			}catch (Exception ex) {
				QueryPlugIn.displayLog(ex.getMessage(), ex);
			}
			
			
			if (config.getColumnConfiguration() != null) {
				String[] cols = config.getColumnConfiguration().split(SimpleQuery.COLUMN_SPLITTER);
				List<QueryColumn> current = new ArrayList<>();
				for (String c : cols) {
					for (QueryColumn qc : allColumns) {
						if (qc.getKey().equals(c)) {
							current.add(qc);
							break;
						}
					}
				}
				selectedColumns.addAll(current);
				allColumns.removeAll(current);
			}
			
			Display.getDefault().asyncExec(()->{
				tblAvailableColumns.refresh();
				tblVisibleColumns.refresh();
			});
			
			return Status.OK_STATUS;
		}
	};
	
	private Job loadConfigurableModelsJob = new Job("loading configurable models"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<ConfigurableModel> cms = new ArrayList<>();
			try(Session s = HibernateManager.openSession()){
				
				cms.addAll(QueryFactory.buildQuery(s, ConfigurableModel.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}) //$NON-NLS-1$
						.list());
			}
			
			
			Display.getDefault().asyncExec(()->{
				for (ConfigurableModel cm : cms) {
					MenuItem mi = new MenuItem(mnuAddColumns, SWT.PUSH);
					mi.setText(MessageFormat.format("Add All Attributes from ''{0}''", cm.getName()));
					mi.addListener(SWT.Selection, e->addFromConfigurableModel(cm));			
					
					MenuItem miSortCm = new MenuItem(mnuSortColumns, SWT.PUSH);
					miSortCm.setText(MessageFormat.format("Configurable Model: {0}", cm.getName()));
					miSortCm.addListener(SWT.Selection, e->sortDefaultOtherCmAttribute(cm));					
				}
			});
			return Status.OK_STATUS;
		}		
	};
	
	class LoadColumnsAndExecute extends Job{

		private Consumer<List<String>> consumer;
		private ConfigurableModel cm;
		
		public LoadColumnsAndExecute( ConfigurableModel cm, Consumer<List<String>> consumer) {
			super("load configurable model"); //$NON-NLS-1$
			this.consumer = consumer;
			this.cm = cm;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<String> keys = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				cm = session.get(ConfigurableModel.class, cm.getUuid());
			
				//load all nodes
				List<CmNode> toprocess = new ArrayList<>();
				toprocess.addAll(cm.getNodes());
				while(!toprocess.isEmpty()) {
					CmNode node = toprocess.remove(0);
					toprocess.addAll(node.getChildren());
					for (CmAttribute a : node.getCmAttributes()) {
						keys.add(a.getAttribute().getKeyId());
					}
				}
			}
			
			//run process
			consumer.accept(keys);
			return Status.OK_STATUS;
		}
		
	}

}