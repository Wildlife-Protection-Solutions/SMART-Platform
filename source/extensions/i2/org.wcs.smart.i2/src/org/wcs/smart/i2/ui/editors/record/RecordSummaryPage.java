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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RecordManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Summary Page for record editor.
 * 
 * @author Emily
 *
 */
public class RecordSummaryPage extends EditorPart{

	private FormToolkit  toolkit;

	private ToolItem wsetItem;
	private ToolItem deleteItem;
	private ToolItem editItem;
	
	private Composite topPart;
	private Label headerLabel;
	
	private EntityListComposite entityPanel;
	private AttachmentListComposite attachmentPanel;
	private LocationListComposite locationPanel;
	
	private RecordEditor recordEditor;
	
	public RecordSummaryPage(RecordEditor parent){
		this.recordEditor =  parent;
	}

	public void clearLists(){
		attachmentPanel.getAttachmentsToDelete().clear();
		attachmentPanel.getNewEntityAttachments().clear();
		entityPanel.getEntityLinksAdded().clear();
		entityPanel.getEntityLinksToDelete().clear();
	}


	public LocationListComposite getLocationPanel(){
		return this.locationPanel;
	}
	public List<IntelEntityAttachment> getNewAttachments(){
		return attachmentPanel.getNewEntityAttachments();
	}
	
	public List<IntelRecordAttachment> getDeleteAttachments(){
		return attachmentPanel.getAttachmentsToDelete();
	}
	
	public List<IntelEntityRecord> getNewEntityLinks(){
		return entityPanel.getEntityLinksAdded();
	}
	public List<IntelEntityRecord> getDeleteEntityLinks(){
		return entityPanel.getEntityLinksToDelete();
	}
	
	public void doAfterSave(){
		headerLabel.setText(recordEditor.getRecord().getTitle());
		entityPanel.refreshEntities();
	}
	
