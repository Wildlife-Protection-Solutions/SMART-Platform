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
package org.wcs.smart.event.ui.filter;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;

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
	
	protected AttributeListItem currentSelection = null;

	/*
	 * Job to load the attribute list options
	 */
	protected Job loadItemsJobs = new Job("Loading datamodel list items"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			final ArrayList<AttributeListItem> items = new ArrayList<AttributeListItem>();
			try(Session s = HibernateManager.openSession()){
				s.beginTransaction();
				try{
					Attribute a = s.get(Attribute.class, attribute.getUuid());
					items.addAll(a.getAttributeList());
					items.forEach(l->l.getName());
					
					//add the any item
					items.add(0, DropItemFactory.ANY_ATTRIBUTE);				
					if (currentSelection != null && !items.contains(currentSelection)){
						//item is not longer active; but still in query
						items.add(currentSelection);
					}
				}finally{
					s.getTransaction().rollback();
				}
			}
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					if (listViewer == null || listViewer.getCombo().isDisposed()) return;
					listViewer.setInput(items);
					if (currentSelection != null){
						listViewer.setSelection(new StructuredSelection(currentSelection));
					}else{
						listViewer.setSelection(new StructuredSelection(DropItemFactory.ANY_ATTRIBUTE));
					}
					getTargetPanel().redraw();
				}});
			return Status.OK_STATUS;
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
	
	
	/**
	 * @param data - a listItem 
	 */
	public void initializeData(Object data){
		currentSelection = (AttributeListItem)data;
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
		
		AttributeListItem it = null;
		if (currentSelection != null){
			it = currentSelection;
		}else{
			IStructuredSelection sel = (IStructuredSelection) listViewer.getSelection();
			if (sel != null && !sel.isEmpty()){
				it = (AttributeListItem) sel.getFirstElement();
			}
		}
		if (it != null && (it.getUuid() != null || it == DropItemFactory.ANY_ATTRIBUTE)){			
			query.append(it.getKeyId());
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
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		lblAttribute = new Label(main, SWT.NONE);

		listViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		
		FontData fd = (listViewer.getCombo().getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		listViewer.getCombo().setFont(smallerFont);
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof AttributeListItem) return ((AttributeListItem) element).getName();
				return super.getText(element);
			}
		});
		
		listViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				AttributeListItem newSelection = (AttributeListItem) listViewer.getStructuredSelection().getFirstElement();
				AttributeListItem lastSelection = currentSelection;
				
				currentSelection = newSelection;
				if (! (lastSelection != null && lastSelection.equals(newSelection))){
					queryChanged();	
				}
			}
		});
		listViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		initDrag(main);
		initDrag(lblAttribute);
		
		
		lblAttribute.setText(formatStringForLabel(this.text + " = ")); //$NON-NLS-1$
		loadItemsJobs.schedule();
	}


}
