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
package org.wcs.smart.er.ui.surveydesign.editor;

import java.text.DateFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignProperty;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignCompositeFactory.PanelType;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Survey Design Summary Editor Page
 * @author elitvin
 * @since 3.0.0
 */
public class SurveyDesignSummaryEditorPage extends EditorPart {

	private Form form;
	private Text txtName;
	private Text txtStartDate;
	private Text txtEndDate;
	private Text txtStatus;
	private Text txtKey;
	private Text txtDescription;
	private Text txtConfigurableModel;
	private Button btnDistance;
	private TableViewer missionPropertiesList;
	private TableViewer propertiesList;
	
	private FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	
	private SurveyDesignEditor parentEditor;
	
	public SurveyDesignSummaryEditorPage(SurveyDesignEditor parent) {
		this.parentEditor = parent;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		toolkit.setBorderStyle(SWT.BORDER);
		form = toolkit.createForm(parent);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		GridLayout glayout = new GridLayout();
		glayout.verticalSpacing = 0;
		glayout.marginHeight = 0;
		form.getBody().setLayout(glayout);

//		if (canEdit()) {
			Hyperlink translateLink = toolkit.createHyperlink(form.getBody(), Messages.SurveyDesignSummaryEditorPage_TranslateLink, SWT.WRAP);
			translateLink.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
			translateLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					translateName();
				}
			});
