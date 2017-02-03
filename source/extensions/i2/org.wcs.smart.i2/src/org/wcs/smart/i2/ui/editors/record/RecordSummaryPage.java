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
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

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
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
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
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.themes.ColorUtil;
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
import org.wcs.smart.i2.ui.SectionTabHeader;
import org.wcs.smart.i2.ui.SmartSection;
import org.wcs.smart.i2.ui.dialogs.AttributeFieldEditor;
import org.wcs.smart.i2.ui.views.RecordNarrativeView.FieldType;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Summary Page for record editor.
 * 
 * @author Emily
 *
 */
public class RecordSummaryPage extends EditorPart{

	private FormToolkit  toolkit;

	private Composite summaryPart;
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

	private Composite historyPart; 
	
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
		
		Color c = toolkit.getColors().getColor(IFormColors.TB_BG);
		Color e2 = new Color(getSite().getShell().getDisplay(), ColorUtil.blend(c.getRGB(), new RGB(255,255,255),50));
		SectionTabHeader tabList = new SectionTabHeader(new String[]{"Summary", "History"}, detailSection, toolkit, e2);
		tabList.addDisposeListener(e->e2.dispose());
		tabList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		Composite tabPart = toolkit.createComposite(detailSection, SWT.NONE);
		tabPart.setLayout(new StackLayout());
		tabPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		summaryPart = toolkit.createComposite(tabPart, SWT.NONE);
		summaryPart.setLayout(new GridLayout());
		summaryPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		historyPart = toolkit.createComposite(tabPart, SWT.NONE);
		historyPart.setLayout(new GridLayout());
		historyPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tabList.setContent(new Composite[]{summaryPart, historyPart}, tabPart);
		tabList.selectTab(0);
		
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
		if (summaryPart != null){
			for (Control c : summaryPart.getChildren()){
				c.dispose();
			}
		}
		
		if (historyPart != null){
			for (Control c : historyPart.getChildren()){
				c.dispose();
			}
		}
		
		headerLabel.setText(recordEditor.getRecord().getTitle() == null ? "" : recordEditor.getRecord().getTitle());
		
