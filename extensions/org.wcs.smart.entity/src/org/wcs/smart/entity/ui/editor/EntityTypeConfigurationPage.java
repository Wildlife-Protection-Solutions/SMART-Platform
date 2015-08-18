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
package org.wcs.smart.entity.ui.editor;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
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
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.entity.EntityPermissionManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.ui.newwizard.StatusComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.ca.properties.AddAttributeDialog1;
import org.wcs.smart.ui.ca.properties.AddAttributeDialog2;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.KeyInputDialog;

/**
 * Editor page for displaying and modifying the
 * entity type configuration page.
 * 
 * @author Emily
 *
 */
public class EntityTypeConfigurationPage extends EditorPart implements IEntityTypeEditorPage {
	
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private Form form;

	private Text txtDateCreated;
	private Text txtCreatedBy;
	private Text txtType;
	private Text txtStatus;
	private Text txtKey;
	private Text txtName;
	private Text txtDmAttribute;

	private TableViewer attributeTable;
	
	private EntityTypeEditor parentEditor;
	private ScrolledComposite summaryScroll;
	
	Job fireDataModelListeners = new Job(Messages.EntityTypeConfigurationPage_DataModeUpdateJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try{
				DataModelManager.INSTANCE.fireChangeListeners();
			}catch(final Exception ex){
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						EntityPlugIn.displayLog(ex.getMessage(), ex);
					}});
			}
			return Status.OK_STATUS;
		}
		
	};
		
	/**
	 * Creates a new plan editor page
	 * @param editor
	 */
	public EntityTypeConfigurationPage(EntityTypeEditor editor){
		this.parentEditor = editor;
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
		super.setSite(site);
		super.setInput(input);
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
		toolkit.setBorderStyle(SWT.BORDER);
		form = toolkit.createForm(parent);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		

		GridLayout glayout = new GridLayout();
		glayout.verticalSpacing = 0;
		glayout.marginHeight = 0;
		form.getBody().setLayout(glayout);
		
		if (EntityPermissionManager.canCreateEditDeleteTypes()) {
			Hyperlink translateLink = toolkit.createHyperlink(form.getBody(), Messages.EntityTypeConfigurationPage_TranslateLabel, SWT.WRAP);
			translateLink.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
			translateLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					translateName();
				}
			});
		}

		final Section summary = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE );
		summary.setLayout(new GridLayout(2, false));
		summary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		summary.setText(Messages.EntityTypeConfigurationPage_SummarySectionName);
		summary.addExpansionListener(new ExpansionAdapter() {			
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				summary.getParent().layout(true, true);
			}
		});
		
		summaryScroll = new ScrolledComposite(summary, SWT.V_SCROLL | SWT.H_SCROLL );
		
		summaryScroll.setExpandHorizontal(true);
		summaryScroll.setExpandVertical(true);
		toolkit.adapt(summaryScroll);
		
		final Composite content = toolkit.createComposite(summaryScroll, SWT.NONE);
		summaryScroll.setContent(content);
		summary.setClient(summaryScroll);

		GridLayout layout1 = new GridLayout(2, false);
		layout1.marginHeight = 0;
		content.setLayout(layout1);
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		final Composite topContent = toolkit.createComposite(content, SWT.NONE);
		GridLayout layout2 = new GridLayout(2, false);
		layout2.marginHeight = 0;
		topContent.setLayout(layout1);
		topContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftContent = toolkit.createComposite(topContent, SWT.NONE);
		leftContent.setLayout(new GridLayout(3, false));
		leftContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite rightContent = toolkit.createComposite(topContent, SWT.NONE);
		rightContent.setLayout(new GridLayout(3, false));
		rightContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)rightContent.getLayout()).marginLeft = 20;
		
		//left
		Label l = toolkit.createLabel(leftContent, Messages.EntityTypeConfigurationPage_StatusLabel);
		l.setToolTipText(Messages.EntityTypeConfigurationPage_StatusTooltip);
		txtStatus = toolkit.createText(leftContent, "", SWT.NONE); //$NON-NLS-1$
		txtStatus.setEditable(false);
		txtStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtStatus.getLayoutData()).widthHint = 100;
		
		Hyperlink editLink = createEditLink(toolkit, leftContent);
		if (editLink != null){
			editLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					EntityTypeEditPropertyDialog dialog = new EntityTypeEditPropertyDialog(getSite().getShell(),
						new StatusComposite(), parentEditor.getEntityType());
					dialog.open();
				}
			});
		}

		l = toolkit.createLabel(leftContent, Messages.EntityTypeConfigurationPage_NameLabel);
		l.setToolTipText(Messages.EntityTypeConfigurationPage_NameTooltip);
		txtName = toolkit.createText(leftContent, "", SWT.NONE); //$NON-NLS-1$
		txtName.setEditable(false);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtName.getLayoutData()).widthHint = 100;
		
		editLink = createEditLink(toolkit, leftContent);
		if (editLink != null){
			editLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					translateName();
				}
			});
		}
		
		l = toolkit.createLabel(leftContent, Messages.EntityTypeConfigurationPage_KeyLabel);
		txtKey = toolkit.createText(leftContent, "", SWT.NONE); //$NON-NLS-1$
		txtKey.setEditable(false);
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtKey.getLayoutData()).widthHint = 100;
		
		l.setToolTipText(Messages.EntityTypeConfigurationPage_KeyTooltip);
		editLink = createEditLink(toolkit, leftContent);
		if (editLink != null){
			editLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					
					//translateName();
					if (!MessageDialog
							.openConfirm(
									getSite().getShell(),
									Messages.EntityTypeConfigurationPage_EditKeyDialogTitle,
									Messages.EntityTypeConfigurationPage_EditKeyDialogWarning )) {
						return;
					}
					
					Session s = HibernateManager.openSession();
					s.beginTransaction();
					try{
						EntityType et = parentEditor.getEntityType();
						Query q = s.createQuery("SELECT keyId FROM EntityType where conservationArea = :ca and uuid != :uuid"); //$NON-NLS-1$
						q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
						q.setParameter("uuid", et.getUuid()); //$NON-NLS-1$
						@SuppressWarnings("unchecked")
						List<String> items = q.list();
						List<NamedKeyItem> siblings = new ArrayList<NamedKeyItem>();
						for (String i : items){
							NamedKeyItem k = new EntityType();
							k.setKeyId(i);
							siblings.add(k);
						}
						KeyInputDialog id = new KeyInputDialog(getSite().getShell(), et.getKeyId(), siblings);
						int ret = id.openNoWarning();
						if (ret != Window.CANCEL) {
							parentEditor.getEntityType().setKeyId(id.getValue());
							s.saveOrUpdate(et);
							s.getTransaction().commit();
							
							EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_TYPE_MODIFIED, et);
						}else{
							s.getTransaction().rollback();
						}
					}catch(Exception ex){
						EntityPlugIn.displayLog(Messages.EntityTypeConfigurationPage_EditKeyError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
						s.getTransaction().rollback();
					}finally{
						s.close();
					}
				}
			});
		}
		
		
		// - right
		toolkit.createLabel(rightContent, Messages.EntityTypeConfigurationPage_TypeLabel);
		txtType = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtType.setEditable(false);
		txtType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtType.getLayoutData()).widthHint = 100;
		toolkit.createLabel(rightContent, ""); //$NON-NLS-1$
		
		toolkit.createLabel(rightContent, Messages.EntityTypeConfigurationPage_CreateByLabel);
		txtCreatedBy = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtCreatedBy.setEditable(false);
		txtCreatedBy.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtCreatedBy.getLayoutData()).widthHint = 100;
		toolkit.createLabel(rightContent, ""); //$NON-NLS-1$
		
		toolkit.createLabel(rightContent, Messages.EntityTypeConfigurationPage_DateCreatedLabel);
		txtDateCreated = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtDateCreated.setEditable(false);
		txtDateCreated.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtDateCreated.getLayoutData()).widthHint = 100;
		toolkit.createLabel(rightContent, ""); //$NON-NLS-1$
		
		l=toolkit.createLabel(rightContent, Messages.EntityTypeConfigurationPage_AttributeLabel);
		l.setToolTipText(Messages.EntityTypeConfigurationPage_AttributeTooltip);
		txtDmAttribute = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtDmAttribute.setEditable(false);
		txtDmAttribute.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtDmAttribute.getLayoutData()).widthHint = 100;
		toolkit.createLabel(rightContent, ""); //$NON-NLS-1$
		
		summaryScroll.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		summaryScroll.addControlListener(new ControlAdapter() {
		      public void controlResized(ControlEvent e) {    	 
		        Rectangle r = summaryScroll.getClientArea();
		        Point p2 = topContent.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		        
		        int newHeight = content.computeSize(r.width, SWT.DEFAULT).y;
		        int newWidth = p2.x;
		        summaryScroll.setMinSize(newWidth, newHeight);
		      }
		    });
		
		/* --- target section --- */
		final Section attributeSelection = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
		attributeSelection.setText(Messages.EntityTypeConfigurationPage_AttributeLabels);
		attributeSelection.setLayout(new GridLayout(1, false));
		attributeSelection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributeSelection.addExpansionListener(new ExpansionAdapter() {
			
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				if (attributeSelection.isExpanded()){
					attributeSelection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));	
					summary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				}else{
					attributeSelection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					if (summary.isExpanded()){
						summary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
					}else{
						summary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					}
				}
				attributeSelection.getParent().layout();
			}
		});
	
		ScrolledComposite scroll = new ScrolledComposite(attributeSelection, SWT.V_SCROLL | SWT.H_SCROLL );
		
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		toolkit.adapt(scroll);
		
		Composite targetContent = toolkit.createComposite(scroll);
		attributeSelection.setClient(scroll);
		scroll.setContent(targetContent);
		
		targetContent.setLayout(new GridLayout(2, false));
		targetContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// --- attribute table list
		Composite attributeTableComp = toolkit.createComposite(targetContent);
		attributeTableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributeTable = new TableViewer(attributeTableComp, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		attributeTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributeTable.setContentProvider(ArrayContentProvider.getInstance());
		attributeTable.getTable().setHeaderVisible(true);
		attributeTable.getTable().setLinesVisible(true);
		attributeTable.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editAttribute();
			}
		});
		TableViewerColumn colNone = new TableViewerColumn(attributeTable, SWT.NONE);
		colNone.getColumn().setResizable(false);
		colNone.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element){
				return ""; //$NON-NLS-1$
			}
		});
		
		
		TableViewerColumn colKey = new TableViewerColumn(attributeTable, SWT.NONE);
		colKey.getColumn().setText(Messages.EntityTypeConfigurationPage_KeyColumnName);
		colKey.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof EntityAttribute){
					return ((EntityAttribute) element).getKeyId();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn colAlias = new TableViewerColumn(attributeTable, SWT.NONE);
		colAlias.getColumn().setText(Messages.EntityTypeConfigurationPage_AliasColumnName);
		colAlias.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof EntityAttribute){
					String l = ((EntityAttribute) element).getName();
					if (l == null || l.length() == 0){
						l = ((EntityAttribute) element).getDmAttribute().getName();
					}
					return l;
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn colAttribute = new TableViewerColumn(attributeTable, SWT.NONE);
		colAttribute.getColumn().setText(Messages.EntityTypeConfigurationPage_DataModelAttributeNameColumnName);
		colAttribute.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof EntityAttribute){
					return ((EntityAttribute) element).getDmAttribute().getName();
				}
				return super.getText(element);
			}
			@Override
			public Image getImage(Object element) {
				if (element instanceof EntityAttribute){
					return DataModel.getAttributeImage(((EntityAttribute) element).getDmAttribute().getType());
				}
				return super.getImage(element);
				
			}
		});
		
		TableViewerColumn colRequired = new TableViewerColumn(attributeTable, SWT.NONE);
		colRequired.getColumn().setText(Messages.EntityTypeConfigurationPage_RequiredColumnName);
		colRequired.getColumn().setToolTipText(Messages.EntityTypeConfigurationPage_RequiredColumnName);
		colRequired.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof EntityAttribute){
					if (((EntityAttribute) element).getIsRequired()){
						return Messages.EntityTypeConfigurationPage_RequiredYesLabel;
					}else{
						return Messages.EntityTypeConfigurationPage_RequiredNoLabel;
					}
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn colPrimary = new TableViewerColumn(attributeTable, SWT.NONE);
		colPrimary.getColumn().setText(Messages.EntityTypeConfigurationPage_PrimaryColumnName);
		colPrimary.getColumn().setToolTipText(Messages.EntityTypeConfigurationPage_PrimaryColumnName);
		colPrimary.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof EntityAttribute){
					if (((EntityAttribute) element).getIsPrimary()){
						return Messages.EntityTypeConfigurationPage_PrimaryYesLabel;
					}else{
						return Messages.EntityTypeConfigurationPage_PrimaryNoLabel;
					}
				}
				return super.getText(element);
			}
		});
		TableColumnLayout layout = new TableColumnLayout();
		attributeTableComp.setLayout( layout );
		
		layout.setColumnData( colNone.getColumn(), new ColumnWeightData( 0, 0, false ) );
		layout.setColumnData( colKey.getColumn(), new ColumnWeightData( 15 ) );
		layout.setColumnData( colAlias.getColumn(), new ColumnWeightData( 30 ) );
		layout.setColumnData( colAttribute.getColumn(), new ColumnWeightData( 35 ) );
		layout.setColumnData( colRequired.getColumn(), new ColumnWeightData( 10 ) );
		layout.setColumnData( colPrimary.getColumn(), new ColumnWeightData( 10 ) );
		
		
		if (EntityPermissionManager.canCreateEditDeleteTypes()){
			Composite buttonComp = toolkit.createComposite(targetContent);
			buttonComp.setLayout(new GridLayout());
			buttonComp.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, true));
		
			final Button btnMoveUp = toolkit.createButton(buttonComp, Messages.EntityTypeConfigurationPage_MoveUpLabel, SWT.PUSH);
			btnMoveUp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			((GridData)btnMoveUp.getLayoutData()).widthHint = 100;
			btnMoveUp.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					EntityType et = parentEditor.getEntityType();
					ArrayList<EntityAttribute> entities = new ArrayList<EntityAttribute>();
					entities.addAll(et.getAttributes());
				
					for (Iterator<?> iterator = ((StructuredSelection)attributeTable.getSelection()).iterator(); iterator.hasNext();) {
						Object type = (Object) iterator.next();
						int i = entities.indexOf(type);
						entities.remove(i);
						i--;
						if (i <=0){
							i = 0;
						}
						entities.add(i, (EntityAttribute)type);
					}
					boolean isChanged = false;
					for (int i = 0; i < entities.size();i++){
						if (entities.get(i).getOrder() != i+1){
							isChanged = true;
						}
						entities.get(i).setOrder(i+1);
					}
					if (isChanged){
						parentEditor.saveEntityType();
					}
				}
			});
			btnMoveUp.setEnabled(false);
			
			final Button btnMoveDown = toolkit.createButton(buttonComp, Messages.EntityTypeConfigurationPage_MoveDownLabel, SWT.PUSH);
			btnMoveDown.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			((GridData)btnMoveDown.getLayoutData()).widthHint = 100;
			btnMoveDown.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					EntityType et = parentEditor.getEntityType();
					
					ArrayList<EntityAttribute> entities = new ArrayList<EntityAttribute>();
					entities.addAll(et.getAttributes());
					
					List<?> items = ((StructuredSelection)attributeTable.getSelection()).toList();
					for (int k = items.size() -1; k >=0; k --){
						Object type = (Object) items.get(k);
						int i = entities.indexOf(type);
						entities.remove(i);
						i++;
						if (i >= entities.size()){
							entities.add((EntityAttribute)type);
						}else{
							entities.add(i, (EntityAttribute)type);
						}	
					}
					boolean isChanged = false;
					for (int i = 0; i < entities.size();i++){
						if (entities.get(i).getOrder() != i+1){
							isChanged = true;
						}
						entities.get(i).setOrder(i+1);
					}
					if (isChanged){
						parentEditor.saveEntityType();
					}
				}
			});
			btnMoveDown.setEnabled(false);
		
			Composite buttonTableComp = toolkit.createComposite(targetContent);
			buttonTableComp.setLayout(new GridLayout(3, false));
			buttonTableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false,2,1));
		
			Button btnAdd = toolkit.createButton(buttonTableComp, DialogConstants.ADD_BUTTON_TEXT, SWT.PUSH);
			btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			((GridData)btnAdd.getLayoutData()).widthHint = 100;
			btnAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					addAttribute();
				}
			});
		
			final Button btnEdit = toolkit.createButton(buttonTableComp, DialogConstants.EDIT_BUTTON_TEXT, SWT.PUSH);
			btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			((GridData)btnEdit.getLayoutData()).widthHint = 100;
			btnEdit.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					editAttribute();
				}
			});
			btnEdit.setEnabled(false);
			
			final Button btnDelete = toolkit.createButton(buttonTableComp, DialogConstants.DELETE_BUTTON_TEXT, SWT.PUSH);
			btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			((GridData)btnDelete.getLayoutData()).widthHint = 100;
			btnDelete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					deleteAttribute();
				}
			});
			btnDelete.setEnabled(false);
			
			attributeTable.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					btnMoveDown.setEnabled(!attributeTable.getSelection().isEmpty());
					btnMoveUp.setEnabled(!attributeTable.getSelection().isEmpty());
					btnEdit.setEnabled(!attributeTable.getSelection().isEmpty());
					btnDelete.setEnabled(!attributeTable.getSelection().isEmpty());
				}
			});
		}
		scroll.setMinSize(targetContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	/*
	 * Opens the translate names dialog for modifying
	 * the entity type name.
	 */
	private void translateName() {
		if (!EntityPermissionManager.canCreateEditDeleteTypes()) return;
		final EntityType toEdit = EntityTypeConfigurationPage.this.parentEditor.getEntityType();
		TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(
				getEditorSite().getShell(), toEdit);
		
		if (dialog.open() == IDialogConstants.OK_ID) {
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				session.saveOrUpdate(toEdit);
				session.getTransaction().commit();
			}catch (Exception ex){
				session.getTransaction().rollback();
				EntityPlugIn.displayLog(ex.getMessage(), ex);
				return;
			}finally{
				session.close();
			}
			
			EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_TYPE_MODIFIED, toEdit);
		}
	}
	
	/*
	 * Adds a new attribute to the entity type.
	 */
	private void addAttribute(){
		if (!EntityPermissionManager.canCreateEditDeleteTypes()) return;
		final List<Attribute> dmAttributes = new ArrayList<Attribute>();
		final ArrayList<Attribute> attributeToAdd = new ArrayList<Attribute>();
		
		final Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			//load attributes from datamodel
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getSite().getShell());
			try{
				pmd.run(true, false, new IRunnableWithProgress() {
			
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
						monitor.beginTask(Messages.EntityTypeConfigurationPage_LoadAttributeProgressName, 1);
				
						Criteria c = s.createCriteria(Attribute.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()));	 //$NON-NLS-1$
						@SuppressWarnings("unchecked")
						List<Attribute> atts = c.list();
						dmAttributes.addAll(atts);
					}
				});
			}catch (Exception ex){
				EntityPlugIn.displayLog(ex.getMessage(), ex);
				return;
			}
			
			//display add attribute dialog 1
			DataModel tmpDm = new DataModel(SmartDB.getCurrentConservationArea(), Collections.<Category>emptyList(), dmAttributes);
			AddAttributeDialog1 d1 = new AddAttributeDialog1(parentEditor.getSite().getShell(), 
				null, tmpDm, SmartDB.getCurrentLanguage(), s){
			
				@Override
				protected void buttonPressed(int buttonId) {
					if (IDialogConstants.NEXT_ID == buttonId) {
						setReturnCode(NEXT);
					} else if (IDialogConstants.FINISH_ID == buttonId) {
						Object[] checked = checkboxTableViewer.getCheckedElements();
						for (int i = 0; i < checked.length; i++) {
							Object x = checked[i];
							if (x instanceof Attribute){
								attributeToAdd.add((Attribute)x);
							}
						}
						setReturnCode(FINISH);
					} else if (IDialogConstants.CANCEL_ID == buttonId) {
						setReturnCode(CANCEL);
					}
					close();
				}
			};
		
			int ret = d1.open();
			if (ret == AddAttributeDialog1.CANCEL){
				return;
			}else if (ret == AddAttributeDialog1.NEXT){
				Attribute att = new Attribute();
				att.setConservationArea(SmartDB.getCurrentConservationArea());
			
				AddAttributeDialog2 d2 = new AddAttributeDialog2(getSite().getShell(), att,
						tmpDm.getAttributes(),
						SmartDB.getCurrentConservationArea().getDefaultLanguage(), s);
			
				//show new attribute dialog
				ret = d2.open();
				if (ret == Window.CANCEL){
					return;
				}
				attributeToAdd.add(att);	
				DataModelManager.INSTANCE.fireAddListener(s, att);
			}
			s.getTransaction().commit();
			
			//schedule the data model changed listener; we have saved modifications to the data model
			fireDataModelListeners.schedule();
		}catch (Exception ex){
			EntityPlugIn.displayLog(Messages.EntityTypeConfigurationPage_SaveError, ex);
		}finally{
			if (s.getTransaction().isActive()){
				s.getTransaction().rollback();
			}
			s.close();
		}

		addAttributes(attributeToAdd);
	}
	

	
	/*
	 * Adds the set of attributes to the entity type.
	 */
	private void addAttributes(List<Attribute> attributes){
		EntityType et = parentEditor.getEntityType();
		if (et.getAttributes() == null){
			et.setAttributes(new ArrayList<EntityAttribute>());
		}
		for (Attribute attribute : attributes){
			//create a new Entity Attribute
			EntityAttribute ea = new EntityAttribute();
			ea.setDmAttribute(attribute);
			ea.setEntityType(et);
			ea.setIsRequired(true);
			ea.setIsPrimary(true);
			ea.setOrder(et.getAttributes().size()+1);
			
			ea.setKeyId(DataModelManager.INSTANCE.generateKey(attribute.getKeyId(), et.getAttributes()));
			
			et.getAttributes().add(ea);
			
			
		}
		parentEditor.saveEntityType();
	}
	
	/*
	 * Deletes attributes from the entity type
	 */
	private void deleteAttribute(){
		if (!EntityPermissionManager.canCreateEditDeleteTypes()) return;
		final List<EntityAttribute> toDelete = new ArrayList<EntityAttribute>();
		
		for (Iterator<?> iterator = ((StructuredSelection)attributeTable.getSelection()).iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof EntityAttribute){
				toDelete.add((EntityAttribute) type);
			}
		}
		boolean delete = false;
		if (toDelete.size() > 1){
			if (MessageDialog.openConfirm(getSite().getShell(), Messages.EntityTypeConfigurationPage_DeleteDialogTitle, MessageFormat.format(Messages.EntityTypeConfigurationPage_DeleteDialogMessage, new Object[]{toDelete.size()}))){
				delete = true;
			}
		}else{
			String attName = toDelete.get(0).getName();
			if (attName.length() == 0){
				attName = toDelete.get(0).getDmAttribute().getName();
			}else{
				attName = attName + " (" + toDelete.get(0).getDmAttribute().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (MessageDialog.openConfirm(getSite().getShell(), Messages.EntityTypeConfigurationPage_DeleteDialogTitle, MessageFormat.format(Messages.EntityTypeConfigurationPage_DeleteConfirmMessage, 
					new Object[]{attName}))){
				delete = true;
			}
		}
		if (delete){
			
			ProgressMonitorDialog deleteDialog = new ProgressMonitorDialog(getSite().getShell());
			try{
			deleteDialog.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					Session s = HibernateManager.openSession();
					try{
						s.beginTransaction();
						
						s.saveOrUpdate(parentEditor.getEntityType());
						
						Query q = s.createQuery("DELETE EntityAttributeValue WHERE id.entityAttribute IN (:toDelete)"); //$NON-NLS-1$
						q.setParameterList("toDelete", toDelete); //$NON-NLS-1$
						q.executeUpdate();						
						
						parentEditor.getEntityType().getAttributes().removeAll(toDelete);
						for (final EntityAttribute ea : toDelete){
							
							//at this point we should try to delete the attribute as well
							boolean canDeleteAttribute = false;
							try{
								if (DeleteManager.canDelete(ea.getDmAttribute(), s)){
									canDeleteAttribute = true;
								}
							}catch (Exception ex){
								//something is using this attribute therefore
								//it cannot be deleted
							}
							
							if (canDeleteAttribute){
								final int[] ret = {-1};
								Display.getDefault().syncExec(new Runnable(){

									@Override
									public void run() {
										MessageDialog dialog = new MessageDialog(getSite().getShell(), Messages.EntityTypeConfigurationPage_DeleteAttributeDialogTitle, null,
												MessageFormat.format(Messages.EntityTypeConfigurationPage_DeleteAttributeDialogMessage, new Object[]{ ea.getDmAttribute().getName() } ), 
												MessageDialog.CONFIRM, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
										ret[0] = dialog.open();
									}});
							
								if (ret[0] == 0){  //YES
									boolean deletel = DataModelManager.INSTANCE.validateDelete(ea.getDmAttribute(), monitor, s);
									if (deletel){
										DataModelManager.INSTANCE.fireDeleteListener(s, ea.getDmAttribute());
										s.delete(ea.getDmAttribute());
									}
								}
							}
						}
						
						s.getTransaction().commit();
						
						try{
							EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_TYPE_MODIFIED, parentEditor.getEntityType());
						}catch(Exception ex){
							EntityPlugIn.displayLog(ex.getMessage(), ex);
						}
					}catch (Exception ex){
						EntityPlugIn.displayLog(Messages.EntityTypeConfigurationPage_DeleteAttributeError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
						
					}finally{
						if (s.getTransaction().isActive()){
							s.getTransaction().rollback();
						}
						s.close();
					}
				}
			});
			}catch (Exception ex){
				EntityPlugIn.displayLog(Messages.EntityTypeConfigurationPage_DeleteAttributeError2 + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			}
			
			fireDataModelListeners.schedule();
		}
	}
	
	/*
	 * Edits a attribute
	 */
	private void editAttribute(){
		if (!EntityPermissionManager.canCreateEditDeleteTypes()) return;
		boolean fire = false;
		Object type = ((StructuredSelection)attributeTable.getSelection()).getFirstElement();
		if (type instanceof EntityAttribute){
			Session s = HibernateManager.openSession();
			try{
				s.saveOrUpdate(type);
				EntityTypeEditDmAttributeDialog dia = new EntityTypeEditDmAttributeDialog(getSite().getShell(), (EntityAttribute)type, s);
				if (dia.open()==EntityTypeEditDmAttributeDialog.OK){
					s.beginTransaction();
					try{
						((EntityAttribute) type).setName(((EntityAttribute) type).findName(SmartDB.getCurrentLanguage()));
						s.saveOrUpdate(type);
						s.getTransaction().commit();
					}catch (Exception ex){
						EntityPlugIn.displayLog(Messages.EntityTypeConfigurationPage_EditErrorDescription + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
						s.getTransaction().rollback();
						return;
					}
				}
				fire = dia.fireEvents();
				
			}finally{
				s.close();
			}
			
			if (fire){
				EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_TYPE_MODIFIED, ((EntityAttribute) type).getEntityType());
			}
		}
		
	}
	
	/**
	 * Updates the widgets in the composite 
	 * with the entity type.
	 * @param currentSession open session
	 * @param typeChanged if the entity type structure has changed (if false
	 * only an entity may have been modified)
	 * 
	 */
	public void updatePage(Session currentSession, boolean typeChanged) {
		EntityType type = this.parentEditor.getEntityType();
		form.setText(MessageFormat.format(Messages.EntityTypeConfigurationPage_PageName, 
				new Object[]{parentEditor.getEntityType().getName()}));
		
		type.getNames().size();
		txtName.setText(type.getName());
		txtKey.setText(type.getKeyId());
		txtDmAttribute.setText(type.getDmAttribute().getName());
		txtStatus.setText(type.getStatus().getGuiName(Locale.getDefault()));
		txtType.setText(type.getType().getGuiName(Locale.getDefault()));
		if (type.getCreator() != null){
			txtCreatedBy.setText(SmartLabelProvider.getFullLabel(type.getCreator()));
		}
		if (type.getDateCreated() != null){
			txtDateCreated.setText(DateFormat.getDateInstance().format(type.getDateCreated()));	
		}
		
		
		attributeTable.setInput(type.getAttributes());			
		
		if (type.getAttributes() != null){
			type.getAttributes().size();
		}
	}
	
	/**
	 * Creates an edit hyperlink button
	 * @param tolkit toolkit
	 * @param parent parent composite
	 * @param partEditor editor to use
	 * @return hyperlink created
	 */
	private Hyperlink createEditLink(FormToolkit tolkit, Composite parent){
		if (EntityPermissionManager.canCreateEditDeleteTypes()){
			Hyperlink editLink = toolkit.createHyperlink(parent, DialogConstants.EDIT_LINK_TEXT, SWT.WRAP);
			return editLink;
		}else{
			//create empty label
			toolkit.createLabel(parent, ""); //$NON-NLS-1$
			return null;
		}
	}


	@Override
	public void setFocus() {
		txtName.setFocus();
	}

}
