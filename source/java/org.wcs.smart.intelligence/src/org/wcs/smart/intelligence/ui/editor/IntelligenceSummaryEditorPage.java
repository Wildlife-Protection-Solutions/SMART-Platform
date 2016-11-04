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
package org.wcs.smart.intelligence.ui.editor;

import java.text.DateFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.common.attachment.SmartAttachmentLabelProvider;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligenceEventManager;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.handlers.ShowInformantDataHandler;
import org.wcs.smart.intelligence.ui.panel.IntelligenceCompositeFactory.PanelType;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.OpenPatrolHandler;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.map.location.SmartPointLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.user.UserLevelManager;
/**
 * Ingeliigence editor summary page
 * @author Emily
 *
 */
public class IntelligenceSummaryEditorPage extends EditorPart {

	private Form form;
	private Text txtDateReceived;
	private Text txtSource;
	private Hyperlink lnkPatrolID;
	private Hyperlink lnkInformantID;
	private Text txtShortName;
	private Text txtDescription;
	private Text txtFromDate;
	private Text txtToDate;
	private Label lblPoints;
	private TableViewer pointsList;
	
	private TableViewer attachmentsList;
	private Label txtCreator;

	private FormToolkit toolkit ;
	
	private IntelligenceEditor parentEditor;
	
