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
import org.wcs.smart.entity.model.Status;

/**
 * Composite for entity type status field
 * @author Emily
 *
 */
public class StatusComposite extends AbstractEntityComposite {

	private ComboViewer statusCombo ;
	@Override
	public String getName() {
		return Messages.StatusComposite_CompositeName;
	}

	@Override
	public String getDescription() {
		return Messages.StatusComposite_CompositeDescription;
	}

	@Override
	public String validate() {
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(2, false));
		((GridLayout)part.getLayout()).marginWidth = 20;
		part.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label l = new Label(part, SWT.NONE);
		l.setText(Messages.StatusComposite_StatusLabel);
		
		statusCombo = new ComboViewer(part, SWT.DROP_DOWN | SWT.READ_ONLY);
		statusCombo.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		statusCombo.setContentProvider(ArrayContentProvider.getInstance());
		statusCombo.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return ((Status)element).getGuiName(Locale.getDefault());
			}
		});
		statusCombo.setInput(Status.values());
		statusCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChange(new Event());
			}
		});
		return part;
	}

	@Override
	public void updateEntityType(EntityType entityType) {
		entityType.setStatus( (Status) ((IStructuredSelection)statusCombo.getSelection()).getFirstElement() );

	}

	@Override
	public void initFields(EntityType entityType, Session session) {
		statusCombo.setSelection(new StructuredSelection(entityType.getStatus()));
	}

}
