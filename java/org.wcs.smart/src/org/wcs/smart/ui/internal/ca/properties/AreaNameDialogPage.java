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
package org.wcs.smart.ui.internal.ca.properties;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;


/**
 * Dialog for allowing users to update Area names
 * and keys.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AreaNameDialogPage extends TitleAreaDialog {
	
	private Session session;
	private Area.AreaType type;
	private TableViewer tableViewer;
	
	private boolean dirty = false;
	
	public AreaNameDialogPage(Shell parentShell, Area.AreaType type) {
		super(parentShell);
		
		this.type = type;
	}

	
	@Override
	public boolean close(){
		boolean ret = super.close();
		getSession().getTransaction().rollback();
		getSession().close();
		return ret;
	}
	private Session getSession(){
		if (session == null){
			session = HibernateManager.openSession();
		}
		return session;
	}
	
	/**
	 * Loads all the areas for the given area type
	 */
	private void loadAreaTypes(){
		Session session = getSession();
		session.beginTransaction();
		List<?> areas = session.createCriteria(Area.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).add(Restrictions.eq("type", type)).list();
		
		tableViewer.setInput(areas.toArray());
		tableViewer.refresh();
	}
	
	
	/**
	 * Saves all changes
	 */
	private void saveAreaTypes(){
		try{
			getSession().getTransaction().commit();
			dirty = false;
			getSession().beginTransaction();
		}catch (Exception ex){
			SmartPlugIn.log("Could not save changes.\n\n" + ex.getMessage(), ex);
		}
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite)super.createDialogArea(parent);
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new TableColumnLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tableViewer = new TableViewer(main, SWT.BORDER | SWT.MULTI
				| SWT.FULL_SELECTION);

		TableViewerColumn colName = new TableViewerColumn(tableViewer,SWT.NONE);
		TableColumn column = colName.getColumn();
		column.setText("Name");
		column.setResizable(true);
		column.setMoveable(false);
		TableColumnLayout layout = (TableColumnLayout) tableViewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(60, ColumnWeightData.MINIMUM_WIDTH, true));
		
		colName.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element) {
				if (element instanceof Area){
					return ((Area)element).getId();
				}
				return "";
			}
		});
		colName.setEditingSupport(new EditingSupport(colName.getViewer()) {
			@Override
			protected void setValue(Object element, Object value) {
				setErrorMessage(null);
				if (!( ((String)value).equals(((Area)element).getId()))){
					String newId = (String)value;
					if (newId.length() > Area.ID_MAX_LENGTH){
						setErrorMessage("Name has been truncated to " + Area.ID_MAX_LENGTH + " characters.");
						newId = newId.substring(0, Area.ID_MAX_LENGTH);
					}
					
					((Area)element).setId(newId);
					tableViewer.refresh();
					setDirty();
				}
			}
			
			@Override
			protected Object getValue(Object element) {
				String value = ((Area)element).getId();
				if (value == null) return "";
				return value;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(tableViewer.getTable());
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		
		TableViewerColumn colKey = new TableViewerColumn(tableViewer,SWT.NONE);
		column = colKey.getColumn();
		column.setText("Key");
		column.setResizable(true);
		column.setMoveable(false);
		layout.setColumnData(column, new ColumnWeightData(40, ColumnWeightData.MINIMUM_WIDTH, true));
		
		colKey.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element) {
				if (element instanceof Area){
					return ((Area)element).getKeyId();
				}
				return "";
			}
		});
		colKey.setEditingSupport(new EditingSupport(colKey.getViewer()) {
			@Override
			protected void setValue(Object element, Object value) {
				setErrorMessage(null);
				if (!( ((String)value).equals(((Area)element).getKeyId()))){
					String newKey = ((String)value).toLowerCase();
					//validate that the key is different from all the others
					//and does not contain weird characters
					if (!SmartUtils.isSimpleString(newKey, RegExLevel.ALLOWED_CHARS_SIMPLE_REGEX, Area.KEY_MAX_LENGTH)){
						setErrorMessage("Invalid key.  Key mush only contain " + RegExLevel.ALLOWED_CHARS_SIMPLE_REGEX.textDesc +" and be less than " + Area.KEY_MAX_LENGTH + " characters.");
						return;
					}else{
						Object[] data = (Object[]) tableViewer.getInput();
						for (int i = 0; i < data.length; i ++){
							if (((Area)data[i]) == element) continue;
							if (((Area)data[i]).getKeyId().equals(newKey)){
								setErrorMessage("Keys cannot be duplicated.");
								return;
							}
						}				
					}
					
					((Area)element).setKeyId(newKey);
					tableViewer.refresh();
					setDirty();
				}
			}
			
			@Override
			protected Object getValue(Object element) {
				String value = ((Area)element).getKeyId();
				if (value == null) return "";
				return value;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(tableViewer.getTable());
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		
		loadAreaTypes();		
		
		getShell().setText("Modify Area Labels");
		setTitle("Modify " + type.getGuiName());
		setMessage("Update area names here.  Modifying keys is not recommended as it will affect other parts of the system.");
		
		return composite; 
	}
	
	private void setDirty(){
		getButton(IDialogConstants.OK_ID).setEnabled(true);
		dirty = true;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btn = createButton(parent, IDialogConstants.OK_ID, "Save", true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, "Close", false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID){
			if (dirty){
				if (!MessageDialog.openConfirm(getShell(), "Confirm Close", "All unsaved changes will be lost.  Are you sure you want to close?")){
					return;
				}
			}
			close();
		}else if (buttonId == IDialogConstants.OK_ID){
			saveAreaTypes();
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}
		
	/** dialog is resizable
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}
}
