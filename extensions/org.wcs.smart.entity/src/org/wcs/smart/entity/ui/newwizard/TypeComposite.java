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
package org.wcs.smart.entity.ui.newwizard;

import java.util.Locale;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.EntityType.Type;

/**
 * Composite for modifying the type property of entity types.
 * @author Emily
 *
 */
public class TypeComposite extends AbstractEntityComposite{

	private ComboViewer typeviewer;
	
	@Override
	public String getName() {
		return Messages.TypeComposite_CompositeName;
	}

	@Override
	public String getDescription() {
		return Messages.TypeComposite_CompositeDescription;
	}

	@Override
	public String validate() {
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.TypeComposite_TypeLabel);
		
		typeviewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		typeviewer.setContentProvider(ArrayContentProvider.getInstance());
		typeviewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return ((EntityType.Type)element).getGuiName(Locale.getDefault());
			}
		});
		typeviewer.setInput(EntityType.Type.values());
		typeviewer.setSelection(new StructuredSelection(EntityType.Type.TRANSIENT));
		typeviewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChange(new Event());
			}
		});
		typeviewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		return main;
	}

	@Override
	public void updateEntityType(EntityType entityType) {
		EntityType.Type type = (Type) ((IStructuredSelection)typeviewer.getSelection()).getFirstElement();
		entityType.setType(type);
		
	}

	@Override
	public void initFields(EntityType entityType, Session session) {
		if (entityType.getType() == null){
			typeviewer.setSelection(new StructuredSelection(EntityType.Type.TRANSIENT));
		}else{
			typeviewer.setSelection(new StructuredSelection(entityType.getType()));
		}
		
	}

}
