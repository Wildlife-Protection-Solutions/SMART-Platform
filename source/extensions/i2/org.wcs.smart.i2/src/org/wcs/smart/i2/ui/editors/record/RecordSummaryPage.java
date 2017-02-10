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
import java.util.Locale;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
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
import org.eclipse.swt.graphics.Image;
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
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordAttributeValueList;
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
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Summary Page for record editor.
 * 
 * @author Emily
 *
 */
public class RecordSummaryPage extends EditorPart{

	private FormToolkit  toolkit;

	private Composite summaryPart;
	private Composite historyPart;
	
	private Label headerLabel;
	
	private EntityListComposite entityPanel;
	private AttachmentListComposite attachmentPanel;
//	private LocationListComposite locationPanel;
	
	private RecordEditor recordEditor;
	
	private SmartSection detailSection;
	
	private Label lblLastModified;
	private Label lblLastModifiedBy;
	
	private SashForm sashForm;
	private RecordButtonToolbar buttonToolBar;

	//attribute panel
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
	
	public RecordSummaryPage(RecordEditor parent){
		this.recordEditor =  parent;
	}

	public void clearLists(){
		attachmentPanel.getAttachmentsToDelete().clear();
		attachmentPanel.getNewEntityAttachments().clear();
		entityPanel.getEntityLinksAdded().clear();
		entityPanel.getEntityLinksToDelete().clear();
	}


//	public LocationListComposite getLocationPanel(){
//		return this.locationPanel;
//	}
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
		
//		SmartSection expLocation = createSectionHeader(sashForm, toolkit, "Locations");
//		locationPanel = new LocationListComposite(expLocation, toolkit, recordEditor);
//		locationPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true)); 
//		
		sashForm.setWeights(new int[]{3,1,1});
		sashForm.addListener(SWT.Resize, (e)->{
			List<SmartSection> sections = (List<SmartSection>) sashForm.getData(SmartSection.KIDS_KEY);
			int max = (Integer)sashForm.getData(SmartSection.MAX_KEY);
			if ( max >= 0){
				sections.get(max).maximize();
			}else{
				sections.get(0).resizeMinSize();
				//resize all so that the minimum size is respected
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
			TableComboViewer cmbStatus = new TableComboViewer(leftPart, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
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
				
				@Override
				public Image getImage(Object element){
					if (element instanceof IntelRecord.Status){
						return RecordLabelProvider.getRecordStatusImage((Status)element);
					}
					return super.getImage(element);
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
			Composite temp = toolkit.createComposite(leftPart);
			temp.setLayout(new GridLayout(2, false));
			temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)temp.getLayout()).marginWidth = 0;
			((GridLayout)temp.getLayout()).marginHeight = 0;
			Label l = toolkit.createLabel(temp, "");
			l.setImage(RecordLabelProvider.getRecordStatusImage(recordEditor.getRecord().getStatus()));
			
			l = toolkit.createLabel(temp,  RecordLabelProvider.getRecordStatusLabel(recordEditor.getRecord().getStatus()));
			l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
		}
		
		toolkit.createLabel(leftPart, "Source:");
		if (recordEditor.getEditMode()){
			TableComboViewer cmbSource = new TableComboViewer(leftPart, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
			toolkit.adapt(cmbSource.getControl(), true, true);
			cmbSource.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbSource.setContentProvider(ArrayContentProvider.getInstance());
			cmbSource.setLabelProvider(new RecordSourceLabelProvider());
			cmbSource.setInput(new String[]{DialogConstants.LOADING_TEXT});
			loadRecordSources(cmbSource);
		}else{
			Composite temp = toolkit.createComposite(leftPart);
			temp.setLayout(new GridLayout(2, false));
			temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)temp.getLayout()).marginWidth = 0;
			((GridLayout)temp.getLayout()).marginHeight = 0;
			
			Label l = toolkit.createLabel(temp, "");
			Image img = null;
			if (recordEditor.getRecord().getRecordSource() != null && recordEditor.getRecord().getRecordSource().getIcon() != null){
				try{
					img = AWTSWTImageUtils.convertToSWTImage(recordEditor.getRecord().getRecordSource().getIconAsImage());
				}catch (Exception ex){
					
				}
			}
			final Image toDispose= img;
			l.setImage(toDispose);
			l.addDisposeListener(e->{if (toDispose != null) toDispose.dispose();});
			l = toolkit.createLabel(temp,  recordEditor.getRecord().getRecordSource().getName());
			l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
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
//		locationPanel.init();
		
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

		

		
		sashForm.setWeights(new int[]{3,1,1});
	}
	
	/*
	 * configures controls for record attributes
	 */
	private void configureAttributePanel(IntelRecordSource source){
		//get CRS
		CoordinateReferenceSystem  crs = GeometryUtils.SMART_CRS;
		Projection currentProjection = HibernateManager.getCurrentViewProjection();
		if (currentProjection != null ){
			try{
				CoordinateReferenceSystem parsed = ReprojectUtils.stringToCrs(currentProjection.getDefinition());
				if (!CRS.equalsIgnoreMetadata(crs, parsed)){
					crs = parsed;
				}
			}catch (Exception ex){
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}
		}
		
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
		
		HashMap<IntelRecordAttributeValue, Label> readOnlyLabels = new HashMap<>();
		
		for (IntelRecordSourceAttribute a : source.getAttributes()){
			String name = getName(a);
			IntelRecordAttributeValue v = findAttributeValue(a);
			
			if (recordEditor.getEditMode()){
				if (recordEditor.getRecord().getAttributes() == null){
					recordEditor.getRecord().setAttributes(new ArrayList<>());
				}
				if (a.getAttribute() != null){
					AttributeFieldEditor af = new AttributeFieldEditor(content, a.getAttribute(), a.getIsMultiple(), name);
					editorFields.put(a, af);					
					if (v != null) af.initControl(v);
					af.addSelectionListener(dirtyListener);
					af.adapt(toolkit);
					
					if (a.getAttribute().getType() == IntelAttribute.AttributeType.POSITION){
						//modify position attributes we need to update map
						af.addSelectionListener(new SelectionAdapter() {	
							@Override
							public void widgetSelected(SelectionEvent event) {
								IntelRecordAttributeValue tmp = new IntelRecordAttributeValue();
								tmp.setAttribute(a);
								af.updateValue(tmp);
								recordEditor.getMapPage().updateLocationAttribute(tmp);
							}
						});
					}
				}else{
					Label l = toolkit.createLabel(content, name + ":");
					l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

					Composite tmp  = toolkit.createComposite(content, SWT.NONE);
					tmp.setLayout(new GridLayout(2, false));
					((GridLayout)tmp.getLayout()).marginWidth = 0;
					((GridLayout)tmp.getLayout()).marginHeight = 0;
					tmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					
					EntityCheckboxDropDownViewer editor = new EntityCheckboxDropDownViewer(tmp, a.getEntityType(), a.getIsMultiple());
					editorFields.put(a, editor);
					if (v != null) editor.initControl(v.getAttributeListItems());
					editor.addSelectionChangedListener(dirtyListener2);
					editor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					toolkit.adapt(editor);
					
					Hyperlink link = toolkit.createHyperlink(tmp, "link...", SWT.NONE);
					link.setToolTipText("Link entities to record");
					link.addHyperlinkListener(new HyperlinkAdapter() {
						@Override
						public void linkActivated(HyperlinkEvent e) {
							for (Object x : editor.getCheckObjects()){
								if (x instanceof EntityCheckboxDropDownViewer.EntityItem){
									IntelEntity tmp = new IntelEntity();
									tmp.setUuid(((EntityCheckboxDropDownViewer.EntityItem) x).uuid);
									recordEditor.linkEntity(tmp);
								}
							}
							
						}
					});
				}
			}else{
				String value = "";
				if (v != null){
					value = v.getAttributeValueAsString(Locale.getDefault(), crs);
				}
				
				Label l = toolkit.createLabel(content, name + ":");
				l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				
				l = toolkit.createLabel(content,value);
				l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				if (v != null){
					if (a.isListAttribute()){
						readOnlyLabels.put(v, l);
					}
				}
			}
		}
		if (!recordEditor.getEditMode()){
			loadListAttribute(readOnlyLabels);
		}
		sc.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		srcAttributePanel.layout(true, true);
	}
	
	/*
	 * loads list attribute names for labels when editor is in read only mode;
	 * when not in read only mode we do not need to do this
	 */
	private void loadListAttribute(final HashMap<IntelRecordAttributeValue, Label> attributes){
		Job j = new Job("loading attribute values"){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final HashMap<Label, String> labels = new HashMap<>();
				
				Session session =  HibernateManager.openSession();
				try{
					for (Entry<IntelRecordAttributeValue,Label> v : attributes.entrySet()){
						if (v.getKey().getAttributeListItems().isEmpty()){
							labels.put(v.getValue(), "");
						}else{
							StringBuilder sb = new StringBuilder();
							sb.append("(" + v.getKey().getAttributeListItems().size() + ") ");
							if (v.getKey().getAttribute().getAttribute() != null){
								//attributes
								for (IntelRecordAttributeValueList l : v.getKey().getAttributeListItems()){
									String lbl = ((IntelAttributeListItem)session.get(IntelAttributeListItem.class, l.getId().getElementUuid())).getName();
									sb.append(lbl);
									sb.append(", ");
								}
							}else if(v.getKey().getAttribute().getEntityType() != null){
								//entity types
								for (IntelRecordAttributeValueList l : v.getKey().getAttributeListItems()){
									String lbl = ((IntelEntity)session.get(IntelEntity.class, l.getId().getElementUuid())).getIdAttributeAsText();
									sb.append(lbl);
									sb.append(", ");
								}
							}
							labels.put(v.getValue(), sb.substring(0, sb.length()-2));
						}
					}
				}finally{
					session.close();
				}
				Display.getDefault().syncExec(()->{
					for(Entry<Label,String> v : labels.entrySet()){
						if (v.getKey().isDisposed()) return;
						v.getKey().setText(v.getValue());
					}
				});
				return org.eclipse.core.runtime.Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
	}
	
	/*
	 * finds the name for a record attribute
	 */
	private String getName(IntelRecordSourceAttribute a){
		String name = a.getName();
		if (name == null || name.isEmpty()){
			if (a.getAttribute() != null){
				name = a.getAttribute().getName();
			}else if (a.getEntityType() != null){
				name = a.getEntityType().getName();
			}
		}
		return name;
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

	/*
	 * finds the record attribute value for a given attribute; null if not found
	 */
	private IntelRecordAttributeValue findAttributeValue(IntelRecordSourceAttribute a ){
		if (recordEditor.getRecord().getAttachments() == null) return null;
		for (IntelRecordAttributeValue v  : recordEditor.getRecord().getAttributes()){
			if (v.getAttribute().equals(a)){
				return v;
			}
		}
		return null;
	}
	
	/*
	 * updates record attribute values
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		
	}
	
	/**
	 * updates the record attribute values with values from ui fields 
	 * @param monitor
	 * @return
	 */
	public boolean saveAttributes(IProgressMonitor monitor) {
		boolean showError = false;
		for (Entry<IntelRecordSourceAttribute, Object> editor: editorFields.entrySet()){
			if (editor.getValue() instanceof AttributeFieldEditor){
				if (!((AttributeFieldEditor)editor.getValue()).isValid()){
					showError = true;
					break;
				}
			}
		}
		if (showError){
			MessageDialog.openError(getSite().getShell(), "Error", "Record contains invalid attribute values.  You must fix these before you can save the editor");
			return false;
		}
			
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
		return true;
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

	/*
	 * Loads all possible record sources from db and populates 
	 * provided combo
	 * @param cmbSource
	 */
	private void loadRecordSources(final Viewer cmbSource){
		Job srcLoader = new Job("load record sources"){

			@SuppressWarnings("unchecked")
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
				allInput.add(0, "");
				Display.getDefault().syncExec(()->{
					if (cmbSource.getControl().isDisposed()) return;
					
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
		
	}
}