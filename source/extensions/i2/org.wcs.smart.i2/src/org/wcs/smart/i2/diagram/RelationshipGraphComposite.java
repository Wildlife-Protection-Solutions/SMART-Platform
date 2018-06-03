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
package org.wcs.smart.i2.diagram;

import org.eclipse.gef.layout.algorithms.SpringLayoutAlgorithm;
import org.eclipse.gef.zest.fx.jface.ZestContentViewer;
import org.eclipse.gef.zest.fx.jface.ZestFxJFaceModule;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.i2.model.IntelEntity;

/**
 * Composite that contains controls related to relationship graph visualization.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipGraphComposite extends Composite {

	private RelationshipGraphFilterComposite cmpFilter;
	private ZestContentViewer graphViewer;
	private FormToolkit toolkit;
	
	public RelationshipGraphComposite(Composite parent, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = layout.verticalSpacing = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}

	private void createContent(Composite parent) {
		Composite topCmp = toolkit.createComposite(parent, SWT.NONE);
		topCmp.setLayout(new GridLayout(2, false));
		topCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		cmpFilter = new RelationshipGraphFilterComposite(topCmp);

		toolkit.createLabel(topCmp, "Style:");
		
		Composite mainCmp = new Composite(parent, SWT.NONE);
		StackLayout stackLayout = new StackLayout();
		stackLayout.marginHeight = stackLayout.marginWidth = 0;
		mainCmp.setLayout(stackLayout);
		mainCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite sizingCmp = toolkit.createComposite(mainCmp, SWT.NONE);
		sizingCmp.setLayout(new GridLayout());
		
		Composite graphCmp = toolkit.createComposite(mainCmp, SWT.NONE);
		graphCmp.setLayout(new FillLayout(SWT.VERTICAL));
		
		((StackLayout)mainCmp.getLayout()).topControl = graphCmp; //NOTE: this is a hack to coordinate sizing of composites with GridLayout and FillLayout
		
		graphViewer = new ZestContentViewer(new ZestFxJFaceModule());
		graphViewer.createControl(graphCmp, SWT.NONE);
		graphViewer.setContentProvider(new RelationshipGraphContentProvider());
		graphViewer.setLabelProvider(new RelationshipGraphLabelProvider(graphViewer));
		graphViewer.setLayoutAlgorithm(new SpringLayoutAlgorithm());
		graphViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				System.out.println(
						"Selection changed: " + (event.getSelection()));
			}
		});
	}

	public void setInput(IntelEntity entity) {
		graphViewer.setInput(entity);
	}

}
