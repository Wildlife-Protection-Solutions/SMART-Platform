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
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.search.IntelEntitySearchResult;
import org.wcs.smart.i2.search.IntelSearchResult;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.editors.record.RecordEditor;
import org.wcs.smart.i2.ui.handler.CompareEntitiesHandler;
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

	private static final DecimalFormat DFORMAT = new DecimalFormat("0.000");
	
	private Color selectionColor = null;
	private Color mouseOverColor = null;
	
	private Composite core = null;
	private Font smallerFont = null;
	private Font boldFont = null;
	
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
				if (smallerFont != null){
					smallerFont.dispose();
					smallerFont = null;
				}
				if (boldFont != null){
					boldFont.dispose();
					boldFont = null;
				}
			}
		});
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
	
	public List<IntelEntitySearchResult> getEntities(){
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
		
		Label img = toolkit.createLabel(core, "");
		img.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		img.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		
		Label l = toolkit.createLabel(core, MessageFormat.format("Search error: {0}", ex.getMessage()), SWT.WRAP);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 150;
		l.setToolTipText(ex.getMessage());
		
		this.layout(true);
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
			toolkit.createLabel(core, "Searching...");
			sc = null;
		}else{
			toolkit.createLabel(core, MessageFormat.format("{0} of {1} ", entities.getResults().size(), entities.getTotalMatched()));
			
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
			for (IntelEntitySearchResult i : entities.getResults()){		
				EntityComponent entityComposite = new EntityComponent(main, i, cnt++, components);
				components.add(entityComposite);
				toolkit.adapt(entityComposite);
				entityComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				Label l = toolkit.createLabel(main, "", SWT.SEPARATOR | SWT.HORIZONTAL);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
			}
			layout(true);
			sc.setMinSize(main.computeSize(sc.getClientArea().width, SWT.DEFAULT));
			core.addListener(SWT.Resize, event -> {
				sc.setMinSize(main.computeSize(sc.getClientArea().width, SWT.DEFAULT));
			});
			createMenu(main);
		}
		
		layout(true);
	}
	
	private void openEntity(){
		if (!getCurrentSelection().isEmpty()){
			(new OpenEntityHandler()).openEntity(getCurrentSelection().get(0), context);
		}
	}
	
	private void createMenu(Composite parent){
		Menu menu = new Menu(parent);
		
		MenuItem mnuOpen = new MenuItem(menu, SWT.PUSH);
		mnuOpen.setText("Open...");
		mnuOpen.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openEntity();
			}
		});
		MenuItem mnuExport = new MenuItem(menu, SWT.PUSH);
		mnuExport.setText("Export...");
		
		
		MenuItem mnuCompare = new MenuItem(menu, SWT.PUSH);
		mnuCompare.setText("Compare...");
		mnuCompare.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				try{
					(new CompareEntitiesHandler()).compare(getCurrentSelection(), context.get(EPartService.class));
				}catch (Exception ex){
					MessageDialog.openInformation(getShell(), "Error", ex.getMessage());
				}
			}
		});
		
		
		MenuItem mnuWorkingset = new MenuItem(menu, SWT.PUSH);
		mnuWorkingset.setText("Add to Working Set");
		mnuWorkingset.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
		mnuWorkingset.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (IntelEntity ie : getCurrentSelection()){
					WorkingSetManager.INSTANCE.addToActiveWorkingSet(ie, context);
				}
			}
		});
		
		
		
		menu.addMenuListener(new MenuListener() {
			private MenuItem mnuAddToRecord;
			private Menu subRecord ;
			@Override
			public void menuShown(MenuEvent e) {
				boolean hasSelection = !getCurrentSelection().isEmpty();
				mnuOpen.setEnabled(hasSelection);
				mnuExport.setEnabled(hasSelection);
				mnuWorkingset.setEnabled(hasSelection && WorkingSetManager.INSTANCE.isSet());
				mnuCompare.setEnabled(getCurrentSelection().size() > 0);
				
				if (mnuAddToRecord == null || mnuAddToRecord.isDisposed()){
					mnuAddToRecord = new MenuItem(menu, SWT.CASCADE);
					mnuAddToRecord.setText("Add to Record ");
				
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
				if (subRecord.getItemCount() == 0){
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

		private IntelEntitySearchResult item;
		private Color backgroundColor = null;
		private boolean isSelected = false;
		private boolean mouseOver = false;
		private int index;
		private List<EntityComponent> siblings;
		
		public EntityComponent(Composite parent, IntelEntitySearchResult item, int index, List<EntityComponent> siblings){
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
			
			Thumbnail t = new Thumbnail(item.getEntity().getPrimaryAttachment(), 50);
			Composite c = t.createThumbnail(this);
			addListener(c);
			toolkit.adapt(c);
			c.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
			((GridData)c.getLayoutData()).widthHint = 50;
			((GridData)c.getLayoutData()).heightHint = 50;
			
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
			sb.append(MessageFormat.format("Date Created: {0}", DateFormat.getDateInstance().format(item.getEntity().getDateCreated())));
			sb.append("\n");
			sb.append(MessageFormat.format("Date Modified: {0}", DateFormat.getDateInstance().format(item.getEntity().getDateModified())));
			l.setToolTipText(sb.toString());
			
			int spacer = 2;
			Composite typecomp = toolkit.createComposite(right);
			typecomp.setLayout(new GridLayout(2, false));
			((GridLayout)typecomp.getLayout()).marginWidth = 0;
			((GridLayout)typecomp.getLayout()).marginHeight = 0;
			typecomp.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true));
			if (item.getEntity().getEntityType().getIcon() != null){
				final Label l1 = toolkit.createLabel(typecomp,"");
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
			
			
			l = toolkit.createLabel(right, MessageFormat.format("Rating: {0}", DFORMAT.format(item.getRating())));
			l.setToolTipText(item.getMatchString());
			l.setFont(smallerFont);
			addListener(l);
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, true));
			
//			Composite right = toolkit.createComposite(this, SWT.NONE);
//			right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
//			right.setLayout(new GridLayout(2, false));
//			addListener(right);
			
		
//			l = toolkit.createLabel(right, "Modified:");
//			l.setFont(smallerFont);
//			addListener(l);
//			l = toolkit.createLabel(right, DateFormat.getDateInstance().format(item.getEntity().getDateModified()));
//			l.setFont(smallerFont);
//			addListener(l);
			
			
			
			
//			l = toolkit.createLabel(right, "Created:");
//			l.setFont(smallerFont);
//			addListener(l);
//			
//			l = toolkit.createLabel(right, DateFormat.getDateInstance().format(item.getEntity().getDateCreated()));
//			l.setFont(smallerFont);
//			addListener(l);
			
			addDragSources();
		}
		
		private void addDragSources(){
			final IntelEntitySelectionTransfer trans = IntelEntitySelectionTransfer.getTransfer();
			DragSourceAdapter listener = new DragSourceAdapter() {
				
				@Override
				public void dragStart(DragSourceEvent event) {
					IntelEntitySelectionTransfer.getTransfer().setSelection(new StructuredSelection(item));				
				}
				@Override
				public void dragSetData(DragSourceEvent event) {
					if (IntelEntitySelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
						event.data = new StructuredSelection(item);
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
				openEntity();
			}else if (event.type == SWT.MouseEnter){
				mouseOver = true;
				colorAll();
			}else if (event.type == SWT.MouseExit){
				
				mouseOver = false;
				colorAll();
			}else if (event.type == SWT.MouseDown){
				
				Integer lastSelection = (Integer) getParent().getData("last_selection_index");
				if (lastSelection == null) lastSelection = 0;
				getParent().setData("last_selection_index", index);
				
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
