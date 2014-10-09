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

package org.wcs.smart.er.ui.mission.export;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.wcs.smart.util.SmartUtils;

/**
* Dialog to allow users to export multiple objects at once into xml file.
* 
* @author jeffloun
* @since 4.0.0
*/
public abstract class XmlMultiExportTreeViewerDialog extends TitleAreaDialog implements IUpdatableView {

	protected Text txtFile;
	private Button btnIncludeAttachments;
	private CheckboxTreeViewer chReports;
		
	private String dirName;
	private boolean includeAttachements;
	private List<byte[]> objUuids;

	private String filterLinkText;
	private Link filterLink;
	
	/**
	 * Creates a new dialog
	 * @param parentShell parent shell
	 */
	public XmlMultiExportTreeViewerDialog(Shell parentShell, String filterLinkText) {
		super(parentShell);
		this.filterLinkText = filterLinkText;
	}

	@Override
	protected void okPressed() {
		this.objUuids = new ArrayList<byte[]>();
		Object[] checked = chReports.getCheckedElements();

		Map<String, String> file2Obj = new HashMap<String, String>();
		for (int i = 0; i < checked.length; i ++){
			TreeItem ti = (TreeItem) checked[i];
			if(ti instanceof MissionTreeItem){
				String objName = String.valueOf(ti.getName() );
				String fileName = SmartUtils.getFileName(objName);
				if (file2Obj.containsKey(fileName)) {
					//output file name conflict error (two exported items will try to write data in a same file)
					MessageDialog.openWarning(getShell(), "Invalid File Names", MessageFormat.format("Objects with names {0} and {1} will be written to the same output file with the name {2}.\n\nRename one of the objects or export them separately.", file2Obj.get(fileName), objName, fileName));
					return;
				}
				file2Obj.put(fileName, objName);
				objUuids.add( ti.getUuid() );
			}
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
	public List<byte[]> getObjectUuids(){
		return this.objUuids;
	}
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button b = createButton(parent, IDialogConstants.OK_ID, "Export", true);
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
		lbl.setText("Destination Folder" + "*:"); //$NON-NLS-1$
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
		btnBrowse.setText("Browse");
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
		lbl.setText("Include Attachements" + "**:"); //$NON-NLS-1$
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
		
		Composite treeContainer = new Composite(main, SWT.NONE);
		GridLayout glt = new GridLayout(3, false);
		glt.verticalSpacing = glt.marginLeft = glt.marginRight = glt.marginTop = glt.marginBottom = 0;
		treeContainer.setLayout(glt);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		gd.heightHint = 250;
		treeContainer.setLayoutData(gd);
		
		
		
		chReports = new CheckboxTreeViewer(treeContainer, SWT.MULTI | SWT.BORDER);
		chReports.setLabelProvider(new CheckBoxTreeLabelProvider());
		chReports.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chReports.setContentProvider(new CheckBoxTreeContentProvider());
		chReports.setAutoExpandLevel(CheckboxTreeViewer.ALL_LEVELS);
		
		
		chReports.setCheckStateProvider(new ICheckStateProvider() {
			public boolean isGrayed(Object element) {
				return false;
			}
			
			@Override
			public boolean isChecked(Object element) {
				Object parent = ((CheckBoxTreeContentProvider)chReports.getContentProvider()).getParent(element);
				if (parent == null){
					return false;
				}else{
					return chReports.getChecked(parent);
				}
			}
		});
		
		chReports.getTree().addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (chReports.getSelection().isEmpty()){
					return;
				}
				if (e.keyCode == SWT.SPACE){
					IStructuredSelection selection = ((IStructuredSelection)chReports.getSelection());
					selection.getFirstElement();
					boolean value = chReports.getChecked(   selection.getFirstElement() );
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						chReports.setChecked(tp, !value);
					}
					e.doit = false;
							
				}
				
			}
		});
		chReports.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {

				if (event.getElement() instanceof SurveyTreeItem ){
					boolean newState = event.getChecked();
					//check or uncheck all sub Missions
					List<Object> objects = new ArrayList<Object>();
					objects.add(event.getElement());
					while(objects.size() > 0){
						Object o = objects.remove(0);
						chReports.setChecked(o, newState);
						if (o instanceof SurveyTreeItem ){
							Object[] kids = ((CheckBoxTreeContentProvider)chReports.getContentProvider()).getChildren(o);
							for (Object kid : kids){
								objects.add(kid);
							}
						}
					}	
					chReports.setGrayed(event.getElement(), false);
				}
				//if checked then we want to check all parent elements
				if (event.getChecked()){
					
					Object parent = ((CheckBoxTreeContentProvider)chReports.getContentProvider()).getParent(event.getElement());
					while(parent != null){
						chReports.setGrayChecked(parent, true);
						parent = ((CheckBoxTreeContentProvider)chReports.getContentProvider()).getParent(parent);
					}
				}else{
					//we want de-select parent if appropriate 
					Object parent = ((CheckBoxTreeContentProvider)chReports.getContentProvider()).getParent(event.getElement());
					while(parent != null){
						//if any of the children are checked then
						//we need to unselect 
						boolean checked = false;
						Object[] kids = ((CheckBoxTreeContentProvider)chReports.getContentProvider()).getChildren(parent);
						for (Object k : kids){
							if (chReports.getChecked(k)){
								checked = true;
								break;
							}
						}
						chReports.setGrayChecked(parent, checked);
						
						parent = ((CheckBoxTreeContentProvider)chReports.getContentProvider()).getParent(parent);
					}
				}
				
			}
		});
		
		Composite lowerComp = new Composite(main, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.verticalSpacing = gl.marginLeft = gl.marginRight = gl.marginTop = gl.marginBottom = 0;
		lowerComp.setLayout(gl);
		lowerComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP,true,false,3,1));
		
		final Link selectAll = new Link(lowerComp, SWT.NONE);
		selectAll.setText("<a>" + "Select All" + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		
		lbl = new Label(lowerComp, SWT.VERTICAL | SWT.SEPARATOR);
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.heightHint = selectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		lbl.setLayoutData(gd);
		
		final Link deselectAll = new Link(lowerComp, SWT.NONE);
		deselectAll.setText("<a>" + "Deselect All" + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		
		Listener listener = new Listener(){
			@Override
			public void handleEvent(Event event) {
				chReports.setAllChecked(event.widget == selectAll);
			}};
		deselectAll.addListener(SWT.Selection, listener);
		selectAll.addListener(SWT.Selection, listener);
			
		lbl = new Label(main, SWT.WRAP);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		gd.widthHint = 250;
		lbl.setText("*" + "Existing files may automatically be overwritten."); //$NON-NLS-1$
		lbl.setLayoutData(gd);
		
		
		lbl = new Label(main, SWT.WRAP);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		gd.widthHint = 250;
		lbl.setText("**" + "If attachments are included a zip file will be generated that includes the selected object and attachments. Otherwise only a xml file is exported."); //$NON-NLS-1$
		lbl.setLayoutData(gd);
		
		return composite;

	}

	protected CheckboxTreeViewer getTreeViewer() {
		return chReports;
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