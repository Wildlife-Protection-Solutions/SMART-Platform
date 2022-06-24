/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.report.internal.ui.viewer.parameter;

import java.util.List;
import java.util.Locale;

import org.eclipse.birt.report.engine.api.IParameterDefn;
import org.eclipse.birt.report.engine.api.IScalarParameterDefn;
import org.eclipse.birt.report.engine.api.impl.ParameterSelectionChoice;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.birt.parameter.ISmartBirtParameter;
import org.wcs.smart.birt.parameter.ParameterManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Combo box selection component that supports 
 * BIRT list parameters and the custom SMART list parameters
 * 
 * @author Emily
 *
 */
public class ComboParameterComponent extends AbstractBirtParameter {

	private ComboViewer cmbOptions = null;

	public ComboParameterComponent(IParameterDefn def) {
		super(def);
	}

	@Override
	public void createComposite(Composite parent, IDialogSettings settings, Listener onParameterModified) {
		Object initValue = super.getInitializeValue(settings);

		createNameLabel(parent);
		
		cmbOptions = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbOptions.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbOptions.setContentProvider(ArrayContentProvider.getInstance());
		cmbOptions.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ParameterSelectionChoice) {
					ParameterSelectionChoice cc = (ParameterSelectionChoice)element;
					if (cc.getLabel() == null || cc.getLabel().isBlank()) return cc.getValue().toString();
					return cc.getLabel();
				}
				return element.toString();
			}
		});
		
		List data = null;
		
		if(def.getHandle().getCustomXml() != null && 
				def.getHandle().getCustomXml().startsWith(ISmartBirtParameter.KEY)) {
			String key = def.getHandle().getCustomXml().substring(ISmartBirtParameter.KEY.length());
			
			try(Session session = HibernateManager.openSession()){
				data = ParameterManager.INSTANCE.findParameter(key)
						.getValues(session, SmartDB.getConservationAreaConfiguration().getConservationAreas(), Locale.getDefault());
			}
		}else {
			data = def.getSelectionList();
		}
		
		if (!def.isRequired()) {
			data.add(0, ""); //$NON-NLS-1$
		}
		cmbOptions.setInput(data);
		
		if (initValue != null) {
			if (data.contains(initValue)) {
				cmbOptions.setSelection(new StructuredSelection(initValue));
				
			}else {
				for (Object x : def.getSelectionList()) {
					if (x instanceof ParameterSelectionChoice && ((ParameterSelectionChoice)x).getValue().toString().equals(initValue)) {
						cmbOptions.setSelection(new StructuredSelection(x));
						break;
					}
				}
			}
		}
		if (cmbOptions.getStructuredSelection().isEmpty() && def.isRequired() && !data.isEmpty()) {
			cmbOptions.setSelection(new StructuredSelection(data.get(0)));
		}
		cmbOptions.addSelectionChangedListener(e->onParameterModified.handleEvent(new Event()));
	}

	@Override
	public Object getParameterValue() {
		
		Object x = cmbOptions.getStructuredSelection().getFirstElement();
		if (x == null) return null;

		if ((x instanceof ParameterSelectionChoice)) {
			if (def instanceof IScalarParameterDefn && ((IScalarParameterDefn)def)
					.getScalarParameterType().equalsIgnoreCase(DesignChoiceConstants.SCALAR_PARAM_TYPE_MULTI_VALUE)) {
				return new Object[] {((ParameterSelectionChoice) x).getValue()};
			}
			return ((ParameterSelectionChoice) x).getValue();
		}else {
			return x;
		}
	}

}
