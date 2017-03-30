/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.views;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.EntityManager;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.search.IntelSearchResult;
import org.wcs.smart.i2.search.IntelSearchResultItem;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.editors.record.RecordEditor;
import org.wcs.smart.i2.ui.entity.exporter.EntityRelationshipExportDialog;
import org.wcs.smart.i2.ui.handler.CompareEntitiesHandler;
import org.wcs.smart.i2.ui.handler.OpenAttachmentViewHandler;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.util.E3Utils;

/**
 * Displays the entity search results in a table.
 * 
 * @author Emily
 *
 */
public class EntitySearchResultTable extends Composite {

	private static final String ICON_SIZE_KEY = "org.wcs.smart.i2.ui.views.EntitySearchResultTable.iconsize"; //$NON-NLS-1$
	private enum IconSize{
		SMALL(50, Messages.EntitySearchResultTable_SmallOp), 
		MEDIUM(100, Messages.EntitySearchResultTable_MediumOp),
		LARGE(200, Messages.EntitySearchResultTable_LargeOp);
		
		int size;
		String label;
		
		IconSize(int size, String label){
			this.size = size;
			this.label = label;
		}
	}
	
	private IconSize iconSize = IconSize.SMALL;
	
	private Color selectionColor = null;
	private Color mouseOverColor = null;
	
	private Composite core = null;
	private Font smallerFont = null;
	private Font boldFont = null;
	private Label iconSizeLabel = null;
	
	private IntelSearchResult entities;
	private FormToolkit toolkit = null;
	private List<EntityComponent> components = null;
	
	private IEclipseContext context;
	private ScrolledComposite sc;

	public EntitySearchResultTable(Composite parent, FormToolkit toolkit, IEclipseContext context) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		this.context = context;
				
