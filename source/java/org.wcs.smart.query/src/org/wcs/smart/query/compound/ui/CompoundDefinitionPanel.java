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
package org.wcs.smart.query.compound.ui;

import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.QuerySourceProvider;
import org.wcs.smart.query.ui.definition.ListDefinitionPanel;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * Definition panel for list of queries.
 * 
 * @author Emily
 *
 */
public class CompoundDefinitionPanel extends ListDefinitionPanel {

	public static final String ID = "org.wcs.smart.query.compound.definition"; //$NON-NLS-1$
	
	public CompoundDefinitionPanel() {
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getGuiName() {
		return Messages.CompoundDefinitionPanel_TabName;
	}

	@Override
	public String validate() {
		String error = null;
		for (DropItem it : getItems()){
			if (it instanceof QueryDropItem){
				((QueryDropItem)it).validate();
				String wasValid = (String) QuerySourceProvider.getSourceProviderFromContext().getCurrentState().get(QuerySourceProvider.QUERY_DATE_VALID);
				if (wasValid != null){
					error = wasValid;
					break;
				}
			}
		}
		return error;
	}

}
