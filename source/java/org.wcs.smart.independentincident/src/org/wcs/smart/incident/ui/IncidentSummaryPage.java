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
package org.wcs.smart.incident.ui;

import java.text.Collator;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.locationtech.jts.geom.Point;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.ui.newwizard.CommentComposite;
import org.wcs.smart.incident.ui.newwizard.DateTimeComposite;
import org.wcs.smart.incident.ui.newwizard.DistanceDirectionComposite;
import org.wcs.smart.incident.ui.newwizard.EditIncidentDialog;
import org.wcs.smart.incident.ui.newwizard.IdComposite;
import org.wcs.smart.incident.ui.newwizard.IncidentAttachmentComposite;
import org.wcs.smart.incident.ui.newwizard.LocationComposite;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.observation.ui.ObservationAttachmentLabelProvider;
import org.wcs.smart.observation.ui.input.ObservationWizard;
import org.wcs.smart.observation.ui.input.ObservationWizardDialog;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SmartUtils;

/**
 * Incident editor summary page
 * @author Emily
 *
 */
public class IncidentSummaryPage extends EditorPart {

	private IncidentEditor editor;

	private FormToolkit toolkit;
	
	private Text txtComments;
	private Text txtIncidentId;
	private Text txtDate;
	private Text txtTime;
	private Text txtLocation;
	private Label lblLocation;
	private Text txtDistance;
	private Text txtPrjLocation;
	private Text txtDirection;
	private Label txtType;
	private Label lblLastModified;
//	private Label lblLastModifiedBy;
	
	private ListViewer attachments;
	
	private Composite observationComp;
	private Font boldFont;
	
	public IncidentSummaryPage(IncidentEditor editor){
		this.editor = editor;
	}
	
	@Override
	public void dispose() {
		if (toolkit != null){
			toolkit.dispose();
			toolkit = null;
		}
		super.dispose();
	}
	
