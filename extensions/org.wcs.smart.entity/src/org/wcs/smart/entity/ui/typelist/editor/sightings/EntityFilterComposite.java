package org.wcs.smart.entity.ui.typelist.editor.sightings;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
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

	private static final String FILTER_TEXT = "Entity Filter";
	
	public enum EntityFilter{
		
		ALL("All"),
		ALLACTIVE("Active Only"),
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
	
	private List<Entity> entities;
	private List<Button> topButtons;
	
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
		parent.addDisposeListener(new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				dispose();
			}
		});
	}
	
	public Label getLabel(){
		return this.lblText;
	}
	
	public void setEntities(List<Entity> entities){
		this.entities = entities;
		if (filter != null){
			filter.setEntities(entities);
		}
	}
	
	@Override
	public void dispose(){
		super.dispose();
		if (filter != null){
			filter.dispose();
			filter = null;
		}
	}
	
	public void createComposite(){
		Composite comp = new Composite(this, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		comp.setLayout(gl);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		lblText = new Label(comp, SWT.NONE);
		lblText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		lblText.setText(FILTER_TEXT);
		lblText.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				showOptions();	
			}
		});
		
		Button btnDown = new Button(comp, SWT.ARROW | SWT.DOWN);
		
		btnDown.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				showOptions();	
			}
		});
		
		updateLabel();
	}
	
	private void showOptions(){
		if (filter == null){
			filter = new DropDownFilter(getShell(), new Listener(){

				@Override
				public void handleEvent(Event event) {
					updateLabel();
				}});
			filter.setEntities(entities);
		}
		filter.positionAndShow(this);
	}

	private void updateLabel(){
		EntityFilter lFilter = EntityFilter.ALLACTIVE;
		if (filter != null){
			EntityFilter tmp = filter.getFilter();
			if (tmp != null){
				lFilter = tmp;
			}
		}
		lblText.setText(FILTER_TEXT + " (" + lFilter.guiName + ")");
	}
	
	
	
	
	class DropDownFilter{
		private Shell main;
		
		private Listener changeListener;
		private Shell pShell;
		private CheckboxTableViewer entityListViewer;
		
		private Listener shellListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (isVisible()){
					hide();
				}
			}
		};;
		
		
		public DropDownFilter(Shell parent, Listener listener){
			this.changeListener = listener;
			
			main = new Shell(parent, SWT.SINGLE | SWT.BORDER | SWT.NO_FOCUS);
			main.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_WHITE));
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
			pShell = parent.getShell();
			pShell.addListener(SWT.Move, shellListener);
			pShell.addListener(SWT.Resize, shellListener);
			pShell.addListener(SWT.FocusOut, shellListener);
		}
		
		public void positionAndShow(Control position){
			if (position == null) return;
			
			Rectangle r = position.getBounds();
			Point pnt = position.getParent().toDisplay(r.x, r.y);
			
			main.setBounds(pnt.x, pnt.y + r.height, r.width, 150);
			main.setVisible(true);
			
			entityListViewer.getControl().setFocus();
		}
		
		public EntityFilter getFilter(){
			for (Button btn : topButtons){
				if (btn.getSelection()){
					return (EntityFilter) btn.getData();
				}
			}	
			return null;
		}
		public List<Entity> getSelectedEntities(){
			if (getFilter() == EntityFilter.CUSTOM){
				List<Entity> selection = new ArrayList<Entity>();
				for (Iterator<?> iterator = ((StructuredSelection)entityListViewer.getSelection()).iterator(); iterator.hasNext();) {
					Entity entity = (Entity) iterator.next();
					selection.add(entity);
				}
				return selection;
				
				
			}
			return null;
		}
		
		
		private void createControl(Composite parent){
			topButtons = new ArrayList<Button>();
			Composite top = new Composite(parent, SWT.NONE);
			top.setLayout(new GridLayout());
			top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			top.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			
			for (EntityFilter f : EntityFilter.values()){
				final Button btn = new Button(top, SWT.RADIO);
				btn.setText(f.getGuiName());
				if (f == EntityFilter.ALLACTIVE){
					btn.setSelection(true);
				}
				topButtons.add(btn);
				btn.setData(f);
				btn.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_WHITE));
				btn.addSelectionListener(new SelectionAdapter() {
					
					@Override
					public void widgetSelected(SelectionEvent e) {
						boolean isCustom = false;
						if (((EntityFilter)btn.getData()) == EntityFilter.CUSTOM){
							entityListViewer.getControl().setEnabled(true);
							isCustom = true;
						}else{
							entityListViewer.getControl().setEnabled(false);
						}
						if (btn.getSelection()){
							changeListener.handleEvent(new Event());
							if (!isCustom){
								hide();
							}
						}
						
					}
				});
			}
			
			entityListViewer = CheckboxTableViewer.newCheckList(top, SWT.BORDER);
			entityListViewer.setContentProvider(ArrayContentProvider.getInstance());
			entityListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			((GridData)entityListViewer.getControl().getLayoutData()).horizontalIndent = 20;
			entityListViewer.setLabelProvider(new LabelProvider(){
				public String getText(Object element){
					return ((Entity)element).getId();
				}
			});
			entityListViewer.getControl().setEnabled(false);
			
		}
		
		public void setEntities(List<Entity> entities){
			entityListViewer.setInput(entities);
		}
		
		public void hide(){
			main.setVisible(false);
		}
		
		public void dispose(){
			if (shellListener != null ){
				pShell.removeListener(SWT.Move, shellListener);
				pShell.removeListener(SWT.Resize, shellListener);
				pShell.removeListener(SWT.FocusOut, shellListener);
			}
			changeListener = null;
			main.dispose();
			main = null;
		}
	}
}