//		}
		
		Section summarySection = toolkit.createSection(form.getBody(), Section.TITLE_BAR);
		summarySection.setLayout(new GridLayout());
		summarySection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		summarySection.setText(Messages.SurveyDesignSummaryEditorPage_DesignSectionLabel);
		
		ScrolledForm main = toolkit.createScrolledForm(summarySection);
		main.getBody().setLayout(new GridLayout(1, true));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		summarySection.setClient(main);
		
		Composite content = toolkit.createComposite(main.getBody(), SWT.NONE);
		GridLayout contentLayout = new GridLayout(6, false);
		contentLayout.verticalSpacing = 10;
		content.setLayout(contentLayout);
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)content.getLayout()).marginRight = 10;

		toolkit.createLabel(content, Messages.SurveyDesignSummaryEditorPage_Name);
		txtName = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtName.setEditable(false);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		Hyperlink editLink = createLink(content);
		editLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				translateName();
			}
		});
		
		
		toolkit.createLabel(content, Messages.SurveyDesignSummaryEditorPage_Status);
		txtStatus = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtStatus.setEditable(false);
		txtStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(content, PanelType.STATUS); 

		toolkit.createLabel(content, Messages.SurveyDesignSummaryEditorPage_StartDate);
		txtStartDate = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtStartDate.setEditable(false);
		txtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolkit.createLabel(content, ""); //$NON-NLS-1$

		toolkit.createLabel(content, Messages.SurveyDesignSummaryEditorPage_Key);
		txtKey = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtKey.setEditable(false);
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(content, PanelType.NAME);

		toolkit.createLabel(content, Messages.SurveyDesignSummaryEditorPage_EndDate);
		txtEndDate = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtEndDate.setEditable(false);
		txtEndDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(content, PanelType.DATES); 

		Label emptySpace = toolkit.createLabel(content, ""); //$NON-NLS-1$
		emptySpace.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		toolkit.createLabel(content, Messages.SurveyDesignSummaryEditorPage_ConfigurableModel);
		txtConfigurableModel = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtConfigurableModel.setEditable(false);
		txtConfigurableModel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(content, PanelType.MODEL);

		emptySpace = toolkit.createLabel(content, ""); //$NON-NLS-1$
		emptySpace.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		toolkit.createLabel(content, SmartUtils.formatStringForLabel(Messages.SurveyDesignSummaryEditorPage_DistanceDirectionLabel));
		btnDistance = toolkit.createButton(content, "", SWT.CHECK); //$NON-NLS-1$
		btnDistance.setEnabled(false);
		btnDistance .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(content, PanelType.DISTANCE);
		
		emptySpace = toolkit.createLabel(content, ""); //$NON-NLS-1$
		emptySpace.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		Label l = toolkit.createLabel(content, Messages.SurveyDesignSummaryEditorPage_Description);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		txtDescription = toolkit.createText(content, "", SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		txtDescription.setEditable(false);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1);
		gd.heightHint = 40;
		gd.widthHint = 100;
		txtDescription.setLayoutData(gd);
		Hyperlink lnk = createEditLink(content, PanelType.DESCRIPTION);
		lnk.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		
		
		l = toolkit.createLabel(content, Messages.SurveyDesignSummaryEditorPage_Properties);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		Composite propertiesTableCmp = new Composite(content, SWT.NONE);
		propertiesTableCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		((GridData)propertiesTableCmp.getLayoutData()).widthHint = 60;
		TableColumnLayout tableLayout = new TableColumnLayout();
		propertiesTableCmp.setLayout(tableLayout);
		Table propertiesTable = toolkit.createTable(propertiesTableCmp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		propertiesTable.setHeaderVisible(true);
		propertiesTable.setLinesVisible(true);
		propertiesList = new TableViewer(propertiesTable);
		propertiesList.setContentProvider(ArrayContentProvider.getInstance());
		createPropertyColumns(propertiesList);
		propertiesTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)propertiesTable.getLayoutData()).minimumHeight = 60;
		Hyperlink pLink = createEditLink(content, PanelType.PROPERTIES);
		pLink.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		
		Section missionSection = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED);
		missionSection.setText(Messages.SurveyDesignSummaryEditorPage_PropertiesSectionLabel);
		missionSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite missionComp = toolkit.createComposite(missionSection);
		missionSection.setClient(missionComp);
		missionComp.setLayout(new GridLayout(2, false));
		
		l = toolkit.createLabel(missionComp, Messages.SurveyDesignSummaryEditorPage_PropertiesDescription, SWT.WRAP);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Table missionPropertiesTable = toolkit.createTable(missionComp, SWT.V_SCROLL | SWT.H_SCROLL);
		missionPropertiesList = new TableViewer(missionPropertiesTable);
		missionPropertiesList.setContentProvider(ArrayContentProvider.getInstance());
		missionPropertiesList.setLabelProvider(new MissionPropertyLabelProvider());
		missionPropertiesTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)missionPropertiesTable.getLayoutData()).minimumHeight = 60;
		((GridData)missionPropertiesTable.getLayoutData()).widthHint = 60;
		Hyperlink locLink = createEditLink(missionComp, PanelType.MISSION_PROPERTIES);
		locLink.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		
		initValues();
	}

	private void createPropertyColumns(TableViewer viewer) {
		final TableViewerColumn colName = createTableViewerColumn(viewer, Messages.SurveyDesignSummaryEditorPage_NameColumn, 180);
		colName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof SurveyDesignProperty) {
					SurveyDesignProperty p = (SurveyDesignProperty) element;
					return p.getName();
				}
				return super.getText(element);
			}
		});

		final TableViewerColumn colValue = createTableViewerColumn(viewer, Messages.SurveyDesignSummaryEditorPage_ValueColumn, 180);
		colValue.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof SurveyDesignProperty) {
					SurveyDesignProperty p = (SurveyDesignProperty) element;
					return p.getValue();
				}
				return super.getText(element);
			}
		});
	}

	private TableViewerColumn createTableViewerColumn(TableViewer viewer, String title, int weight) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setResizable(true);
		column.setMoveable(true);

		TableColumnLayout layout = (TableColumnLayout) viewer.getTable() .getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(weight, ColumnWeightData.MINIMUM_WIDTH, true));

		return viewerColumn;
	}
	
	/**
	 * Updates the widgets with the value from the intelligence.
	 */
	public void initValues() {
		SurveyDesign design = parentEditor.getSurveyDesign();
		setPartName(design.getName() );		
		form.setText(design.getName() + ": " + Messages.SurveyDesignSummaryEditorPage_SummaryPageLabel); //$NON-NLS-1$
		
		txtName.setText(design.getName());
		
		String value = design.getStartDate() != null ? DateFormat.getDateInstance(DateFormat.LONG).format(design.getStartDate()) : ""; //$NON-NLS-1$
		txtStartDate.setText(value);

		value = design.getEndDate() != null ? DateFormat.getDateInstance(DateFormat.LONG).format(design.getEndDate()) : ""; //$NON-NLS-1$
		txtEndDate.setText(value);

		txtStatus.setText(design.getState().getGuiName());
		txtKey.setText(design.getKeyId());

		txtDescription.setText(design.getDescription() != null ? design.getDescription() : ""); //$NON-NLS-1$
		
		if (design.getConfigurableModel() != null) {
			txtConfigurableModel.setText(design.getConfigurableModel().getName());
		} else {
			txtConfigurableModel.setText(Messages.ConfigurableModelComposite_DataModel);
		}
		btnDistance.setSelection(design.getTrackDistanceDirection());
		
		missionPropertiesList.setInput(design.getMissionProperties().toArray());
		propertiesList.setInput(design.getProperties().toArray());

	}
	
	private Hyperlink createLink(Composite parent){
		Hyperlink editLink = toolkit.createHyperlink(parent, DialogConstants.EDIT_LINK_TEXT, SWT.WRAP);
		
	//		if (!this.parentEditor.canEdit()) {
	//			editLink.setEnabled(false);
	//				editLink.setVisible(false);
	//			}
		
		return editLink;
	}
	
	
	private Hyperlink createEditLink(Composite parent, final PanelType panelType) {
		Hyperlink editLink = createLink(parent);
		if (panelType != null) {
			editLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					showEditDialog(panelType);
				}
			});
		}
		return editLink;
	}

	protected void showEditDialog(PanelType panelType) {
		final EditSurveyDesignItemDialog editDialog = new EditSurveyDesignItemDialog(getEditorSite().getShell(), panelType, parentEditor.getSurveyDesign());
		editDialog.open();
	}

	@Override
	public void setFocus() {
		txtName.setFocus();
	}

	@Override
	public void dispose(){
		super.dispose();
		if (toolkit != null){
			toolkit.dispose();
			toolkit = null;
		}
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
	}

	@Override
	public void doSaveAs() {
		// not allowed
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	/*
	 * Opens the translate names dialog for modifying
	 * the entity type name.
	 */
	private void translateName() {
//		if (!EntityPermissionManager.canCreateEditDeleteTypes()) return;
		final SurveyDesign toEdit = parentEditor.getSurveyDesign();
		
		TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(
				getEditorSite().getShell(), toEdit);
		
		if (dialog.open() == IDialogConstants.OK_ID) {
			SaveSurveyDesignJob saveJob = new SaveSurveyDesignJob(toEdit);    
	    	saveJob.schedule();
	    	try {
				saveJob.join(); //we don't want to close editor if save failed
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

	    	if (Status.OK_STATUS.equals(saveJob.getResult())) {
				SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_MODIFIED, toEdit);
	    	}
		}
	}
}
