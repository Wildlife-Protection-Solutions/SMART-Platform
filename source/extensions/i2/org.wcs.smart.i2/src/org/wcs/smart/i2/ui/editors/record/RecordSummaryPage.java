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

import java.text.Collator;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.internal.SharedImages;
import org.eclipse.ui.part.EditorPart;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.control.MultiLineText;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RecordManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordAttributeValueList;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.search.AttributeRecordSearch;
import org.wcs.smart.i2.search.BasicRecordSearch;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.RecordLabelProvider;
import org.wcs.smart.i2.ui.RecordSourceLabelProvider;
import org.wcs.smart.i2.ui.Resources;
import org.wcs.smart.i2.ui.SectionTabHeader;
import org.wcs.smart.i2.ui.SmartSection;
import org.wcs.smart.i2.ui.dialogs.AttributeFieldEditor;
import org.wcs.smart.i2.ui.views.RecordNarrativeView.FieldType;
import org.wcs.smart.i2.ui.views.RecordsView;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.E3Utils;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;


/**
 * Summary Page for record editor.
 * 
 * @author Emily
 *
 */
public class RecordSummaryPage extends EditorPart{

	private static int[] LAST_WEIGHTS = null;
	
	private FormToolkit  toolkit;

	private Composite summaryPart;
	private Composite historyPart;
	private ScrolledComposite summaryScroll;
	
	private Label headerLabel;
	
	private EntityListComposite entityPanel;
	private AttachmentListComposite attachmentPanel;
	private RecordLocationSummaryComposite observationPanel;
	
	private RecordEditor recordEditor;
	
	private SmartSection detailSection;
	
	private Label lblShortName;
	private Text txtShortName;
	private ControlDecoration decShortName;
	private Label lblLastModified;
	private Label lblLastModifiedBy;
	
	private SashForm sashForm;
	private RecordButtonToolbar buttonToolBar;