		Composite header = new Composite(summaryPart, SWT.NONE);
		header.setLayout(new GridLayout(2, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = new Composite(header, SWT.NONE);
		leftPart.setLayout(new GridLayout(2, false));
		leftPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
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
			TableComboViewer cmbSource = new TableComboViewer(leftPart, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
			toolkit.adapt(cmbSource.getControl(), true, true);
			cmbSource.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbSource.setContentProvider(ArrayContentProvider.getInstance());
			cmbSource.setLabelProvider(new RecordSourceLabelProvider());
			cmbSource.setInput(new String[]{DialogConstants.LOADING_TEXT});
			
			Job srcLoader = new Job("load record sources"){

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					List<Object> allInput = new ArrayList<Object>();
					Session s = HibernateManager.openSession();
					try{
						List<IntelRecordSource> sources = s.createCriteria(IntelRecordSource.class)
								.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
								.list();
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
						allInput.addAll(sources);
						
						
					}finally{
						s.close();
					}
					Display.getDefault().syncExec(()->{
						if (cmbSource.getControl().isDisposed()) return;
						allInput.add(0, "");
						cmbSource.setInput(allInput);
						if (recordEditor.getRecord().getRecordSource() != null) cmbSource.setSelection(new StructuredSelection(recordEditor.getRecord().getRecordSource()));
						cmbSource.addSelectionChangedListener(new ISelectionChangedListener() {
							@Override
							public void selectionChanged(SelectionChangedEvent event) {
								Object x =  ((IStructuredSelection)cmbSource.getSelection()).getFirstElement();
								if (x == null || !(x instanceof IntelRecordSource)){
									recordEditor.getRecord().setRecordSource( null);	
								}else{
									recordEditor.getRecord().setRecordSource( (IntelRecordSource)x );
								}
								recordEditor.setDirty(true);
								
								configureAttributePanel( recordEditor.getRecord().getRecordSource() );
							}
						});
					});
					return org.eclipse.core.runtime.Status.OK_STATUS;
				}
				
			};
			srcLoader.setSystem(false);
			srcLoader.schedule();
		}else{
			Label l = toolkit.createLabel(leftPart,  recordEditor.getRecord().getRecordSource().getName());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
			
		Composite rightPart = new Composite(header, SWT.NONE);
		rightPart.setLayout(new GridLayout(1, false));
		rightPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Hyperlink lnkNarrative = toolkit.createHyperlink(rightPart, "Narrative...", SWT.NONE);
		lnkNarrative.setToolTipText("opens narrative in new window");
		lnkNarrative.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lnkNarrative.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				recordEditor.openExternalText(FieldType.NARRATIVE);
			}
		});

		Hyperlink lnkScratchpad = toolkit.createHyperlink(rightPart, "Scratchpad...", SWT.NONE);
		lnkScratchpad.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lnkScratchpad.setToolTipText("opens scratchpad in new window");
		lnkScratchpad.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				recordEditor.openExternalText(FieldType.SCRATCHPAD);
			}
		});
		
		srcAttributePanel = toolkit.createComposite(leftPart);
		srcAttributePanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		srcAttributePanel.setLayout(new GridLayout());
		((GridLayout)srcAttributePanel.getLayout()).marginWidth = 0;
		((GridLayout)srcAttributePanel.getLayout()).marginHeight = 0;
		configureAttributePanel( recordEditor.getRecord().getRecordSource() );
		
		summaryPart.layout();
		
		entityPanel.init();
		locationPanel.init();
		
		attachmentPanel.refreshAttachmentTable();
		attachmentPanel.updateEditMode();
		entityPanel.updateEditMode();
		
		enableWs(WorkingSetManager.INSTANCE.isSet() && recordEditor.getRecord().getUuid() != null);
		
		// history part
		Composite infoComp = toolkit.createComposite(historyPart);
		infoComp.setLayout(new GridLayout(2, false));
		infoComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Label l = toolkit.createLabel(infoComp, "Created: ");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		l = toolkit.createLabel(infoComp, recordEditor.getRecord().getDateCreated() == null ? "" : DateFormat.getDateInstance().format(recordEditor.getRecord().getDateCreated()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		l = toolkit.createLabel(infoComp, "Modified: ");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		lblLastModified = toolkit.createLabel(infoComp, recordEditor.getRecord().getDateModified() == null ? "" : DateFormat.getDateInstance().format(recordEditor.getRecord().getDateModified()));
		lblLastModified.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		l = toolkit.createLabel(infoComp, "Created By: ");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		l = toolkit.createLabel(infoComp, recordEditor.getRecord().getCreatedBy() == null ? "" :  SmartLabelProvider.getFullLabel(recordEditor.getRecord().getCreatedBy()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		l = toolkit.createLabel(infoComp, "Last Modified By: ");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		lblLastModifiedBy = toolkit.createLabel(infoComp, recordEditor.getRecord().getLastModifiedBy() == null ? "" :  SmartLabelProvider.getFullLabel(recordEditor.getRecord().getLastModifiedBy()));
		lblLastModifiedBy.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
	}
	
	private Composite srcAttributePanel;
	private HashMap<IntelRecordSourceAttribute, Object> editorFields;
	
	private SelectionAdapter dirtyListener = new SelectionAdapter(){
		@Override
		public void widgetSelected(SelectionEvent e) {
			recordEditor.setDirty(true);
		}
	};
	
	private ISelectionChangedListener dirtyListener2 = new ISelectionChangedListener() {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			recordEditor.setDirty(true);
		}
	};
	private void configureAttributePanel(IntelRecordSource source){
		if (srcAttributePanel.isDisposed()) return;
		for (Control kid : srcAttributePanel.getChildren()){
			kid.dispose();
		}
		editorFields = new HashMap<IntelRecordSourceAttribute, Object>();
		
		if (source == null) return;
		if (source.getAttributes().size() > 0){
			Label l = toolkit.createLabel(srcAttributePanel, "", SWT.SEPARATOR | SWT.HORIZONTAL);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		ScrolledComposite sc = new ScrolledComposite(srcAttributePanel, SWT.V_SCROLL);
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		Composite content = toolkit.createComposite(sc);
		sc.setContent(content);
		content.setLayout(new GridLayout(2, false));
		((GridLayout)content.getLayout()).marginWidth = 5;
		((GridLayout)content.getLayout()).marginHeight = 0;
		
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
					AttributeFieldEditor af = new AttributeFieldEditor(content, a.getAttribute(), a.getIsMultiple(), name);
					editorFields.put(a, af);
					IntelRecordAttributeValue v = findAttributeValue(a);
					if (v != null) af.initControl(v);
					af.addSelectionListener(dirtyListener);
					af.adapt(toolkit);
				}else{
					Label l = toolkit.createLabel(content, name + ":");
					l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

					EntityCheckboxDropDownViewer editor = new EntityCheckboxDropDownViewer(content, a.getEntityType());
					
					editorFields.put(a, editor);
					IntelRecordAttributeValue v = findAttributeValue(a);
					if (v != null) editor.initControl(v.getAttributeListItems());
					editor.addSelectionChangedListener(dirtyListener2);
					toolkit.adapt(editor);
					
				}
			
			}else{
				String value = "NO VALUETOTOD";
				for (IntelRecordAttributeValue v  : recordEditor.getRecord().getAttributes()){
					if (v.getAttribute().equals(a)){
						value = v.getAttributeValueAsString();
						break;
					}
				}
				Label l = toolkit.createLabel(content, name + ":");
				l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				
				toolkit.createLabel(content,value);
			}
		}
		sc.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
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

	private IntelRecordAttributeValue findAttributeValue(IntelRecordSourceAttribute a ){
		for (IntelRecordAttributeValue v  : recordEditor.getRecord().getAttributes()){
			if (v.getAttribute().equals(a)){
				return v;
			}
		}
		return null;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		IntelRecord r = recordEditor.getRecord();
		if (r.getRecordSource() == null){
			r.getAttributes().clear();
		}else{
			//remove any attributes that are not associated with the source
			List<IntelRecordAttributeValue> toRemove = new ArrayList<IntelRecordAttributeValue>();
			for (IntelRecordAttributeValue v : r.getAttributes()){
				if (!r.getRecordSource().getAttributes().contains(v.getAttribute())){
					toRemove.add(v);
				}
			}
			r.getAttributes().removeAll(toRemove);
		}
		
		for (Entry<IntelRecordSourceAttribute, Object> editor: editorFields.entrySet()){
			boolean isNew = false;
			boolean add = false;
			IntelRecordAttributeValue toUpdate = findAttributeValue(editor.getKey());
			if (toUpdate == null){
				toUpdate = new IntelRecordAttributeValue();
				toUpdate.setRecord(r);
				toUpdate.setAttribute(editor.getKey());
				isNew = true;
			}
			
			if (editor.getValue() instanceof AttributeFieldEditor){
				add = ((AttributeFieldEditor)editor.getValue()).updateValue(toUpdate);  
			}else if (editor.getValue() instanceof EntityCheckboxDropDownViewer){
				add = ((EntityCheckboxDropDownViewer)editor.getValue()).updateValue(toUpdate);
			}
			if (add && isNew){
				r.getAttributes().add(toUpdate);
			}else if (!add){
				r.getAttributes().remove(toUpdate);
			}				
		}
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