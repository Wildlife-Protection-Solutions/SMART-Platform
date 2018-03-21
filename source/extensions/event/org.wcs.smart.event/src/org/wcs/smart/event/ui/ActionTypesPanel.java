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
package org.wcs.smart.event.ui;

import java.text.MessageFormat;
import java.util.Locale;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.event.ActionTypeManager;
import org.wcs.smart.event.model.IActionParameter;
import org.wcs.smart.event.model.IActionType;

/**
 * Configure events action type panel
 * 
 * @author Emily
 *
 */
public class ActionTypesPanel extends Composite {

	private TableViewer lstTypes;
	
	private Composite rightPart;
	
	public ActionTypesPanel(Composite parent, int style) {
		super(parent, SWT.NONE);
		
		createComposite();
	}
	
	private void createComposite() {
		setLayout(new GridLayout());
		
		Label l = new Label(this, SWT.NONE);
		l.setText("Lists all the action types supported by the system.");
		
		l = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		SashForm parts = new SashForm(this,  SWT.NONE);
		parts.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = new Composite(parts, SWT.BORDER);
		leftPart.setLayout(new GridLayout());
		((GridLayout)leftPart.getLayout()).marginWidth = 0;
		((GridLayout)leftPart.getLayout()).marginHeight = 0;
		
		lstTypes = new TableViewer(leftPart, SWT.V_SCROLL | SWT.NONE);
		lstTypes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstTypes.setContentProvider(ArrayContentProvider.getInstance());
		lstTypes.getTable().setHeaderVisible(true);
		
		TableViewerColumn column1 = new TableViewerColumn(lstTypes, SWT.NONE);
		column1.getColumn().setText("Action Types");
		column1.getColumn().setWidth(200);
		column1.setLabelProvider(new AssetTypeLabelProvider());
		
		lstTypes.setInput(ActionTypeManager.INSTANCE.getActionTypes());
		lstTypes.addSelectionChangedListener(e->updateDetails());
		
		Composite rightPartOuter = new Composite(parts, SWT.BORDER);
		rightPartOuter.setLayout(new GridLayout());
		((GridLayout)rightPartOuter.getLayout()).marginWidth = 0;
		((GridLayout)rightPartOuter.getLayout()).marginHeight = 0;
		rightPartOuter.setBackground(parts.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		rightPart = new Composite(rightPartOuter, SWT.NONE);
		rightPart.setLayout(new GridLayout());
		rightPart.setBackground(parts.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		rightPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		parts.setWeights(new int[] {3, 5});
		
	}
	
	private void updateDetails() {
		Object element = lstTypes.getStructuredSelection().getFirstElement();
		if (!(element instanceof IActionType)) return;
		IActionType type = (IActionType) element;
		
		for (Control k : rightPart.getChildren()) k.dispose();
		
		String name = type.getName(Locale.getDefault());
		String key = type.getKey();
		
		Label l = new Label(rightPart, SWT.NONE);
		l.setText(name);
		l.setBackground(rightPart.getBackground());
		
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font boldFont = new Font(l.getDisplay(), fd);
		l.addListener(SWT.Dispose, e->boldFont.dispose());
		l.setFont(boldFont);
		l.setBackground(rightPart.getBackground());
		
		l = new Label(rightPart, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		ScrolledComposite scroll = new ScrolledComposite(rightPart, SWT.V_SCROLL );
		scroll.setBackground(rightPart.getBackground());
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite content = new Composite(scroll, SWT.NONE);
		content.setBackground(rightPart.getBackground());
		content.setLayout(new GridLayout());
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		scroll.setContent(content);
		
		l = new Label(content, SWT.NONE);
		l.setText("Description");
		l.setBackground(rightPart.getBackground());
		
		fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font boldFont2 = new Font(l.getDisplay(), fd);
		l.addListener(SWT.Dispose, e->boldFont2.dispose());
		l.setFont(boldFont2);
		
		l = new Label(content, SWT.WRAP);
		l.setText(type.getDescription(Locale.getDefault()));
		l.setBackground(rightPart.getBackground());
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		l = new Label(content, SWT.NONE);

		l = new Label(content, SWT.NONE);
		l.setText("Parameters");
		l.setBackground(rightPart.getBackground());
		l.setFont(boldFont2);
		
		for (IActionParameter p : type.getActionParameters()) {
			l = new Label(content, SWT.NONE);
			l.setText(p.getName(Locale.getDefault()));
			l.setBackground(rightPart.getBackground());
		}
		
		l = new Label(content, SWT.NONE);
		
		
//		l = new Label(content, SWT.NONE);
//		l.setText("Key");
//		l.setBackground(rightPart.getBackground());
//		l.setFont(boldFont2);
//		
//		l = new Label(content, SWT.NONE);
//		l.setText(key);
//		l.setBackground(rightPart.getBackground());
		
		rightPart.layout(true);
		content.setSize(content.computeSize(scroll.getSize().x-20, SWT.DEFAULT));
		scroll.addListener(SWT.Resize, e->{
			content.setSize(content.computeSize(scroll.getSize().x-20, SWT.DEFAULT));	
		});
	}

}
