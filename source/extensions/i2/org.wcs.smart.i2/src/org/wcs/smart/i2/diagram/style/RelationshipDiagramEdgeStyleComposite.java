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
import org.eclipse.swt.widgets.Control;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.RelationshipDiagramEdgeStyleOptions;

/**
 * Composite containing controls to manage edge style for relationship diagram.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipDiagramEdgeStyleComposite extends Composite {

	private Button btnOverride;
	private RelationshipDiagramEdgeStyleOptionsComposite edgeCmp;

	private boolean fireListeners = true;
	private List<IEdgeStyleOptionsChangeListener> listeners = new ArrayList<>();
	
	public RelationshipDiagramEdgeStyleComposite(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}
	
	private void enableAll(Composite c, boolean state) {
		List<Control> items = new ArrayList<>();
		items.add(c);
		while(!items.isEmpty()) {
			Control kid = items.remove(0);
			kid.setEnabled(state);
			if (kid instanceof Composite) {
				for (Control kk : ((Composite)kid).getChildren()) items.add(kk);
			}
		}
	}
	
	public void setSourceOptions(RelationshipDiagramEdgeStyleOptions options) {
		fireListeners = false;
		btnOverride.setSelection(options != null);
		enableAll(edgeCmp, options != null);
		edgeCmp.setSourceOptions(options);
		fireListeners = true;
		this.layout(true);
	}
	
	private void handleOverrideChanged() {
		RelationshipDiagramEdgeStyleOptions newOptions = btnOverride.getSelection() ? RelationshipDiagramStyleFactory.createDefaultEdgeOptions() : null;
		fireOptionsChanged(newOptions);
		setSourceOptions(newOptions);
	}

	private void createContent(Composite parent) {
		btnOverride = new Button(parent, SWT.CHECK);
		btnOverride.setText(Messages.RelationshipDiagramEdgeStyleComposite_Override);
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
		
		edgeCmp = new RelationshipDiagramEdgeStyleOptionsComposite(parent);
		edgeCmp.addOptionsChangeListener(new IEdgeStyleOptionsChangeListener() {
			@Override
			public void optionsChanged(RelationshipDiagramEdgeStyleOptions options) {
				if (fireListeners) {
					fireOptionsChanged(options);
				}
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
