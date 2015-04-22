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

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.Language;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.DialogConstants;
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
		List<?> areas = session.createCriteria(Area.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).add(Restrictions.eq("type", type)).list(); //$NON-NLS-1$ //$NON-NLS-2$
		
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
			ConservationAreaManager.getInstance().fireAreaChanged(type);
			getSession().beginTransaction();
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.AreaNameDialogPage_Error_Save + ex.getLocalizedMessage(), ex);
		}
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite)super.createDialogArea(parent);
		
		Composite main = new Composite(composite, SWT.NONE);
//		main.setLayout(new TableColumnLayout());
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tableViewer = new TableViewer(main, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.MULTI
				| SWT.FULL_SELECTION);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 200;
		gd.heightHint = 200;
		tableViewer.getTable().setLayoutData(gd);

		createKeyColumn();
		createColumn(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		List<Language> sortedLangs = new ArrayList<Language>();
		sortedLangs.addAll(SmartDB.getCurrentConservationArea().getLanguages());
		Collections.sort(sortedLangs, new Comparator<Language>() {
			@Override
			public int compare(Language l0, Language l1) {
				if (l0.isDefault()) return -1;
				return Collator.getInstance().compare(l0.getDisplayName(), l1.getDisplayName());
			}
		});
		for (Language l : sortedLangs) {
			if (l.isDefault()) continue;
			createColumn(l);
		}
		
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		
		loadAreaTypes();		
		
		getShell().setText(Messages.AreaNameDialogPage_DialogTitle);
		setTitle(Messages.AreaNameDialogPage_MessageTitle + type.getGuiName());
		setMessage(Messages.AreaNameDialogPage_DialogMessage);
		
		return composite; 
	}
	
	private void createColumn(final Language l){
		TableViewerColumn colName = new TableViewerColumn(tableViewer,
				SWT.NONE);
		TableColumn column = colName.getColumn();
		column.setText(l.getDisplayName());
		column.setResizable(true);
		column.setMoveable(false);
		column.setWidth(150);

		colName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Area) {
					return ((Area) element).findName(l);
				}
				return ""; //$NON-NLS-1$
			}
		});
		colName.setEditingSupport(new EditingSupport(colName.getViewer()) {
			@Override
			protected void setValue(Object element, Object value) {
				if (!(((String) value).equals(((Area) element).findName(l)))) {
					String newId = (String) value;
					((Area) element).updateName(l, newId);
					if (l.isDefault()){
						((Area)element).setName(newId);
					}
					tableViewer.refresh();
					setDirty();
				}
				validate();
			}

			@Override
			protected Object getValue(Object element) {
				String value = ((Area) element).findName(l);
				if (value == null)
					return ""; //$NON-NLS-1$
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
	}
	
	private void createKeyColumn(){
		TableViewerColumn colKey = new TableViewerColumn(tableViewer,SWT.NONE);
		TableColumn column = colKey.getColumn();
		column.setText(Messages.AreaNameDialogPage_Key_ColumnName);
		column.setResizable(true);
		column.setMoveable(false);
		column.setWidth(150);
		colKey.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element) {
				if (element instanceof Area){
					return ((Area)element).getKeyId();
				}
				return ""; //$NON-NLS-1$
			}
		});
		colKey.setEditingSupport(new EditingSupport(colKey.getViewer()) {
			@Override
			protected void setValue(Object element, Object value) {
				setErrorMessage(null);
				if (!( ((String)value).equals(((Area)element).getKeyId()))){
					String newKey = ((String)value).toLowerCase();
					((Area)element).setKeyId(newKey);
					tableViewer.refresh();
					setDirty();	
				}
				validate();
			}
			
			@Override
			protected Object getValue(Object element) {
				String value = ((Area)element).getKeyId();
				if (value == null) return ""; //$NON-NLS-1$
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
	}

	private void setDirty(){
		dirty = true;
	}
	
	private void validate(){
		Object[] data = (Object[]) tableViewer.getInput();
		
		HashSet<String> keys = new HashSet<String>();
		setErrorMessage(null);
		for (int i = 0; i < data.length; i ++){
			Area area = (Area) data[i];
			
			if (keys.contains(area.getKeyId())){
				setErrorMessage(Messages.AreaNameDialogPage_Error_DuplicateKey);
				break;
			}
			keys.add(area.getKeyId());
			
			if (!SmartUtils.isSimpleString(area.getKeyId(), RegExLevel.ALLOWED_CHARS_SIMPLE_REGEX, Area.KEY_MAX_LENGTH)){
				setErrorMessage(MessageFormat.format(Messages.AreaNameDialogPage_Error_InvalidKey, new Object[]{RegExLevel.ALLOWED_CHARS_SIMPLE_REGEX.textDesc,  Area.KEY_MAX_LENGTH }));
				break;
			}else if (area.getKeyId().substring(0, 1).matches("[\\p{Nd}_]")){ //$NON-NLS-1$
				setErrorMessage(Messages.AreaNameDialogPage_Error_InvalidKey2);
				break;
			}
			
			for (org.wcs.smart.ca.Label name : area.getNames()){
				String newName = name.getValue();
				if (newName.length() > Area.NAME_MAX_LENGTH) {
					setErrorMessage(MessageFormat
							.format(Messages.AreaNameDialogPage_Warning_NameTruncate1,
									new Object[] { Area.NAME_MAX_LENGTH }));
					break;
				}
				
				if (name.getLanguage().isDefault() && newName.trim().isEmpty()){
					setErrorMessage(MessageFormat.format(Messages.AreaNameDialogPage_DefaultNameMissing,
							new Object[]{name.getLanguage().getDisplayName()}));
					break;
				}
			}
		}		

		
		if (getErrorMessage() == null){
			getButton(IDialogConstants.OK_ID).setEnabled(true);	
		}else{
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		//update focus to ensure editing is completed
		getButtonBar().setFocus();
		
		if (buttonId == IDialogConstants.CANCEL_ID){
			if (dirty){
				if (!MessageDialog.openConfirm(getShell(), Messages.AreaNameDialogPage_ConfirmClose_DialogTitle, Messages.AreaNameDialogPage_ConfirmClose_DialogMessage)){
					return;
				}
			}
			close();
		}else if (buttonId == IDialogConstants.OK_ID){
			saveAreaTypes();
			getButton(IDialogConstants.OK_ID).setEnabled(dirty);
		}
	}
		
	/** dialog is resizable
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}
}