	//attribute panel
	private Composite srcAttributePanel;
	private Composite dataSourceComposite;
	
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
		attachmentPanel.getRemovedEntityAttachments().clear();
		entityPanel.getEntityLinksAdded().clear();
		entityPanel.getEntityLinksToDelete().clear();
	}

	public List<IntelEntityAttachment> getNewAttachments(){
		return attachmentPanel.getNewEntityAttachments();
	}
	
	public List<IntelEntityAttachment> getRemovedEntityAttachments(){
		return attachmentPanel.getRemovedEntityAttachments();
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
		if (recordEditor.getRecord().getDateModified() == null) {
			lblLastModified.setText(""); //$NON-NLS-1$
		}else {
			lblLastModified.setText(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(recordEditor.getRecord().getDateModified()));
		}
		lblLastModifiedBy.setText(recordEditor.getRecord().getLastModifiedBy() == null ? "" : SmartLabelProvider.getFullLabel(recordEditor.getRecord().getLastModifiedBy())); //$NON-NLS-1$
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
		((GridLayout)parent.getLayout()).marginWidth= 5;
		((GridLayout)parent.getLayout()).marginHeight = 5;
		
		Composite buttonPanel = toolkit.createComposite(parent, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(3, false));
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)buttonPanel.getLayout()).marginWidth = 5;
		((GridLayout)buttonPanel.getLayout()).marginHeight = 5;
		((GridLayout)buttonPanel.getLayout()).horizontalSpacing = 0;
		
		SmartUiUtils.setCSSClass(buttonPanel,SmartUiUtils.FORM_HEADER_CLASS);
		
		headerLabel = toolkit.createLabel(buttonPanel, ""); //$NON-NLS-1$
		
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
		
		detailSection = createSectionHeader(sashForm, toolkit, Messages.RecordSummaryPage_DetailsSection);
		
		SectionTabHeader tabList = new SectionTabHeader(new String[]{Messages.RecordSummaryPage_SummarySection, Messages.RecordSummaryPage_HistorySection}, detailSection, toolkit);
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
		
		SmartSection expEntities = new SmartSection(sashForm, toolkit, Messages.RecordSummaryPage_EntitiesSection){
			@Override
			public void populateHeaderAdditions(Composite parent){
			
				Composite bits = toolkit.createComposite(parent);
				bits.setLayout(new GridLayout(2, false));
				((GridLayout)bits.getLayout()).marginWidth = 0;
				((GridLayout)bits.getLayout()).marginHeight = 0;
				bits.setBackground(bits.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				bits.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
				
				FontData fd = bits.getFont().getFontData()[0];
				fd.setHeight(fd.getHeight() - 2);
				Font smallerfont = new Font(bits.getDisplay(), fd);
				
				fd.setStyle(SWT.BOLD);
				Font boldfont = new Font(bits.getDisplay(), fd);
				
				bits.addListener(SWT.Dispose, e->{smallerfont.dispose();boldfont.dispose();});
				Hyperlink list = toolkit.createHyperlink(bits, Messages.RecordSummaryPage_EntityListLink, SWT.NONE);
				Hyperlink details = toolkit.createHyperlink(bits, Messages.RecordSummaryPage_EntityDetailLink, SWT.NONE);
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
		
		SmartSection expAttachments = createSectionHeader(sashForm, toolkit, Messages.RecordSummaryPage_AttachmentsSection);
		attachmentPanel = new AttachmentListComposite(expAttachments, toolkit, recordEditor);
		attachmentPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SmartSection expLocation = createSectionHeader(sashForm, toolkit, Messages.RecordSummaryPage_ObservationsTabName);
		observationPanel = new RecordLocationSummaryComposite(expLocation,toolkit, recordEditor);
		observationPanel .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		initWeights();
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
		for (Control kid : sashForm.getChildren()) {
			kid.addListener(SWT.Resize, e->{
				LAST_WEIGHTS = sashForm.getWeights();	
			});
		}
	}
	
	private void initWeights() {
		int[] weights = LAST_WEIGHTS;
		if (weights != null) {
			sashForm.setWeights(weights);
		}else {
			sashForm.setWeights(new int[]{2,1,1,1});
		}
	}

	private SmartSection createSectionHeader(SashForm parent, FormToolkit toolkit, String text){
		SmartSection sec = new SmartSection(parent, toolkit, text);
		toolkit.adapt(sec);
		return sec;
	}
	
	public void setEditMode(boolean editMode){		
		buttonToolBar.setEditMode(editMode);
	}

	public void refresh() {
		observationPanel.init();
	}
	public void initPage(){
		if (summaryPart != null){
			if (summaryPart.isDisposed()) return;
			for (Control c : summaryPart.getChildren()){
				c.dispose();
			}
		}
		
		if (historyPart != null){
			for (Control c : historyPart.getChildren()){
				c.dispose();
			}
		}
		
		headerLabel.setText(recordEditor.getRecord().getTitle() == null ? "" : recordEditor.getRecord().getTitle()); //$NON-NLS-1$
		
		Composite header = new Composite(summaryPart, SWT.NONE);
		header.setLayout(new GridLayout(2, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = new Composite(header, SWT.NONE);
		leftPart.setLayout(new GridLayout());
		leftPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		summaryScroll = new ScrolledComposite(leftPart, SWT.V_SCROLL);
		summaryScroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		summaryScroll.setExpandHorizontal(true);
		summaryScroll.setExpandVertical(true);
		
		Composite summaryContent = toolkit.createComposite(summaryScroll);
		summaryScroll.setContent(summaryContent);
		summaryContent.setLayout(new GridLayout(2, false));
		((GridLayout)summaryContent.getLayout()).marginWidth = 5;
		((GridLayout)summaryContent.getLayout()).marginHeight = 0;
		((GridLayout)summaryContent.getLayout()).horizontalSpacing = 7; 

		
		lblShortName = toolkit.createLabel(summaryContent, Messages.RecordSummaryPage_TitleLabel);
		int style = SWT.BORDER;
		if (!recordEditor.getEditMode()){
			style = SWT.NONE;
		}
	
		txtShortName = toolkit.createText(summaryContent, recordEditor.getRecord().getTitle(), style);
		txtShortName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				decShortName.hide();
				recordEditor.getRecord().setTitle(txtShortName.getText());
				recordEditor.setDirty(true);
				checkDuplicateName.schedule(250);
			}
		});
		txtShortName.setEditable(recordEditor.getEditMode());
		txtShortName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtShortName.setTextLimit(IntelRecord.MAX_TITLE_LENGTH);
		
		decShortName = new ControlDecoration(txtShortName, SWT.TOP | SWT.LEFT);
		decShortName.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(SharedImages.IMG_DEC_FIELD_WARNING));
		decShortName.hide();
		checkDuplicateName.schedule(250);
		
		
		toolkit.createLabel(summaryContent, Messages.RecordSummaryPage_PrimaryDateLabel);
		if (recordEditor.getEditMode()){
			DateTime dtPrimaryDate = new DateTime(summaryContent, SWT.BORDER | SWT.DATE | SWT.LONG | SWT.CALENDAR | SWT.DROP_DOWN);
	
			SmartUtils.initDateTimeWidget(dtPrimaryDate, recordEditor.getRecord().getPrimaryDate().toLocalDate());
			toolkit.adapt(dtPrimaryDate);
			dtPrimaryDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			dtPrimaryDate.addListener(SWT.Selection, e->{
				recordEditor.getRecord().setPrimaryDate( SmartUtils.toDate(dtPrimaryDate).atStartOfDay() );
				recordEditor.setDirty(true);
			});
		}else {
			toolkit.createLabel(summaryContent, DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(recordEditor.getRecord().getPrimaryDate()));
		}

		toolkit.createLabel(summaryContent, Messages.RecordSummaryPage_ProfileLbl);
		Composite pcomp = toolkit.createComposite(summaryContent);
		pcomp.setLayout(new GridLayout(2, false));
		((GridLayout)pcomp.getLayout()).marginWidth = 0;
		((GridLayout)pcomp.getLayout()).marginHeight = 0;
		pcomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label ll = toolkit.createLabel(pcomp, ""); //$NON-NLS-1$
		ll.setImage(Resources.INSTANCE.getImage(recordEditor.getRecord().getProfile()));
		
		ll = toolkit.createLabel(pcomp, ""); //$NON-NLS-1$
		ll.setText(recordEditor.getRecord().getProfile().getName());
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(summaryContent, Messages.RecordSummaryPage_StatusLabel);
		
		if (recordEditor.getEditMode() && IntelSecurityManager.INSTANCE.canEditRecordStatus(recordEditor.getRecord().getProfile())){
			TableComboViewer cmbStatus = new TableComboViewer(summaryContent, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
			toolkit.adapt(cmbStatus.getControl(), true, true);
			cmbStatus.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			SmartUiUtils.configure(cmbStatus);
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
						return Resources.INSTANCE.getImage((Status)element);
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
			Composite temp = toolkit.createComposite(summaryContent);
			temp.setLayout(new GridLayout(2, false));
			temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)temp.getLayout()).marginWidth = 0;
			((GridLayout)temp.getLayout()).marginHeight = 0;
			Label l = toolkit.createLabel(temp, ""); //$NON-NLS-1$
			l.setImage(Resources.INSTANCE.getImage(recordEditor.getRecord().getStatus()));
			l = toolkit.createLabel(temp, ""); //$NON-NLS-1$
			l.setText(RecordLabelProvider.getRecordStatusLabel(recordEditor.getRecord().getStatus()));
		}

		toolkit.createLabel(summaryContent, Messages.RecordSummaryPage_SourceLabel);
		if (recordEditor.getEditMode()){
			TableComboViewer cmbSource = new TableComboViewer(summaryContent, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
			toolkit.adapt(cmbSource.getControl(), true, true);
			cmbSource.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			SmartUiUtils.configure(cmbSource);
			cmbSource.setContentProvider(ArrayContentProvider.getInstance());
			cmbSource.setLabelProvider(new RecordSourceLabelProvider());
			cmbSource.setInput(new String[]{DialogConstants.LOADING_TEXT});
			loadRecordSources(cmbSource);
		}else{
			Composite temp = toolkit.createComposite(summaryContent);
			temp.setLayout(new GridLayout(2, false));
			temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)temp.getLayout()).marginWidth = 0;
			((GridLayout)temp.getLayout()).marginHeight = 0;
			
			Label l = toolkit.createLabel(temp, ""); //$NON-NLS-1$
			if (recordEditor.getRecord().getRecordSource() != null && recordEditor.getRecord().getRecordSource().getIcon() != null){
				Image img = Resources.INSTANCE.getImage(recordEditor.getRecord().getRecordSource());
				l.setImage(img);
			}
			
			if (recordEditor.getRecord().getRecordSource() != null){
				l = toolkit.createLabel(temp,  recordEditor.getRecord().getRecordSource().getName());
				l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			}
		}
			
		Composite rightPart = new Composite(header, SWT.NONE);
		rightPart.setLayout(new GridLayout(1, false));
		rightPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Hyperlink lnkNarrative = toolkit.createHyperlink(rightPart, Messages.RecordSummaryPage_NarrativeLabel, SWT.NONE);
		lnkNarrative.setToolTipText(Messages.RecordSummaryPage_narrativetooltip);
		lnkNarrative.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lnkNarrative.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				recordEditor.openExternalText(FieldType.NARRATIVE);
			}
		});

		Hyperlink lnkScratchpad = toolkit.createHyperlink(rightPart, Messages.RecordSummaryPage_ScratchpadLink, SWT.NONE);
		lnkScratchpad.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lnkScratchpad.setToolTipText(Messages.RecordSummaryPage_scratchpadtooltip);
		lnkScratchpad.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				recordEditor.openExternalText(FieldType.SCRATCHPAD);
			}
		});
		
		srcAttributePanel = toolkit.createComposite(summaryContent);
		srcAttributePanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		srcAttributePanel.setLayout(new GridLayout());
		((GridLayout)srcAttributePanel.getLayout()).marginWidth = 0;
		((GridLayout)srcAttributePanel.getLayout()).marginHeight = 0;
		configureAttributePanel( recordEditor.getRecord().getRecordSource() );
		
		summaryScroll.setMinSize(summaryContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		summaryPart.layout();
		
		entityPanel.init();
		observationPanel.init();
		
		attachmentPanel.refreshAttachmentTable();
		attachmentPanel.updateEditMode();
		entityPanel.updateEditMode();
		
		enableWs(WorkingSetManager.INSTANCE.isSet() && recordEditor.getRecord().getUuid() != null);
		
		// history part
		Composite infoComp = toolkit.createComposite(historyPart);
		infoComp.setLayout(new GridLayout(2, false));
		infoComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Label l = toolkit.createLabel(infoComp, Messages.RecordSummaryPage_CreateLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		l = toolkit.createLabel(infoComp, recordEditor.getRecord().getDateCreated() == null ? "" : DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(recordEditor.getRecord().getDateCreated())); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		l = toolkit.createLabel(infoComp, Messages.RecordSummaryPage_ModifiedLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		lblLastModified = toolkit.createLabel(infoComp, recordEditor.getRecord().getDateModified() == null ? "" : DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(recordEditor.getRecord().getDateModified())); //$NON-NLS-1$
		lblLastModified.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		l = toolkit.createLabel(infoComp, Messages.RecordSummaryPage_CreatedByLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		l = toolkit.createLabel(infoComp, recordEditor.getRecord().getCreatedBy() == null ? "" :  SmartLabelProvider.getFullLabel(recordEditor.getRecord().getCreatedBy())); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		l = toolkit.createLabel(infoComp, Messages.RecordSummaryPage_ModifiedByLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		lblLastModifiedBy = toolkit.createLabel(infoComp, recordEditor.getRecord().getLastModifiedBy() == null ? "" :  SmartLabelProvider.getFullLabel(recordEditor.getRecord().getLastModifiedBy())); //$NON-NLS-1$
		lblLastModifiedBy.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		historyPart.layout();
		
		initWeights();

	}
	
	private void createDataSourceLabel() {
		for (Control c : dataSourceComposite.getChildren()) c.dispose();
		
		if (recordEditor.getRecord().getSmartSource() == null) {
			if (IntelSecurityManager.INSTANCE.canAccessFieldData()) {
				if (recordEditor.getEditMode()) {
					Hyperlink lnkSetSource = toolkit.createHyperlink(dataSourceComposite, Messages.RecordSummaryPage_SetLink, SWT.NONE);
					lnkSetSource.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					((GridData)lnkSetSource.getLayoutData()).widthHint = 100;				
					lnkSetSource.addHyperlinkListener(new HyperlinkAdapter() {
						@Override
						public void linkActivated(HyperlinkEvent e) {
							setRecordSource();
						}
					});
				}
			}
			
		}else {
			String[] key = recordEditor.getRecord().getSmartSource().split(IntelRecord.SRC_KEY_DELIMITER);

			try {				
				String type = key[0];
				String wpuuid = key[1];
				
				String name = null;
				UUID wpUuid = null;
				try {
					wpUuid = UuidUtils.stringToUuid(wpuuid);
					
					try(Session session = HibernateManager.openSession()){
						Waypoint wp = session.get(Waypoint.class, wpUuid);
						name = WaypointSourceEngine.INSTANCE.getSource(type).getSourceLabel(wp, session, Locale.getDefault());
					}
				}catch (Exception ex) {
					//some problem occured while trying to 
					//find smart source				
				}
				
				if (IntelSecurityManager.INSTANCE.canAccessFieldData()) {
				
					Composite temp = toolkit.createComposite(dataSourceComposite);
					temp.setLayout(new GridLayout());
					temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

					if (name == null || wpUuid == null) {
						//error source has been deleted from SMART
						Label l = toolkit.createLabel(temp, Messages.RecordSummaryPage_RecordSourceLinkNotFound);
						l.setToolTipText(Messages.RecordSummaryPage_RecordSourceLinkNotFoundTooltip);
						l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					}else {
						Hyperlink lnkSmartSource = toolkit.createHyperlink(temp, name, SWT.NONE);
						lnkSmartSource.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						((GridData)lnkSmartSource.getLayoutData()).widthHint = 100;
						
						final UUID uuid = wpUuid;
						lnkSmartSource .addHyperlinkListener(new HyperlinkAdapter() {
							@Override
							public void linkActivated(HyperlinkEvent e) {
								WaypointSourceEngine.INSTANCE.findUiProvider(type).findAndShow(uuid);
							}
						});
					}
					
					if (recordEditor.getEditMode()) {
						temp.setLayout(new GridLayout(2, false));
						Hyperlink lnkSetSource = toolkit.createHyperlink(temp, DialogConstants.EDIT_LINK_TEXT, SWT.NONE);
						lnkSetSource.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
						lnkSetSource.addHyperlinkListener(new HyperlinkAdapter() {
							@Override
							public void linkActivated(HyperlinkEvent e) {
								setRecordSource();
							}
						});
					}
					((GridLayout)temp.getLayout()).marginWidth = 0;
					((GridLayout)temp.getLayout()).marginHeight = 0;
					
				}else {
					Label ll = toolkit.createLabel(dataSourceComposite, name);
					ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					((GridData)ll.getLayoutData()).widthHint = 100;
				}
			}catch (Exception ex) {
				
			}
		}
		dataSourceComposite.getParent().layout(true, true);
	}
	
	private void setRecordSource() {
		RecordSourceSelectionDialog dialog = new RecordSourceSelectionDialog(getSite().getShell(), recordEditor.getRecord().getPrimaryDate().toLocalDate());
		if (dialog.open() != Window.OK) return;
		
		recordEditor.getRecord().setSmartSource(dialog.getWaypoint());
		recordEditor.setDirty(true);
		createDataSourceLabel();
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

		Label sepl = toolkit.createLabel(srcAttributePanel, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
		sepl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite content = toolkit.createComposite(srcAttributePanel);
		content.setLayout(new GridLayout(2, false));
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)content.getLayout()).horizontalSpacing = 7; 
		((GridLayout)content.getLayout()).marginWidth = 0;
		((GridLayout)content.getLayout()).marginHeight = 0;
		
		Label l = toolkit.createLabel(content, Messages.RecordSummaryPage_SmartSourceLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		dataSourceComposite = toolkit.createComposite(content);
		dataSourceComposite.setLayout(new GridLayout());
		dataSourceComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)dataSourceComposite.getLayout()).marginWidth = 0;
		((GridLayout)dataSourceComposite.getLayout()).marginHeight = 0;
		createDataSourceLabel();
		
		
		HashMap<IntelRecordAttributeValue, Label> readOnlyLabels = new HashMap<>();
		if (source != null) {		
			for (IntelRecordSourceAttribute a : source.getAttributes()){
				String name = IIntelligenceLabelProvider.getName(a);
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
						
						af.addResizeListener(e->summaryScroll.setMinSize(summaryScroll.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT)));
						
						if (a.getDuplicateCheck()) addDuplicateIdChecker(af);
					}else{
						l = toolkit.createLabel(content, name + ":"); //$NON-NLS-1$
						l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
	
						Composite tmp  = toolkit.createComposite(content, SWT.NONE);
						tmp.setLayout(new GridLayout(2, false));
						((GridLayout)tmp.getLayout()).marginWidth = 0;
						((GridLayout)tmp.getLayout()).marginHeight = 0;
						tmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						
						EntityCheckboxDropDownViewer editor = new EntityCheckboxDropDownViewer(tmp, a.getEntityType(),recordEditor.getRecord().getProfile(), a.getIsMultiple());
						editorFields.put(a, editor);
						if (v != null) editor.initControl(v.getAttributeListItems());
						editor.addSelectionChangedListener(dirtyListener2);
						editor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						toolkit.adapt(editor);
						
						Hyperlink link = toolkit.createHyperlink(tmp, Messages.RecordSummaryPage_linkentitesLabel, SWT.NONE);
						link.setToolTipText(Messages.RecordSummaryPage_linkentitiestooltip);
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
					String value = ""; //$NON-NLS-1$
					if (v != null){
						value = v.getAttributeValueAsString(Locale.getDefault(), crs);
					}
					
					l = toolkit.createLabel(content, name + ":"); //$NON-NLS-1$
					l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
					
					if (v != null && v.getAttribute() != null 
							&& v.getAttribute().getAttribute() != null 
							&& v.getAttribute().getAttribute().getType() == AttributeType.TEXT) {
						MultiLineText txt = new MultiLineText(content);
						txt.setEditable(false);
						txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						txt.setText(value);
						((GridData)txt.getLayoutData()).widthHint = 100;
						txt.addListener(SWT.Resize, e->summaryScroll.setMinSize(summaryScroll.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT)));
					}else {
						l = toolkit.createLabel(content,value);
						l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
						if (v != null){
							if (a.isListAttribute()){
								readOnlyLabels.put(v, l);
							}
						}
					}
				}
			}
			if (!recordEditor.getEditMode()){
				loadListAttribute(readOnlyLabels);
			}
		}

		srcAttributePanel.layout(true, true);
	}
	
	/*
	 * loads list attribute names for labels when editor is in read only mode;
	 * when not in read only mode we do not need to do this
	 */
	private void loadListAttribute(final HashMap<IntelRecordAttributeValue, Label> attributes){
		Job j = new Job(Messages.RecordSummaryPage_loadingAttributesJob){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final HashMap<Label, String> labels = new HashMap<>();
				
				try(Session session = HibernateManager.openSession()){
					for (Entry<IntelRecordAttributeValue,Label> v : attributes.entrySet()){
						if (v.getKey().getAttributeListItems().isEmpty()){
							labels.put(v.getValue(), ""); //$NON-NLS-1$
						}else{
							StringBuilder sb = new StringBuilder();
							sb.append("(" + v.getKey().getAttributeListItems().size() + ") "); //$NON-NLS-1$ //$NON-NLS-2$
							if (v.getKey().getAttribute().getAttribute() != null){
								//attributes
								for (IntelRecordAttributeValueList l : v.getKey().getAttributeListItems()){
									if (v.getKey().getAttribute().getAttribute().getType() == AttributeType.LIST) {
										String lbl = ((IntelAttributeListItem)session.get(IntelAttributeListItem.class, l.getId().getElementUuid())).getName();
										sb.append(lbl);
									}else if (v.getKey().getAttribute().getAttribute().getType() == AttributeType.EMPLOYEE) {
										String lbl = SmartLabelProvider.getFullLabel( ((Employee)session.get(Employee.class, l.getId().getElementUuid())) );
										sb.append(lbl);
									}
									sb.append(", "); //$NON-NLS-1$
								}
							}else if(v.getKey().getAttribute().getEntityType() != null){
								//entity types
								for (IntelRecordAttributeValueList l : v.getKey().getAttributeListItems()){
									String lbl = ((IntelEntity)session.get(IntelEntity.class, l.getId().getElementUuid())).getIdAttributeAsText();
									sb.append(lbl);
									sb.append(", "); //$NON-NLS-1$
								}
							}
							labels.put(v.getValue(), sb.substring(0, sb.length()-2));
						}
					}
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
		if (recordEditor.getRecord().getAttributes() == null) return null;
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
			MessageDialog.openError(getSite().getShell(), Messages.RecordSummaryPage_ErrorDialogTitle, Messages.RecordSummaryPage_ErrorDialogMessage);
			return false;
		}
		
		IntelRecord r = recordEditor.getRecord();
		if (r.getAttributes() == null) r.setAttributes(new ArrayList<>());
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
		Job srcLoader = new Job(Messages.RecordSummaryPage_loadingSourcesJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<Object> allInput = new ArrayList<Object>();
				try(Session s = HibernateManager.openSession()){
					IntelProfile p = s.get(IntelProfile.class, recordEditor.getRecord().getProfile().getUuid());
					
					List<IntelRecordSource> sources = p.getRecordSources().stream().map(e->e.getRecordSource()).collect(Collectors.toList());
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
					sources.sort((a,b)->Collator.getInstance().compare(a.getName(),b.getName()));
					allInput.addAll(sources);
				}
				allInput.add(0, ""); //$NON-NLS-1$
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
							summaryScroll.setMinSize(summaryScroll.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
						}
					});
				});
				return org.eclipse.core.runtime.Status.OK_STATUS;
			}
			
		};
		srcLoader.setSystem(false);
		srcLoader.schedule();
	}
	
	
	private void addDuplicateIdChecker(AttributeFieldEditor editor){
		//check for duplicate identifiers
		editor.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkDuplicate(editor);
			}
		});
		checkDuplicate(editor);
	}
	
	private void checkDuplicate(AttributeFieldEditor editor) {
		if (editor.getAttributeLabel() != null && editor.getAttributeLabel().getMenu() != null) {
			editor.getAttributeLabel().getMenu().dispose();
			editor.getAttributeLabel().setMenu(null);
		}
		
		try(Session session = HibernateManager.openSession()){
			IntelRecordSource s = recordEditor.getRecord().getRecordSource();
			
			IntelRecordSourceAttribute a = null;
			for (IntelRecordSourceAttribute t : s.getAttributes()) {
				if (t.getAttribute() != null && t.getAttribute().equals(editor.getAttribute())) {
					a = t;
					break;
				}
			}
			
			IntelRecordAttributeValue tmp = new IntelRecordAttributeValue();
			tmp.setAttribute(a);
			editor.updateValue(tmp);
			if (RecordManager.INSTANCE.isDuplicateId(tmp.getAttributeValue(), editor.getAttribute(), s, SmartDB.getCurrentConservationArea(), session, recordEditor.getRecord().getUuid())){
				
				String warnMessage = MessageFormat.format(Messages.RecordSummaryPage_recordDuplicateAttribute,s.getName());  
				editor.setWarningMessage(warnMessage);
				
				if (editor.getAttributeLabel() != null) {
					Menu mnu = new Menu(editor.getAttributeLabel());
					MenuItem mi = new MenuItem(mnu, SWT.DEFAULT);
					mi.setText(Messages.RecordSummaryPage_FindDuplicates);
					mi.addListener(SWT.Selection, e->{
						AttributeRecordSearch search = new AttributeRecordSearch(s, tmp);

						MPart mpart = recordEditor.getContext().get(EPartService.class).findPart(RecordsView.ID);
						if (mpart != null) {
							((RecordsView)E3Utils.getSourceObject(mpart)).setBasicSearch(search);
						}
						
					});
					editor.getAttributeLabel().setMenu(mnu);
					
				}
			}else{
				editor.setWarningMessage(null);
				
				
			}
			
		}
	}
	/*
	 * job to check for duplicate names
	 */
	private Job checkDuplicateName = new Job("check duplicate name") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String[] title = new String[] {null};
			
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					if (txtShortName == null || txtShortName.isDisposed()) return;
					title[0] = txtShortName.getText();					
				}
				
			});
			if (title[0] == null) return org.eclipse.core.runtime.Status.OK_STATUS;
			IntelRecord record = recordEditor.getRecord();
			if (record == null) return org.eclipse.core.runtime.Status.OK_STATUS;
			
			boolean isDuplicate = false;
			try(Session s = HibernateManager.openSession()){
				isDuplicate = RecordManager.INSTANCE.isDuplicateName(title[0], record.getConservationArea(), s, record.getUuid());
			}
			final boolean fisDuplicate = isDuplicate;
			
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					if (txtShortName == null || txtShortName.isDisposed()) return;
					if (fisDuplicate) {
						decShortName.setDescriptionText(Messages.RecordSummaryPage_recordDuplicateName);
						decShortName.show();
						if(lblShortName.getMenu() == null) {
							Menu m = new Menu(lblShortName);
							MenuItem mi = new MenuItem(m, SWT.DEFAULT);
							mi.setText(Messages.RecordSummaryPage_FindDuplicates);
							mi.addListener(SWT.Selection, e->{
								String search = txtShortName.getText();
								MPart mpart = recordEditor.getContext().get(EPartService.class).findPart(RecordsView.ID);
								if (mpart != null) {
									BasicRecordSearch bsearch = new BasicRecordSearch(record.getRecordSource(), null, search);
									bsearch.setTitleExact(true);
									((RecordsView)E3Utils.getSourceObject(mpart)).setBasicSearch(bsearch);
								}
								
							});
							lblShortName.setMenu(m);
						}
					}else {
						if(lblShortName.getMenu() != null) {
							lblShortName.getMenu().dispose();
							lblShortName.setMenu(null);
						}
					}
				}
						
			});
					
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}
		
	};
}