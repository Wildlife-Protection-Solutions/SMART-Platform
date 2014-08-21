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
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
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
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.common.attachment.SmartAttachmentLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.ui.newwizard.CommentComposite;
import org.wcs.smart.incident.ui.newwizard.DateTimeComposite;
import org.wcs.smart.incident.ui.newwizard.DistanceDirectionComposite;
import org.wcs.smart.incident.ui.newwizard.EditIncidentDialog;
import org.wcs.smart.incident.ui.newwizard.IdComposite;
import org.wcs.smart.incident.ui.newwizard.IncidentAttachmentComposite;
import org.wcs.smart.incident.ui.newwizard.LocationComposite;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.ui.input.ObservationWizard;
import org.wcs.smart.observation.ui.input.ObservationWizardDialog;
import org.wcs.smart.ui.properties.DialogConstants;

import com.vividsolutions.jts.geom.Point;

/**
 * Incident editor summary page
 * @author Emily
 *
 */
public class IncidentSummaryPage extends EditorPart {

	private IncidentEditor editor;

	private FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	
	private Text txtComments;
	private Text txtIncidentId;
	private Text txtDate;
	private Text txtTime;
	private Text txtLocation;
	private Text txtDistance;
	private Text txtDirection;
	private ListViewer attachments;
	private TableViewer observationTable ;
	
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
		Session session = HibernateManager.openSession();
		session.saveOrUpdate(editor.getIncident());
		try{
			if (incident.getComment() == null){
				txtComments.setText(""); //$NON-NLS-1$
			}else{
				this.txtComments.setText(incident.getComment());
			}
		
			this.txtIncidentId.setText(String.valueOf(incident.getId()));
			this.txtDate.setText(DateFormat.getDateInstance().format(incident.getDateTime()));
			this.txtTime.setText(DateFormat.getTimeInstance().format(incident.getDateTime()));
			ObservationOptions observationOptions = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), session);
			Point p = Projection.transform(incident.getX(), incident.getY(), observationOptions.getViewProjection());
			this.txtLocation.setText(p.getX() + Messages.IncidentSummaryPage_LocationSeparator + p.getY());
		
			if (editor.getOptions().getTrackDistanceDirection()){
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
			if (incident.getObservations() != null){
				for (WaypointObservation wo : incident.getObservations()){
					if (wo.getAttachments() != null) {
						allAtts.addAll(wo.getAttachments());
					}
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
			this.observationTable.setInput(incident.getObservations());
		}finally{
			session.close();
		}
		
	}
	@Override
	public void createPartControl(Composite parent) {
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
		
		toolkit.createLabel(left, Messages.IncidentSummaryPage_LocationLabel);
		txtLocation = toolkit.createText(left, ""); //$NON-NLS-1$
		txtLocation.setEditable(false);
		txtLocation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createEdit(left, canEdit, LocationComposite.ID);

		if (editor.getOptions().getTrackDistanceDirection()){
			toolkit.createLabel(left, Messages.IncidentSummaryPage_DistanceLabel);
			txtDistance = toolkit.createText(left, ""); //$NON-NLS-1$
			txtDistance.setEditable(false);
			txtDistance.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			createEdit(left, canEdit, DistanceDirectionComposite.ID);
		
			toolkit.createLabel(left, Messages.IncidentSummaryPage_DirectionLabel);
			txtDirection = toolkit.createText(left, ""); //$NON-NLS-1$
			txtDirection.setEditable(false);
			txtDirection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			createEdit(left, canEdit, DistanceDirectionComposite.ID);
		}
		
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
		attachments.setLabelProvider(new SmartAttachmentLabelProvider(){
			
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
		
		Section observationSection = toolkit.createSection(frmSummary.getBody(), Section.TITLE_BAR   );
		observationSection.setText(Messages.IncidentSummaryPage_ObservationLabel);
		observationSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite observationTableComp = toolkit.createComposite(observationSection);
		observationTableComp.setLayout(new GridLayout(1, false));
		observationSection.setClient(observationTableComp);
		
		Composite innerTable = toolkit.createComposite(observationTableComp);
		innerTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableColumnLayout tlayout = new TableColumnLayout();
		innerTable.setLayout(tlayout);
		
		observationTable = new TableViewer(innerTable, SWT.FULL_SELECTION | SWT.BORDER);
		observationTable.setContentProvider(ArrayContentProvider.getInstance());

		observationTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		observationTable.getTable().setLinesVisible(true);
		observationTable.getTable().setHeaderVisible(true);
		
		editor.getSite().setSelectionProvider(observationTable);
		
		TableViewerColumn colFirstName = new TableViewerColumn(observationTable, SWT.NONE);
		colFirstName.getColumn().setWidth(200);
		colFirstName.getColumn().setText(Messages.IncidentSummaryPage_CategoryLabel);
		colFirstName.setLabelProvider(new ColumnLabelProvider() {
		  @Override
		  public String getText(Object element) {
		    WaypointObservation p = (WaypointObservation) element;
		    return p.getCategory().getFullCategoryName();
		  }
		});
		tlayout.setColumnData(colFirstName.getColumn(), new ColumnWeightData(20));
		
		TableViewerColumn colAttributes = new TableViewerColumn(observationTable, SWT.NONE);
		colAttributes.getColumn().setWidth(200);
		colAttributes.getColumn().setText(Messages.IncidentSummaryPage_AttributeLabel);
		colAttributes.setLabelProvider(new ColumnLabelProvider() {
		  @Override
		  public String getText(Object element) {
		    StringBuilder sb = new StringBuilder();
			for (WaypointObservationAttribute att : ((WaypointObservation)element).getAttributes()){
				sb.append(att.getAttribute().getName());
				sb.append(Messages.IncidentSummaryPage_EqualsFunction);
				sb.append(att.getAttributeValueAsString());
				sb.append("   "); //$NON-NLS-1$
			}
			return sb.toString();
		  }
		});
		tlayout.setColumnData(colAttributes.getColumn(), new ColumnWeightData(70));
		
		if (editor.getOptions().getTrackObserver()){
			TableViewerColumn colObserver = new TableViewerColumn(observationTable, SWT.NONE);
			colObserver.getColumn().setText(Messages.IncidentSummaryPage_ColumnLabel);
			colObserver.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					WaypointObservation o =  ((WaypointObservation)element);
					if (o.getObserver() == null){
						return ""; //$NON-NLS-1$
					}else{
						return o.getObserver().getShortLabel();
					}
				}
			});
			tlayout.setColumnData(colObserver.getColumn(), new ColumnWeightData(15));
		}
		
		TableViewerColumn colAttachments = new TableViewerColumn(observationTable, SWT.NONE);
		colAttachments.getColumn().setText(Messages.IncidentSummaryPage_AttachmentsColumnName);
		colAttachments.setLabelProvider(new ColumnLabelProvider() {
		  @Override
		  public String getText(Object element) {
			  WaypointObservation o =  ((WaypointObservation)element);
			  if (o.getAttachments() != null && o.getAttachments().size() > 0){				  
				  return MessageFormat.format(Messages.IncidentSummaryPage_AttachmentsColumnContent, new Object[]{String.valueOf(o.getAttachments().size())});
			  }
			  return ""; //$NON-NLS-1$
		  }
		});
		tlayout.setColumnData(colAttachments.getColumn(), new ColumnWeightData(15));
		
		
		
		
		if (canEdit == null){
			Button btnEdit = toolkit.createButton(observationTableComp, Messages.IncidentSummaryPage_EditButtonName, SWT.PUSH);
			btnEdit.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					editIncident();
				}
			});
			
			observationTable.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					editIncident();
				}
			});
			
		}
		
		initData(editor.getIncident());
	}
	
	private void editIncident(){
		ObservationWizardDialog wd = new ObservationWizardDialog(getEditorSite().getShell(),
				new ObservationWizard(editor.getIncident(), getEmployees()));
		wd.open();
	}
	
	private List<Employee> employees = null;
	
	private List<Employee> getEmployees(){
		if (employees == null){
			Job j = new Job(Messages.IncidentSummaryPage_EmployeeLoadJobName){
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					try{
						employees = HibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), s);
					}finally{
						s.close();
					}
					Collections.sort(employees, new Comparator<Employee>() {
						@Override
						public int compare(Employee arg0, Employee arg1) {
							return Collator.getInstance().compare(arg0.getFullLabel().toUpperCase(), arg1.getFullLabel().toUpperCase());
						}
					});
					return Status.OK_STATUS;
				}
			};
			j.schedule();
			
			try {
				j.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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

