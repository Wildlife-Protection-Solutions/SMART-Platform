package org.wcs.smart.i2.ui.views;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;

public class EntityListComposite extends Composite {

	private Color selectionColor = null;
	
	private Composite core = null;
	
	private List<IntelEntity> entities;
	private FormToolkit toolkit = null;
	
	public EntityListComposite(Composite parent, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		
		
		Color color = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
		selectionColor = new Color(parent.getDisplay(), blend(new RGB(0, 0, 0), color.getRGB(), 25));
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				selectionColor.dispose();
			}
		});
	}
	
	public void setEntities(List<IntelEntity> entities){
		this.entities = entities;
		createTable();
	}
	
	private void createTable(){
		if (core != null){
			core.dispose();
		}
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		core = toolkit.createComposite(this, SWT.NONE);
		core.setLayout(new GridLayout());
		((GridLayout)core.getLayout()).marginWidth = 0;
		((GridLayout)core.getLayout()).marginHeight = 0;
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		ScrolledComposite sc = new ScrolledComposite(core, SWT.V_SCROLL | SWT.BORDER | SWT.H_SCROLL);
		toolkit.adapt(sc);
		Composite main = toolkit.createComposite(sc, SWT.NONE);
		sc.setContent(main);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout());
		main.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		for (IntelEntity i : entities){
			
			
			Composite entityComposite = toolkit.createComposite(main);
			entityComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			entityComposite.setLayout(new GridLayout(3, false));
			EntityEventListener listener = new EntityEventListener(entityComposite);
			addListener(entityComposite, listener);
//			((GridLayout)entityComposite.getLayout()).marginWidth = 5;
//			((GridLayout)entityComposite.getLayout()).marginHeight = 5;
//			
//			entityComposite.addPaintListener(new PaintListener() {
//				
//				@Override
//				public void paintControl(PaintEvent e) {
//					
//					Boolean x = (Boolean) entityComposite.getData("in");
//					
//					if (x != null && x){
//						Rectangle r = entityComposite.getClientArea();
//						e.gc.setBackground(entityComposite.getDisplay().getSystemColor(SWT.COLOR_BLUE));
//						e.gc.drawRectangle(0,0,r.width - 1, r.height - 1);
//					}
//				}
//			});
//			
//			entityComposite.addListener(SWT.MouseEnter, new Listener(){
//
//				@Override
//				public void handleEvent(Event event) {
//					entityComposite.setData("in", true);
//					entityComposite.redraw();
//					
//				}});
//			entityComposite.addListener(SWT.MouseExit, new Listener(){
//
//				@Override
//				public void handleEvent(Event event) {
//					entityComposite.setData("in", false);
//					entityComposite.redraw();
//					
//				}});
			
			Label l = toolkit.createLabel(entityComposite, "IMAGE HERE");
			addListener(l, listener);
			Composite middle = toolkit.createComposite(entityComposite, SWT.NONE);
			middle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			middle.setLayout(new GridLayout(2, false));
			addListener(middle, listener);
			
			l = toolkit.createLabel(middle, i.getIdAttributeAsText());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			addListener(l, listener);
			l = toolkit.createLabel(middle,"");
			l.setImage(EntityTypeLabelProvider.INSTANCE.getImage(i.getEntityType()));
			addListener(l, listener);
			l = toolkit.createLabel(middle, "");
			l.setText(EntityTypeLabelProvider.INSTANCE.getText(i.getEntityType()));
			addListener(l, listener);
			
			Composite right = toolkit.createComposite(entityComposite, SWT.NONE);
			right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			right.setLayout(new GridLayout(2, false));
			addListener(right, listener);
			l = toolkit.createLabel(right, "Modified:");
			addListener(l, listener);
			l = toolkit.createLabel(right, DateFormat.getDateInstance().format(i.getDateModified()));
			addListener(l, listener);
			l = toolkit.createLabel(right, "Created:");
			addListener(l, listener);
			l = toolkit.createLabel(right, DateFormat.getDateInstance().format(i.getDateCreated()));
			addListener(l, listener);
			
			l = toolkit.createLabel(main, "", SWT.SEPARATOR | SWT.HORIZONTAL);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			
		}
		sc.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		layout(true);
	}
	
	private void addListener(Control c, EntityEventListener l){
		c.addListener(SWT.MouseEnter, l);
		c.addListener(SWT.MouseExit, l);
	}
	
	

	private class EntityEventListener implements Listener{

		private Composite parent;
		public EntityEventListener(Composite parent){
			this.parent = parent;
		}
		
		private void colorAll(Color color){
			List<Control> kids = new ArrayList<Control>();
			kids.add(parent);
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
			if (event.type == SWT.MouseEnter){
				
				
				colorAll(selectionColor);
				
			}else if (event.type == SWT.MouseExit){
				colorAll(event.widget.getDisplay().getSystemColor(SWT.COLOR_WHITE));
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
