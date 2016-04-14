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
package org.wcs.smart.report.internal.ui.viewer.parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.birt.report.engine.api.IParameterGroupDefn;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * Grouped BIRT parameters.  Wraps a collection of parameters into a group.
 * @author egouge
 * @since 1.0.0
 */
public class GroupedReportParameters implements IBirtParameterComponent {


	private List<IBirtParameterComponent> params = new ArrayList<IBirtParameterComponent>();
	
	private IParameterGroupDefn base;
	
	/**
	 * creates a new group
	 * @param base
	 */
	public GroupedReportParameters(IParameterGroupDefn base){
		this.base = base;
		
		
	}
	
	/**
	 * Adds a subcomponent to the group
	 * @param param
	 */
	public void addComponent(IBirtParameterComponent param){
		params.add(param);
	}
	
	/**
	 * @see org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameterComponent#getParameters()
	 */
	@Override
	public HashMap<String, Object> getParameters() {
		HashMap<String, Object> values = new HashMap<String, Object>();
		for(IBirtParameterComponent p : params){
			values.putAll(p.getParameters());
		}
		return values;
	}

	/**
	 * @see org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameterComponent#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Composite createComposite(Composite parent, IDialogSettings settings) {
		Group g = new Group(parent, SWT.NONE);
		g.setText(base.getDisplayName() != null ? base.getDisplayName() : base.getName());
		g.setLayout(new GridLayout(1, false));
		
		for (IBirtParameterComponent p : params){
			Composite c = p.createComposite(g, settings);
			c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		return g;
	}

}
