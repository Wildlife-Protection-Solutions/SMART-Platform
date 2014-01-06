package org.wcs.smart.entity.ui.typelist.editor.sightings;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.entity.model.Entity;

public class EntityFilterComposite extends Composite{

	public enum EntityFilter{
		
		ALL("All"),
		ALLACTIVE("Active"),
		CUSTOM("Custom");
		
		private String guiName;
		private EntityFilter(String guiName){
			this.guiName = guiName;
		}
		public String getGuiName(){
			return this.guiName;
		}
	};
	
	private Label lblText;
	private Label lblDescription;
	
	private List<Entity> entities;
	private List<Button> topButtons;
	private CheckboxTableViewer entityListViewer;
	private DropDownFilter filter;
	
	public EntityFilterComposite(Composite parent) {
		super(parent, SWT.BORDER);
		
		GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		setLayout(gl);
		createComposite();
	}
	
	public void setEntityies(List<Entity> entities){
		this.entities = entities;
		if (filter != null){
			filter.setEntityies(entities);
		}
	}
	
	@Override
	public void dispose(){
		super.dispose();
		if (filter != null){
			filter.dispose();
		}
	}
	
	public void createComposite(){
		Composite comp = new Composite(this, SWT.BORDER);
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		comp.setLayout(gl);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lblText = new Label(comp, SWT.NONE);
		lblText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lblText.setText("Data");
		
		Button btnDown = new Button(comp, SWT.ARROW | SWT.DOWN);
		
		btnDown.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				showOptions();	
			}
		});
		
//		lblDescription = new Label(comp, SWT.NONE);
//		lblDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
	}
	
	private void showOptions(){
		if (filter == null){
			filter = new DropDownFilter(getShell());
			filter.setEntityies(entities);
		}
		filter.positionAndShow(lblText);
	}

	
	class DropDownFilter{
		private Shell main;
		
		public DropDownFilter(Shell parent){
			main = new Shell(parent, SWT.SINGLE |SWT.BORDER | SWT.RESIZE);
			
			// close dialog if user selects outside of the shell
			main.addListener(SWT.Deactivate, new Listener() {
				public void handleEvent(Event e){
					hide();
				}
			});
			
			GridLayout gl = new GridLayout(1, false);
			gl.horizontalSpacing = 0;
			gl.verticalSpacing = 0;
			gl.marginWidth = 0;
			gl.marginHeight = 0;
			main.setLayout(gl);
			
			createControl(main);
		}
		
		public void positionAndShow(Control position){
			Rectangle r = position.getBounds();
			Point pnt = position.getParent().toDisplay(r.x, r.y);
			
			main.setBounds(pnt.x + 25, pnt.y + r.height, 200, 150);
			main.setVisible(true);
			
			entityListViewer.getControl().setFocus();
		}
		
		private void createControl(Composite parent){
			topButtons = new ArrayList<Button>();
			Composite top = new Composite(parent, SWT.NONE);
			top.setLayout(new GridLayout());
			top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			for (EntityFilter f : EntityFilter.values()){
				final Button btn = new Button(top, SWT.RADIO);
				btn.setText(f.getGuiName());
				if (f == EntityFilter.ALLACTIVE){
					btn.setSelection(true);
				}
				topButtons.add(btn);
				btn.setData(f);
				
				btn.addSelectionListener(new SelectionAdapter() {
					
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (((EntityFilter)btn.getData()) == EntityFilter.CUSTOM){
							entityListViewer.getControl().setEnabled(true);
						}else{
							//TODO: fire event
							entityListViewer.getControl().setEnabled(false);
							hide();
						}
					}
				});
			}
			
			Label l = new Label(top, SWT.SEPARATOR | SWT.HORIZONTAL);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			entityListViewer = CheckboxTableViewer.newCheckList(top, SWT.BORDER);
			entityListViewer.setContentProvider(ArrayContentProvider.getInstance());
			entityListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			entityListViewer.setLabelProvider(new LabelProvider(){
				public String getText(Object element){
					return ((Entity)element).getId();
				}
			});
			
		}
		
		public void setEntityies(List<Entity> entities){
			entityListViewer.setInput(entities);
		}
		
		public void hide(){
			main.setVisible(false);
		}
		
		public void dispose(){
			System.out.println("disposing");
			main.dispose();
			main = null;
		}
	}
}
