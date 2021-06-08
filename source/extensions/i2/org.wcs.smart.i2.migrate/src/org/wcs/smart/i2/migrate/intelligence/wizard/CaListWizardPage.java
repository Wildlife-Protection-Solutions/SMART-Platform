/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.i2.migrate.intelligence.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.migrate.internal.Messages;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;

/**
 * List Conservation Areas with intelligence records
 * 
 * @author Emily
 *
 */
public class CaListWizardPage extends WizardPage {

	private CheckboxTableViewer tblConservationAreas;
	
	protected CaListWizardPage() {
		super("CALISTPAGE"); //$NON-NLS-1$
	}

	public List<ConservationArea> getConservationAreas() {
		List<ConservationArea> items = new ArrayList<>();
		for (Object c : tblConservationAreas.getCheckedElements()) {
			if (c instanceof ConservationArea) {
				items.add((ConservationArea) c);
			}
		}
		return items;
	}
	
	@Override
	public boolean isPageComplete() {
		return tblConservationAreas.getCheckedElements().length != 0;
	}
	
	@Override
	public void createControl(Composite parent) {
	
		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new GridLayout());
		((GridLayout)temp.getLayout()).marginWidth = 0;
		((GridLayout)temp.getLayout()).marginHeight = 0;
		
		tblConservationAreas = CheckboxTableViewer.newCheckList(temp,  SWT.MULTI);
		tblConservationAreas.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblConservationAreas.getTable().addKeyListener(new CheckboxSelectorKeyAdapter(tblConservationAreas));
		tblConservationAreas.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof ConservationArea){
					return ((ConservationArea)element).getNameLabel();
				}
				return super.getText(element);
			}
		});
		tblConservationAreas.setContentProvider(ArrayContentProvider.getInstance());
		tblConservationAreas.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				getContainer().updateButtons();	
			}
		});
		setControl(temp);
		
		setTitle(Messages.CaListWizardPage_Title);
		setMessage(Messages.CaListWizardPage_Message);
	}

	public void setConservationArea(List<ConservationArea> cas) {
		tblConservationAreas.setInput(cas);
	}
}
