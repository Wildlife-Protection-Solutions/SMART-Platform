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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.InternalEntityManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.search.IntelSearchResult;
import org.wcs.smart.i2.search.IntelSearchResultItem;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.dialogs.ExportEntityToFileDialog;
import org.wcs.smart.i2.ui.editors.record.RecordEditor;
import org.wcs.smart.i2.ui.entity.exporter.EntityRelationshipExportDialog;
import org.wcs.smart.i2.ui.handler.CompareEntitiesHandler;
import org.wcs.smart.i2.ui.handler.NewRecordHandler;
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
	
	private static final int PAGE_SIZE = 50;
	
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

	private int startIndex = 0;
	private int pageSize = PAGE_SIZE;
	
	
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
		
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
	}
	
	/**
	 * set to null to configure loading message; set to empty list of results return 
	 * no records
	 * 
	 * @param entities
	 */
	public void setEntities(IntelSearchResult entities){
		this.entities = entities;
		this.startIndex = 0;
		createTable();
		core.layout(true);
		if (sc != null){
			sc.setMinSize(sc.getChildren()[0].computeSize(sc.getClientArea().width, SWT.DEFAULT));	
		}
		
	}
	
	public List<IntelEntity> getEntities(){
		ArrayList<IntelEntity> selections = new ArrayList<IntelEntity>();
		if (components == null) return selections;
		for (EntityComponent c : components) selections.add(c.getEntity());
		return selections;
	}
	
	public List<IntelEntity> getCurrentSelection(){
		ArrayList<IntelEntity> selections = new ArrayList<IntelEntity>();
		if (components == null) return selections;
		for (EntityComponent c : components){
			if (c.isSelected) selections.add(c.getEntity());
		}
		return selections;
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
			
			createPageControl(top);
			
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
			try(Session s = HibernateManager.openSession()){
				for (int i = startIndex; i < startIndex + pageSize; i ++) {
					if (i >= entities.getAllResults().size()) break;
					IntelSearchResultItem item = entities.getAllResults().get(i);
	
					//load entity
					IntelEntity e = s.get(IntelEntity.class, item.getEntityUuid());
					lazyLoadEntity(e, s);
					
					EntityComponent entityComposite = new EntityComponent(main, item, e, cnt++, components);
					components.add(entityComposite);
					toolkit.adapt(entityComposite);
					entityComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					
					Label l = toolkit.createLabel(main, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));	
				}
			}
			createPageControl(main);
			
			layout(true);
			sc.setMinSize(main.computeSize(sc.getClientArea().width, SWT.DEFAULT));
			core.addListener(SWT.Resize, event -> {
				sc.setMinSize(main.computeSize(sc.getClientArea().width, SWT.DEFAULT));
			});
			createMenu(main);	
		}
		layout(true);
	}
	
	/**
	 * Loads necessary fields from entity for query results returning the
	 * same entity object for convienence
	 * 
	 * @param it
	 * @param session
	 * @return
	 */
	private IntelEntity lazyLoadEntity(IntelEntity it, Session session){
		it.getIdAttributeAsText();
		it.getEntityType();
		it.getEntityType().getIcon();
		if (it.getPrimaryAttachment() != null){
			try {
				it.getPrimaryAttachment().getCopyFromLocation();
				it.getPrimaryAttachment().computeFileLocation(session);
			} catch (Exception e) {
				Intelligence2PlugIn.log("Unable to compute attachment location", e); //$NON-NLS-1$
			}
		}
		return it;
	}
	
	private Composite createPageControl(Composite parent) {
		Composite bottomComp = toolkit.createComposite(parent, SWT.NONE);
		bottomComp.setLayout(new GridLayout(4, false));
		((GridLayout)bottomComp.getLayout()).marginWidth = 0;
		((GridLayout)bottomComp.getLayout()).marginHeight = 0;

		int start = startIndex + 1;
		if (entities.getTotalMatched() == 0) start = 0;
		int end = startIndex + pageSize;
		if (end > entities.getTotalMatched()) {
			end = (int)entities.getTotalMatched();
		}
		toolkit.createLabel(bottomComp, MessageFormat.format(Messages.EntitySearchResultTable_ResultsLabel, start, end,entities.getTotalMatched()));
		
		if (entities.getTotalMatched() <= pageSize) {
			return bottomComp;
		}

		Hyperlink back = toolkit.createHyperlink(bottomComp, "<", SWT.NONE); //$NON-NLS-1$
		back.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				startIndex = startIndex - pageSize;
				if (startIndex < 0) startIndex = 0;
				createTable();
			}
		});
		if (startIndex - pageSize < 0) back.setEnabled(false);
		
		
		Hyperlink more = toolkit.createHyperlink(bottomComp, "...", SWT.NONE); //$NON-NLS-1$
		more.addListener(SWT.MouseDown, e->{
			Shell shell = new Shell(getShell(), SWT.NO_TRIM | SWT.ON_TOP );
			shell.setLayout(new GridLayout());
			((GridLayout)shell.getLayout()).marginWidth = 0;
			((GridLayout)shell.getLayout()).marginHeight = 0;
			
			Composite c = new Composite(shell, SWT.BORDER);
			c.setLayout(new GridLayout());
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			c.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			((GridLayout)c.getLayout()).marginHeight = 1;
			((GridLayout)c.getLayout()).marginWidth = 1;
			((GridLayout)c.getLayout()).verticalSpacing = 1;
			
			Label l = new Label(c, SWT.NONE);
			l.setText(Messages.EntitySearchResultTable_GoToFirstResultItem);
			l.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			l.addListener(SWT.MouseEnter, evt->l.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION)));
			l.addListener(SWT.MouseExit, evt->l.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND)));
			l.addListener(SWT.MouseUp, evt->{startIndex = 0; createTable();shell.close();shell.dispose();});
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Label l2 = new Label(c, SWT.NONE);
			l2.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			l2.setText(MessageFormat.format(Messages.EntitySearchResultTable_GotoLastResultItem, entities.getTotalMatched()));
			l2.addListener(SWT.MouseEnter, evt->l2.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION)));
			l2.addListener(SWT.MouseExit, evt->l2.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND)));
			l2.addListener(SWT.MouseUp, evt->{
				startIndex = (int)((entities.getTotalMatched() / pageSize) * pageSize); 
				createTable();
				shell.close();
				shell.dispose();
			});
			l2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			ScrolledComposite src = new ScrolledComposite(c,  SWT.V_SCROLL | SWT.READ_ONLY);
			src.setExpandHorizontal(true);
			src.setExpandVertical(true);
			src.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			src.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

			Composite core = new Composite(src, SWT.NONE);
			core.setLayout(new GridLayout());
			core.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			((GridLayout)core.getLayout()).marginWidth = 0;
			((GridLayout)core.getLayout()).marginHeight = 0;
			((GridLayout)core.getLayout()).verticalSpacing = 1;
			src.setContent(core);
			for (int i = 0; i < entities.getTotalMatched(); i += pageSize) {
				final int ii = i;
				Label l3 = new Label(core, SWT.NONE);
				l3.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
				l3.setText(MessageFormat.format("{0}-{1}", (i+1), Math.min(i+pageSize, entities.getTotalMatched()))); //$NON-NLS-1$
				l3.addListener(SWT.MouseEnter, evt->l3.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION)));
				l3.addListener(SWT.MouseExit, evt->l3.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND)));
				l3.addListener(SWT.MouseUp, evt->{startIndex = ii; createTable();shell.close();shell.dispose();});
				l3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			}
			src.setMinSize(core.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
			shell.pack();
			shell.setSize(150,  150);
			shell.layout(true);
			more.getParent().getParent().layout(true);
			Point p2 = more.toDisplay(more.getLocation());
			shell.setLocation(p2.x-150, p2.y + more.getSize().y);
			shell.open();
			shell.addListener(SWT.Deactivate, evt->{shell.dispose();});
			
		});
		
		Hyperlink next = toolkit.createHyperlink(bottomComp, ">", SWT.NONE); //$NON-NLS-1$
		next.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				startIndex = startIndex + pageSize;
				createTable();
			}
		});
		if (startIndex + pageSize > entities.getTotalMatched()) next.setEnabled(false);
		
		return bottomComp;
		
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
	
	private void exportEntityToFile(boolean filter){
		List<UUID> toexport = null;
		if (filter) {
			toexport = getCurrentSelection().stream().map(e->e.getUuid()).collect(Collectors.toList());
		}else {
			toexport = entities.getAllResults().stream().map(e->e.getEntityUuid()).collect(Collectors.toList());
		}
		if (toexport.isEmpty()) return;
		ExportEntityToFileDialog dialog = new ExportEntityToFileDialog(getShell(), toexport);
		dialog.open();
	}
	
	private void printEntities(){
		List<UUID> entityUuids = getCurrentSelection().stream().map(e->e.getUuid()).collect(Collectors.toList());
		InternalEntityManager.INSTANCE.printEntities(getShell(), entityUuids);
	}
	
	private void deleteEntities(){
		List<UUID> toDelete = getCurrentSelection().stream().map(e->e.getUuid()).collect(Collectors.toList());
		InternalEntityManager.INSTANCE.deleteEntities(getShell(), context.get(EPartService.class), context.get(IEventBroker.class),toDelete);
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
		
		MenuItem miExport = new MenuItem(menu, SWT.CASCADE);
		miExport.setText(Messages.EntitySearchResultTable_ExportMenu);
		
		Menu exportMenu  = new Menu(miExport);
		miExport.setMenu(exportMenu);
		
		MenuItem mnuExport = new MenuItem(exportMenu, SWT.PUSH);
		mnuExport.setText(Messages.EntitySearchResultTable_ExportMenuItem2);
		mnuExport.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY_EXPORT));
		mnuExport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportEntity();
			}
		});
		
		MenuItem mnuExportXml = new MenuItem(exportMenu, SWT.PUSH);
		mnuExportXml.setText(Messages.EntitySearchResultTable_ExportToXML);
		mnuExportXml.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportEntityToFile(true);
			}
		});
		
		MenuItem mnuExportXml2 = new MenuItem(exportMenu, SWT.PUSH);
		mnuExportXml2.setText(Messages.EntitySearchResultTable_ExportAllEntitiesToXml);
		mnuExportXml2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportEntityToFile(false);
			}
		});
		
		if (IntelSecurityManager.INSTANCE.canDeleteEntity()){
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
		
		
		MenuItem mnuWorkingset = null;
		if (IntelSecurityManager.INSTANCE.canEditWorkingSet()) {
			new MenuItem(menu, SWT.SEPARATOR);
			mnuWorkingset = new MenuItem(menu, SWT.PUSH);
			mnuWorkingset.setText(Messages.EntitySearchResultTable_AddToWsMenuItem);
			mnuWorkingset.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
			mnuWorkingset.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					WorkingSetManager.INSTANCE.addEntityToActiveWorkingSet(getCurrentSelection(), context);
				}
			});
		}
		
		MenuItem fmnuWorkingset = mnuWorkingset;
		menu.addMenuListener(new MenuListener() {
			private MenuItem mnuAddToRecord = null;
			private Menu subRecord = null;
			@Override
			public void menuShown(MenuEvent e) {
				boolean hasSelection = !getCurrentSelection().isEmpty();
				mnuOpen.setEnabled(hasSelection);
				mnuPrint.setEnabled(hasSelection);
				if (fmnuWorkingset != null) fmnuWorkingset.setEnabled(hasSelection && WorkingSetManager.INSTANCE.isSet());
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
				if (IntelSecurityManager.INSTANCE.canCreateRecord()) {
					MenuItem createRecord = new MenuItem(subRecord, SWT.PUSH);
					createRecord.setText(Messages.EntitySearchResultTable_CreateNewRecord);
					createRecord.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
					createRecord.addListener(SWT.Selection, cr->{
						List<UUID> uuids = new ArrayList<>();
						getCurrentSelection().forEach(entity->uuids.add(entity.getUuid()));
						IEclipseContext kid = context.createChild();
						kid.set(NewRecordHandler.ENTITY_UUID_LINK, uuids);
						(new NewRecordHandler()).createNewRecord(kid);
					});
				}
				if (IntelSecurityManager.INSTANCE.canEditRecord()) {
					Collection<MPart> parts = context.get(EPartService.class).getParts();
					boolean first = false;
					for (MPart p : parts){
						if (E3Utils.isCompatibilityEditor(p)){
							Object editor = E3Utils.getSourceObject(p);
							if (editor instanceof RecordEditor && ((RecordEditor)editor).getEditMode()){
								if (!first) {
									new MenuItem(subRecord, SWT.SEPARATOR);
									first = true;
								}
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
	
	private class EntityComponent extends Composite implements Listener{

		private static final String LAST_SELECTION_INDEX_KEY = "last_selection_index"; //$NON-NLS-1$
		
		private IntelSearchResultItem item;
		private Color backgroundColor = null;
		private boolean isSelected = false;
		private boolean mouseOver = false;
		private int index;
		private List<EntityComponent> siblings;
		private IntelEntity entity;
		
		public EntityComponent(Composite parent, IntelSearchResultItem item, IntelEntity entity, int index, List<EntityComponent> siblings){
			super(parent, SWT.NONE);
			this.item = item;
			this.index = index;
			this.siblings = siblings;
			this.entity = entity;
			createPart();
		}
		
		public IntelEntity getEntity() {
			return this.entity;
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
			
			Thumbnail t = new Thumbnail(entity.getPrimaryAttachment(), iconSize.size);
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
			
			Label l = toolkit.createLabel(right, entity.getIdAttributeAsText(), SWT.WRAP);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			l.setFont(boldFont);
			((GridData)l.getLayoutData()).widthHint = 100;
			addListener(l);
			StringBuilder sb = new StringBuilder();
			sb.append(MessageFormat.format(Messages.EntitySearchResultTable_DateCreatedLabel, DateFormat.getDateInstance().format(entity.getDateCreated())));
			sb.append("\n"); //$NON-NLS-1$
			sb.append(MessageFormat.format(Messages.EntitySearchResultTable_DateModifiedLabel, DateFormat.getDateInstance().format(entity.getDateModified())));
			l.setToolTipText(sb.toString());
			
			int spacer = 2;
			Composite typecomp = toolkit.createComposite(right);
			typecomp.setLayout(new GridLayout(2, false));
			((GridLayout)typecomp.getLayout()).marginWidth = 0;
			((GridLayout)typecomp.getLayout()).marginHeight = 0;
			typecomp.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true));
			if (entity.getEntityType().getIcon() != null){
				final Label l1 = toolkit.createLabel(typecomp,""); //$NON-NLS-1$
				l1.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, true));
				l1.setImage(EntityTypeLabelProvider.createImageDescriptor(entity.getEntityType()).createImage());
				l1.addDisposeListener((e)->{if (l1.getImage() != null) l1.getImage().dispose();});
				addListener(l1);
				spacer = 1;
			}
			
			l = toolkit.createLabel(typecomp, EntityTypeLabelProvider.getText(entity.getEntityType()));
			l.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true, spacer, 1));
			addListener(l);
			l.setFont(smallerFont);
			
			
			l = toolkit.createLabel(right, MessageFormat.format(Messages.EntitySearchResultTable_RatingLabel, item.getFormattedRating()));
			if (item.getMatchedString() != null) l.setToolTipText(item.getMatchedString());
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
