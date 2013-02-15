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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.attachment.SmartAttachmentLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligenceEventManager;
import org.wcs.smart.intelligence.IntelligenceEventManager.EventType;
import org.wcs.smart.intelligence.IntelligenceEventManager.IIntelligenceEventListener;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.panel.IntelligenceCompositeFactory.PanelType;
import org.wcs.smart.ui.map.location.SmartPointLabelProvider;

/**
 * The Intelligence Editor
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceEditor extends EditorPart {

	public static final String ID = "org.wcs.smart.intelligence.IntelligenceEditor"; //$NON-NLS-1$

	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private Intelligence intelligence;
	
	private boolean isDirty;
	private Form form;

	private Text txtDateReceived;
	private Text txtSource;
	private Text txtPatrolID;
	private Text txtShortName;
	private Text txtDescription;
	private Text txtFromDate;
	private Text txtToDate;
	private TableViewer pointsList;
	private TableViewer attachmentsList;

	/**
	 * listener for intelligence change events.
	 */
	private IIntelligenceEventListener intelligenceListener = new IIntelligenceEventListener(){
		@Override
		public void eventFired(int type, Intelligence source) {
			initValues();
		}
	};

	/**
	 * listener for intelligence delete events.
	 */
	private IIntelligenceEventListener deleteListener = new IIntelligenceEventListener(){
		@Override
		public void eventFired(int type, Intelligence source) {
			if (source.equals(IntelligenceEditor.this.intelligence)) {
				//close this editor
				IntelligenceEditor.this.getEditorSite().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						IntelligenceEditor.this.getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(IntelligenceEditor.this, false);					
					}
				});
			}
		}
	};
	
	/**
	 * Default constructor
	 */
	public IntelligenceEditor() {
		super();
		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_MODIFIED, intelligenceListener);
		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_DELETED, deleteListener);
	}

	@Override
	public void dispose() {
		IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_MODIFIED, intelligenceListener);
		IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_DELETED, deleteListener);
		super.dispose();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof IntelligenceEditorInput)) {
			throw new IllegalArgumentException("Invalid editor input."); //$NON-NLS-1$
		}
		setSite(site);
		setInput(input);
	}

	public void setDirty(boolean isDirty){
		this.isDirty = isDirty;
	}
	
	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite container = toolkit.createComposite(parent, SWT.NONE);

		toolkit.paintBordersFor(container);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		form = toolkit.createForm(container);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		form.getBody().setLayout(new GridLayout(1, true));

		Composite content = toolkit.createComposite(form.getBody(), SWT.NONE);
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

		toolkit.createLabel(content, Messages.IntelligenceSource_IntelligenceSource_Label);
		txtSource = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtSource.setEditable(false);
		txtSource.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, content, PanelType.SOURCE); 

		toolkit.createLabel(content, Messages.IntelligenceSource_PatrolId_Label);
		txtPatrolID = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtPatrolID.setEditable(false);
		txtPatrolID.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
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
//		((GridData)txtDescription.getLayoutData()).widthHint=100;
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

		Label locLbl = toolkit.createLabel(content, Messages.IntelligenceLocation_Location_Label);
		locLbl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		Table pointsTable = toolkit.createTable(content, SWT.V_SCROLL | SWT.H_SCROLL);
		pointsList = new TableViewer(pointsTable);
		pointsList.setContentProvider(ArrayContentProvider.getInstance());
		pointsList.setLabelProvider(new SmartPointLabelProvider());
		pointsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		Hyperlink locLink = createEditLink(toolkit, content, PanelType.LOCATION);
		locLink.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));

		Label attachLbl = toolkit.createLabel(content, Messages.IntelligenceAttachments_Attachments_Label);
		attachLbl .setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		Table attachTable = toolkit.createTable(content, SWT.V_SCROLL | SWT.H_SCROLL);
		attachmentsList = new TableViewer(attachTable);
		attachmentsList.setContentProvider(ArrayContentProvider.getInstance());
		attachmentsList.setLabelProvider(new SmartAttachmentLabelProvider());
		attachTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		Hyperlink attachLink = createEditLink(toolkit, content, PanelType.ATTACHMENTS);
		attachLink.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		
		initValues();
	}

	/**
	 * Updates the widgets with the value from the intelligence.
	 */
	private void initValues() {
		Intelligence intel = getIntelligence();
		setPartName(intel.getShortName());		
		form.setText(intel.getShortName());
		String none = Messages.IntelligenceEditor_NoValue;
		String value = null;
		txtDateReceived.setText(DateFormat.getDateInstance(DateFormat.LONG).format(intel.getReceivedDate()));
		txtSource.setText(intel.getSource().getName());
		value = intel.getPatrol() != null ? intel.getPatrol().getId() : none;
		txtPatrolID.setText(value);
		txtShortName.setText(intel.getShortName());
		txtDescription.setText(intel.getDescription());
		txtFromDate.setText(DateFormat.getDateInstance(DateFormat.LONG).format(intel.getFromDate()));
		value = intel.getToDate() != null ? DateFormat.getDateInstance(DateFormat.LONG).format(intel.getToDate()) : txtFromDate.getText();
		txtToDate.setText(value);
		pointsList.setInput(intel.getPoints().toArray());
		attachmentsList.setInput(intel.getAttachments().toArray());
	}

	public Intelligence getIntelligence(){
		if (intelligence == null){
			byte[] puuid = ((IntelligenceEditorInput) getEditorInput()).getUuid();
			Session session = HibernateManager.openSession();
			//load patrol items so don't have lazy loading issues later.
			session.beginTransaction();
			intelligence = (Intelligence) session.load(Intelligence.class, puuid);
			if (intelligence.getPatrol() != null) {
				intelligence.getPatrol().getId();
			}
			intelligence.getPoints().size();
			intelligence.getAttachments().size();
			session.getTransaction().commit();
			session.close();
		}
		return intelligence;
	}

	/**
	 * Creates an edit hyperlink button
	 * @param toolkit toolkit
	 * @param parent parent composite
	 * @param partEditor editor to use
	 * @return hyperlink created
	 */
	private Hyperlink createEditLink(FormToolkit toolkit, Composite parent, final PanelType panelType) {
		Hyperlink editLink = toolkit.createHyperlink(parent, Messages.IntelligenceEditor_Edit_LinkLabel, SWT.WRAP);
		
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
		return SmartDB.getCurrentEmployee().getSmartUserLevel() != Employee.SmartUserLevel.ANALYST;
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
			final EditItelligenceItemDialog editDialog = new EditItelligenceItemDialog(getEditorSite().getShell(), panelType, getIntelligence());
			ret = editDialog.open();
		} finally {
			
		}
		
		if (ret == IDialogConstants.OK_ID){
			return true;
		}
		return false;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
	}

	@Override
	public void setFocus() {
		txtDateReceived.setFocus();
	}

	@Override
	public void doSaveAs() {
		//not allowed
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}
