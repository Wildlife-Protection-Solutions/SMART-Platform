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
package org.wcs.smart.dataentry.dialog.composite;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Info composite for {@link CmAttribute} of boolean type
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class BooleanAttributeInfoComposite extends CmAttributeInfoComposite {

	private ComboViewer defaultViewer ;
	private boolean initializingControl = false;
	
	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public BooleanAttributeInfoComposite(Composite parent, ConfigurableModel model) {
		super(parent, model);
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.dataentry.dialog.composite.CmAttributeInfoComposite#createTypeSpecificControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createTypeSpecificControls(Composite container) {
		createIsVisibleControl(container);
		createDefaultControl(container);
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				initializingControl = true;
				try{
					CmAttributeOption op = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
					if (op != null && op.getBooleanValue() != null){
						defaultViewer.setSelection(new StructuredSelection(op.getBooleanValue()));
					}else{
						defaultViewer.setSelection(new StructuredSelection("")); //$NON-NLS-1$
					}
				}finally{
					initializingControl = false;
				}
			}
		});
	}
	
	private void createDefaultControl(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.CmAttributeInfoComposite_Option_DefaultValue);
		label.setToolTipText(Messages.BooleanAttributeInfoComposite_defaultValueTooltip);
		
		defaultViewer = new ComboViewer(container, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		defaultViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		defaultViewer.setContentProvider(ArrayContentProvider.getInstance());
		defaultViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof Boolean){
					if ((Boolean)element){
						return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
					}else{
						return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
					}
				}
				return ""; //$NON-NLS-1$
			}
		});
		defaultViewer.setInput(new Object[]{"", Boolean.TRUE, Boolean.FALSE}); //$NON-NLS-1$
		
		defaultViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)defaultViewer.getSelection()).getFirstElement();
				CmAttributeOption op = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
				if (op == null){
					op = CmAttributeOptionFactory.createDefaultValueOption(getSourceObject());
					getSourceObject().getCmAttributeOptions().put(op.getOptionId(),op);
				}
				if (x != null && x instanceof Boolean){
					op.setBooleanValue((Boolean)x);
				}else{
					getSourceObject().getCmAttributeOptions().remove(op.getOptionId());
					op.setBooleanValue(null);
				}
				if (!initializingControl){
					fireModelChanged();
				}
			}
		});
		
		
	}

}
