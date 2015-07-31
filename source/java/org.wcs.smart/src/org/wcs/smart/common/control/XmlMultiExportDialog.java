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
package org.wcs.smart.common.control;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.common.filter.IUpdatableView;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog to allow users to export multiple objects at once into xml file.
 * 
 * @author egouge
 * @author elitvin
 * @since 1.0.0
 */
public abstract class XmlMultiExportDialog extends TitleAreaDialog implements IUpdatableView {

	private static final String LOADING_TEXT = Messages.XmlMultiExportDialog_LoadingText;
	
	protected Text txtFile;
	private Button btnIncludeAttachments;
	private CheckboxTableViewer tableViewer;
	
	private String dirName;
	private boolean includeAttachements;
	private List<UUID> objUuids;

	private String filterLinkText;
	private Link filterLink;
	
	/**
	 * Creates a new dialog
	 * @param parentShell parent shell
	 */
	public XmlMultiExportDialog(Shell parentShell, String filterLinkText) {
		super(parentShell);
		this.filterLinkText = filterLinkText;
	}

	@Override
	protected void okPressed() {
		this.objUuids = new ArrayList<UUID>();
		Object[] checked = tableViewer.getCheckedElements();
		int objNameIndex = 0;
		if (checked.length > 0 && ((Object[])checked[0]).length >= 3) {
			//expected table viewer input [gui name, uuid, object name]; if name is missing use gui name as object name
			objNameIndex = 2; 
		}
		Map<String, String> file2Obj = new HashMap<String, String>();
		for (int i = 0; i < checked.length; i ++){
			String objName = String.valueOf(((Object[])checked[i])[objNameIndex]);
			String fileName = SmartUtils.getFileName(objName);
			if (file2Obj.containsKey(fileName)) {
				//output file name conflict error (two exported items will try to write data in a same file)
				MessageDialog.openWarning(getShell(), Messages.XmlMultiExportDialog_WarnDialog_Title, MessageFormat.format(Messages.XmlMultiExportDialog_FilenameConflict_Message, file2Obj.get(fileName), objName, fileName));
				return;
			}
			file2Obj.put(fileName, objName);
			objUuids.add( (UUID)((Object[])checked[i])[1] );
		}
		super.okPressed();
	}
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		dirName = txtFile.getText();
		includeAttachements = btnIncludeAttachments.getSelection();
		super.buttonPressed(buttonId);
	}

	/**
	 * @return the filename selected by user
	 */
	public String getDirectory() {
		return this.dirName;
	}

	/**
	 * @return if attachments should be included
	 */
	public boolean getIncludeAttachments() {
		return this.includeAttachements;
	}

	/**
	 * 
	 * @return list of uuids to export
	 */
	public List<UUID> getObjectUuids(){
		return this.objUuids;
	}
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button b = createButton(parent, IDialogConstants.OK_ID, Messages.XmlMultiExportDialog_ExportButton, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

		b.setEnabled(false);
		if (txtFile.getText() != null && txtFile.getText().length() > 0) {
			b.setEnabled(true);
		}
	}

	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.XmlMultiExportDialog_DestinationFolderLabel + "*:"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		txtFile = new Text(main, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		
		txtFile.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (txtFile.getText() != null && txtFile.getText().length() > 0) {
					if (getButton(IDialogConstants.OK_ID) != null){
						getButton(IDialogConstants.OK_ID).setEnabled(true);
					}
				}
			}
		});
		String value = getDefaultOutputFolder();
		if (value != null){
			txtFile.setText(value);
		}
				
		Button btnBrowse = new Button(main, SWT.NONE);
		btnBrowse.setText(Messages.XmlMultiExportDialog_BrowseButton);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.SAVE);
			
				if (txtFile.getText().length() > 0) {
					dd.setFilterPath(txtFile.getText());
				}
				String f = dd.open();
				if (f != null) {
					txtFile.setText(f);
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});

		lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.XmlMultiExportDialog_IncludeAttachmentsLabel + "**:"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		btnIncludeAttachments = new Button(main, SWT.CHECK);
		btnIncludeAttachments.setSelection(getDefaultIncludeAttachments());
			
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		btnIncludeAttachments.setLayoutData(gd);
		
		filterLink = new Link(main, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		filterLink.setLayoutData(gd);
		filterLink.setText(filterLinkText);
		
		filterLink.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleFilterLinkClicked();
			}
		});
		
		tableViewer = CheckboxTableViewer.newCheckList(main, SWT.BORDER | SWT.MULTI);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		gd.heightHint = 200;
		tableViewer.getControl().setLayoutData(gd);
		tableViewer.getTable().addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (tableViewer.getSelection().isEmpty()){
					return;
				}
				if (e.keyCode == SWT.SPACE){
					IStructuredSelection selection = ((IStructuredSelection)tableViewer.getSelection());
					selection.getFirstElement();
					boolean value = tableViewer.getChecked(   selection.getFirstElement() );
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						tableViewer.setChecked(tp, !value);
					}
					e.doit = false;
							
				}
				
			}
		});
		tableViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof Object[]){
					return (String)((Object[])element)[0];
				}
				return super.getText(element);
			}
		});
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		String[][] loadingdata = {{LOADING_TEXT, null}}; 
		tableViewer.setInput( loadingdata );
		loadObjectData();
		
		Composite lowerComp = new Composite(main, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.verticalSpacing = gl.marginLeft = gl.marginRight = gl.marginTop = gl.marginBottom = 0;
		lowerComp.setLayout(gl);
		lowerComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP,true,false,3,1));
		
		final Link selectAll = new Link(lowerComp, SWT.NONE);
		selectAll.setText("<a>" + Messages.XmlMultiExportDialog_SelectAllLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		
		lbl = new Label(lowerComp, SWT.VERTICAL | SWT.SEPARATOR);
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.heightHint = selectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		lbl.setLayoutData(gd);
		
		final Link deselectAll = new Link(lowerComp, SWT.NONE);
		deselectAll.setText("<a>" + Messages.XmlMultiExportDialog_DeselectAll_Label + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		
		Listener listener = new Listener(){
			@Override
			public void handleEvent(Event event) {
				tableViewer.setAllChecked(event.widget == selectAll);
			}};
		deselectAll.addListener(SWT.Selection, listener);
		selectAll.addListener(SWT.Selection, listener);
			
		lbl = new Label(main, SWT.WRAP);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		gd.widthHint = 250;
		lbl.setText("*" + Messages.XmlMultiExportDialog_OverwriteWarningLabel); //$NON-NLS-1$
		lbl.setLayoutData(gd);
		
		
		lbl = new Label(main, SWT.WRAP);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		gd.widthHint = 250;
		lbl.setText("**" + Messages.XmlMultiExportDialog_AttachmentInfoLabel); //$NON-NLS-1$
		lbl.setLayoutData(gd);
		
		return composite;

	}

	protected CheckboxTableViewer getTableViewer() {
		return tableViewer;
	}
	
	protected boolean getDefaultIncludeAttachments() {
		return true;
	}

	protected String getDefaultOutputFolder() {
		return ""; //$NON-NLS-1$
	}
	
	protected abstract void handleFilterLinkClicked();
	
	protected abstract void loadObjectData();
	
	@Override
	public boolean isResizable(){
		return true;
	}

	@Override
	public void updateContent() {
		loadObjectData();
	}
	
}
