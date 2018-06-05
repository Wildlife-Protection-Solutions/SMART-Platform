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
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.common.control.ColorSelector;
import org.wcs.smart.common.control.ColorSelector.IColorSelectionChangeListener;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions.ImageSizeOption;

/**
 * Composite containing controls to manage node style options for relationship diagram.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipDiagramNodeStyleOptionsComposite extends Composite {
	
	private RelationshipDiagramNodeStyleOptions options;
	
	private ComboViewer cmbImageSize;
	private ColorSelector csBackgroundColor;
	private ColorSelector csForegroundColor;
	
	private boolean fireListeners = true;
	private List<INodeStyleOptionsChangeListener> listeners = new ArrayList<>();

	public RelationshipDiagramNodeStyleOptionsComposite(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}
	
	public void setSourceOptions(RelationshipDiagramNodeStyleOptions options) {
		this.options = options;
		if (options != null) {
			fireListeners = false;
			cmbImageSize.setSelection(new StructuredSelection(options.getImageSize()));
			csBackgroundColor.setColor(options.getBackgroudColor());
			csForegroundColor.setColor(options.getForegroundColor());
			fireListeners = true;
		}
	}
	
	private void createContent(Composite parent) {
		Label lblImageSize = new Label(parent, SWT.NONE);
		lblImageSize.setText("Image Size:");
		
		cmbImageSize = new ComboViewer(this, SWT.READ_ONLY | SWT.BORDER);
		cmbImageSize.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		cmbImageSize.setContentProvider(ArrayContentProvider.getInstance());
		cmbImageSize.setInput(ImageSizeOption.values());
		cmbImageSize.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (options != null && fireListeners) {
					IStructuredSelection sel = (IStructuredSelection) cmbImageSize.getSelection();
					options.setImageSize((ImageSizeOption)sel.getFirstElement());
					fireOptionsChanged(options);
				}
			}
		});

		Label lblBackgroundColor = new Label(parent, SWT.NONE);
		lblBackgroundColor.setText("Background Color:");
		
		csBackgroundColor = new ColorSelector(parent);
		csBackgroundColor.addColorSelectionChangeListener(new IColorSelectionChangeListener() {
			@Override
			public void colorSelectionChanged(Color color) {
				if (options != null && fireListeners) {
					options.setBackgroudColor(color);
					fireOptionsChanged(options);
				}
			}
		});

		Label lblForegroundColor = new Label(parent, SWT.NONE);
		lblForegroundColor.setText("Foreground Color:");
		
		csForegroundColor = new ColorSelector(parent);
		csForegroundColor.addColorSelectionChangeListener(new IColorSelectionChangeListener() {
			@Override
			public void colorSelectionChanged(Color color) {
				if (options != null && fireListeners) {
					options.setForegroundColor(color);
					fireOptionsChanged(options);
				}
			}
		});
	}

	public void addOptionsChangeListener(INodeStyleOptionsChangeListener listener) {
		listeners.add(listener);
	}
	
	private void fireOptionsChanged(RelationshipDiagramNodeStyleOptions ops) {
		for (INodeStyleOptionsChangeListener l : listeners) {
			l.optionsChanged(ops);
		}
	}

}
