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
package org.wcs.smart.entity.ui.editor.sightings;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.query.EntityFilter;
import org.wcs.smart.entity.query.EntityFilter.EntityFilterType;
/**
 * An entity filter composite that allows users to define
 * an entity filter.
 * 
 * @author Emily
 *
 */
public class EntityFilterComposite extends Composite{


	private Label lblText;
	private List<Entity> entities;
	private List<Button> topButtons;
	private Composite comp;
	private Button btnDown ;
	private DropDownFilter filter;
	private static EntityFilter DEFAULT_FILTER = new EntityFilter(EntityFilterType.ALLACTIVE);
	
	/**
	 * Creates a new entity filter
	 * @param parent
	 */
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

	/**
	 * 
	 * @return the selected filter
	 */
	public EntityFilter getFilter(){
		if (filter == null){
			return DEFAULT_FILTER;
		}else{
			return filter.getFilter();
		}
	}
	
	/**
	 * Sets the entity filter options.
	 * 
	 * @param entities
	 */
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
	
	private void createComposite(){
		comp = new Composite(this, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		comp.setLayout(gl);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		lblText = new Label(comp, SWT.NONE);
		lblText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		lblText.setText(""); //$NON-NLS-1$
		lblText.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (filter != null && filter.main.isVisible()){
					filter.hide();
				}else{
					showOptions();
				}
			}
		});
		
		btnDown = new Button(comp, SWT.ARROW | SWT.DOWN);
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
		if (filter != null){
			EntityFilter tmp = filter.getFilter();
			lblText.setText(tmp.asString());
		}else{
			lblText.setText(DEFAULT_FILTER.asString());			
		}

	}
	
	/**
	 * Adapts the composite for the toolkit
	 * @param toolkit
	 */
	public void adapt(FormToolkit toolkit) {
		toolkit.adapt(this);
		toolkit.adapt(comp, false, false);
		toolkit.adapt(lblText, false, false);
		toolkit.adapt(btnDown, true, true);
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
					EntityFilterType t = (EntityFilterType) btn.getData();
					if (t == EntityFilterType.CUSTOM){
						return new EntityFilter(t, getSelectedEntities());
					}else{
						return new EntityFilter(t);
					}
				}
			}	
			return null;
		}
		
		private List<Entity> getSelectedEntities(){
			List<Entity> selection = new ArrayList<Entity>();
			for (Object x : entityListViewer.getCheckedElements()){
				selection.add((Entity)x);
			}
			return selection;
		}
		
		
		private void createControl(Composite parent){
			topButtons = new ArrayList<Button>();
			Composite top = new Composite(parent, SWT.NONE);
			top.setLayout(new GridLayout());
			top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			top.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			
			for (EntityFilterType f : EntityFilterType.values()){
				final Button btn = new Button(top, SWT.RADIO);
				btn.setText(f.getGuiName());
				if (f == EntityFilterType.ALLACTIVE){
					btn.setSelection(true);
				}
				topButtons.add(btn);
				btn.setData(f);
				btn.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_WHITE));
				btn.addSelectionListener(new SelectionAdapter() {
					
					@Override
					public void widgetSelected(SelectionEvent e) {
						boolean isCustom = false;
						if (((EntityFilterType)btn.getData()) == EntityFilterType.CUSTOM){
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
