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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.RelationshipDiagramEdgeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramStyleOptions;

/**
 * Composite containing controls to manage default style options for relationship diagram.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipDiagramDefaultStyleComposite extends Composite {
	
	private RelationshipDiagramStyleOptions options;
	
	private RelationshipDiagramNodeStyleOptionsComposite nodeCmp;
	private RelationshipDiagramEdgeStyleOptionsComposite edgeCmp;

	private List<IStyleOptionsChangeListener> listeners = new ArrayList<>();
	
	public RelationshipDiagramDefaultStyleComposite(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}
	
	public void setSourceOptions(RelationshipDiagramStyleOptions options) {
		this.options = options;
		nodeCmp.setSourceOptions(options.getDefaultNodeStyle());
		edgeCmp.setSourceOptions(options.getDefaultEdgeStyle());
	}

	private void createContent(Composite parent) {
		Group grpNode = new Group(parent, SWT.NONE);
		grpNode.setText(Messages.RelationshipDiagramDefaultStyleComposite_NodeGroup_Title);
		grpNode.setLayout(new GridLayout());
		grpNode.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		nodeCmp = new RelationshipDiagramNodeStyleOptionsComposite(grpNode);
		nodeCmp.addOptionsChangeListener(new INodeStyleOptionsChangeListener() {
			@Override
			public void optionsChanged(RelationshipDiagramNodeStyleOptions ops) {
				fireOptionsChanged(options);
			}
		});
		
		Group grpEdge = new Group(parent, SWT.NONE);
		grpEdge.setText(Messages.RelationshipDiagramDefaultStyleComposite_EdgeGroup_Title);
		grpEdge.setLayout(new GridLayout());
		grpEdge.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		edgeCmp = new RelationshipDiagramEdgeStyleOptionsComposite(grpEdge);
		edgeCmp.addOptionsChangeListener(new IEdgeStyleOptionsChangeListener() {
			@Override
			public void optionsChanged(RelationshipDiagramEdgeStyleOptions ops) {
				fireOptionsChanged(options);
			}
		});
	}

	public void addOptionsChangeListener(IStyleOptionsChangeListener listener) {
		listeners.add(listener);
	}
	
	private void fireOptionsChanged(RelationshipDiagramStyleOptions ops) {
		for (IStyleOptionsChangeListener l : listeners) {
			l.optionsChanged(ops);
		}
	}
	
}
