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
import java.util.Arrays;
import java.util.List;

import org.eclipse.birt.core.framework.IConfigurationElement;
import org.eclipse.birt.report.designer.internal.ui.util.UIHelper;
import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Twistie;
import org.eclipse.ui.part.EditorPart;
import org.osgi.framework.Bundle;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RecordManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.ui.dialogs.NewEntityDialog;
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
	private ToolItem printItem;
	private ToolItem saveItem;
	
	private Composite topPart;
	private Label headerLabel;
	
	private EntityListComposite entityPanel;
	private AttachmentListComposite attachmentPanel;
	private LocationListComposite locationPanel;
	
	private RecordEditor recordEditor;
	
	private Label lblLastModified;
	private Label lblLastModifiedBy;
	
	private SashForm sashForm;
	private int currentMaximized = -1; //current maximized sash section
	
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
		lblLastModified.setText(DateFormat.getDateInstance().format(recordEditor.getRecord().getDateModified()));
		lblLastModifiedBy.setText(recordEditor.getRecord().getLastModifiedBy() == null ? "" : SmartLabelProvider.getFullLabel(recordEditor.getRecord().getLastModifiedBy()));
	}
	
	public void linkEntity(IntelEntity toLink){
		if (!recordEditor.getEditMode()) return;
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
				
		Menu formatsOpMenu = new Menu(getSite().getShell(), SWT.POP_UP);
		EmitterInfo pdfEmitter = null;
		for (EmitterInfo einfo : ReportEngineManager.getBirtReportEngine().getEmitterInfo()){
			if (einfo.getFormat().equalsIgnoreCase("PDF")){
				pdfEmitter = einfo;
			}
			MenuItem mi = new MenuItem(formatsOpMenu,SWT.PUSH);
			mi.setText(einfo.getFormat());
			if (einfo.getIcon() != null){
				IConfigurationElement confElem = einfo.getEmitter();
				if ( confElem != null ){
					String pluginId = confElem.getDeclaringExtension( ).getNamespace( );
					Bundle bundle = Platform.getBundle( pluginId );
					mi.setImage( UIHelper.getImage( bundle, einfo.getIcon(), false ));
					mi.addListener (SWT.Dispose, e-> {if (!mi.getImage().isDisposed()) mi.getImage().dispose();});
				}
			}
			
			mi.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					IntelReportManager.INSTANCE.exportRecord(recordEditor.getRecord(), einfo);
				}
			});
		}
		saveItem = new ToolItem(buttonBar, SWT.PUSH);
		saveItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVE_EDIT));
		saveItem.setToolTipText("save");
		saveItem.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent event){
				getSite().getPage().saveEditor(recordEditor, false);
			}
		});
		saveItem.setEnabled(recordEditor.isDirty());
		recordEditor.addPropertyListener((source, propId) -> {
			if (propId == IEditorPart.PROP_DIRTY){
				Display.getDefault().syncExec(()->saveItem.setEnabled(isDirty()));
			}
		});
		
		final EmitterInfo pdfFormat = pdfEmitter;
		printItem = new ToolItem(buttonBar, SWT.DROP_DOWN);
		printItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_PDF));
		printItem.setToolTipText("print to pdf");
		printItem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent event){
				 if (event.detail == SWT.ARROW) {
			          Rectangle rect = printItem.getBounds();
			          Point pt = new Point(rect.x, rect.y + rect.height);
			          pt = buttonBar.toDisplay(pt);
			          formatsOpMenu.setLocation(pt.x, pt.y);
			          formatsOpMenu.setVisible(true);
			    }else{
			    	if (pdfFormat != null){
			    		IntelReportManager.INSTANCE.exportRecord(recordEditor.getRecord(), pdfFormat);
			    	}else{	
			    		MessageDialog.openError(getSite().getShell(), "Error", "Could not find PDF exporter.");
			    	}
			    }
			}	
		});
		
		ToolItem refreshItem = new ToolItem(buttonBar, SWT.PUSH);
		refreshItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_REFRESH));
		refreshItem.setToolTipText("refresh record");
		refreshItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean doAction = true;
				if (recordEditor.isDirty()){
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
		if (IntelSecurityManager.INSTANCE.canDeleteRecord()){
			deleteItem.setEnabled(recordEditor.getEditMode());	
		}else{
			deleteItem.setEnabled(false);
		}
		
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
		

		
		sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Section top = createSectionHeader(sashForm, "Narrative");
		topPart = toolkit.createComposite(top, SWT.NONE);
		topPart.setLayout(new GridLayout(5, false));
		topPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Section expEntities = createSectionHeader(sashForm, "Entities");
		entityPanel = new EntityListComposite(expEntities, toolkit, recordEditor);
		entityPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)entityPanel.getLayoutData()).horizontalSpan = 0;
		((GridData)entityPanel.getLayoutData()).verticalSpan = 0;
				
		Section expAttachments = createSectionHeader(sashForm, "Attachments");
		attachmentPanel = new AttachmentListComposite(expAttachments, toolkit, recordEditor);
		attachmentPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Section expLocation = createSectionHeader(sashForm, "Locations");
		locationPanel = new LocationListComposite(expLocation, toolkit, recordEditor);
		locationPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true)); 
		
		
		sashForm.addListener(SWT.Resize, (e)->{
			if (currentMaximized >= 0){ 
				maximizePosition(currentMaximized);
			}else{
				//resize all so that the minimum size is respected
				resizeMinSize();
			}
		});
		Section[] sections = new Section[]{top, expEntities, expAttachments, expLocation};
		sashForm.setData(sections);
		for (Section c : sections){
			c.addListener(SWT.Resize, (e)->{
				currentMaximized = -1;
				if (c.getClientArea().height > c.getMinSize()) c.setMinimized(false);
			});
		}		
	}
	
	private void resizeMinSize(){
		Section[] sections = (Section[]) sashForm.getData();
		int[] newweights = new int[sections.length];
		Arrays.fill(newweights, -1);
		int cnt = 0;
		int off = 0;
		for (int i = 0; i < sections.length; i ++){
			
			if (sections[i].isMinimized){
				newweights[i] = sections[i].getMinSize();
				off += newweights[i];
			}else{
				cnt++;
			}
		}
		int totalHeight = sashForm.getClientArea().height - off;
		for (int i = 0; i < sections.length; i ++){
			if (newweights[i] == -1){
				newweights[i] = totalHeight / cnt;
			}
		}		
		for (Section s : sections) s.processingEvent = true;
		try{
			sashForm.setWeights(newweights);
		}finally{
			for (Section s : sections) s.processingEvent = false;
		}
	}
	
	
	private void maximizePosition(int position){
		Section[] sections = (Section[]) sashForm.getData();
		for (int i = 0; i < sections.length; i ++){
			sections[i].setMinimized(i != position);
		}
		resizeMinSize();
		currentMaximized = position;
	}

	private Section createSectionHeader(SashForm parent, String text){
		Section sec = new Section(parent, text);
		toolkit.adapt(sec);
		return sec;
	}
	
	public void setEditMode(boolean editMode){		
		if (editItem.isDisposed()) return;
		editItem.setSelection(editMode);
		if (IntelSecurityManager.INSTANCE.canDeleteRecord()){
			deleteItem.setEnabled(editMode);		
		}
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
		lblLastModified = toolkit.createLabel(topPart, recordEditor.getRecord().getDateModified() == null ? "" : DateFormat.getDateInstance().format(recordEditor.getRecord().getDateModified()));
		lblLastModified.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(topPart, "Last Modified By:");
		lblLastModifiedBy = toolkit.createLabel(topPart, recordEditor.getRecord().getLastModifiedBy() == null ? "" : SmartLabelProvider.getFullLabel(recordEditor.getRecord().getLastModifiedBy()));
		lblLastModifiedBy.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		l = toolkit.createLabel(topPart, "Narrative:");
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		l.addListener(SWT.MouseDoubleClick, (e)->maximizePosition(0));
		
		Text txtDescription = toolkit.createText(topPart, recordEditor.getRecord().getDescription(), SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
//		txtDescription.addListener(SWT.MouseDoubleClick, (e)->maximizePosition(0));
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
			
			if (IntelSecurityManager.INSTANCE.canCreateEntity()){
				MenuItem newEntity = new MenuItem(menu, SWT.CASCADE);
				newEntity.setText("New Entity ... ");
				newEntity.addSelectionListener(new SelectionAdapter() {			
					@Override
					public void widgetSelected(SelectionEvent e) {				
						NewEntityDialog dialog = new NewEntityDialog(getSite().getShell());
						ContextInjectionFactory.inject(dialog, recordEditor.getContext());
						if (dialog.open() == NewEntityDialog.OK){
							recordEditor.linkEntity(dialog.getNewEntity());
						}
					}
				});
			}
			
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

	private class Section extends Composite{

		private boolean isMinimized;
		private Composite header;
		private Twistie img;
		
		private boolean processingEvent = false;
		
		public Section(SashForm parent, String text) {
			super(parent, SWT.NONE);
			createContents(parent, text);
		}
		
		public void setMinimized(boolean min){
			if (processingEvent) return;
			isMinimized = min;
			if (isMinimized){
				img.setExpanded(false);
			}else{
				img.setExpanded(true);
			}
		}
		
		public int getMinSize(){
			int size = header.computeSize(SWT.DEFAULT, SWT.DEFAULT).y+2;
			return size;
		}
		
		public int getIndex(){
			Section[] items = (Section[]) sashForm.getData();
			for (int i = 0; i < items.length; i ++){;
				if (items[i] == this) return i;
			}
			return -1;
		}
		private void createContents(final SashForm sash, String text){
			setLayout(new GridLayout());
			((GridLayout)getLayout()).marginWidth = 0;
			((GridLayout)getLayout()).marginHeight = 0;
			
			header = toolkit.createComposite(this, SWT.NONE);
			header.setLayout(new GridLayout(3, false));
			header.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
			header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)header.getLayout()).marginWidth = 2;
			((GridLayout)header.getLayout()).marginHeight = 2;
			
			img = new Twistie(header, SWT.NONE){
				@Override
				protected void handleActivate(Event e) {
					
				}
			};
			img.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
			img.setExpanded(true);
//			img.getListeners(SWT.MouseDown)
			Label l =toolkit.createLabel(header, text);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			l.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
			FontData fd = l.getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD);
			final Font boldFont = new Font(getDisplay(), fd);
			l.addDisposeListener((e) -> {boldFont.dispose();});
			l.setFont(boldFont);

			Label exp = toolkit.createLabel(header, text);
			exp.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_SECTION_EXPAND));
			exp.addListener(SWT.MouseUp, (e)-> maximizePosition(getIndex()));
			exp.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
			
			MouseAdapter listener = new MouseAdapter() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					currentMaximized = -1;
					Section[] items = (Section[]) sash.getData();
					int index = -1;
					int sumWeights = 0;
					int weights[] = sash.getWeights();
					for (int i : weights) sumWeights += i;
					//re-portion to match current sash size then resize components
					int sumWeights2 = 0;
					
					for (int i = 0; i < weights.length; i ++){
						weights[i] = (int)( ( 1.0 * weights[i] / sumWeights) * sash.getClientArea().height);
						if (weights[i] < items[i].getMinSize()) weights[i] = items[i].getMinSize();
						sumWeights2 += weights[i];
					}
					
					for (int i = 0; i < items.length; i ++){
						if (items[i] == Section.this){
							index = i;
							break;
						}
					}
					
					int delta = 0;
					if (!isMinimized){
						setMinimized(true);
						delta = getClientArea().height - getMinSize();
					}else{
						//want to set the default height to some logical portion 
						//of entire page and take away from another expanded element
						setMinimized(false);
						
						int height = sumWeights2;
						for (Section s : items){
							height -= s.getMinSize();
						}
						delta = -(height / items.length);
					}
					
					
					if (index < 0){
						resizeMinSize();
						return;
					}
					//find non minimized section to subtract from
					if (isMinimized){
						weights[index] = getMinSize();
					}else{
						weights[index] = -delta + getMinSize();
					}
					int start = index + 1;
					if (start >= items.length) start = 0;
					while(start != index ){
						if (!items[start].isMinimized){
							int newweight = weights[start] + delta;
							if (newweight < items[start].getMinSize()){
								delta = delta + (newweight - items[start].getMinSize());
								newweight = items[start].getMinSize();
								weights[start] = newweight;
							}else{
								weights[start] = newweight;
								break;
							}
							
						}
						
						start ++;
						if (start >= items.length) start = 0;
					}
					processingEvent = true;
					try{
						sash.setWeights(weights);	
					}finally{
						processingEvent = false;
					}
					
				}
			};
			l.addMouseListener(listener);
			img.addMouseListener(listener);
			
			l.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));
			img.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));
			exp.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));
			
		}
		
	}
}