		Color color = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
		selectionColor = new Color(parent.getDisplay(), blend(new RGB(255, 255, 255), color.getRGB(), 75));
		mouseOverColor = new Color(parent.getDisplay(), blend(new RGB(255, 255, 255), color.getRGB(), 90));
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				selectionColor.dispose();
				mouseOverColor.dispose();
				if (smallerFont != null){
					smallerFont.dispose();
					smallerFont = null;
				}
				if (boldFont != null){
					boldFont.dispose();
					boldFont = null;
				}
				if (iconSize != null){
					Intelligence2PlugIn.getDefault().getPreferenceStore().putValue(ICON_SIZE_KEY, iconSize.name());
				}
			}
		});
		
		String initIconSize = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(ICON_SIZE_KEY);
		if (initIconSize != null){
			try{
				iconSize = IconSize.valueOf(initIconSize);
			}catch (Exception ex){
				// use the default value
			}
			
		}
	}
	
	/**
	 * set to null to configure loading message; set to empty list of results return 
	 * no records
	 * 
	 * @param entities
	 */
	public void setEntities(IntelSearchResult entities){
		this.entities = entities;
		createTable();
		core.layout(true);
		if (sc != null){
			sc.setMinSize(sc.getChildren()[0].computeSize(sc.getClientArea().width, SWT.DEFAULT));	
		}
		
	}
	
	public List<IntelSearchResultItem> getEntities(){
		return this.entities.getResults();
	}
	
	public void setSearchError(Exception ex){
		if (core != null){
			core.dispose();
			core = null;
		}
		core = toolkit.createComposite(this, SWT.NONE);
		core.setLayout(new GridLayout(2, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label img = toolkit.createLabel(core, ""); //$NON-NLS-1$
		img.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		img.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		
		Label l = toolkit.createLabel(core, MessageFormat.format(Messages.EntitySearchResultTable_SearchError, ex.getMessage()), SWT.WRAP);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 150;
		l.setToolTipText(ex.getMessage());
		
		this.layout(true);
	}
	
	private void setIconSize(IconSize newSize){
		this.iconSize = newSize;
		createTable();
		core.layout(true);
		if (sc != null){
			sc.setMinSize(sc.getChildren()[0].computeSize(sc.getClientArea().width, SWT.DEFAULT));	
		}
	}
	
	private void createTable(){
		if (core != null){
			core.dispose();
			core = null;
			components = null;
		}
		
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		core = toolkit.createComposite(this, SWT.NONE);
		core.setLayout(new GridLayout());
		((GridLayout)core.getLayout()).marginWidth = 0;
		((GridLayout)core.getLayout()).marginHeight = 0;
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (smallerFont == null){
			FontData fd = core.getFont().getFontData()[0];
			fd.height = fd.height - 1;
			smallerFont = new Font(Display.getDefault(), fd);
		}
		if (boldFont == null){
			FontData fd = core.getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD); 
			boldFont = new Font(Display.getDefault(), fd);
		}
		if (entities == null){
			toolkit.createLabel(core, Messages.EntitySearchResultTable_SearchingLabel);
			sc = null;
		}else{
			Composite top = toolkit.createComposite(core);
			top.setLayout(new GridLayout(3, false));
			top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)top.getLayout()).marginWidth = 0;
			((GridLayout)top.getLayout()).marginHeight = 0;
			((GridLayout)top.getLayout()).horizontalSpacing = 0;
			((GridLayout)top.getLayout()).verticalSpacing = 0;
			
			toolkit.createLabel(top, MessageFormat.format(Messages.EntitySearchResultTable_SearchResultCntLabel, entities.getResults().size(), entities.getTotalMatched()));
			
			iconSizeLabel = toolkit.createLabel(top, iconSize.label);
			iconSizeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			iconSizeLabel.setToolTipText(Messages.EntitySearchResultTable_ThumbSizeOp);
			Button btnDown = toolkit.createButton(top, "", SWT.ARROW | SWT.DOWN); //$NON-NLS-1$
			btnDown.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
			btnDown.setToolTipText(Messages.EntitySearchResultTable_ThumbSizeTooltip);
			Menu mnuIconSize = new Menu(btnDown);
			MenuItem small = new MenuItem(mnuIconSize, SWT.RADIO);
			MenuItem medium = new MenuItem(mnuIconSize, SWT.RADIO);
			MenuItem large = new MenuItem(mnuIconSize, SWT.RADIO);
			small.setText(IconSize.SMALL.label);
			medium.setText(IconSize.MEDIUM.label);
			large.setText(IconSize.LARGE.label);
			
			if(iconSize == IconSize.SMALL){
				small.setSelection(true);
			}else if (iconSize == IconSize.MEDIUM){
				medium.setSelection(true);
			}else if (iconSize == IconSize.LARGE){
				large.setSelection(true);
			}
			small.addListener(SWT.Selection, e-> {if (small.getSelection()) setIconSize(IconSize.SMALL);});
			medium.addListener(SWT.Selection, e-> {if (medium.getSelection()) setIconSize(IconSize.MEDIUM);});
			large.addListener(SWT.Selection, e-> {if (large.getSelection()) setIconSize(IconSize.LARGE);});
			btnDown.addListener(SWT.MouseDown, e->mnuIconSize.setVisible(true));
			
			
			sc = new ScrolledComposite(core, SWT.V_SCROLL |  SWT.H_SCROLL);
			toolkit.adapt(sc);
			Composite main = toolkit.createComposite(sc, SWT.NONE);
			sc.setContent(main);
			sc.setExpandHorizontal(true);
			sc.setExpandVertical(true);
			main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			main.setLayout(new GridLayout());
			main.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));

			components = new ArrayList<EntitySearchResultTable.EntityComponent>();
			int cnt = 0;
			for (IntelSearchResultItem i : entities.getResults()){		
				EntityComponent entityComposite = new EntityComponent(main, i, cnt++, components);
				components.add(entityComposite);
				toolkit.adapt(entityComposite);
				entityComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				Label l = toolkit.createLabel(main, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
			}
			toolkit.createLabel(main, MessageFormat.format(Messages.EntitySearchResultTable_SearchTimeResult, entities.getTotalTime() / Math.pow(10, 9)));
			layout(true);
			sc.setMinSize(main.computeSize(sc.getClientArea().width, SWT.DEFAULT));
			core.addListener(SWT.Resize, event -> {
				sc.setMinSize(main.computeSize(sc.getClientArea().width, SWT.DEFAULT));
			});
			createMenu(main);
			
			
		}
		
		layout(true);
	}
	
	private void openEntities(){
		if (!getCurrentSelection().isEmpty()){
			getCurrentSelection().forEach(e -> 	(new OpenEntityHandler()).openEntity(e, context));
		}
	}
	
	private void exportEntity(){
		if (getCurrentSelection().isEmpty()) return;
		IntelEntity ie = getCurrentSelection().get(0);
		
		EntityRelationshipExportDialog dialog = new EntityRelationshipExportDialog(ie, getShell());
		dialog.open();
	}
	
	private void printEntities(){
		EntityExportDialog dialog = new EntityExportDialog(getShell(), getCurrentSelection());
		dialog.open();
	}
	
	private void deleteEntities(){
		List<IntelEntity> itemsToDelete = getCurrentSelection();
		if (itemsToDelete.isEmpty()) return;
		
		if (!MessageDialog.openQuestion(getShell(), Messages.EntitySearchResultTable_DeleteEntityTitle, MessageFormat.format(Messages.EntitySearchResultTable_DeleteEntityMsg, itemsToDelete.size()))) return; 
		
		
		//look for any dirty record editors and save them first
		List<RecordEditor> editors = new ArrayList<>();
		StringBuilder names = new StringBuilder();
		for(MPart p : context.get(EPartService.class).getParts()){
			Object x = E3Utils.getSourceObject(p);
			if ( x instanceof RecordEditor && ((RecordEditor)x).isDirty()){
				editors.add((RecordEditor)x);
				names.append(((RecordEditor)x).getPartName());
				names.append(", "); //$NON-NLS-1$
			}
		}
		if (!editors.isEmpty()){
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.EntitySearchResultTable_SaveRequiredMsg);
			sb.append("\n"); //$NON-NLS-1$
			sb.append(names.substring(0, names.length() - 2));
			
			if (!MessageDialog.openQuestion(getShell(), Messages.EntitySearchResultTable_DeleteEntitiesTitle2, sb.toString())){
				return;
			}
			for (RecordEditor p : editors){
				try{
					//context.get(EPartService.class).savePart(p, false); -> this doesn't work it still prompts the user; we do not want to prompt user
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().saveEditor(p, false);
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
				}
			}
		}
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.EntitySearchResultTable_DeleteTaskName, itemsToDelete.size());
					Session s = HibernateManager.openSession(new AttachmentInterceptor());
					try{
						s.beginTransaction();
						for (IntelEntity entity : itemsToDelete){	
							EntityManager.INSTANCE.deleteEntity(entity, s);
							monitor.worked(1);
						}
						s.getTransaction().commit();
					}catch (Exception ex){
						s.getTransaction().rollback();
						throw new InvocationTargetException(ex);
						
					}finally{
						s.close();
					}
					try{
						context.get(IEventBroker.class).send(IntelEvents.ENTITY_DELETE, itemsToDelete);
					}catch (Exception ex){
						//error with events;
						Intelligence2PlugIn.displayLog(Messages.EntitySearchResultTable_RefreshError + ex.getMessage(), ex);
					}
				}
			});
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(Messages.EntitySearchResultTable_DeleteError + ex.getMessage(), ex);
			return;
		}
	}
	
	private void createMenu(Composite parent){
		Menu menu = new Menu(parent);
		
		MenuItem mnuOpen = new MenuItem(menu, SWT.PUSH);
		mnuOpen.setText(Messages.EntitySearchResultTable_OpenItem);
		mnuOpen.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				openEntities();
			}
		});
		
		MenuItem mnuOpenThumb = new MenuItem(menu, SWT.PUSH);
		mnuOpenThumb.setText(Messages.EntitySearchResultTable_OpenThumbItem);
		mnuOpenThumb.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (IntelEntity entity : getCurrentSelection()){
					if (entity.getPrimaryAttachment() != null){
						(new OpenAttachmentViewHandler()).execute(entity.getPrimaryAttachment(), context);
					}
				}
			}
		});
		
		MenuItem mnuCompare = new MenuItem(menu, SWT.PUSH);
		mnuCompare.setText(Messages.EntitySearchResultTable_CompareItem);
		mnuCompare.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				try{
					(new CompareEntitiesHandler()).compare(getCurrentSelection(), context.get(EPartService.class));
				}catch (Exception ex){
					MessageDialog.openInformation(getShell(), Messages.EntitySearchResultTable_CompareErrorDialogTitle, ex.getMessage());
				}
			}
		});
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		MenuItem mnuPrint = new MenuItem(menu, SWT.PUSH);
		mnuPrint.setText(Messages.EntitySearchResultTable_PrintMenuItem);
		mnuPrint.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_PDF));
		mnuPrint.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				printEntities();
			}
		});
		
		MenuItem mnuExport = new MenuItem(menu, SWT.PUSH);
		mnuExport.setText(Messages.EntitySearchResultTable_ExportMenuItem);
		mnuExport.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY_EXPORT));
		mnuExport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportEntity();
			}
		});
		
		if (IntelSecurityManager.INSTANCE.canEditEntity()){
			MenuItem mnuDelete = new MenuItem(menu, SWT.PUSH);
			mnuDelete.setText(Messages.EntitySearchResultTable_DeleteMenuItem);
			mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			mnuDelete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					deleteEntities();
				}
			});
		}
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		MenuItem mnuWorkingset = new MenuItem(menu, SWT.PUSH);
		mnuWorkingset.setText(Messages.EntitySearchResultTable_AddToWsMenuItem);
		mnuWorkingset.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
		mnuWorkingset.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				WorkingSetManager.INSTANCE.addEntityToActiveWorkingSet(getCurrentSelection(), context);
			}
		});
		
		
		
		menu.addMenuListener(new MenuListener() {
			private MenuItem mnuAddToRecord = null;
			private Menu subRecord = null;
			@Override
			public void menuShown(MenuEvent e) {
				boolean hasSelection = !getCurrentSelection().isEmpty();
				mnuOpen.setEnabled(hasSelection);
				mnuPrint.setEnabled(hasSelection);
				mnuWorkingset.setEnabled(hasSelection && WorkingSetManager.INSTANCE.isSet());
				mnuCompare.setEnabled(getCurrentSelection().size() > 0);
				
				if (mnuAddToRecord == null || mnuAddToRecord.isDisposed()){
					mnuAddToRecord = new MenuItem(menu, SWT.CASCADE);
					mnuAddToRecord.setText(Messages.EntitySearchResultTable_AddToRecordMenuItem);
				
					subRecord = new Menu(menu);
					mnuAddToRecord.setMenu(subRecord);
				}
				if (subRecord != null && !subRecord.isDisposed()){
					for (MenuItem mi : subRecord.getItems()){
						mi.dispose();
					}
				}
				
				Collection<MPart> parts = context.get(EPartService.class).getParts();
				for (MPart p : parts){
					if (E3Utils.isCompatibilityEditor(p)){
						Object editor = E3Utils.getSourceObject(p);
						if (editor instanceof RecordEditor && ((RecordEditor)editor).getEditMode()){
							MenuItem relate = new MenuItem(subRecord, SWT.PUSH);
							relate.setText( ((RecordEditor)editor).getRecord().getTitle()  );
							relate.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									if (!getCurrentSelection().isEmpty()){
										for (IntelEntity entity : getCurrentSelection()){
											((RecordEditor)editor).linkEntity(entity);
										}
									}
								}
							});
						}
					}
				}
				if (subRecord == null || subRecord.getItemCount() == 0){
					mnuAddToRecord.dispose();
				}
				
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
//		parent.setMenu(menu);
		List<Control> kids = new ArrayList<Control>();
		kids.add(parent);
		while(!kids.isEmpty()){
			Control c = kids.remove(0);
			c.setMenu(menu);
			if (c instanceof Composite){
				for (Control cc : ((Composite)c).getChildren()){
					kids.add(cc);
				}
			}
		}
	}
	
	
	public List<IntelEntity> getCurrentSelection(){
		ArrayList<IntelEntity> selections = new ArrayList<IntelEntity>();
		if (components == null) return selections;
		
		for (EntityComponent c : components){
			if (c.isSelected) selections.add(c.item.getEntity());
		}
		return selections;
		
	}
	private class EntityComponent extends Composite implements Listener{

		private static final String LAST_SELECTION_INDEX_KEY = "last_selection_index"; //$NON-NLS-1$
		
		private IntelSearchResultItem item;
		private Color backgroundColor = null;
		private boolean isSelected = false;
		private boolean mouseOver = false;
		private int index;
		private List<EntityComponent> siblings;
		
		public EntityComponent(Composite parent, IntelSearchResultItem item, int index, List<EntityComponent> siblings){
			super(parent, SWT.NONE);
			this.item = item;
			createPart();
			this.index = index;
			this.siblings = siblings;
		}
		
		private void clearSelection(){
			for (EntityComponent c : siblings){
				c.isSelected = false;
				c.colorAll();
			}
		}
		private void createPart(){
			setLayout(new GridLayout(2, false));
			((GridLayout)getLayout()).marginHeight = 2;
			addListener(this);
			
			Thumbnail t = new Thumbnail(item.getEntity().getPrimaryAttachment(), iconSize.size);
			Composite c = t.createThumbnail(this);
			addListener(c);
			toolkit.adapt(c);
			c.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
			((GridData)c.getLayoutData()).widthHint = iconSize.size;
			((GridData)c.getLayoutData()).heightHint = iconSize.size;
			
			Composite right = toolkit.createComposite(this, SWT.NONE);
			right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			right.setLayout(new GridLayout(2, false));
			((GridLayout)right.getLayout()).marginWidth = 0;
			((GridLayout)right.getLayout()).marginHeight = 0;
			addListener(right);
			
			Label l = toolkit.createLabel(right, item.getEntity().getIdAttributeAsText(), SWT.WRAP);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			l.setFont(boldFont);
			((GridData)l.getLayoutData()).widthHint = 100;
			addListener(l);
			StringBuilder sb = new StringBuilder();
			sb.append(MessageFormat.format(Messages.EntitySearchResultTable_DateCreatedLabel, DateFormat.getDateInstance().format(item.getEntity().getDateCreated())));
			sb.append("\n"); //$NON-NLS-1$
			sb.append(MessageFormat.format(Messages.EntitySearchResultTable_DateModifiedLabel, DateFormat.getDateInstance().format(item.getEntity().getDateModified())));
			l.setToolTipText(sb.toString());
			
			int spacer = 2;
			Composite typecomp = toolkit.createComposite(right);
			typecomp.setLayout(new GridLayout(2, false));
			((GridLayout)typecomp.getLayout()).marginWidth = 0;
			((GridLayout)typecomp.getLayout()).marginHeight = 0;
			typecomp.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true));
			if (item.getEntity().getEntityType().getIcon() != null){
				final Label l1 = toolkit.createLabel(typecomp,""); //$NON-NLS-1$
				l1.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, true));
				l1.setImage(EntityTypeLabelProvider.createImageDescriptor(item.getEntity().getEntityType()).createImage());
				l1.addDisposeListener((e)->{if (l1.getImage() != null) l1.getImage().dispose();});
				addListener(l1);
				spacer = 1;
			}
			
			l = toolkit.createLabel(typecomp, EntityTypeLabelProvider.getText(item.getEntity().getEntityType()));
			l.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true, spacer, 1));
			addListener(l);
			l.setFont(smallerFont);
			
			
			l = toolkit.createLabel(right, MessageFormat.format(Messages.EntitySearchResultTable_RatingLabel, item.getFormattedRating()));
			l.setToolTipText(item.getMatchString());
			l.setFont(smallerFont);
			addListener(l);
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, true));
			
			addDragSources();
		}
		
		private void addDragSources(){
			final IntelEntitySelectionTransfer trans = IntelEntitySelectionTransfer.getTransfer();
			DragSourceAdapter listener = new DragSourceAdapter() {
				
				@Override
				public void dragStart(DragSourceEvent event) {
					IntelEntitySelectionTransfer.getTransfer().setSelection(new StructuredSelection(getCurrentSelection()));				
				}
				@Override
				public void dragSetData(DragSourceEvent event) {
					if (IntelEntitySelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
						event.data = new StructuredSelection(getCurrentSelection());
					}
				}
				@Override
				public void dragFinished(DragSourceEvent event) {
					IntelEntitySelectionTransfer.getTransfer().setSelection(null);
				}
			};
			
			//add parent and all kids as drag sources
			List<Control> kids = new ArrayList<Control>();
			kids.add(this);
			while(!kids.isEmpty()){
				Control kid = kids.remove(0);
				DragSource dragSource = new DragSource(kid, DND.DROP_LINK);
				dragSource.setTransfer(new Transfer[]{trans});
				dragSource.addDragListener(listener);
				
				if (kid instanceof Composite){
					for (Control cc : ((Composite)kid).getChildren()){
						kids.add(cc);
					}
				}
			}
		}
		
		private void addListener(Control c){
			c.addListener(SWT.MouseEnter, this);
			c.addListener(SWT.MouseExit, this);
			c.addListener(SWT.MouseDown, this);
			c.addListener(SWT.MouseUp, this);
			c.addListener(SWT.MouseMove, this);
			c.addListener(SWT.MouseDoubleClick, this);
		}
		
		private void colorAll(){
			if (backgroundColor == null){
				backgroundColor = getBackground();
			}
			Color color = null;
			if(isSelected){
				color = selectionColor;
			}else{
				if (mouseOver){
					color = mouseOverColor;
				}else{
					color = backgroundColor;
				}
			}
			
			
			List<Control> kids = new ArrayList<Control>();
			kids.add(this);
			while(!kids.isEmpty()){
				Control c = kids.remove(0);
				c.setBackground(color);
				if (c instanceof Composite){
					for (Control cc : ((Composite)c).getChildren()){
						kids.add(cc);
					}
				}
			}
		}
		
		
		@Override
		public void handleEvent(Event event) {
			if (event.type == SWT.MouseDoubleClick){
				openEntities();
			}else if (event.type == SWT.MouseEnter){
				mouseOver = true;
				colorAll();
			}else if (event.type == SWT.MouseExit){
				
				mouseOver = false;
				colorAll();
			}else if (event.type == SWT.MouseMove){
				
			}else if (event.type == SWT.MouseDown){
				if (event.stateMask == 0 && !isSelected) changeSelection(event);
			}else if (event.type == SWT.MouseUp){
				changeSelection(event);
			}
		}
		
		private void changeSelection(Event event){

			Integer lastSelection = (Integer) getParent().getData(LAST_SELECTION_INDEX_KEY);
			if (lastSelection == null) lastSelection = 0;
			getParent().setData(LAST_SELECTION_INDEX_KEY, index);
			
			if ((event.stateMask & SWT.CTRL) != 0){
				if (event.button == 1) isSelected = !isSelected;
			}else if ((event.stateMask & SWT.SHIFT) != 0){
				boolean newSelection = !isSelected;
				//clearSelection();
				int from = lastSelection;
				int to = index;
				if (index < lastSelection){
					from = index;
					to = lastSelection;
				}
				
				for (int i = from; i <= to; i ++){
					if (i == index){
						siblings.get(i).isSelected = true;
					}else{
						siblings.get(i).isSelected = newSelection;		
					}
					siblings.get(i).colorAll();
				}
				
			}else{
				if (event.button == 1){
					clearSelection();
				}else if (!isSelected){
					clearSelection();
				}
				isSelected = true;
			}
			colorAll();
		}
	}
	
    private static int blend(int v1, int v2, int ratio) {
        int b = (ratio * v1 + (100 - ratio) * v2) / 100;
        return Math.min(255, b);
    }

    /**
     * Blends c1 and c2 based in the provided ratio.
     * 
     * @param c1
     *            first color
     * @param c2
     *            second color
     * @param ratio
     *            percentage of the first color in the blend (0-100)
     * @return the RGB value of the blended color
     * @since 3.1
     */
    private static RGB blend(RGB c1, RGB c2, int ratio) {
        int r = blend(c1.red, c2.red, ratio);
        int g = blend(c1.green, c2.green, ratio);
        int b = blend(c1.blue, c2.blue, ratio);
        return new RGB(r, g, b);
    }
}