	public IntelligenceSummaryEditorPage(IntelligenceEditor parent){
		this.parentEditor = parent;
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
		toolkit = new FormToolkit(getSite().getShell().getDisplay());
		
		toolkit.setBorderStyle(SWT.BORDER);
		Composite container = toolkit.createComposite(parent, SWT.NONE);

		toolkit.paintBordersFor(container);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		form = toolkit.createForm(container);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		form.getBody().setLayout(new GridLayout(1, true));

		
		
		if (canEdit()){
			Hyperlink translateLink = toolkit.createHyperlink(form.getBody(), Messages.IntelligenceEditor_Translate_Link, SWT.WRAP);
			translateLink.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
			translateLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(
						getEditorSite().getShell(), parentEditor.getIntelligence());
					if (dialog.open() == IDialogConstants.OK_ID) {
						IntelligenceHibernateManager.saveIntelligence(parentEditor.getIntelligence());
						IntelligenceEventManager.getInstance().intelligenceChanged(0, parentEditor.getIntelligence());
					}
				}
			});
		}

		ScrolledForm main = toolkit.createScrolledForm(form.getBody());
		main.getBody().setLayout(new GridLayout(1, true));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite content = toolkit.createComposite(main.getBody(), SWT.NONE);
		GridLayout leftLayout = new GridLayout(3, false);
		leftLayout.verticalSpacing = 10;
		content.setLayout(leftLayout);
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)content.getLayout()).marginRight = 10;
	
		toolkit.createLabel(content, Messages.IntelligenceReceived_ReceivedDate_Label);
		txtDateReceived = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtDateReceived.setEditable(false);
		txtDateReceived.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, content, PanelType.RECIEVED); 

		toolkit.createLabel(content, Messages.Intelligence_Creator_Label);
		txtCreator = toolkit.createLabel(content, "", SWT.NONE); //$NON-NLS-1$
		txtCreator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		toolkit.createLabel(content, Messages.IntelligenceSource_IntelligenceSource_Label);
		txtSource = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtSource.setEditable(false);
		txtSource.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, content, PanelType.SOURCE); 

		toolkit.createLabel(content, Messages.IntelligenceSource_PatrolId_Label);
		lnkPatrolID = toolkit.createHyperlink(content, "", SWT.WRAP); //$NON-NLS-1$
		lnkPatrolID.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		lnkPatrolID.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				openPatrol(parentEditor.getIntelligence().getPatrol());
			}
		});
		toolkit.createLabel(content, ""); //$NON-NLS-1$

		toolkit.createLabel(content, Messages.IntelligenceSummaryEditorPage_Informant_Label);
		lnkInformantID = toolkit.createHyperlink(content, "", SWT.WRAP); //$NON-NLS-1$
		lnkInformantID.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		lnkInformantID.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				openInformant(parentEditor.getIntelligence().getInformant());
			}
		});
		toolkit.createLabel(content, ""); //$NON-NLS-1$
		
		toolkit.createLabel(content, Messages.IntelligenceDesc_Name_Label);
		txtShortName = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtShortName.setEditable(false);
		txtShortName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, content, PanelType.DESCRIPTION); 
		
		Label descLbl = toolkit.createLabel(content, Messages.IntelligenceDesc_Description_Label);
		descLbl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		txtDescription = toolkit.createText(content, "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		txtDescription.setEditable(false);
		txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		((GridData)txtDescription.getLayoutData()).heightHint=80;
		((GridData)txtDescription.getLayoutData()).widthHint=100;
		Hyperlink descLink = createEditLink(toolkit, content, PanelType.DESCRIPTION); 
		descLink.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		
		toolkit.createLabel(content, Messages.IntelligenceDates_From_Label);
		txtFromDate = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtFromDate.setEditable(false);
		txtFromDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, content, PanelType.DATES); 

		toolkit.createLabel(content, Messages.IntelligenceDates_To_Label);
		txtToDate = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtToDate.setEditable(false);
		txtToDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolkit.createLabel(content, ""); //$NON-NLS-1$

		lblPoints = toolkit.createLabel(content, Messages.IntelligenceLocation_Location_Label);
		lblPoints.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		Table pointsTable = toolkit.createTable(content, SWT.V_SCROLL | SWT.H_SCROLL);
		pointsList = new TableViewer(pointsTable);
		pointsList.setContentProvider(ArrayContentProvider.getInstance());
		pointsList.setLabelProvider(new SmartPointLabelProvider(parentEditor));
		pointsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		((GridData)pointsTable.getLayoutData()).heightHint = 50;
		Hyperlink locLink = createEditLink(toolkit, content, PanelType.LOCATION);
		locLink.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));

		Label attachLbl = toolkit.createLabel(content, Messages.IntelligenceAttachments_Attachments_Label);
		attachLbl .setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		Table attachTable = toolkit.createTable(content, SWT.V_SCROLL | SWT.H_SCROLL);
		attachmentsList = new TableViewer(attachTable);
		attachmentsList.setContentProvider(ArrayContentProvider.getInstance());
		attachmentsList.setLabelProvider(new SmartAttachmentLabelProvider());
		attachmentsList.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection)attachmentsList.getSelection();
				AttachmentUtil.openAttachment((ISmartAttachment) sel.getFirstElement());
			}
		});
		attachTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		((GridData)attachTable.getLayoutData()).heightHint = 50;
		Hyperlink attachLink = createEditLink(toolkit, content, PanelType.ATTACHMENTS);
		attachLink.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		
		initValues();
	}
	
	
	/**
	 * Updates the widgets with the value from the intelligence.
	 */
	public void initValues() {
		Intelligence intel = parentEditor.getIntelligence();
		setPartName(intel.getName());		
		form.setText(intel.getName());
		String none = Messages.IntelligenceEditor_NoValue;
		String value = null;
		txtDateReceived.setText(DateFormat.getDateInstance(DateFormat.LONG).format(intel.getReceivedDate()));
		txtCreator.setText(intel.getCreator() != null ? SmartLabelProvider.getFullLabel(intel.getCreator()) : ""); //$NON-NLS-1$
		txtSource.setText(intel.getSource() != null ? intel.getSource().getName() : ""); //$NON-NLS-1$
		value = intel.getPatrol() != null ? intel.getPatrol().getId() : none;
		lnkPatrolID.setText(value);
		lnkPatrolID.setEnabled(intel.getPatrol() != null);
		value = intel.getInformant() != null ? intel.getInformant().getId() : none;
		lnkInformantID.setText(value);
		lnkInformantID.setEnabled(intel.getInformant() != null);
		txtShortName.setText(intel.getName());
		txtDescription.setText(intel.getDescription());
		txtFromDate.setText(DateFormat.getDateInstance(DateFormat.LONG).format(intel.getFromDate()));
		value = intel.getToDate() != null ? DateFormat.getDateInstance(DateFormat.LONG).format(intel.getToDate()) : ""; //$NON-NLS-1$
		txtToDate.setText(value);
		if (intel.getToDate() == null){
			txtToDate.setEnabled(false);
		}
		lblPoints.setToolTipText(parentEditor.getProjection().getName());
		pointsList.getTable().setToolTipText(parentEditor.getProjection().getName());
		pointsList.setInput(intel.getPoints().toArray());
		attachmentsList.setInput(intel.getAttachments().toArray());
	}
	
	/**
	 * Creates an edit hyperlink button
	 * @param toolkit toolkit
	 * @param parent parent composite
	 * @param partEditor editor to use
	 * @return hyperlink created
	 */
	private Hyperlink createEditLink(FormToolkit toolkit, Composite parent, final PanelType panelType) {
		Hyperlink editLink = toolkit.createHyperlink(parent, DialogConstants.EDIT_LINK_TEXT, SWT.WRAP);
		
		boolean canEdit = canEdit();
		editLink.setEnabled(canEdit);
		editLink.setVisible(canEdit);
		
		if (panelType != null){
			editLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					showEditDialog(panelType);
				}
			});
		}
		return editLink;
	}
	
	private boolean canEdit() {
		//analyst users can never edit
		return SmartDB.getCurrentEmployee().supportsUser(UserLevelManager.ADMIN, UserLevelManager.MANAGER, UserLevelManager.DATA_ENTRY);
	}
	
	/**
	 * Displays and edit dialog for editing a particular item in
	 * intelligence object.
	 * 
	 * @param panelType type of inner panel to be created
	 * @return  true if changes made, false otherwise
	 */
	private boolean showEditDialog(PanelType panelType){
		
		int ret = -1;
		try {
			final EditItelligenceItemDialog editDialog = new EditItelligenceItemDialog(getEditorSite().getShell(), panelType, parentEditor.getIntelligence());
			ret = editDialog.open();
		} finally {
			ApplicationGIS.getToolManager().setCurrentEditor(parentEditor);
		}
		
		if (ret == IDialogConstants.OK_ID){
			return true;
		}
		return false;
	}

	private void openPatrol(Patrol p){
		if (p == null) return;
		MWindow activeWindow = ((IEclipseContext)getSite().getService(IEclipseContext.class)).get(MWindow.class);
		(new OpenPatrolHandler()).openPatrol(new PatrolEditorInput(p), activeWindow);
	}

	protected void openInformant(Informant i) {
		if (i == null) {
			return;
		}
		try {
			IEclipseContext context = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			IEclipseContext local = EclipseContextFactory.create();
			local.setParent(context.get(EPartService.class).getActivePart().getContext());
			local.set(Informant.class, i);
			
			ContextInjectionFactory.invoke(new ShowInformantDataHandler(), Execute.class, local);
			
		} catch (Throwable t) {
			SmartPatrolPlugIn.displayLog(t.getLocalizedMessage(), t);
		}
	}
	
	@Override
	public void setFocus() {
		txtDateReceived.setFocus();
	}

	@Override
	public void dispose(){
		super.dispose();
		if (toolkit != null){
			toolkit.dispose();
			toolkit = null;
		}
	}
}
