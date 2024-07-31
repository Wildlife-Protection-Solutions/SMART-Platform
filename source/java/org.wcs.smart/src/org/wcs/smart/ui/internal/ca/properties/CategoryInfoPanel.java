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
package org.wcs.smart.ui.internal.ca.properties;


import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.IconPanel;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.properties.DataModelLabelProvider;

/**
 * Category information panel for displaying and editing
 * category information.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CategoryInfoPanel extends Composite {

	protected Button chMultiple;
	private NameKeyComposite nameKeyFields;

	private IconPanel iconPanel;
	
	private TableViewer tblAllAttributes;
	
	/**
	 * Creates a new category information panel
	 * @param parent
	 * @param style
	 * @param canEdit  if the fields should be editable or only viewable
	 * @param createNew if the current category is being modified or created 
	 * @param lang
	 */
	public CategoryInfoPanel(Composite parent, int style, 
			boolean canEdit, boolean createNew) {
		super(parent, style);
		setLayout(new GridLayout(3, false));
		
		nameKeyFields = new NameKeyComposite();
		nameKeyFields.createControls(this, canEdit, createNew, 
				new NameKeyComposite.IChangeListener() {
			@Override
			public void itemModified() {
				validate();
			}
		});
		
		new Label(this, SWT.NONE);
		
		chMultiple = new Button(this, SWT.CHECK);
		chMultiple.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		chMultiple.setText(Messages.CategoryInfoPanel_OpMultiple);
		chMultiple.setSelection(true);
		if (!canEdit){
			chMultiple.setEnabled(false);
		}else{
			chMultiple.addSelectionListener(new SelectionAdapter(){

				@Override
				public void widgetSelected(SelectionEvent e) {
					validate();	
				}
			});
		}
		
		Label ll = new Label(this, SWT.NONE);
		ll.setText(Messages.CategoryInfoPanel_IconLabel);
		ll.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		
		iconPanel = new IconPanel(this, canEdit);
		iconPanel.addListener(SWT.Selection, e->validate());
		iconPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		
		ll = new Label(this, SWT.NONE);
		ll.setText(Messages.CategoryInfoPanel_AttributeDisplayOrder);
		ll.setToolTipText(Messages.CategoryInfoPanel_DisplayOrderTooltip);
		ll.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		tblAllAttributes = new TableViewer(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		tblAllAttributes.setLabelProvider(new DataModelLabelProvider());
		tblAllAttributes.setContentProvider(ArrayContentProvider.getInstance());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd.heightHint = 250;
		tblAllAttributes.getControl().setLayoutData(gd);
		
		if (canEdit) {
			Composite btnPanel = new Composite(this, SWT.NONE);
			btnPanel.setLayout(new GridLayout());
			((GridLayout)btnPanel.getLayout()).marginWidth = 0;
			((GridLayout)btnPanel.getLayout()).marginHeight = 0;
			btnPanel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
			
			Button btnMoveUp = new Button(btnPanel, SWT.PUSH);
			btnMoveUp.setText(Messages.CategoryInfoPanel_MoveUp);
			btnMoveUp.addListener(SWT.Selection, e->move(-1));
			btnMoveUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			btnMoveUp.setEnabled(false);
			
			Button btnMoveDown = new Button(btnPanel, SWT.PUSH);
			btnMoveDown.setText(Messages.CategoryInfoPanel_MoveDown);
			btnMoveDown.addListener(SWT.Selection, e->move(1));
			btnMoveDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			btnMoveDown.setEnabled(false);
			
			tblAllAttributes.addSelectionChangedListener(e->{
				btnMoveUp.setEnabled(!tblAllAttributes.getSelection().isEmpty());
				btnMoveDown.setEnabled(!tblAllAttributes.getSelection().isEmpty());
			});
			
			/* drag and drop support */
			int operations = DND.DROP_MOVE;
			Transfer[] transferTypes = new Transfer[]{LocalSelectionTransfer.getTransfer()};
			tblAllAttributes.addDragSupport(operations, transferTypes, new DragSourceListener() {
				@Override
				public void dragStart(DragSourceEvent event) {
					LocalSelectionTransfer.getTransfer().setSelection(tblAllAttributes.getSelection());
					event.doit = true;
					
				}
				
				@Override
				public void dragSetData(DragSourceEvent event) {
					if (LocalSelectionTransfer.getTransfer()
							.isSupportedType(event.dataType)) {
						event.data = tblAllAttributes.getSelection();
					}
				}
				
				@Override
				public void dragFinished(DragSourceEvent event) {
					LocalSelectionTransfer.getTransfer().setSelection(null);
					tblAllAttributes.refresh();
				}
			});
			
			ViewerDropAdapter dropAdapter = new ViewerDropAdapter(tblAllAttributes) {
				
				@Override
				public boolean validateDrop(Object target, int operation,
						TransferData transferType) {
					return (target instanceof CategoryAttribute);
				}
				
				@Override
				public boolean performDrop(Object data) {
					StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
					if (selection == null){
						return false;
					}
					
					List<CategoryAttribute> items = selection.stream().filter(e->e instanceof CategoryAttribute)
							.map(e->(CategoryAttribute)e)
							.sorted((a,b)->Integer.compare(a.getOrder(), b.getOrder()))
							.collect(Collectors.toList());
					
					CategoryAttribute obj = items.get(0);
					
					CategoryAttribute target = (CategoryAttribute)getCurrentTarget();
					
					if (target.equals(obj)) return false;

					int index = getAttributeList().indexOf(obj);
					int toIndex = getAttributeList().indexOf(target);
					
					if (index == -1 || toIndex == -1) return false;
					
					getAttributeList().removeAll(items);
					toIndex = getAttributeList().indexOf(target);
					if (toIndex == -1) {
						toIndex = index;
					}
					
					if (getCurrentLocation() == LOCATION_AFTER) toIndex++;
						
					for (CategoryAttribute a : items.reversed()) {
						getAttributeList().add(toIndex, a);
					}
					orderList();

					validate();
					return true;
				}
			};
			tblAllAttributes.addDropSupport(operations, transferTypes,dropAdapter);
		}else {
			tblAllAttributes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));	
		}
	}

	private void orderList() {
		for (int i = 0; i < getAttributeList().size(); i++){
			((CategoryAttribute)getAttributeList().get(i)).setOrder(i+1);
		}
	}
	
	private void move(int amount) {
		List<CategoryAttribute> items = tblAllAttributes.getStructuredSelection()
				.stream()
				.filter(e->e instanceof CategoryAttribute)
				.map(e->(CategoryAttribute)e)
				.sorted((a,b)->Integer.compare(a.getOrder(), b.getOrder()))
				.collect(Collectors.toList());
		
		CategoryAttribute obj = items.get(0);
		
		int index = getAttributeList().indexOf(obj);
		if (index == -1) return;
		
		int toIndex = index += amount ;
		if (toIndex < 0) toIndex = 0;
		
		getAttributeList().removeAll(items);
		if (toIndex > getAttributeList().size()) toIndex = getAttributeList().size();

		for (CategoryAttribute a : items.reversed()) {
			getAttributeList().add(toIndex, a);
		}
		orderList();
		tblAllAttributes.refresh();
		validate();
	}
	
	@SuppressWarnings("unchecked")
	private List<CategoryAttribute> getAttributeList(){
		return (List<CategoryAttribute>) this.tblAllAttributes.getInput();
	}
	
	/**
	 * Updates the fields of the composite with the values
	 * from the category.
	 * @param c the category
	 * @param language display language
	 */
	public void setCategory(Category c, Collection<? extends NamedKeyItem> siblings, Language language){
		nameKeyFields.initFields(c, siblings, language);
		chMultiple.setSelection(c.getIsMultiple());
		
		iconPanel.setIcon(c.getIcon());
		
		tblAllAttributes.setInput(c.getAllAttributes());
		((DataModelLabelProvider)tblAllAttributes.getLabelProvider()).setLanguage(language);
		tblAllAttributes.refresh();
		layout(true);
	}
	
	/**
	 * Updates the given category with the fields
	 * from the gui.
	 * 
	 * @param c the category to update
	 */
	public void updateCategory(Category c){
		nameKeyFields.updateFields(c);
		c.setIsMultiple(chMultiple.getSelection());
		c.setIcon(iconPanel.getIcon());
		
		for (CategoryAttribute ca : getAttributeList()) {
			
			CategoryAttribute found = null;
			for(CategoryAttribute o : c.getAllAttributes()) {
				if (o.getAttribute().equals(ca.getAttribute())) {
					found = o;
					break;
				}
			}
			if (found == null) {
				//this should NEVER happen
				throw new IllegalStateException(Messages.CategoryInfoPanel_InvalidDmState);
			}
			found.setOrder(ca.getOrder());			
		}
		
	}
	
	public boolean validate(){
		return nameKeyFields.validate();
	}
	
}
