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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.ui.RecordLabelProvider;
import org.wcs.smart.i2.ui.RecordSourceLabelProvider;
import org.wcs.smart.i2.ui.SmartSection;
import org.wcs.smart.i2.ui.dialogs.AttributeFieldEditor;
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
		topPart.setLayout(new GridLayout());
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
		
		Composite header = new Composite(topPart, SWT.BORDER);
		header.setLayout(new GridLayout(2, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = new Composite(header, SWT.BORDER);
		leftPart.setLayout(new GridLayout(2, false));
		leftPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(leftPart, "Title:");
		int style = SWT.BORDER;
		if (!recordEditor.getEditMode()){
			style = SWT.NONE;
		}
		Text txtShortName = toolkit.createText(leftPart, recordEditor.getRecord().getTitle(), style);
		txtShortName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				recordEditor.getRecord().setTitle(txtShortName.getText());
				recordEditor.setDirty(true);
			}
		});
		txtShortName.setEditable(recordEditor.getEditMode());
		txtShortName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtShortName.setTextLimit(IntelRecord.MAX_TITLE_LENGTH);
		
		toolkit.createLabel(leftPart, "Status:");
		if (recordEditor.getEditMode()){
			ComboViewer cmbStatus = new ComboViewer(leftPart, SWT.DROP_DOWN | SWT.READ_ONLY);
			toolkit.adapt(cmbStatus.getControl(), true, true);
			cmbStatus.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
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
			Label l = toolkit.createLabel(leftPart,  RecordLabelProvider.getRecordStatusLabel(recordEditor.getRecord().getStatus()));
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		
		toolkit.createLabel(leftPart, "Source:");
		if (recordEditor.getEditMode()){
			ComboViewer cmbSource = new ComboViewer(leftPart, SWT.DROP_DOWN | SWT.READ_ONLY);
			toolkit.adapt(cmbSource.getControl(), true, true);
			cmbSource.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbSource.setContentProvider(ArrayContentProvider.getInstance());
			cmbSource.setLabelProvider(new RecordSourceLabelProvider());
			
//			cmbSource.setInput(IntelRecord.Status.values());
			cmbSource.setSelection(new StructuredSelection(recordEditor.getRecord().getStatus()));
			cmbSource.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					IntelRecordSource recordSource = (IntelRecordSource) ((IStructuredSelection)cmbSource.getSelection()).getFirstElement();
					recordEditor.getRecord().setRecordSource( recordSource );
					recordEditor.setDirty(true);
					
					configureAttributePanel( recordSource );
				}
			});
			
			
			Job srcLoader = new Job("load record sources"){

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					List<IntelRecordSource> sources = new ArrayList<IntelRecordSource>();
					Session s = HibernateManager.openSession();
					try{
						sources.addAll(
								s.createCriteria(IntelRecordSource.class)
								.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
								.list());
						sources.forEach(src -> {
							src.getName();
							if (src.getAttributes() != null){
								src.getAttributes().forEach(a -> {
									a.getName();
									if (a.getAttribute() != null && a.getAttribute().getAttributeList() != null){
										a.getAttribute().getAttributeList().forEach(l -> l.getName());
									}
								});
							}
						});
						
						
					}finally{
						s.close();
					}
					Display.getDefault().syncExec(()->cmbSource.setInput(sources));
					return org.eclipse.core.runtime.Status.OK_STATUS;
				}
				
			};
			srcLoader.setSystem(false);
			srcLoader.schedule();
		}else{
			Label l = toolkit.createLabel(leftPart,  recordEditor.getRecord().getRecordSource().getName());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
		}
		
		Composite rightPart = new Composite(header, SWT.NONE);
		rightPart.setLayout(new GridLayout(2, false));
		rightPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Label l = toolkit.createLabel(rightPart, "Created:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		l = toolkit.createLabel(rightPart, recordEditor.getRecord().getDateCreated() == null ? "" : DateFormat.getDateInstance().format(recordEditor.getRecord().getDateCreated()));
		
		l = toolkit.createLabel(rightPart, "Modified:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		lblLastModified = toolkit.createLabel(rightPart, recordEditor.getRecord().getDateModified() == null ? "" : DateFormat.getDateInstance().format(recordEditor.getRecord().getDateModified()));
		
		l = toolkit.createLabel(rightPart, "Created By:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		l = toolkit.createLabel(rightPart, recordEditor.getRecord().getCreatedBy() == null ? "" : SmartLabelProvider.getFullLabel(recordEditor.getRecord().getCreatedBy()));
		
		l = toolkit.createLabel(rightPart, "Modified By:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		lblLastModifiedBy = toolkit.createLabel(rightPart, recordEditor.getRecord().getLastModifiedBy() == null ? "" : SmartLabelProvider.getFullLabel(recordEditor.getRecord().getLastModifiedBy()));
		
//		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Hyperlink lnkNarrative = toolkit.createHyperlink(header, "Narrative...", SWT.NONE);
		lnkNarrative.setToolTipText("opens narrative in new window");
		lnkNarrative.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				recordEditor.openExternalText(FieldType.NARRATIVE);
			}
		});
//		toolkit.createLabel(topPart, "");
		
//		lblLastModified.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
//		lblLastModifiedBy.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Hyperlink lnkScratchpad = toolkit.createHyperlink(header, "Scratchpad...", SWT.NONE);
		lnkScratchpad.setToolTipText("opens scratchpad in new window");
		lnkScratchpad.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				recordEditor.openExternalText(FieldType.SCRATCHPAD);
			}
		});
		
		srcAttributePanel = toolkit.createComposite(header);
		srcAttributePanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		configureAttributePanel( recordEditor.getRecord().getRecordSource() );
		
		topPart.layout();
		
		entityPanel.init();
		locationPanel.init();
		
		attachmentPanel.refreshAttachmentTable();
		attachmentPanel.updateEditMode();
		entityPanel.updateEditMode();
		
		enableWs(WorkingSetManager.INSTANCE.isSet() && recordEditor.getRecord().getUuid() != null);
	}
	
	private Composite srcAttributePanel;
	
	private void configureAttributePanel(IntelRecordSource source){
		if (srcAttributePanel.isDisposed()) return;
		for (Control kid : srcAttributePanel.getChildren()){
			kid.dispose();
		}
		
		if (source == null) return;
		
		srcAttributePanel.setLayout(new GridLayout(2, false));
		for (IntelRecordSourceAttribute a : source.getAttributes()){
			String name = a.getName();
			if (name == null || name.isEmpty()){
				if (a.getAttribute() != null){
					name = a.getAttribute().getName();
				}else if (a.getEntityType() != null){
					name = a.getEntityType().getName();
				}
			}
			
			
			if (recordEditor.getEditMode()){
				if (recordEditor.getRecord().getAttributes() == null){
					recordEditor.getRecord().setAttributes(new ArrayList<>());
				}
				if (a.getAttribute() != null){
					AttributeFieldEditor af = new AttributeFieldEditor(srcAttributePanel, a.getAttribute());
					for (IntelRecordAttributeValue v  : recordEditor.getRecord().getAttributes()){
						if (v.getAttribute().equals(a)){
							af.initControl(v);
							break;
						}
					}
					
					af.adapt(toolkit);
					af.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							boolean found = false;
							for (IntelRecordAttributeValue v  : recordEditor.getRecord().getAttributes()){
								if (v.getAttribute().equals(a)){
									af.updateValue(v);
									found = true;
									break;
									
								}
							}
							if (!found){
								IntelRecordAttributeValue newValue = new IntelRecordAttributeValue();
								newValue.setAttribute(a);
								newValue.setRecord(recordEditor.getRecord());
								af.updateValue(newValue);
								recordEditor.getRecord().getAttributes().add(newValue);
							}
							recordEditor.setDirty(true);
						}
					});
				}else{
					toolkit.createLabel(srcAttributePanel, name);
					toolkit.createLabel(srcAttributePanel, "ENTITY TODO:");
				}
			
			}else{
				String value = "NO VALUETOTOD";
				for (IntelRecordAttributeValue v  : recordEditor.getRecord().getAttributes()){
					if (v.getAttribute().equals(a)){
						value = v.getAttributeValueAsString();
						break;
					}
				}
				toolkit.createLabel(srcAttributePanel, name);
				toolkit.createLabel(srcAttributePanel,value);
			}
		}
		srcAttributePanel.layout(true, true);
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