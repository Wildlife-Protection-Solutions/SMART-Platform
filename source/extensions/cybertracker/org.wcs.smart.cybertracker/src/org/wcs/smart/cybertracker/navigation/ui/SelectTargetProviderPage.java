/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.navigation.ui;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.cybertracker.internal.Messages;

/**
 * Wizard page for selecting the target provider
 * 
 * @author Emily
 *
 */
public class SelectTargetProviderPage extends WizardPage {

	public static final String PAGE_NAME = "Select Provider"; //$NON-NLS-1$
	
	private TableViewer options;
	private INavigationLayerTargetProvider selection = null;
	
	protected SelectTargetProviderPage() {
		super(PAGE_NAME);
	}

	public INavigationLayerTargetProvider getProvider() {
		return selection;
	}
	
	@Override
	public void createControl(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label t = new Label(main, SWT.NONE);
		t.setText(Messages.SelectTargetProviderPage_ImportSource);
		
		Composite wrapper = new Composite(main, SWT.NONE);
		wrapper.setLayout(new TableColumnLayout());
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		options = new TableViewer(wrapper, SWT.BORDER | SWT.V_SCROLL);
		options.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((INavigationLayerTargetProvider)element).getTypeName();
			}
			@Override
			public Image getImage(Object element) {
				return ((INavigationLayerTargetProvider)element).getImage();
			}
		});
		
		options.setContentProvider(ArrayContentProvider.getInstance());
		options.setInput(((TargetImportWizard)getWizard()).getTargetProviders());
		
		TableViewerColumn tc = new TableViewerColumn(options, SWT.NONE);
		((TableColumnLayout)wrapper.getLayout()).setColumnData(tc.getColumn(), new ColumnWeightData(100));
		
		options.addDoubleClickListener(e->{
			selection = (INavigationLayerTargetProvider) options.getStructuredSelection().getFirstElement();
			getWizard().getContainer().showPage(getNextPage());
		});
		
		options.addSelectionChangedListener(e->{
			selection = (INavigationLayerTargetProvider) options.getStructuredSelection().getFirstElement();
			getWizard().getContainer().updateButtons();
		});
		setControl(main);
		
		setTitle(Messages.SelectTargetProviderPage_Title);
		setMessage(Messages.SelectTargetProviderPage_Message);
	}

    @Override
	public IWizardPage getNextPage() {
    	if (options == null) return null;
    	if (getProvider() == null) return null;
    	
    	return getProvider().getPages().get(0);
    }
}
