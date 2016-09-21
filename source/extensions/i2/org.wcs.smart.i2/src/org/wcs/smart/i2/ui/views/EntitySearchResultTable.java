package org.wcs.smart.i2.ui.views;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.editors.EntityEditor;
import org.wcs.smart.i2.ui.editors.record.RecordEditor;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.util.E3Utils;

public class EntitySearchResultTable extends Composite {

	private Color selectionColor = null;
	private Color mouseOverColor = null;
	
	private Composite core = null;
	
	private List<IntelEntity> entities;
	private FormToolkit toolkit = null;
	private List<EntityComponent> components = null;
	
	private EPartService pService; 
	
	public EntitySearchResultTable(Composite parent, FormToolkit toolkit, EPartService pService) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		this.pService = pService;
				
		Color color = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
		selectionColor = new Color(parent.getDisplay(), blend(new RGB(255, 255, 255), color.getRGB(), 75));
		mouseOverColor = new Color(parent.getDisplay(), blend(new RGB(255, 255, 255), color.getRGB(), 90));
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				selectionColor.dispose();
			}
		});
	}
	
	/**
	 * set to null to configure loading message; set to empty list of results return 
	 * no records
	 * 
	 * @param entities
	 */
	public void setEntities(List<IntelEntity> entities){
		this.entities = entities;
		createTable();
	}
	
	public List<IntelEntity> getEntities(){
		return this.entities;
	}
	
	public void setSearchError(Exception ex){
		if (core != null){
			core.dispose();
			core = null;
		}
		core = toolkit.createComposite(this, SWT.NONE);
		core.setLayout(new GridLayout(2, false));
		Label img = toolkit.createLabel(this, "");
		img.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		toolkit.createLabel(this, MessageFormat.format("Search error: {0}",ex.getMessage()));
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
		
		if (entities == null){
			toolkit.createLabel(core, "Searching...");
		}else{
			toolkit.createLabel(core, MessageFormat.format("{0} results", entities.size()));
			
			ScrolledComposite sc = new ScrolledComposite(core, SWT.V_SCROLL |  SWT.H_SCROLL);
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
			for (IntelEntity i : entities){		
				EntityComponent entityComposite = new EntityComponent(main, i, cnt++, components);
				components.add(entityComposite);
				toolkit.adapt(entityComposite);
				entityComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				Label l = toolkit.createLabel(main, "", SWT.SEPARATOR | SWT.HORIZONTAL);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
			}
			sc.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
			createMenu(main);
		}
		
		layout(true);
	}
	
	private void openEntity(){
		if (!getCurrentSelection().isEmpty()){
			(new OpenEntityHandler()).openEntity(getCurrentSelection().get(0));
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
		
		MenuItem mnuWorkingset = new MenuItem(menu, SWT.PUSH);
		mnuWorkingset.setText("Add to Working Set");
	
		
		MenuItem mnuAddToRecord = new MenuItem(menu, SWT.CASCADE);
		mnuAddToRecord.setText("Add to Record ");
		Menu subRecord = new Menu(menu);
		mnuAddToRecord.setMenu(subRecord);
		
		menu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				boolean hasSelection = !getCurrentSelection().isEmpty();
				mnuOpen.setEnabled(hasSelection);
				mnuExport.setEnabled(hasSelection);
				mnuWorkingset.setEnabled(hasSelection);
				
				
				for (MenuItem mi : subRecord.getItems()){
					mi.dispose();
				}
				Collection<MPart> parts = pService.getParts();
				for (MPart p : parts){
					if (E3Utils.isCompatibilityEditor(p)){
						Object editor = E3Utils.getSourceObject(p);
						if (editor instanceof RecordEditor && ((RecordEditor)editor).getEditMode()){
							MenuItem relate = new MenuItem(subRecord, SWT.PUSH);
							relate.setText( ((RecordEditor)editor).getEditorInput().getName()  );
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
			if (c.isSelected) selections.add(c.item);
		}
		return selections;
		
	}
	private class EntityComponent extends Composite implements Listener{

		private IntelEntity item;
		private Color backgroundColor = null;
		private boolean isSelected = false;
		private boolean mouseOver = false;
		private int index;
		private List<EntityComponent> siblings;
		
		public EntityComponent(Composite parent, IntelEntity item, int index, List<EntityComponent> siblings){
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
			setLayout(new GridLayout(3, false));
			((GridLayout)getLayout()).marginHeight = 0;
			addListener(this);
			
			Thumbnail t = new Thumbnail(item.getPrimaryAttachment(), 50);
			Composite c = t.createThumbnail(this);
			addListener(c);
			toolkit.adapt(c);
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			((GridData)c.getLayoutData()).widthHint = 50;
			((GridData)c.getLayoutData()).heightHint = 50;
			
			Composite middle = toolkit.createComposite(this, SWT.NONE);
			middle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			middle.setLayout(new GridLayout(2, false));
			addListener(middle);
			
			Label l = toolkit.createLabel(middle, item.getIdAttributeAsText());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			addListener(l);
			l = toolkit.createLabel(middle,"");
			l.setImage(EntityTypeLabelProvider.INSTANCE.getImage(item.getEntityType()));
			addListener(l);
			l = toolkit.createLabel(middle, "");
			l.setText(EntityTypeLabelProvider.INSTANCE.getText(item.getEntityType()));
			addListener(l);
			
			Composite right = toolkit.createComposite(this, SWT.NONE);
			right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			right.setLayout(new GridLayout(2, false));
			addListener(right);
			l = toolkit.createLabel(right, "Modified:");
			addListener(l);
			l = toolkit.createLabel(right, DateFormat.getDateInstance().format(item.getDateModified()));
			addListener(l);
			l = toolkit.createLabel(right, "Created:");
			addListener(l);
			l = toolkit.createLabel(right, DateFormat.getDateInstance().format(item.getDateCreated()));
			addListener(l);
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
					isSelected = !isSelected;
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
					
					clearSelection();
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
