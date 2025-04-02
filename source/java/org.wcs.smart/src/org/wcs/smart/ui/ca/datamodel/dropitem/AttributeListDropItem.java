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
package org.wcs.smart.ui.ca.datamodel.dropitem;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Attribute list type drop item.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeListDropItem extends DropItem {
	
	protected String text;
	protected String key;
	protected Label lblAttribute;
	protected ComboViewer listViewer;

	private Font smallerFont;
	protected Attribute attribute = null;
	
	protected ListItem currentSelection = null;

	//if true only active list items will be displayed as options
	private boolean onlyActive = false;
	private Session session;
	
	/*
	 * Job to load the attribute list options
	 */
	protected Job loadItemsJobs = new Job("loading datamodel list items"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			ArrayList<ListItem> items;
			if (session == null) {
				items = SmartUtils.doInSessionAndRollback(s->loadData(s));					
			}else {
				items = loadData(session);
			}
			
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					if (listViewer == null || listViewer.getCombo().isDisposed()) return;
					listViewer.setInput(items);
					if (currentSelection != null){
						listViewer.setSelection(new StructuredSelection(currentSelection));
					}else{
						listViewer.setSelection(new StructuredSelection(IDropItemFactory.ANY_OPTION));
					}
					getTargetPanel().redraw();
				}});
			return Status.OK_STATUS;
		}
		
		private ArrayList<ListItem> loadData(Session s) {
			final ArrayList<ListItem> items = new ArrayList<>();

			Attribute a = s.get(Attribute.class, attribute.getUuid());
			if (onlyActive) {
				a.getActiveListItems().forEach(li->items.add(new ListItem(li.getUuid(), li.getName(), li.getKeyId())));
				items.forEach(l->l.getName());
			}else{
				a.getAttributeList().forEach(li->items.add(new ListItem(li.getUuid(), li.getName(), li.getKeyId())));
				items.forEach(l->l.getName());
			}
			
			//add the any item
			items.add(0, IDropItemFactory.ANY_OPTION);				
			if (currentSelection != null && !items.contains(currentSelection)){
				//item is not longer active; but still in query
				items.add(currentSelection);
			}
			return items;
		}
	};


	/**
	 * Creates a new attribute list drop item
	 * 
	 * @param parent parent composite
	 * @param panel drop target
	 * @param att the category attribute to make up the drop item
	 */
	public AttributeListDropItem(CategoryAttribute att) {
		//super(parent, panel);
		this.key = "category:" + att.getCategory().getHkey() + ":attribute:" + att.getAttribute().getType().typeKey + ":" + att.getAttribute().getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.text = att.getAttribute().getName() + " (" + att.getCategory().getFullCategoryName() + ")";  //$NON-NLS-1$//$NON-NLS-2$
		this.attribute = att.getAttribute();
	}

	
	/**
	 * Creates a new attribute list drop item
	 * @param parent parent composite
	 * @param panel drop target
	 * @param att the attribute to make up the drop item
	 */
	public AttributeListDropItem(Attribute att) {
		//super(parent, panel);
		this.key = "attribute:" + att.getType().typeKey + ":" + att.getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$
		this.text = att.getName() ;
		this.attribute = att;
	}
	
	
	protected AttributeListDropItem(String text, String key) {
		this.text = text;
		this.key = key;
	}
	
	
	/**
	 * If set to true only active list items are displayed in drop
	 * down; otherwise all items are displayed 
	 * @param onlyActive
	 */
	public void setOnlyActive(boolean onlyActive) {
		this.onlyActive = onlyActive;
	}

	/**
	 * Sets the active session to use for loading data. If not set 
	 * a new session will be opened. 
	 * This was added in 8.1.0 due to table locking issues with settings
	 * up the visible when expressions for configurable model attributes. In some
	 * cases derby locked the entire i18n table and there for no other session
	 * could read for it.
	 * 
	 * @param session
	 */
	public void setSession(Session session) {
		this.session = session;
	}
	
	/**
	 * @param data - a listItem 
	 */
	public void initializeData(Object data){
		currentSelection = (ListItem)data;
	}
	
	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		loadItemsJobs.cancel();
		if (smallerFont != null){
			smallerFont.dispose();
		}
		
		lblAttribute = null;
		listViewer = null;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return this.text + " = " + listViewer.getCombo().getText(); //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder query = new StringBuilder(this.key);
		query.append(" = "); //$NON-NLS-1$
		
		ListItem it = null;
		if (currentSelection != null){
			it = currentSelection;
		}else{
			IStructuredSelection sel = (IStructuredSelection) listViewer.getSelection();
			if (sel != null && !sel.isEmpty()){
				it = (ListItem) sel.getFirstElement();
			}
		}
		if (it != null && (it.getUuid() != null || it == IDropItemFactory.ANY_OPTION)){			
			query.append(it.getKey());
		}
		return query.toString();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		lblAttribute = new Label(main, SWT.NONE);

		listViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		
		FontData fd = (listViewer.getCombo().getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		listViewer.getCombo().setFont(smallerFont);
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.setLabelProvider(ListItem.createLabelProvider());
		listViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ListItem newSelection = (ListItem) listViewer.getStructuredSelection().getFirstElement();
				ListItem lastSelection = currentSelection;
				
				currentSelection = newSelection;
				if (! (lastSelection != null && lastSelection.equals(newSelection))){
					queryChanged();	
				}
			}
		});
		listViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		listViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		initDrag(main);
		initDrag(lblAttribute);
		
		
		lblAttribute.setText(formatStringForLabel(this.text + " = ")); //$NON-NLS-1$
		loadItemsJobs.schedule();
	}


}
