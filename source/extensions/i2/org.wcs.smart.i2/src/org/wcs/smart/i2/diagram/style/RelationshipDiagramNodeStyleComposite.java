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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions;

/**
 * Composite containing controls to manage node style for relationship diagram.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipDiagramNodeStyleComposite extends Composite {

	private Button btnOverride;
	private RelationshipDiagramNodeStyleOptionsComposite nodeCmp;

	private boolean fireListeners = true;
	private List<INodeStyleOptionsChangeListener> listeners = new ArrayList<>();
	
	public RelationshipDiagramNodeStyleComposite(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}
	
	public void setSourceOptions(RelationshipDiagramNodeStyleOptions options) {
		fireListeners = false;
		btnOverride.setSelection(options != null);
		nodeCmp.setVisible(options != null);
		nodeCmp.setSourceOptions(options);
		fireListeners = true;
		this.layout(true);
	}
	
	private void handleOverrideChanged() {
		RelationshipDiagramNodeStyleOptions newOptions = btnOverride.getSelection() ? RelationshipDiagramStyleFactory.createDefaultNodeOptions() : null;
		fireOptionsChanged(newOptions);
		setSourceOptions(newOptions);
	}

	private void createContent(Composite parent) {
		btnOverride = new Button(parent, SWT.CHECK);
		btnOverride.setText(Messages.RelationshipDiagramNodeStyleComposite_Override);
		btnOverride.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fireListeners) {
					handleOverrideChanged();
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		nodeCmp = new RelationshipDiagramNodeStyleOptionsComposite(parent);
		nodeCmp.addOptionsChangeListener(new INodeStyleOptionsChangeListener() {
			@Override
			public void optionsChanged(RelationshipDiagramNodeStyleOptions options) {
				if (fireListeners) {
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
