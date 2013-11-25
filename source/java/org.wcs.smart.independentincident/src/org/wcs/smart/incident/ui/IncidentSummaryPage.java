package org.wcs.smart.incident.ui;

import java.text.DateFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.common.attachment.SmartAttachmentLabelProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

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
		this.txtComments.setText(incident.getComment());
		this.txtIncidentId.setText(String.valueOf(incident.getId()));
		this.txtDate.setText(DateFormat.getDateInstance().format(incident.getDateTime()));
		this.txtTime.setText(DateFormat.getTimeInstance().format(incident.getDateTime()));
		this.txtLocation.setText(incident.getX() + ", " + incident.getY());
		this.txtDirection.setText(String.valueOf(incident.getDirection()));
		this.txtDistance.setText(String.valueOf(incident.getDistance()));
		this.attachments.setInput(incident.getAttachments());
		this.observationTable.setInput(incident.getObservations());
		
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
		summarySection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		summarySection.setLayout(new GridLayout(3, false));
		
		toolkit.createLabel(summarySection, "Incident ID:");
		txtIncidentId = toolkit.createText(summarySection, "");
		txtIncidentId.setEditable(false);
		createEdit(summarySection, canEdit);
		
		toolkit.createLabel(summarySection, "Date:");
		txtDate = toolkit.createText(summarySection, "");
		txtDate.setEditable(false);
		createEdit(summarySection, canEdit);
		
		toolkit.createLabel(summarySection, "Time:");
		txtTime = toolkit.createText(summarySection, "");
		txtTime.setEditable(false);
		createEdit(summarySection, canEdit);
		
		toolkit.createLabel(summarySection, "Location:");
		txtLocation = toolkit.createText(summarySection, "");
		txtLocation.setEditable(false);
		createEdit(summarySection, canEdit);

		toolkit.createLabel(summarySection, "Distance:");
		txtDistance = toolkit.createText(summarySection, "");
		txtDistance.setEditable(false);
		createEdit(summarySection, canEdit);
		
		toolkit.createLabel(summarySection, "Direction:");
		txtDirection = toolkit.createText(summarySection, "");
		txtDirection.setEditable(false);
		createEdit(summarySection, canEdit);
		
		toolkit.createLabel(summarySection, "Comments:");
		txtComments = toolkit.createText(summarySection, "", SWT.MULTI);
		txtComments.setEditable(false);
		createEdit(summarySection, canEdit);
		
		toolkit.createLabel(summarySection, "Attachments:");
		attachments = new ListViewer(summarySection);
		attachments.setContentProvider(ArrayContentProvider.getInstance());
		attachments.setLabelProvider(new SmartAttachmentLabelProvider());
		createEdit(summarySection, canEdit);
		
		toolkit.createLabel(summarySection, "Observations:");
		toolkit.createLabel(summarySection, "");
		createEdit(summarySection, canEdit);
		
		Composite observationTableComp = new Composite(summarySection, SWT.NONE);
		observationTableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		observationTableComp.setLayout(new GridLayout(1, false));
		
		observationTable = new TableViewer(observationTableComp);
		observationTable.setContentProvider(ArrayContentProvider.getInstance());
		observationTable.setLabelProvider(new ObservationTableLabelProvider());
	}
	
	private void createEdit(Composite parent, String canEdit){
		if (canEdit == null){
			toolkit.createHyperlink(parent,"edit",SWT.NONE);
		}else{
			toolkit.createLabel(parent, "");
		}
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}
	
	private class ObservationTableLabelProvider extends LabelProvider implements ITableLabelProvider{

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (columnIndex == 0){
				return ((WaypointObservation)element).getCategory().getFullCategoryName();
			}else if (columnIndex == 1){
				StringBuilder sb = new StringBuilder();
				for (WaypointObservationAttribute att : ((WaypointObservation)element).getAttributes()){
					sb.append(att.getAttribute().getName());
					sb.append("=");
					sb.append(att.getAttributeValueAsString());
				}
				return sb.toString();
			}
			return null;
		}
		
	}

}

