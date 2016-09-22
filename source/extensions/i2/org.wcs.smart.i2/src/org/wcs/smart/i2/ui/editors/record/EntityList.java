package org.wcs.smart.i2.ui.editors.record;

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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
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
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.i2.ui.views.EntitySearchResultTable;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.E3Utils;

public class EntityList extends Composite {

	private Color selectionColor = null;
	private Color mouseOverColor = null;
	
	private Composite core = null;
	
	private List<IntelEntityRecord> entities;
	private FormToolkit toolkit = null;
	private List<EntityComponent> components = null;
	
	private EntityListComposite listParent;
	private MenuItem mnuDelete;
	private MenuItem mnuOpen;
	
	public int entityColumnCnt = 2;
	
	public EntityList(EntityListComposite parent, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		this.listParent = parent;
		
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginHeight = 0;
		((GridLayout)getLayout()).marginWidth = 0;
		
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
	public void setEntities(List<IntelEntityRecord> entities){
		this.entities = entities;
		createTable();
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
		
		if (entities == null){
			toolkit.createLabel(core, "");
		}else{
			ScrolledComposite sc = new ScrolledComposite(core, SWT.V_SCROLL |  SWT.H_SCROLL);
			toolkit.adapt(sc);
			Composite main = toolkit.createComposite(sc, SWT.NONE);
			sc.setContent(main);
			sc.setExpandHorizontal(true);
			sc.setExpandVertical(true);
			main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			main.setLayout(new GridLayout(entityColumnCnt, true));
			((GridLayout)main.getLayout()).marginWidth = 0;
			((GridLayout)main.getLayout()).marginHeight = 0;
			main.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));

			components = new ArrayList<EntityComponent>();
			int cnt = 0;
			for (IntelEntityRecord i : entities){		
				EntityComponent entityComposite = new EntityComponent(main, i, cnt++, components);
				components.add(entityComposite);
				toolkit.adapt(entityComposite);
				entityComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			}
			sc.setMinSize(computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
			createMenu(main);
		}
		
		layout(true);
	}
	
	private void openEntity(){
		if (!getCurrentSelection().isEmpty()){
			(new OpenEntityHandler()).openEntity(getCurrentSelection().get(0), listParent.getEditor().getContext());
		}
	}
	
	private void createMenu(Composite parent){
		Menu mnuEntities = new Menu(parent);
		
		mnuEntities.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				
				if (!listParent.getEditor().getEditMode()){
					if (mnuDelete != null){
						mnuDelete.dispose();
						mnuDelete = null;
					}
				}else{
					if (mnuDelete == null || mnuDelete.isDisposed()){
						mnuDelete = new MenuItem(mnuEntities, SWT.PUSH);
						mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
						mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
						mnuDelete.addSelectionListener(new SelectionAdapter() {
							
							@Override
							public void widgetSelected(SelectionEvent e) {
								listParent.deleteEntityLink();
							}
						});
					}
				}
				mnuOpen.setEnabled(!getCurrentSelection().isEmpty());
				if (mnuDelete != null && !mnuDelete.isDisposed()) mnuDelete.setEnabled(!getCurrentSelection().isEmpty());
				
				
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		
		mnuOpen = new MenuItem(mnuEntities, SWT.PUSH);
		mnuOpen.setText("Open");
		mnuOpen.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openEntity();
			}
		});
		
		
		
		List<Control> kids = new ArrayList<Control>();
		kids.add(parent);
		while(!kids.isEmpty()){
			Control c = kids.remove(0);
			if (c.getMenu() == null){
				c.setMenu(mnuEntities);
			}
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

		private static final int THUMB_SIZE = 30;
		
		private IntelEntityRecord item;
		private Color backgroundColor = null;
		private boolean isSelected = false;
		private boolean mouseOver = false;
		private int index;
		private List<EntityComponent> siblings;
		
		public EntityComponent(Composite parent, IntelEntityRecord item, int index, List<EntityComponent> siblings){
			super(parent, SWT.BORDER);
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
			
			Thumbnail t = new Thumbnail(item.getEntity().getPrimaryAttachment(), THUMB_SIZE);
			Composite c = t.createThumbnail(this, SWT.NONE);
			addListener(c);
			toolkit.adapt(c);
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			((GridData)c.getLayoutData()).widthHint = THUMB_SIZE;
			((GridData)c.getLayoutData()).heightHint = THUMB_SIZE;
			
			
			Label l = toolkit.createLabel(this, item.getEntity().getIdAttributeAsText());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			addListener(l);
			
			List<IntelRecordAttachment> allAttachments = listParent.getEditor().getRecord().getAttachments();
			List<IntelAttachment> toShow1 = new ArrayList<IntelAttachment>();
			List<IntelEntityAttachment> toShow2 = new ArrayList<IntelEntityAttachment>();
			for (IntelEntityAttachment att : item.getEntity().getEntityAttachments()){
				for (IntelRecordAttachment a : allAttachments){
					if (att.getAttachment().equals(a.getAttachment())){
						toShow1.add(a.getAttachment());
						break;
					}
				}
			}
			for (IntelEntityAttachment att : listParent.getEditor().getAttachmentPanel().getNewEntityAttachments()){
				if(att.getEntity().equals(item.getEntity())){
					for (IntelRecordAttachment a : allAttachments){
						if (att.getAttachment().equals(a.getAttachment())){
							toShow2.add(att);
							break;
						}
					}
				}
			}
			
			Composite attach = toolkit.createComposite(this, SWT.NONE);
			attach.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			RowLayout layout = new RowLayout();
			layout.wrap = false;
			layout.marginBottom = layout.marginLeft = layout.marginRight = layout.marginTop = 0;
			attach.setLayout(layout);
			
			for (IntelAttachment att : toShow1){
				Thumbnail thumb = new Thumbnail(att, THUMB_SIZE-5);
				Composite c2 = thumb.createThumbnail(attach, SWT.NONE);
				toolkit.adapt(c2);
				c2.setLayoutData(new RowData(THUMB_SIZE-5,THUMB_SIZE-5));
				
			}
			for (IntelEntityAttachment att : toShow2){
				Thumbnail thumb = new Thumbnail(att.getAttachment(), THUMB_SIZE-2);
				Composite c2 = thumb.createThumbnail(attach, SWT.BORDER);
				toolkit.adapt(c2);
				c2.setLayoutData(new RowData(THUMB_SIZE-2,THUMB_SIZE-2));
				
				Menu menu = new Menu(c2);
				c2.setMenu(menu);
				MenuItem mi = new MenuItem(menu, SWT.PUSH);
				mi.setText("Remove Attachment");
				mi.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
				mi.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						listParent.getEditor().getRecord().getEntities().remove(att);
						listParent.getEditor().getAttachmentPanel().getNewEntityAttachments().remove(att);
						setEntities(listParent.getEditor().getRecord().getEntities());
					}
				});
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
