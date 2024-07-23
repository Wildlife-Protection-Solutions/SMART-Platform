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
package org.wcs.smart.cybertracker.ctpackage.ui;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.IconCache;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.SmartMobileDeviceManager;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.SmartMobileDevice;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.IconSelectionDialog;
import org.wcs.smart.ui.IconSelectionDialog.Type;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * dialog for managing device aliases
 * 
 * @since 8.1.0
 */
public class SmartMobileDeviceList extends SmartStyledTitleDialog {

	private TableViewer tableViewer;
	private Button btnDelete, btnAdd;
	private Composite container;
	
	private List<SmartMobileDevice> deletedDevices = null;
	private List<SmartMobileDevice> devices = null;
	private ConservationArea currentCa = null;
	private IconCache iconCache;
	
	private SmartMobileSorter sorter ;
	/*
	 * columns in the station table
	 */
	private enum Column {
		ICON(DialogConstants.ICON_TEXT, 1),
		DEVICE(Messages.SmartMobileDeviceList_IDColumnName, 4),
		NAME(Messages.SmartMobileDeviceList_NameColumnName, 6);
		
		String name;
		int weight;
		
		Column(String name, int weight) {
			this.name = name;
			this.weight = weight;
		}
	};
	
	private Job loadDevices = new Job(DialogConstants.LOADING_TEXT) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try(Session session = HibernateManager.openSession()){
				
				SmartMobileDeviceManager.INSTANCE.createMissingDevices(session, currentCa, Locale.getDefault());
				
				devices = SmartMobileDeviceManager.INSTANCE.getDevices(session, SmartDB.getCurrentConservationArea());
				devices.forEach(e->Hibernate.initialize(e));
			}catch (Exception ex) {
				CyberTrackerPlugIn.log(MessageFormat.format(Messages.SmartMobileDeviceList_LoadError,ex.getMessage()), ex);
				return Status.CANCEL_STATUS;
			}
			
