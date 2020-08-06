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
package org.wcs.smart.asset.query.ui.definition;

import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.definition.BasicFilterDefintionPanel;

/**
 * Filter panel for asset only filters.  Used by Summary Deployment queries.
 * 
 * @author Emily
 *
 */
public class AssetFilterPanel extends BasicFilterDefintionPanel {

	public static final String ID = "org.wcs.smart.asset.query.def.deployment.filter.panel"; //$NON-NLS-1$
	
	public AssetFilterPanel() {
	}

	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	protected void createFilterTypeComposite(Composite parent){
 
	}

	@Override
	public void initItems(QueryProxy q) throws Exception{
		this.currentQuery = q;
		if(q == null) return;
		addItems(q.getDropItems(getId()));

	}

}
