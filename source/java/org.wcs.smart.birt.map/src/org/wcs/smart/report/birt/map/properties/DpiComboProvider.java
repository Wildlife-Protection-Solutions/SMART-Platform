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
package org.wcs.smart.report.birt.map.properties;

import org.eclipse.birt.report.designer.internal.ui.swt.custom.FormWidgetFactory;
import org.eclipse.birt.report.designer.internal.ui.util.WidgetUtil;
import org.eclipse.birt.report.designer.internal.ui.views.attributes.widget.PropertyDescriptor;
import org.eclipse.birt.report.designer.ui.util.ExceptionUtil;
import org.eclipse.birt.report.designer.ui.views.attributes.providers.ChoiceSetFactory;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.metadata.IChoice;
import org.eclipse.birt.report.model.api.metadata.IChoiceSet;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Property Descriptor for DPI map options that displays the dpi options in a
 * combo. DPI options are fixed.
 * 
 * @author Emily
 *
 */
public class DpiComboProvider extends PropertyDescriptor {

	private int style = SWT.BORDER | SWT.READ_ONLY;

	protected ComboViewer viewer;
	protected IChoiceSet choiceSet;
	protected IChoice oldValue;

	public DpiComboProvider(boolean formStyle) {
		setFormStyle(formStyle);
	}

	public void setInput(Object handle) {
		this.input = handle;
		getDescriptorProvider().setInput(input);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.birt.report.designer.ui.attributes.widget.PropertyDescriptor
	 * #resetUIData()
	 */
	private void refresh() {

		if (getDescriptorProvider() instanceof DpiPropertyProvider) {

			DpiPropertyProvider provider = (DpiPropertyProvider) getDescriptorProvider();

			Object lastValue = ((DpiPropertyProvider) getDescriptorProvider()).load();
			
			if (viewer.getInput() != null && oldValue != null
					&& oldValue.getValue().equals(lastValue.toString()))
				//no change; no need to go further
				return;

			IChoice[] choices = ChoiceSetFactory.getElementChoiceSet(provider.getElement(), provider.getProperty()).getChoices();
			viewer.setInput(choices);
			for (IChoice c : choices) {
				if (c.getValue().equals(lastValue.toString())) {
					oldValue = c;
					break;
				}
			}

			if (provider.isReadOnly()) {
				viewer.getControl().setEnabled(false);
			} else {
				viewer.getControl().setEnabled(true);
			}
			if (oldValue != null) {
				viewer.setSelection(new StructuredSelection(oldValue));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.birt.report.designer.internal.ui.views.attributes.widget.
	 * PropertyDescriptor#getControl()
	 */
	public Control getControl() {
		return viewer.getControl();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.extensions.IPropertyDescriptor#
	 * createControl(org.eclipse.swt.widgets.Composite)
	 */
	public Control createControl(Composite parent) {
		CCombo combo;
		if (isFormStyle()) {
			combo = FormWidgetFactory.getInstance().createCCombo(parent);
		} else {
			combo = new CCombo(parent, style);
			combo.setVisibleItemCount(30);
		}
		viewer = new ComboViewer(combo);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof IChoice) {
					return ((IChoice) element).getDisplayName();
				}
				return super.getText(element);
			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				handleComboSelectEvent();

			}
		});
		return viewer.getControl();
	}

	/**
	 * Processes the save action.
	 */
	private void handleComboSelectEvent() {
		try {
			if (!viewer.getSelection().isEmpty()) {
				IChoice c = (IChoice) ((IStructuredSelection) viewer
						.getSelection()).getFirstElement();
				save(c.getValue());
			} else {
				save(null);
			}
		} catch (SemanticException e) {
			viewer.setSelection(new StructuredSelection(oldValue));
			ExceptionUtil.handle(e);
		}
	}

	@Override
	public void save(Object value) throws SemanticException {
		getDescriptorProvider().save(value);
	}

	public void setHidden(boolean isHidden) {
		WidgetUtil.setExcludeGridData(viewer.getControl(), isHidden);
	}

	public void setVisible(boolean isVisible) {
		viewer.getControl().setVisible(isVisible);
	}

	@Override
	public void load() {
		refresh();
	}

}