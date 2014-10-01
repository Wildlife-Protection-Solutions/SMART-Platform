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
package org.wcs.smart.er.ui.mision.editor;

import java.text.DateFormat;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ColumnLayoutData;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.ui.mision.CommentComposite;
import org.wcs.smart.er.ui.mision.DateComposite;
import org.wcs.smart.er.ui.mision.IdComposite;
import org.wcs.smart.er.ui.mision.MissionComposite;
import org.wcs.smart.er.ui.mision.MissionEmployeeComposite;
import org.wcs.smart.er.ui.mision.MissionPropertyValuesComposite;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Mission editor summary page.
 * @author Emily
 *
 */
public class MissionSummaryPage extends EditorPart implements IHyperlinkListener{

	private MissionEditor missionEditor;

	private Text txtSurveyId;
	private Text txtComment;
	private Text txtId;
	private Text txtStart;
	private Text txtEnd;
	private TableViewer lstMembers;
	private TableViewer tblProperties;
	private Form form;
	
	public MissionSummaryPage(MissionEditor missionEditor){
		this.missionEditor = missionEditor;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	
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

	
	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		form = toolkit.createForm(parent);
		form.getBody().setLayout(new GridLayout());
		
		String errorMsg = missionEditor.canEdit();
		boolean canEdit = errorMsg == null;
		if (!canEdit){
			missionEditor.createEditWarning(errorMsg, form.getBody(), toolkit);
		}
		
		Section section = toolkit.createSection(form.getBody(), Section.TITLE_BAR);
		section.setText("Summary");
		section.setLayout(new GridLayout());
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		ScrolledComposite scrolltop = new ScrolledComposite(section, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolltop.setLayout(new GridLayout());
		scrolltop.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolltop.setExpandHorizontal(true);
		scrolltop.setExpandVertical(true);
		
		Composite comp = toolkit.createComposite(scrolltop, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		section.setClient(scrolltop);
		scrolltop.setContent(comp);
		
		Composite left = toolkit.createComposite(comp, SWT.NONE);
		left.setLayout(new GridLayout(3, false));
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		//mission id
		toolkit.createLabel(left, "Mission ID:");
		txtId = toolkit.createText(left, "");
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtId.setEditable(false);
		if (canEdit){
			Hyperlink edit = toolkit.createHyperlink(left, "edit...", SWT.NONE);
			edit.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
			edit.setData(IdComposite.class);
			edit.addHyperlinkListener(this);
		}else{
			new Label(left, SWT.NONE);
		}
		
		//survey id
		toolkit.createLabel(left, "Survey ID:");
		txtSurveyId = toolkit.createText(left, "");
		txtSurveyId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtSurveyId.setEditable(false);
		new Label(left, SWT.NONE);
		
		
		//members id
		Label l = toolkit.createLabel(left, "Members:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		lstMembers = new TableViewer(left, SWT.BORDER);
		lstMembers.setContentProvider(ArrayContentProvider.getInstance());
		lstMembers.setLabelProvider(new LabelProvider(){
			@Override
			public Image getImage(Object element){
				if (element instanceof MissionMember){
					if (((MissionMember) element).getIsLeader()){
						return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.MISSION_LEADER_ICON);
					}
					return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.MISSION_MEMBER_ICON);
				}
				return null;
			}
			
			@Override
			public String getText(Object element){
				if (element instanceof MissionMember){
					if (((MissionMember) element).getIsLeader()){
						return MessageFormat.format("[Leader] {0}", new Object[]{((MissionMember)element).getMember().getFullLabel()});
					}
					return ((MissionMember)element).getMember().getFullLabel();
				}
				return super.getText(element);
			}
		});
		lstMembers.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (canEdit){
			Hyperlink edit = toolkit.createHyperlink(left, "edit...", SWT.NONE);
			edit.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
			edit.setData(MissionEmployeeComposite.class);
			edit.addHyperlinkListener(this);
		}else{
			new Label(left, SWT.NONE);
		}
		
		Composite right = toolkit.createComposite(comp, SWT.NONE);
		right.setLayout(new GridLayout(3, false));
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//comments
		l = toolkit.createLabel(right, "Comments:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		txtComment = toolkit.createText(right, "", SWT.MULTI);
		txtComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtComment.setEditable(false);
		if (canEdit){
			Hyperlink edit = toolkit.createHyperlink(right, "edit...", SWT.NONE);
			edit.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
			edit.setData(CommentComposite.class);
			edit.addHyperlinkListener(this);
		}else{
			new Label(right, SWT.NONE);
		}
		
		Composite prop = toolkit.createComposite(comp);
		prop.setLayout(new GridLayout(3, false));
		prop.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Label mpl = toolkit.createLabel(prop, "Mission Properties:");
		mpl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		Composite tableComp = new Composite(prop, SWT.NONE);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TableColumnLayout tLayout = new TableColumnLayout();
		tableComp.setLayout(tLayout);
		tblProperties = new TableViewer(tableComp, SWT.FULL_SELECTION | SWT.BORDER);
		tblProperties.setContentProvider(ArrayContentProvider.getInstance());
		
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
		tLayout.setColumnData(nameColumn.getColumn(), new ColumnWeightData(25));
		
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
		tLayout.setColumnData(valueColumn.getColumn(), new ColumnWeightData(75));
		
		if (canEdit){
			Hyperlink edit = toolkit.createHyperlink(prop, "edit...", SWT.NONE);
			edit.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
			edit.setData(MissionPropertyValuesComposite.class);
			edit.addHyperlinkListener(this);
		}else{
			new Label(prop, SWT.NONE);
		}
	
		//data section
		Section dataSection = toolkit.createSection(form.getBody(), Section.TITLE_BAR);
		dataSection.setText("Mission Data");
		dataSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				
		Composite dataProp = toolkit.createComposite(dataSection);
		dataProp.setLayout(new GridLayout(5, false));
		dataProp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataSection.setClient(dataProp);
				
		toolkit.createLabel(dataProp,  "Start Date:");
		txtStart = toolkit.createText(dataProp, "");
		txtStart.setEditable(false);
		txtStart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
		toolkit.createLabel(dataProp,  "End Date:");
		txtEnd = toolkit.createText(dataProp, "");
		txtEnd.setEditable(false);
		txtEnd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		if (canEdit){
			Hyperlink edit = toolkit.createHyperlink(dataProp, "edit...", SWT.NONE);
			edit.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
			edit.setData(DateComposite.class);
			edit.addHyperlinkListener(this);
		}else{
			new Label(dataProp, SWT.NONE);
		}
		
		
		Point p = comp.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scrolltop.setMinSize(p.x, p.y+20);
		
		initControls();
	}
	
	
	public void initControls() {
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			Mission mission = missionEditor.getMission();
			session.update(mission);

			form.setText("Mission: " + mission.getId());
			txtSurveyId.setText(mission.getSurvey().getId());
			txtComment.setText(mission.getComment() == null ? "" : mission.getComment()); //$NON-NLS-1$
			txtId.setText(mission.getId());
			lstMembers.setInput(mission.getMembers());
			tblProperties.setInput(mission.getMissionPropertyValues());
			
			txtStart.setText(DateFormat.getDateInstance().format(mission.getStartDate()));
			txtEnd.setText(DateFormat.getDateInstance().format(mission.getEndDate()));
		}catch (Exception ex){
			EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
		} finally {
			session.getTransaction().rollback();
			session.close();
		}
	}

	@Override
	public void setFocus() {
		txtSurveyId.setFocus();
	}
	
	private void editComponent(MissionComposite component){
//		missionEditor.fireEventtest();
		MissionEditorDialog dialog = new MissionEditorDialog(getSite().getShell(), component, missionEditor.getMission(), missionEditor);
		dialog.open();
	}


	@Override
	public void linkEntered(HyperlinkEvent e) {
		
	}

	@Override
	public void linkExited(HyperlinkEvent e) {
	}

	@Override
	public void linkActivated(HyperlinkEvent e) {
		Object x = e.widget.getData();
		if (x == null) return;
		if ( !(x instanceof Class)) return;
		
		Object component = null;
		try {
			component = ((Class)x).newInstance();
		} catch (Exception ex) {
			EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
		}
		if (component != null && component instanceof MissionComposite){
			editComponent((MissionComposite)component);
		}
		
	}
}