			Display.getDefault().asyncExec(()->{
				tableViewer.setInput(devices);
				tableViewer.refresh();
				btnAdd.setEnabled(true);
			});
			return Status.OK_STATUS;
		}
		
	};
	public SmartMobileDeviceList(Shell parent) {
		super(parent);
		this.currentCa = SmartDB.getCurrentConservationArea();
		this.deletedDevices = new ArrayList<>();
	}
	
	@Override
	public Point getInitialSize() {
		Point p = super.getInitialSize();
		if (p.x < 650) p.x = 650;
		return p;		
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}
	
	public void setChangesMade() {
		getButton(IDialogConstants.OK_ID).setEnabled(true);
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite) super.createDialogArea(parent);
		
		this.iconCache = new IconCache(parent);
		parent.addListener(SWT.Dispose, e->iconCache.dispose());
		
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite composite2 = new Composite(container, SWT.NONE);
		composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		((GridData)composite2.getLayoutData()).heightHint = 200;
		
		TableColumnLayout tableLayout = new TableColumnLayout();
		composite2.setLayout(tableLayout);
		
		tableViewer = new TableViewer(composite2, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(tableViewer, new FocusCellHighlighter(tableViewer){});
		
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(tableViewer) {
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		
		TableViewerEditor.create(tableViewer, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | ColumnViewerEditor.KEYBOARD_ACTIVATION);

		createColumns(tableViewer);

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);

		sorter = new SmartMobileSorter(tableViewer);
		tableViewer.setComparator(sorter);
		tableViewer.getTable().addListener(SWT.Selection, e->{
			SmartMobileDevice device = getSelection();
			if (device == null) {
				btnDelete.setEnabled(false);
				return;
			}
			btnDelete.setEnabled(true);
		});
		
		tableViewer.getTable().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				ViewerCell cell = tableViewer.getCell(new Point(event.x, event.y));
				if (cell == null) return;
				if (cell.getColumnIndex() == Column.ICON.ordinal()){
					editIcon();
				}
			}
		});
		
		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false,1, 1));
		((GridLayout)composite.getLayout()).marginWidth = 0;
		((GridLayout)composite.getLayout()).marginHeight = 0;

		btnAdd = new Button(composite, SWT.NONE);
		btnAdd.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,1, 1));
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.addListener(SWT.Selection, e->addDevice());
		
		btnDelete = new Button(composite, SWT.NONE);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,false, 1, 1));
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setEnabled(false);
		btnDelete.addListener(SWT.Selection, e->deleteDevice());
		
		Menu menu = new Menu(tableViewer.getTable());
		tableViewer.getTable().setMenu(menu);
			
		MenuItem miAdd = new MenuItem(menu, SWT.NONE);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.addListener(SWT.Selection, e->addDevice());

		new MenuItem(menu, SWT.SEPARATOR);

		MenuItem miClear = new MenuItem(menu, SWT.PUSH);
		miClear.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miClear.setText(Messages.SmartMobileDeviceList_ClearImage);
		miClear.addListener(SWT.Selection, et->updateIcon(getSelection(), null));
		
		MenuItem miDelete = new MenuItem(menu, SWT.NONE);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addListener(SWT.Selection, e->deleteDevice());

		
		menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {				
				Object x = getSelection();
				miClear.setEnabled(x != null);
				miDelete.setEnabled(x != null);
				miAdd.setEnabled(devices != null);
		}});
		
		
		getShell().setText(Messages.SmartMobileDeviceList_Title);
		setMessage(Messages.SmartMobileDeviceList_Message);
		setTitle(Messages.SmartMobileDeviceList_Title);
		
		btnAdd.setEnabled(false);
		btnDelete.setEnabled(false);
		
		loadDevices.schedule();
		
		return container;
	}
	
	
	private SmartMobileDevice getSelection() {
		Object x = tableViewer.getStructuredSelection().getFirstElement();
		if (x instanceof SmartMobileDevice d) {
			return d;
		}
		return null;
	}

	private void deleteDevice(){
		SmartMobileDevice  device = getSelection();
		if (device == null) return;
		
		if (!MessageDialog.openConfirm(getShell(), Messages.SmartMobileDeviceList_ConfirmDeleteTitle, 
				MessageFormat.format(Messages.SmartMobileDeviceList_ConfirmDeleteMsg, device.getName()))){
			return;
		}
		
		deletedDevices.add(device);
		devices.remove(device);
		setChangesMade();
		tableViewer.refresh();
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected void okPressed() {
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			s.beginTransaction();
			try {
				for (SmartMobileDevice d : deletedDevices) s.remove(d);
				
				List<SmartMobileDevice> saved = new ArrayList<>();
				for (SmartMobileDevice d : devices) {
					d.setIcon(HibernateManager.saveOrMerge(s, d.getIcon()));
					s.flush();
					d = HibernateManager.saveOrMerge(s, d);
					s.flush();
					saved.add(d);
				}				
				s.getTransaction().commit();
				
				devices.clear();
				devices.addAll(saved);
				deletedDevices.clear();
				
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			} catch (Exception ex) {
				s.getTransaction().rollback();
				CyberTrackerPlugIn.displayError(DialogConstants.ERROR_STRING, MessageFormat.format(Messages.SmartMobileDeviceList_SaveError,  ex.getMessage()), ex);
			}
		}
		
	}
	
	private void addDevice(){
		SmartMobileDevice device = new SmartMobileDevice();
		device.setConservationArea(currentCa);
		device.setName(SmartMobileDeviceManager.INSTANCE.generateDeviceName(devices, Locale.getDefault()));
		
		devices.add(device);
		setChangesMade();
		tableViewer.refresh();
		
		tableViewer.editElement(device, Column.NAME.ordinal());
		
	}
	
	
	private void editIcon() {
		SmartMobileDevice device = getSelection();
		
		IconSelectionDialog dialog = new IconSelectionDialog(tableViewer.getControl().getShell(), Type.SELECT);
		if (dialog.open()  != Window.OK) return ;
		updateIcon(device, dialog.getSelectedIcon());
	}
	
	private void updateIcon(SmartMobileDevice device, Icon icon) {
		if (device == null) return;
		iconCache.clearCache(device);
		device.setIcon(icon);
		tableViewer.refresh();
		setChangesMade();
	}
	
	/**
	 * Finds the property for the given column from the station.
	 * 
	 * @param type the property required
	 * @param stn station 
	 * @return string value of the requested property
	 */
	public String findValue(Column type, SmartMobileDevice device) {
		if (type == Column.NAME) {
			return device.getName();
		}else if (type == Column.DEVICE){
			if (device.getDeviceId() == null) return ""; //$NON-NLS-1$
			return device.getDeviceId().toString();
		}
		return ""; //$NON-NLS-1$
	}

	private String validate(Column type, SmartMobileDevice device, Object newValue){
		if (type == Column.NAME){
			if (!findValue(type, device).equals((String)newValue)){
				String newName = (String)newValue;
				if(!SmartUtils.isSimpleString(newName.trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, DmObject.MAX_NAME_LENGTH)){					
					//invalid value, show error
					return 	MessageFormat.format(Messages.SmartMobileDeviceList_NameError1, 
							SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc, DmObject.MAX_NAME_LENGTH);
				}
			}
		}else if (type == Column.DEVICE){
			if (!findValue(type, device).equals((String)newValue)){
				String newName = (String)newValue;
				if (newName.length() > SmartMobileDevice.MAX_DEVICE_ID_LENGTH) {
					return MessageFormat.format(Messages.SmartMobileDeviceList_NameError2,  SmartMobileDevice.MAX_DEVICE_ID_LENGTH);
				}
			}		
		}
		return null;
	}
	/**
	 * Updates the given station object with the new value.
	 * @param type
	 * @param stn
	 * @param newValue
	 */
	private void updateValue(Column type, SmartMobileDevice device, Object newValue) {
		if (type == Column.NAME) {
			if (!findValue(type, device).equals((String)newValue)){
				device.setName((String)newValue);
				setChangesMade();				
			}
		}else if (type == Column.DEVICE){
			if (!findValue(type, device).equals((String)newValue)){
				device.setDeviceId((String)newValue);
				setChangesMade();				
			}		
		}
	}
	
	/*
	 * Creates station table columns
	 */
	private void createColumns(TableViewer viewer) {

		for (int i = 0; i < Column.values().length; i++) {
			final Column colum = Column.values()[i];
			final TableViewerColumn col = createTableViewerColumn(viewer, colum.name, colum.weight, i);
			
			if (colum == Column.ICON) col.getColumn().setWidth( 32 * 3 + 20);

			col.setLabelProvider(new SmartMobileDeviceLabelProvider(colum));
			if (colum == Column.NAME || colum == Column.DEVICE){
				final TextTableEditor ed = new TextTableEditor(viewer, colum);
				ed.editor.addListener(new ICellEditorListener() {
					
					@Override
					public void editorValueChanged(boolean oldValidState, boolean newValidState) {
						setErrorMessage(ed.editor.getErrorMessage());
					}
					
					@Override
					public void cancelEditor() {
						setErrorMessage(null);
					}
					
					@Override
					public void applyEditorValue() {
						setErrorMessage(null);
						
					}
				});
				col.setEditingSupport(ed);			
			}
			
			if (colum != Column.ICON) {
				col.getColumn().addListener(SWT.Selection, e->sorter.setSortColumn(colum, col.getColumn()));				
			}
		}
	}
	
	private TableViewerColumn createTableViewerColumn(TableViewer viewer,
			String title, int weight, int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setResizable(true);
		column.setMoveable(true);

		TableColumnLayout layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(weight,ColumnWeightData.MINIMUM_WIDTH, true));
		return viewerColumn;
	}
	
	
	private class TextTableEditor extends EditingSupport {
		private Column column;
		private TableViewer viewer;
		private CellEditor editor;

		TextTableEditor(TableViewer viewer, Column column) {
			super(viewer);
			this.column = column;
			this.viewer = viewer;
			this.editor = new TextCellEditor(viewer.getTable());	
		}

		@Override
		protected void setValue(Object element, Object value) {
			updateValue(column, (SmartMobileDevice) element, (String) value);
			viewer.refresh();
		}

		@Override
		protected Object getValue(Object element) {
			return findValue(column, (SmartMobileDevice) element);
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			this.editor.setValidator(new ICellEditorValidator() {
				@Override
				public String isValid(Object value) {
					setErrorMessage(null);
					return validate(column, ((SmartMobileDevice)element), (String)value);
				}
			});
			return this.editor;
		}

		@Override
		protected boolean canEdit(Object element) {
			if (element instanceof SmartMobileDevice){
				return true;
			}
			return false;
		}
	}
	
	class SmartMobileDeviceLabelProvider extends ColumnLabelProvider { 
		private Column column;
		
		public SmartMobileDeviceLabelProvider(Column column){
			this.column = column;
		}
		@Override
		public String getText(Object element) {
			if (element instanceof SmartMobileDevice d) {
				String x = findValue(column, d);
				if (x == null){
					return ""; //$NON-NLS-1$
				}
				return x;
			}
			return super.getText(element);
		}
		 
		@Override
		public Image getImage(Object element) {
			if (!(element instanceof SmartMobileDevice)) return null;
			
			if (column != Column.ICON) return null;
			SmartMobileDevice team = (SmartMobileDevice)element;
			return iconCache.getImage(team);
		}
	}
	
	class SmartMobileSorter extends ViewerComparator{
		private Column column = null;
		private int direction = SWT.DOWN;
		private TableViewer viewer;
		
		public SmartMobileSorter(TableViewer viewer) {
			this.viewer = viewer;
		}
		
		public void setSortColumn(Column sort, TableColumn tcolumn){
			
			if (column != null &&column == sort){
				if (direction == SWT.DOWN){
					direction = SWT.UP;
				}else{
					direction = SWT.DOWN;
				}
			}
			this.column = sort;
			viewer.getTable().setSortDirection(direction);
			viewer.getTable().setSortColumn(tcolumn);
			viewer.refresh();
		}
			
		
		@Override
		public int compare(Viewer viewer, Object object1, Object object2){
			if (column == null){
				//no sort column picked
				return 0;
			}
			ColumnLabelProvider ll = (ColumnLabelProvider) this.viewer.getLabelProvider(this.column.ordinal());
			return (this.direction == SWT.DOWN ? -1 : 1) * Collator.getInstance().compare(ll.getText(object1),ll.getText(object2));
		}
	}
}
