package org.wcs.smart.incident.ui;

import java.text.DateFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.SmartAttachmentLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.ui.newwizard.CommentComposite;
import org.wcs.smart.incident.ui.newwizard.DateTimeComposite;
import org.wcs.smart.incident.ui.newwizard.DistanceDirectionComposite;
import org.wcs.smart.incident.ui.newwizard.EditIncidentDialog;
import org.wcs.smart.incident.ui.newwizard.IdComposite;
import org.wcs.smart.incident.ui.newwizard.IncidentAttachmentComposite;
import org.wcs.smart.incident.ui.newwizard.LocationComposite;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.ui.input.ObservationWizard;
import org.wcs.smart.observation.ui.input.ObservationWizardDialog;

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
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void initData(Waypoint incident){
		Session session = HibernateManager.openSession();
		session.saveOrUpdate(editor.getIncident());
		try{
			if (incident.getComment() == null){
				txtComments.setText("");
			}else{
				this.txtComments.setText(incident.getComment());
			}
		
			this.txtIncidentId.setText(String.valueOf(incident.getId()));
			this.txtDate.setText(DateFormat.getDateInstance().format(incident.getDateTime()));
			this.txtTime.setText(DateFormat.getTimeInstance().format(incident.getDateTime()));
			this.txtLocation.setText(incident.getX() + ", " + incident.getY());
		
			if (incident.getDirection() == null){
				this.txtDirection.setText("");
			}else{
				this.txtDirection.setText(String.valueOf(incident.getDirection()));
			}
		
			if (incident.getDistance() == null){
				this.txtDistance.setText("");
			}else{
				this.txtDistance.setText(String.valueOf(incident.getDistance()));
			}		
		
			this.attachments.setInput(incident.getAttachments());
			this.observationTable.setInput(incident.getObservations());
		}finally{
			session.close();
		}
		
	}
	@Override
	public void createPartControl(Composite parent) {
		Form frmSummary = toolkit.createForm(parent);
		frmSummary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		GridLayout layout = new GridLayout(1, true);
		frmSummary.getBody().setLayout(layout);
		
		String canEdit = editor.canEdit();
		if (canEdit != null){
//			Composite warning = toolkit.createComposite(frmPatrolSummary.getBody());
//			warning.setLayout(new GridLayout(2, false));
//			Label lblImage = toolkit.createLabel(warning, null, SWT.NONE);
//			Image x = editor.getSite().getWorkbenchWindow().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
//			lblImage.setImage(x);
//			Label lblWarning = toolkit.createLabel(warning, "", SWT.NONE); //$NON-NLS-1$
//			lblWarning.setText(MessageFormat.format(Messages.PatrolSummaryEditor_Error_CannotEdit, new Object[]{ canEdit }));
		}
		
		Section summarySection = toolkit.createSection(frmSummary.getBody(), Section.TITLE_BAR   );
		summarySection.setText("Incident Summary");
		summarySection.setDescription("Description of incident.");
		summarySection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite top = toolkit.createComposite(summarySection, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		summarySection.setClient(top);
		
		Composite left= toolkit.createComposite(top, SWT.NONE);
		left.setLayout(new GridLayout(3, false));
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite right = toolkit.createComposite(top, SWT.NONE);
		right.setLayout(new GridLayout(3, false));
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		toolkit.createLabel(left, "Incident ID:");
		txtIncidentId = toolkit.createText(left, "");
		txtIncidentId.setEditable(false);
		txtIncidentId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createEdit(left, canEdit, IdComposite.ID);
		
		toolkit.createLabel(left, "Date:");
		txtDate = toolkit.createText(left, "");
		txtDate.setEditable(false);
		txtDate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createEdit(left, canEdit, DateTimeComposite.ID);
		
		toolkit.createLabel(left, "Time:");
		txtTime = toolkit.createText(left, "");
		txtTime.setEditable(false);
		txtTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createEdit(left, canEdit, DateTimeComposite.ID);
		
		toolkit.createLabel(left, "Location:");
		txtLocation = toolkit.createText(left, "");
		txtLocation.setEditable(false);
		txtLocation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createEdit(left, canEdit, LocationComposite.ID);

		toolkit.createLabel(left, "Distance:");
		txtDistance = toolkit.createText(left, "");
		txtDistance.setEditable(false);
		txtDistance.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createEdit(left, canEdit, DistanceDirectionComposite.ID);
		
		toolkit.createLabel(left, "Direction:");
		txtDirection = toolkit.createText(left, "");
		txtDirection.setEditable(false);
		txtDirection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createEdit(left, canEdit, DistanceDirectionComposite.ID);
		
		Label l = toolkit.createLabel(right, "Comments:");
		l.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		txtComments = toolkit.createText(right, "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		txtComments.setEditable(false);
		txtComments.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)txtComments.getLayoutData()).heightHint = 100;
		((GridData)txtComments.getLayoutData()).widthHint = 100;
		createEdit(right, canEdit, CommentComposite.ID);
		
		l = toolkit.createLabel(right, "Attachments:");
		l.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		attachments = new ListViewer(right);
		attachments.setContentProvider(ArrayContentProvider.getInstance());
		attachments.setLabelProvider(new SmartAttachmentLabelProvider());
		attachments.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createEdit(right, canEdit, IncidentAttachmentComposite.ID);
		
		
		Section observationSection = toolkit.createSection(frmSummary.getBody(), Section.TITLE_BAR   );
		observationSection.setText("Observations");
		observationSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite observationTableComp = toolkit.createComposite(observationSection);
		observationTableComp.setLayout(new GridLayout(1, false));
		observationSection.setClient(observationTableComp);
		
		
		observationTable = new TableViewer(observationTableComp, SWT.FULL_SELECTION | SWT.BORDER);
		observationTable.setContentProvider(ArrayContentProvider.getInstance());

		observationTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		observationTable.getTable().setLinesVisible(true);
		observationTable.getTable().setHeaderVisible(true);
		
		editor.getSite().setSelectionProvider(observationTable);
		
		TableViewerColumn colFirstName = new TableViewerColumn(observationTable, SWT.NONE);
		colFirstName.getColumn().setWidth(200);
		colFirstName.getColumn().setText("Category");
		colFirstName.setLabelProvider(new ColumnLabelProvider() {
		  @Override
		  public String getText(Object element) {
		    WaypointObservation p = (WaypointObservation) element;
		    return p.getCategory().getFullCategoryName();
		  }
		});
		
		TableViewerColumn colAttributes = new TableViewerColumn(observationTable, SWT.NONE);
		colAttributes.getColumn().setWidth(200);
		colAttributes.getColumn().setText("Attributes");
		colAttributes.setLabelProvider(new ColumnLabelProvider() {
		  @Override
		  public String getText(Object element) {
		    WaypointObservation p = (WaypointObservation) element;
		    StringBuilder sb = new StringBuilder();
			for (WaypointObservationAttribute att : ((WaypointObservation)element).getAttributes()){
				sb.append(att.getAttribute().getName());
				sb.append("=");
				sb.append(att.getAttributeValueAsString());
				sb.append("   ");
			}
			return sb.toString();
		  }
		});
//		createEdit(observationTableComp, canEdit);
		
		if (canEdit == null){
			Button btnEdit = toolkit.createButton(observationTableComp, "Edit Observations", SWT.PUSH);
			btnEdit.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					ObservationWizardDialog wd = new ObservationWizardDialog(getEditorSite().getShell(),
							new ObservationWizard(editor.getIncident()));
					wd.open();
				}
			});
			
		}
		
		initData(editor.getIncident());
	}
	
	private void createEdit(Composite parent, String canEdit, final String panelId){
		if (canEdit == null){
			Hyperlink l = toolkit.createHyperlink(parent,"edit",SWT.NONE);
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
			l.addHyperlinkListener(new IHyperlinkListener() {
				@Override
				public void linkExited(HyperlinkEvent e) {	}
				
				@Override
				public void linkEntered(HyperlinkEvent e) {	}
				
				@Override
				public void linkActivated(HyperlinkEvent e) {
					// TODO Auto-generated method stub
					EditIncidentDialog d = new EditIncidentDialog(getSite().getShell(), panelId, editor.getIncident());
					d.open();
					initData(editor.getIncident());
				}
			});
		}else{
			toolkit.createLabel(parent, "");
		}
	}

	@Override
	public void setFocus() {
		txtIncidentId.setFocus();
	}
	
	

}