	public void linkEntity(IntelEntity toLink){
		entityPanel.linkEntity(toLink);
	}
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
	}

	public void enableWs(boolean enable){
		wsetItem.setEnabled(enable);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		
		toolkit = new FormToolkit(parent.getDisplay());
		
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginWidth= 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).verticalSpacing= 0;
		
		
		Composite buttonPanel = toolkit.createComposite(parent, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(3, false));
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)buttonPanel.getLayout()).marginWidth= 5;
		((GridLayout)buttonPanel.getLayout()).marginHeight =5;
		
		headerLabel = toolkit.createLabel(buttonPanel, "");
		FontData fd = headerLabel.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font headerFont = new Font(parent.getShell().getDisplay(), fd);
		headerLabel.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				headerFont.dispose();	
			}
		});
		headerLabel.setFont(headerFont);
		headerLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		ToolBar buttonBar = new ToolBar(buttonPanel, SWT.HORIZONTAL | SWT.FLAT);
		buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		ToolItem refreshItem = new ToolItem(buttonBar, SWT.PUSH);
		refreshItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_REFRESH));
		refreshItem.setToolTipText("refresh record");
		refreshItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean doAction = true;
				if (recordEditor.getEditMode()){
					if (!MessageDialog.openConfirm(getSite().getShell(), "Refresh", "Changes will be lost.  Are you sure you want to refresh?")){
						doAction = false;
					}
				}
				if (doAction){
					recordEditor.setDirty(false);
					clearLists();
					recordEditor.refresh();				
				}
			}
		});
		
		deleteItem = new ToolItem(buttonBar, SWT.PUSH);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.setToolTipText("delete record");
		deleteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (MessageDialog.openConfirm(getSite().getShell(), "Delete", "Are you sure you want to delete this record.  This action cannot be undone.")){
					RecordManager.INSTANCE.deleteRecord(recordEditor.getRecord(), recordEditor.getContext());
				}
			}
		});
		deleteItem.setEnabled(recordEditor.getEditMode());
		
		
		wsetItem = new ToolItem(buttonBar, SWT.PUSH);
		wsetItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
		wsetItem.setToolTipText("add to current working set");
		wsetItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				WorkingSetManager.INSTANCE.addToActiveWorkingSet(recordEditor.getRecord(), recordEditor.getContext());
			}
		});
		wsetItem.setEnabled(false);
		
		editItem = new ToolItem(buttonBar, SWT.CHECK);
		editItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
		editItem.setToolTipText("enable or disable editing of record");
		editItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				recordEditor.setEditMode(!recordEditor.getEditMode());
				
			}
		});
		

		
		SashForm sash = new SashForm(parent, SWT.VERTICAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite top = createSectionHeader(sash, "Narrative");
		topPart = toolkit.createComposite(top, SWT.NONE);
		topPart.setLayout(new GridLayout(5, false));
//		((GridLayout)topPart.getLayout()).marginWidth = 0;
//		((GridLayout)topPart.getLayout()).marginHeight = 0;
		topPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Composite expEntities = createSectionHeader(sash, "Entities");
		entityPanel = new EntityListComposite(expEntities, toolkit, recordEditor);
		entityPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)entityPanel.getLayoutData()).horizontalSpan = 0;
		((GridData)entityPanel.getLayoutData()).verticalSpan = 0;
				
		Composite expAttachments = createSectionHeader(sash, "Attachments");
		attachmentPanel = new AttachmentListComposite(expAttachments, toolkit, recordEditor);
		attachmentPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite expLocation = createSectionHeader(sash, "Locations");
		locationPanel = new LocationListComposite(expLocation, toolkit, recordEditor);
		locationPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true)); 
		
	}


	private Composite createSectionHeader(SashForm parent, String text){
		Composite all = toolkit.createComposite(parent, SWT.NONE);
		all.setLayout(new GridLayout());
		((GridLayout)all.getLayout()).marginWidth = 0;
		((GridLayout)all.getLayout()).marginHeight = 0;
		
		Composite header = toolkit.createComposite(all, SWT.NONE);
		header.setLayout(new GridLayout());
		header.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)header.getLayout()).marginWidth = 2;
		((GridLayout)header.getLayout()).marginHeight = 2;
		
		
		Label l =toolkit.createLabel(header, text);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		final Font boldFont = new Font(parent.getDisplay(), fd);
		l.addDisposeListener((e) -> {boldFont.dispose();});
		l.setFont(boldFont);

		l.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				
				int[] weights = parent.getWeights();
				
				//find the index of the control
				int index = -1;
				for (int i = 0; i < parent.getChildren().length; i ++){
					if (parent.getChildren()[i] == all){
						index = i;
						break;
					}
				}
				if (index >= 0){
					//compute the new minimum height for the control
					int minHeight =  header.getClientArea().height;
					int totalweight = 0;
					for (int w : weights){
						totalweight += w;
					}
					double unitperpixel = totalweight / (parent.getClientArea().height* 1.0);
					double units = unitperpixel * minHeight;
					minHeight = (int)units + 5;
					
					if (weights[index] <= minHeight + 10 ){
						//compressed; we want to expand
						int adjust = (int) (unitperpixel * (parent.getClientArea().height / weights.length)) - minHeight;

						//take away from the previous or next collapsed panel
						int inc = -1;
						if (index == 0) inc = 1;						
						for (int i = index; i >=0 && i <= weights.length; i += inc){
							if ((weights[i] > minHeight + 10) && (weights[i] - adjust > minHeight)){
								weights[i] = weights[i] - adjust;
								weights[index] = adjust;
								break;
							}
						}
					}else{
						if (index == 0){
							//see if we can find one that is not collapses; otherwise use index 0
							int changeIndex = 1;
							for (int i = 1; i < weights.length; i ++){
								if (weights[i] > minHeight + 10){
									changeIndex = i; 
									break;
								}
							}
							weights[changeIndex] = weights[0] + weights[changeIndex] - minHeight;
							weights[0] = minHeight;
						}else{
							weights[index-1] = weights[index-1]+ weights[index] - minHeight;
							weights[index] = minHeight;
						}
					}
					
				}
				parent.setWeights(weights);
			}
		});
		return all;
		
	}
	
	public void setEditMode(boolean editMode){		
		editItem.setSelection(editMode);
		deleteItem.setEnabled(editMode);		
	}
	
	public void initPage(){
		if (topPart != null){
			for (Control c : topPart.getChildren()){
				c.dispose();
			}
		}
		
		headerLabel.setText(recordEditor.getRecord().getTitle() == null ? "" : recordEditor.getRecord().getTitle());
		toolkit.createLabel(topPart, "Title:");
		int style = SWT.BORDER;
		if (!recordEditor.getEditMode()){
			style = SWT.NONE;
		}
		Text txtShortName = toolkit.createText(topPart, recordEditor.getRecord().getTitle(), style);
		txtShortName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				recordEditor.getRecord().setTitle(txtShortName.getText());
				recordEditor.setDirty(true);
			}
		});
		txtShortName.setEditable(recordEditor.getEditMode());
		txtShortName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		txtShortName.setTextLimit(IntelRecord.MAX_TITLE_LENGTH);
		
		toolkit.createLabel(topPart, "Status:");
		if (recordEditor.getEditMode()){
			ComboViewer cmbStatus = new ComboViewer(topPart, SWT.DROP_DOWN | SWT.READ_ONLY);
			toolkit.adapt(cmbStatus.getControl(), true, true);
			cmbStatus.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
			cmbStatus.setContentProvider(ArrayContentProvider.getInstance());
			cmbStatus.setLabelProvider(new LabelProvider(){
				@Override
				public String getText(Object element){
					if (element instanceof IntelRecord.Status){
						return ((Enum<Status>) element).name();
					}
					return super.getText(element);
				}
			});
			cmbStatus.setInput(IntelRecord.Status.values());
			cmbStatus.setSelection(new StructuredSelection(recordEditor.getRecord().getStatus()));
			cmbStatus.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					recordEditor.getRecord().setStatus( (Status) ((IStructuredSelection)cmbStatus.getSelection()).getFirstElement());
					recordEditor.setDirty(true);
				}
			});
			
		}else{
			Label l = toolkit.createLabel(topPart, recordEditor.getRecord().getStatus().name());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		}
		
		
		toolkit.createLabel(topPart, "");
		toolkit.createLabel(topPart, "Date Created:");
		
		Label l = toolkit.createLabel(topPart, recordEditor.getRecord().getDateCreated() == null ? "" : DateFormat.getDateInstance().format(recordEditor.getRecord().getDateCreated()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(topPart, "Created By:");
		l = toolkit.createLabel(topPart, recordEditor.getRecord().getCreatedBy() == null ? "" : SmartLabelProvider.getFullLabel(recordEditor.getRecord().getCreatedBy()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(topPart, "");
		toolkit.createLabel(topPart, "Date Last Modified:");
		l = toolkit.createLabel(topPart, recordEditor.getRecord().getDateModified() == null ? "" : DateFormat.getDateInstance().format(recordEditor.getRecord().getDateModified()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(topPart, "Last Modified By:");
		l = toolkit.createLabel(topPart, recordEditor.getRecord().getLastModifiedBy() == null ? "" : SmartLabelProvider.getFullLabel(recordEditor.getRecord().getLastModifiedBy()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		l = toolkit.createLabel(topPart, "Narrative:");
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		Text txtDescription = toolkit.createText(topPart, recordEditor.getRecord().getDescription(), SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		if (recordEditor.getEditMode()){
			txtDescription.addModifyListener(new ModifyListener() {
				
				@Override
				public void modifyText(ModifyEvent e) {
					recordEditor.getRecord().setDescription(txtDescription.getText());
					recordEditor.setDirty(true);
				}
			});
			
			Menu menu = new Menu(txtDescription);
			txtDescription.setMenu(menu);
			
			MenuItem search = new MenuItem(menu, SWT.CASCADE);
			search.setText("Search Entity -> ");
			search.addSelectionListener(new SelectionAdapter() {			
				@Override
				public void widgetSelected(SelectionEvent e) {				
					EntitySearchShell shell = new EntitySearchShell(getSite().getShell(), txtDescription.getSelectionText(), recordEditor);
					shell.open(Display.getDefault().getCursorLocation());
				}
			});
			
			new MenuItem(menu, SWT.SEPARATOR);
			
			MenuItem cut = new MenuItem(menu, SWT.PUSH);
			cut.setText("Cut");
			cut.addSelectionListener(new SelectionAdapter() {			
				@Override
				public void widgetSelected(SelectionEvent e) {
					txtDescription.cut();
				}
			});
			MenuItem copy = new MenuItem(menu, SWT.PUSH);
			copy.setText("Copy");
			copy.addSelectionListener(new SelectionAdapter() {			
				@Override
				public void widgetSelected(SelectionEvent e) {
					txtDescription.copy();
				}
			});
			MenuItem paste = new MenuItem(menu, SWT.PUSH);
			paste.setText("Paste");
			paste.addSelectionListener(new SelectionAdapter() {			
				@Override
				public void widgetSelected(SelectionEvent e) {
					txtDescription.paste();
				}
			});
			
			MenuItem delete = new MenuItem(menu, SWT.PUSH);
			delete.setText("Delete");
			delete.addSelectionListener(new SelectionAdapter() {			
				@Override
				public void widgetSelected(SelectionEvent e) {
					Point sel = txtDescription.getSelection();
					int from = sel.x;
					int to = sel.y;
					txtDescription.setText(txtDescription.getText().substring(0, from) + txtDescription.getText().substring(to));
				}
			});
			
			new MenuItem(menu, SWT.SEPARATOR);

			MenuItem selectAll = new MenuItem(menu, SWT.PUSH);
			selectAll.setText("Select All");
			selectAll.addSelectionListener(new SelectionAdapter() {			
				@Override
				public void widgetSelected(SelectionEvent e) {
					txtDescription.selectAll();
				}
			});
			
			menu.addMenuListener(new MenuListener() {
				
				@Override
				public void menuShown(MenuEvent e) {
					delete.setEnabled(!txtDescription.getSelectionText().isEmpty());
					cut.setEnabled(!txtDescription.getSelectionText().isEmpty());
					copy.setEnabled(!txtDescription.getSelectionText().isEmpty());
				}
				
				@Override
				public void menuHidden(MenuEvent e) {
					
				}
			});
		}else{
			txtDescription.setEditable(false);
		}
	
		topPart.layout();
		
		entityPanel.init();
		locationPanel.init();
		
		attachmentPanel.refreshAttachmentTable();
		attachmentPanel.updateEditMode();
		entityPanel.updateEditMode();
		
		enableWs(WorkingSetManager.INSTANCE.isSet() && recordEditor.getRecord().getUuid() != null);
	}
	
	@Override
	public void setFocus() {

	}
	
	public AttachmentListComposite getAttachmentPanel(){
		return this.attachmentPanel;
	}
	
	public EntityListComposite getEntityPanel(){
		return this.entityPanel;
	}
	
	@Override
	public void dispose(){
		if (toolkit != null) toolkit.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isDirty() {
		return recordEditor.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}