	/**
	 * does nothing
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	/**
	 * does nothing
	 */
	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
	}

	/**
	 * @return false
	 */
	@Override
	public boolean isDirty() {
		return false;
	}

	/**
	 * @return false
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	/**
	 * Initializes the editor fields with values
	 * from the incident.
	 *  
	 * @param incident
	 */
	public void initData(Waypoint incident){
		
		try(Session session = HibernateManager.openSession()){
			session.saveOrUpdate(editor.getIncident());
			if (incident.getComment() == null){
				txtComments.setText(""); //$NON-NLS-1$
			}else{
				this.txtComments.setText(incident.getComment());
			}
		
			StringBuilder sb = new StringBuilder();
			if (incident.getLastModifiedBy() != null) {
				sb.append(MessageFormat.format(Messages.IncidentSummaryPage_LastModified1,
						SmartLabelProvider.getShortLabel(incident.getLastModifiedBy()), 
						DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(incident.getLastModified())));
			}else {
				sb.append(MessageFormat.format(Messages.IncidentSummaryPage_LastModified2, 
						DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(incident.getLastModified())));
			}
			this.lblLastModified.setText(sb.toString());
			this.txtType.setText(WaypointSourceEngine.INSTANCE.getSource(incident.getSourceId()).getName(Locale.getDefault()));
			this.txtIncidentId.setText(incident.getId());
			this.txtDate.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(incident.getDateTime()));
			this.txtTime.setText(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).format(incident.getDateTime()));
			Projection viewProjection = HibernateManager.getCurrentViewProjection(session);
			CoordinateReferenceSystem crs = null;
			try{
				if (viewProjection != null){
					crs = ReprojectUtils.stringToCrs(viewProjection.getDefinition());
					txtLocation.setToolTipText(viewProjection.getName());
					lblLocation.setToolTipText(viewProjection.getName());
				}else{
					crs = SmartDB.DATABASE_CRS;
					txtLocation.setToolTipText(crs.getName().toString());
					lblLocation.setToolTipText(crs.getName().toString());
				}
			}catch(FactoryException ex){
				IncidentPlugIn.log(ex.getMessage(), ex);
			}
			Point p = ReprojectUtils.transform(incident.getRawX(), incident.getRawY(), crs);
			this.txtLocation.setText(p.getX() + Messages.IncidentSummaryPage_LocationSeparator + p.getY());
			
			if (editor.getOptions().getTrackDistanceDirection()){
				
				p = ReprojectUtils.transform(incident.getX(), incident.getY(), crs);
				this.txtPrjLocation.setText(p.getX() + Messages.IncidentSummaryPage_LocationSeparator + p.getY());
				
				
				if (incident.getDirection() == null){
					this.txtDirection.setText(""); //$NON-NLS-1$
				}else{
					this.txtDirection.setText(String.valueOf(incident.getDirection()));
				}
		
				if (incident.getDistance() == null){
					this.txtDistance.setText(""); //$NON-NLS-1$
				}else{
					this.txtDistance.setText(String.valueOf(incident.getDistance()));
				}
			}
		
			//include all attachments 
			List<ISmartAttachment> allAtts = new ArrayList<ISmartAttachment>();
			allAtts.addAll(incident.getAttachments());
			
			for (WaypointObservation wo : incident.getAllObservations()){
				if (wo.getAttachments() != null) {
					allAtts.addAll(wo.getAttachments());
				}
			}
			
			Collections.sort(allAtts, new Comparator<ISmartAttachment>() {

				@Override
				public int compare(ISmartAttachment a, ISmartAttachment b) {
					if (a.getClass().equals(b.getClass())){
						return Collator.getInstance().compare(a.getFilename(), b.getFilename());
					}else if (a instanceof WaypointAttachment){
						return -1;
					}
					return 1;
				}
			});
			this.attachments.setInput(allAtts);
			
			
			for (Control k : observationComp.getChildren()) k.dispose();
			
			((GridLayout)observationComp.getLayout()).verticalSpacing = 10;
			((GridLayout)observationComp.getLayout()).marginWidth = 0;
			((GridLayout)observationComp.getLayout()).marginHeight = 0;
			
			Employee obs = null;
			if (!incident.getAllObservations().isEmpty()) obs = incident.getAllObservations().get(0).getObserver();
			
			
			if (editor.getOptions().getTrackObserver()) {
				Label obsl = toolkit.createLabel(observationComp, 
						MessageFormat.format(Messages.IncidentSummaryPage_ObserverLbl, 
							obs == null ? " " :  //$NON-NLS-1$
							SmartLabelProvider.getShortLabel(obs)));
				obsl.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			}
			
			IconSet iset = QueryFactory.buildQuery(session, IconSet.class, 
					new Object[] {"conservationArea", incident.getConservationArea()}, //$NON-NLS-1$
					new Object[] {"isDefault", true}).uniqueResult(); //$NON-NLS-1$
			
			Listener labelResize = e->{
				Label l = (Label)e.widget;
				String text = l.getText();
				l.setText(""); //$NON-NLS-1$
				int w = l.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				((GridData)l.getLayoutData()).widthHint = w;
				l.setText(text);
			};
			
			for (WaypointObservationGroup g : incident.getObservationGroups()) {
				
				Composite group = toolkit.createComposite(observationComp, SWT.BORDER);
				group.setLayout(new GridLayout());
				group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				for (int i = 0; i < g.getObservations().size(); i ++) {
					WaypointObservation wo = g.getObservations().get(i);
				
					Composite left = toolkit.createComposite(group);
					left.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
					
					if (iset != null && wo.getCategory().getIcon() != null) {
						wo.getCategory().getIcon().getIconFile(iset).computeFileLocation(session);
						
						left.setLayout(new GridLayout(2, false));
						IconFile file = wo.getCategory().getIcon().getIconFile(iset);
						if (file != null) {
							Label img = new Label(left, SWT.NONE);
							img.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2));
							img.setBackground(left.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
							Image image = SmartUtils.getImage(file.getAttachmentFile(), 32);
							img.setImage(image);
							img.addListener(SWT.Dispose, e->image.dispose());
						}
					}else {
						left.setLayout(new GridLayout(1, false));
					}
					((GridLayout)left.getLayout()).marginWidth = 0;
					((GridLayout)left.getLayout()).marginHeight = 0;
					
					Label l = toolkit.createLabel(left, SmartUtils.formatStringForLabel(wo.getCategory().getName()));
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					
					l.setFont(boldFont);
					String name = ""; //$NON-NLS-1$
					if (wo.getCategory().getParent() != null) {
						name += SmartUtils.formatStringForLabel(wo.getCategory().getParent().getFullCategoryName()) ;
					}
					l = toolkit.createLabel(left, name, SWT.WRAP );
					l.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true));
					l.addListener(SWT.Resize,labelResize);
					l.setFont(boldFont);
					
					Composite right = toolkit.createComposite(group);
					right.setLayout(new GridLayout(2, false));
					right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					((GridLayout)right.getLayout()).marginWidth = 0;
					((GridLayout)right.getLayout()).marginHeight = 0;
					
					Composite attributes = toolkit.createComposite(right);
					attributes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					attributes.setLayout(new GridLayout(2, false));
					((GridLayout)attributes.getLayout()).marginWidth = 0;
					((GridLayout)attributes.getLayout()).marginHeight = 0;
					((GridData)attributes.getLayoutData()).horizontalIndent = 64;

					for (WaypointObservationAttribute a : wo.getAttributes()) {
						l = toolkit.createLabel(attributes,SmartUtils.formatStringForLabel(a.getAttribute().getName()) +":"); //$NON-NLS-1$
						l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
						
						l = toolkit.createLabel(attributes, SmartUtils.formatStringForLabel(a.getAttributeValueAsString(Locale.getDefault())), SWT.WRAP);
						l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						l.addListener(SWT.Resize,labelResize);
	
					}
					
					l = toolkit.createLabel(attributes,Messages.IncidentSummaryPage_AttachmentsLabel);
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				
					l = toolkit.createLabel(attributes, MessageFormat.format("{0}", wo.getAttachments().size())); //$NON-NLS-1$
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
					if (i < g.getObservations().size() - 1) {
						l = toolkit.createLabel(group, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
						l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
					}
				}			
			}
			observationComp.layout(true);
			
			ScrolledComposite scroll = (ScrolledComposite) observationComp.getParent();
			int width = scroll.getClientArea().width;
			scroll.setMinSize( observationComp.computeSize( width, SWT.DEFAULT ) );

			observationComp.getParent().layout(true);
		}		
	}
	
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		toolkit.setBorderStyle(SWT.BORDER);
		
		Form frmSummary = toolkit.createForm(parent);
		frmSummary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		GridLayout layout = new GridLayout(1, true);
		frmSummary.getBody().setLayout(layout);
		
		String canEdit = editor.canEdit();
		if (canEdit != null){
			Composite warning = toolkit.createComposite(frmSummary.getBody());
			warning.setLayout(new GridLayout(2, false));
			Label lblImage = toolkit.createLabel(warning, null, SWT.NONE);
			Image x = editor.getSite().getWorkbenchWindow().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			lblImage.setImage(x);
			Label lblWarning = toolkit.createLabel(warning, "", SWT.NONE); //$NON-NLS-1$
			lblWarning.setText(MessageFormat.format(Messages.IncidentSummaryPage_CannotEdit, new Object[]{ canEdit }));
		}
		
		Section summarySection = toolkit.createSection(frmSummary.getBody(), Section.TITLE_BAR   );
		summarySection.setText(Messages.IncidentSummaryPage_SectionLabel);
		summarySection.setDescription(Messages.IncidentSummaryPage_SectionDescription);
		summarySection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite top = toolkit.createComposite(summarySection, SWT.NONE);
		top.setLayout(new GridLayout(2, true));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)top.getLayout()).marginWidth = 0;
		((GridLayout)top.getLayout()).marginHeight = 0;
		summarySection.setClient(top);
		
		Composite left= toolkit.createComposite(top, SWT.NONE);
		left.setLayout(new GridLayout(3, false));
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite right = toolkit.createComposite(top, SWT.NONE);
		right.setLayout(new GridLayout(3, false));
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		toolkit.createLabel(left, Messages.IncidentSummaryPage_IdLabel);
		txtIncidentId = toolkit.createText(left, ""); //$NON-NLS-1$
		txtIncidentId.setEditable(false);
		txtIncidentId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createEdit(left, canEdit, IdComposite.ID);
		
		toolkit.createLabel(left, Messages.IncidentSummaryPage_DateLabel);
		txtDate = toolkit.createText(left, ""); //$NON-NLS-1$
		txtDate.setEditable(false);
		txtDate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createEdit(left, canEdit, DateTimeComposite.ID);
		
		toolkit.createLabel(left, Messages.IncidentSummaryPage_TimeLabel);
		txtTime = toolkit.createText(left, ""); //$NON-NLS-1$
		txtTime.setEditable(false);
		txtTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createEdit(left, canEdit, DateTimeComposite.ID);
		
		lblLocation = toolkit.createLabel(left, Messages.IncidentSummaryPage_LocationLabel);
		txtLocation = toolkit.createText(left, ""); //$NON-NLS-1$
		txtLocation.setEditable(false);
		txtLocation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createEdit(left, canEdit, LocationComposite.ID);

		if (editor.getOptions().getTrackDistanceDirection()){
			toolkit.createLabel(left, Messages.IncidentSummaryPage_DistanceLabel1);
			txtDistance = toolkit.createText(left, ""); //$NON-NLS-1$
			txtDistance.setEditable(false);
			txtDistance.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			createEdit(left, canEdit, DistanceDirectionComposite.ID);
		
			Label l = toolkit.createLabel(left, Messages.IncidentSummaryPage_DirectionLabel1);
			l.setToolTipText(Messages.IncidentSummaryPage_bearingtooltip);
			txtDirection = toolkit.createText(left, ""); //$NON-NLS-1$
			txtDirection.setEditable(false);
			txtDirection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			createEdit(left, canEdit, DistanceDirectionComposite.ID);
			
			toolkit.createLabel(left, Messages.IncidentSummaryPage_PrjLocationLbl);
			txtPrjLocation = toolkit.createText(left, ""); //$NON-NLS-1$
			txtPrjLocation.setEditable(false);
			txtPrjLocation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			toolkit.createLabel(left, ""); //$NON-NLS-1$
		}
		
		toolkit.createLabel(left, Messages.IncidentSummaryPage_IncidentSourceField);
		
		txtType = toolkit.createLabel(left, ""); //$NON-NLS-1$
		txtType.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Label l = toolkit.createLabel(right, Messages.IncidentSummaryPage_CommentsLabel);
		l.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		txtComments = toolkit.createText(right, "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		txtComments.setEditable(false);
		txtComments.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)txtComments.getLayoutData()).heightHint = 70;
		((GridData)txtComments.getLayoutData()).widthHint = 100;
		createEdit(right, canEdit, CommentComposite.ID);
		
		l = toolkit.createLabel(right, Messages.IncidentSummaryPage_AttachmentsLabel);
		l.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		attachments = new ListViewer(right);
		attachments.setContentProvider(ArrayContentProvider.getInstance());
		attachments.setLabelProvider(new ObservationAttachmentLabelProvider(){
			public String getText(Object element) {
				if (element instanceof ObservationAttachment){
					return "**" + super.getText(element); //$NON-NLS-1$
				}
				return super.getText(element);
			}
		});
		
		attachments.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)attachments.getControl().getLayoutData()).heightHint = 70;
		((GridData)attachments.getControl().getLayoutData()).widthHint = 100;
		attachments.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				ISmartAttachment att = (ISmartAttachment) ((StructuredSelection)attachments.getSelection()).getFirstElement();
				if (att != null){
					AttachmentUtil.openAttachment(att);
				}
				
			}
		});
		createEdit(right, canEdit, IncidentAttachmentComposite.ID);
		toolkit.createLabel(right, ""); //$NON-NLS-1$
		l = toolkit.createLabel(right, "**" + Messages.IncidentSummaryPage_ObservationAttachmentsLabel); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2,1));
		
		Section observationSection = toolkit.createSection(frmSummary.getBody(), Section.TITLE_BAR);
		observationSection.setText(Messages.IncidentSummaryPage_ObservationLabel);
		observationSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite observationTableComp = toolkit.createComposite(observationSection);
		observationTableComp.setLayout(new GridLayout(1, false));
		((GridLayout)observationTableComp.getLayout()).marginWidth = 0;
		((GridLayout)observationTableComp.getLayout()).marginHeight = 0;
		observationSection.setClient(observationTableComp);
		
		ScrolledComposite scroll = new ScrolledComposite(observationTableComp, SWT.V_SCROLL| SWT.H_SCROLL);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.addListener( SWT.Resize, event -> {
			  int width = scroll.getClientArea().width;
			  scroll.setMinSize(  observationComp.computeSize(width, SWT.DEFAULT) );
		});
		
		observationComp = toolkit.createComposite(scroll);
		scroll.setContent(observationComp);
		observationComp.setLayout(new GridLayout());
		((GridLayout)observationComp.getLayout()).marginWidth = 0;
		((GridLayout)observationComp.getLayout()).marginHeight = 0;
		
		FontData fd = observationComp.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(l.getDisplay(),fd);
		observationComp.addListener(SWT.Dispose, e->boldFont.dispose());
		
		Composite bottomComp = toolkit.createComposite(observationTableComp);
		bottomComp.setLayout(new GridLayout(2, false));
		((GridLayout)bottomComp.getLayout()).marginWidth = 0;
		((GridLayout)bottomComp.getLayout()).marginHeight = 0;
		bottomComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		if (canEdit == null){
			Button btnEdit = toolkit.createButton(bottomComp, Messages.IncidentSummaryPage_EditButtonName, SWT.PUSH);
			btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			btnEdit.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					editIncident(null);
				}
			});
			
		}
		lblLastModified = toolkit.createLabel(bottomComp, ""); //$NON-NLS-1$
		if (canEdit != null) {
			lblLastModified.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, false, 2, 1));	
		}else {
			lblLastModified.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, false));
		}
		
		initData(editor.getIncident());
	}
	
	
	private void editIncident(WaypointObservation initObs){
		ObservationWizard wizard = new ObservationWizard(editor.getIncident(), getEmployees());
		if (initObs != null) wizard.setToEdit(initObs);
		
		ObservationWizardDialog wd = new ObservationWizardDialog(getEditorSite().getShell(), wizard);
		wd.open();
	}
	
	private List<Employee> employees;
	private List<Employee> getEmployees(){
		employees = null;
		Job j = new Job(Messages.IncidentSummaryPage_EmployeeLoadJobName){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try(Session s = HibernateManager.openSession()){
					ObservationOptions observationOptions = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), s);
					if (!observationOptions.getTrackObserver()){
						employees = null;
					}else{
						employees = HibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), s);
					}
				}
				if (employees != null){
					Collections.sort(employees, new Comparator<Employee>() {
						@Override
						public int compare(Employee arg0, Employee arg1) {
							return Collator.getInstance().compare(
									SmartLabelProvider.getFullLabel(arg0).toUpperCase(), 
									SmartLabelProvider.getFullLabel(arg1).toUpperCase());
						}
					});
				}
				
				return Status.OK_STATUS;
			}
		};
		j.schedule();
			
		try {
			j.join();
		} catch (InterruptedException e) {
			IncidentPlugIn.log(e.getMessage(), e);
		}
		return employees;
		
	}
	
	private void createEdit(Composite parent, String canEdit, final String panelId){
		if (canEdit == null){
			Hyperlink l = toolkit.createHyperlink(parent,DialogConstants.EDIT_LINK_TEXT,SWT.NONE);
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
			l.addHyperlinkListener(new IHyperlinkListener() {
				@Override
				public void linkExited(HyperlinkEvent e) {	}
				
				@Override
				public void linkEntered(HyperlinkEvent e) {	}
				
				@Override
				public void linkActivated(HyperlinkEvent e) {
					EditIncidentDialog d = new EditIncidentDialog(getSite().getShell(), panelId, editor.getIncident());
					d.open();
					ApplicationGIS.getToolManager().setCurrentEditor(editor);
					initData(editor.getIncident());
				}
			});
		}else{
			toolkit.createLabel(parent, ""); //$NON-NLS-1$
		}
	}

	@Override
	public void setFocus() {
		txtIncidentId.setFocus();
	}
	
}

