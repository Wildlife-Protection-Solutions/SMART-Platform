/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.diagram.style;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.common.control.ColorSelector;
import org.wcs.smart.common.control.ColorSelector.IColorSelectionChangeListener;
import org.wcs.smart.i2.internal.IntelligenceLabelProviderImpl;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.RelationshipDiagramEdgeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramEdgeStyleOptions.EdgeStyle;

/**
 * Composite containing controls to manage edge style options for relationship diagram.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipDiagramEdgeStyleOptionsComposite extends Composite {
	
	private RelationshipDiagramEdgeStyleOptions options;

	private ColorSelector csEdgeColor;
	private ComboViewer cbEdgeStyle;
	private Button btnShowLabel;

	private boolean fireListeners = true;
	private List<IEdgeStyleOptionsChangeListener> listeners = new ArrayList<>();
	
	public RelationshipDiagramEdgeStyleOptionsComposite(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}
	
	public void setSourceOptions(RelationshipDiagramEdgeStyleOptions options) {
		this.options = options;
		if (options != null) {
			fireListeners = false;
			csEdgeColor.setColor(options.getColor());
			cbEdgeStyle.setSelection(new StructuredSelection(options.getStyle()));
			btnShowLabel.setSelection(options.isShowLabel());
			fireListeners = true;
		}
	}

	private void createContent(Composite parent) {
		Label lblEdgeColor = new Label(parent, SWT.NONE);
		lblEdgeColor.setText(Messages.RelationshipDiagramEdgeStyleOptionsComposite_Color);
		
		csEdgeColor = new ColorSelector(parent);
		csEdgeColor.addColorSelectionChangeListener(new IColorSelectionChangeListener() {
			@Override
			public void colorSelectionChanged(Color color) {
				if (options != null && fireListeners) {
					options.setColor(color);
					fireOptionsChanged(options);
				}
			}
		});
		
		Label lblEdgeStyle = new Label(parent, SWT.NONE);
		lblEdgeStyle.setText(Messages.RelationshipDiagramEdgeStyleOptionsComposite_Style);
		
		cbEdgeStyle = new ComboViewer(parent, SWT.READ_ONLY);
		cbEdgeStyle.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cbEdgeStyle.setContentProvider(ArrayContentProvider.getInstance());
 		cbEdgeStyle.setInput(EdgeStyle.values());
 		cbEdgeStyle.setLabelProvider(new LabelProvider() {
 			@Override
 			public String getText(Object element) {
 				return IntelligenceLabelProviderImpl.getEdgeStyleName((EdgeStyle)element);
 			}
 		});
		cbEdgeStyle.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (options != null && fireListeners) {
					IStructuredSelection sel = (IStructuredSelection) cbEdgeStyle.getSelection();
					if (sel != null && !sel.isEmpty()) {
						options.setStyle((EdgeStyle)sel.getFirstElement());
						fireOptionsChanged(options);
					}
				}
			}
		});
		
		Label lblShowLabel = new Label(parent, SWT.NONE);
		lblShowLabel.setText(Messages.RelationshipDiagramEdgeStyleOptionsComposite_ShowLabel);
		
		btnShowLabel = new Button(parent, SWT.CHECK);
		btnShowLabel.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (options != null && fireListeners) {
					options.setShowLabel(btnShowLabel.getSelection());
					fireOptionsChanged(options);
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
	}
	
	public void addOptionsChangeListener(IEdgeStyleOptionsChangeListener listener) {
		listeners.add(listener);
	}
	
	private void fireOptionsChanged(RelationshipDiagramEdgeStyleOptions ops) {
		for (IEdgeStyleOptionsChangeListener l : listeners) {
			l.optionsChanged(ops);
		}
	}
	
}
