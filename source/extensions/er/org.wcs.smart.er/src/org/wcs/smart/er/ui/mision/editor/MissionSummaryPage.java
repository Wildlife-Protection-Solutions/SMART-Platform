package org.wcs.smart.er.ui.mision.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionPropertyValue;

public class MissionSummaryPage extends EditorPart {

	private MissionEditor missionEditor;

	private Text txtSurveyId;
	private Text txtComment;
	private Text txtId;
	private ListViewer lstMembers;
	private TableViewer tblProperties;
	private Form form;
	
	public MissionSummaryPage(MissionEditor missionEditor){
		this.missionEditor = missionEditor;
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	
	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		form = toolkit.createForm(parent);
		form.getBody().setLayout(new GridLayout());
		
		Section section = toolkit.createSection(form.getBody(), Section.TITLE_BAR);
		section.setText("Summary");
		section.setLayout(new GridLayout());
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite comp = toolkit.createComposite(section, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		section.setClient(comp);
		
		Composite left = toolkit.createComposite(comp, SWT.NONE);
		left.setLayout(new GridLayout(3, false));
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		//mission id
		toolkit.createLabel(left, "Mission ID:");
		txtId = toolkit.createText(left, "");
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtId.setEditable(false);
		Hyperlink edit = toolkit.createHyperlink(left, "edit...", SWT.NONE);
		edit.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));

		//survey id
		toolkit.createLabel(left, "Survey ID:");
		txtSurveyId = toolkit.createText(left, "");
		txtSurveyId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtSurveyId.setEditable(false);
		edit = toolkit.createHyperlink(left, "edit...", SWT.NONE);
		edit.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		
		//members id
		Label l = toolkit.createLabel(left, "Members:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		lstMembers = new ListViewer(left, SWT.BORDER);
		lstMembers.setContentProvider(ArrayContentProvider.getInstance());
		lstMembers.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof MissionMember){
					return ((MissionMember)element).getMember().getFullLabel();
				}
				return super.getText(element);
			}
		});
		lstMembers.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		edit = toolkit.createHyperlink(left, "edit...", SWT.NONE);
		edit.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		
		Composite right = toolkit.createComposite(comp, SWT.NONE);
		right.setLayout(new GridLayout(3, false));
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//comments
		l = toolkit.createLabel(right, "Comments:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		txtComment = toolkit.createText(right, "", SWT.MULTI);
		txtComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtComment.setEditable(false);
		edit = toolkit.createHyperlink(right, "edit...", SWT.NONE);
		edit.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));


		//mission properties
		Section propSection = toolkit.createSection(form.getBody(), Section.TITLE_BAR);
		propSection.setText("Properties");
		propSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite prop = toolkit.createComposite(propSection);
		prop.setLayout(new GridLayout());
		prop.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		propSection.setClient(prop);
		
		toolkit.createLabel(propSection, "Mission Properties");
		
		tblProperties = new TableViewer(prop, SWT.FULL_SELECTION | SWT.BORDER);
		tblProperties.setContentProvider(ArrayContentProvider.getInstance());
		tblProperties.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tblProperties.getTable().getLayoutData()).heightHint = 150;
		tblProperties.getTable().setHeaderVisible(true);
		tblProperties.getTable().setLinesVisible(true);
		
		TableViewerColumn nameColumn = new TableViewerColumn(tblProperties, SWT.NONE);
		nameColumn.getColumn().setResizable(true);
		nameColumn.getColumn().setText("Property");
		nameColumn.getColumn().setWidth(100);
		nameColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MissionPropertyValue){
					return ((MissionPropertyValue) element).getMissionAttribute().getName();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn valueColumn = new TableViewerColumn(tblProperties, SWT.NONE);
		valueColumn.getColumn().setResizable(true);
		valueColumn.getColumn().setText("Value");
		valueColumn.getColumn().setWidth(100);
		valueColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MissionPropertyValue){
					return ((MissionPropertyValue) element).getValueAsString();
				}
				return super.getText(element);
			}
		});
		
		initControls(missionEditor.getMission());
	}
	
	private void initControls(Mission mission){
		form.setText("Mission: " + mission.getId());
		txtSurveyId.setText(mission.getSurvey().getId());
		txtComment.setText(mission.getComment() == null ? "" : mission.getComment());
		txtId.setText(mission.getId());
		lstMembers.setInput(mission.getMembers());
		tblProperties.setInput(mission.getMissionPropertyValues());
	}

	@Override
	public void setFocus() {
		txtSurveyId.setFocus();
	}
}
