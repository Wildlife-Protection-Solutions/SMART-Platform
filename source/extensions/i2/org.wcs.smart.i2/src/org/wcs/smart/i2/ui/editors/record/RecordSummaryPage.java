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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.ui.RecordLabelProvider;
import org.wcs.smart.i2.ui.SmartSection;
import org.wcs.smart.i2.ui.views.RecordNarrativeView.FieldType;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Summary Page for record editor.
 * 
 * @author Emily
 *
 */
public class RecordSummaryPage extends EditorPart{

	private FormToolkit  toolkit;

	private Composite topPart;
	private Label headerLabel;
	
	private EntityListComposite entityPanel;
	private AttachmentListComposite attachmentPanel;
	private LocationListComposite locationPanel;
	
	private RecordEditor recordEditor;
	
	private SmartSection detailSection;
	
	private Label lblLastModified;
	private Label lblLastModifiedBy;
	
	private SashForm sashForm;
	private RecordButtonToolbar buttonToolBar; 
	
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
		buttonToolBar.enableWs(enable);
	}
	
	@SuppressWarnings("unchecked")
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
		((GridLayout)buttonPanel.getLayout()).marginWidth = 5;
		((GridLayout)buttonPanel.getLayout()).marginHeight = 5;
		((GridLayout)buttonPanel.getLayout()).horizontalSpacing = 0;
		
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
		
		buttonToolBar = new RecordButtonToolbar(buttonPanel, recordEditor, toolkit);
		
		sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		detailSection = createSectionHeader(sashForm, toolkit, "Details");
		topPart = toolkit.createComposite(detailSection, SWT.NONE);
		topPart.setLayout(new GridLayout(6, false));
		topPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SmartSection expEntities = new SmartSection(sashForm, toolkit, "Entities"){
			@Override
			public void populateHeaderAdditions(Composite parent){
			
				Composite bits = toolkit.createComposite(parent);
				bits.setLayout(new GridLayout(2, false));
				((GridLayout)bits.getLayout()).marginWidth = 0;
				((GridLayout)bits.getLayout()).marginHeight = 0;
				bits.setBackground(parent.getBackground());
				bits.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
				
				FontData fd = bits.getFont().getFontData()[0];
				fd.setHeight(fd.getHeight() - 2);
				Font smallerfont = new Font(bits.getDisplay(), fd);
				
				fd.setStyle(SWT.BOLD);
				Font boldfont = new Font(bits.getDisplay(), fd);
				
				bits.addListener(SWT.Dispose, e->{smallerfont.dispose();boldfont.dispose();});
				Hyperlink list = toolkit.createHyperlink(bits, "list", SWT.NONE);
				Hyperlink details = toolkit.createHyperlink(bits, "details", SWT.NONE);
				list.setBackground(bits.getBackground());
				details.setBackground(bits.getBackground());
				list.setFont(boldfont);
				details.setFont(smallerfont);
				
				list.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						list.setFont(boldfont);
						details.setFont(smallerfont);
						entityPanel.setType(EntityListComposite.Type.LIST);
						bits.layout(true);
					}
				});
				details.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						list.setFont(smallerfont);
						details.setFont(boldfont);
						entityPanel.setType(EntityListComposite.Type.DETAILS);
						bits.layout(true);
					}
				});
				
			}
		};
		toolkit.adapt(expEntities);
		
		
		entityPanel = new EntityListComposite(expEntities, toolkit, recordEditor);
		entityPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)entityPanel.getLayoutData()).horizontalSpan = 0;
		((GridData)entityPanel.getLayoutData()).verticalSpan = 0;
				
		SmartSection expAttachments = createSectionHeader(sashForm, toolkit, "Attachments");
		attachmentPanel = new AttachmentListComposite(expAttachments, toolkit, recordEditor);
		attachmentPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SmartSection expLocation = createSectionHeader(sashForm, toolkit, "Locations");
		locationPanel = new LocationListComposite(expLocation, toolkit, recordEditor);
		locationPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true)); 
		
		
		sashForm.addListener(SWT.Resize, (e)->{
			List<SmartSection> sections = (List<SmartSection>) sashForm.getData(SmartSection.KIDS_KEY);
			int max = (Integer)sashForm.getData(SmartSection.MAX_KEY);
			if ( max >= 0){
				sections.get(max).maximize();
				
			}else{
				//resize all so that the minimum size is respected
				sections.forEach(s->s.resizeMinSize());
			}
		});
		
	}
	
	

	private SmartSection createSectionHeader(SashForm parent, FormToolkit toolkit, String text){
		SmartSection sec = new SmartSection(parent, toolkit, text);
		toolkit.adapt(sec);
		return sec;
	}
	
	public void setEditMode(boolean editMode){		
		buttonToolBar.setEditMode(editMode);
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
		txtShortName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
		txtShortName.setTextLimit(IntelRecord.MAX_TITLE_LENGTH);
		
		toolkit.createLabel(topPart, "Status:");
		if (recordEditor.getEditMode()){
			ComboViewer cmbStatus = new ComboViewer(topPart, SWT.DROP_DOWN | SWT.READ_ONLY);
			toolkit.adapt(cmbStatus.getControl(), true, true);
			cmbStatus.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
			cmbStatus.setContentProvider(ArrayContentProvider.getInstance());
			cmbStatus.setLabelProvider(new LabelProvider(){
				@Override
				public String getText(Object element){
					if (element instanceof IntelRecord.Status){
						return RecordLabelProvider.getRecordStatusLabel((Status) element);
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
			Label l = toolkit.createLabel(topPart,  RecordLabelProvider.getRecordStatusLabel(recordEditor.getRecord().getStatus()));
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
		}
		
		
		toolkit.createLabel(topPart, "");
		toolkit.createLabel(topPart, "Date Created:");
		Label l = toolkit.createLabel(topPart, recordEditor.getRecord().getDateCreated() == null ? "" : DateFormat.getDateInstance().format(recordEditor.getRecord().getDateCreated()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolkit.createLabel(topPart, "Created By:");
		l = toolkit.createLabel(topPart, recordEditor.getRecord().getCreatedBy() == null ? "" : SmartLabelProvider.getFullLabel(recordEditor.getRecord().getCreatedBy()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Hyperlink lnkNarrative = toolkit.createHyperlink(topPart, "Narrative...", SWT.NONE);
		lnkNarrative.setToolTipText("opens narrative in new window");
		lnkNarrative.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				recordEditor.openExternalText(FieldType.NARRATIVE);
			}
		});
		toolkit.createLabel(topPart, "");
		toolkit.createLabel(topPart, "Date Last Modified:");
		lblLastModified = toolkit.createLabel(topPart, recordEditor.getRecord().getDateModified() == null ? "" : DateFormat.getDateInstance().format(recordEditor.getRecord().getDateModified()));
		lblLastModified.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolkit.createLabel(topPart, "Last Modified By:");
		lblLastModifiedBy = toolkit.createLabel(topPart, recordEditor.getRecord().getLastModifiedBy() == null ? "" : SmartLabelProvider.getFullLabel(recordEditor.getRecord().getLastModifiedBy()));
		lblLastModifiedBy.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Hyperlink lnkScratchpad = toolkit.createHyperlink(topPart, "Scratchpad...", SWT.NONE);
		lnkScratchpad.setToolTipText("opens scratchpad in new window");
		lnkScratchpad.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				recordEditor.openExternalText(FieldType.SCRATCHPAD);
			}
		});
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