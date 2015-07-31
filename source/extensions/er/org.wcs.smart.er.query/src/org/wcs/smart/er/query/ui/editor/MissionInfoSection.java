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
package org.wcs.smart.er.query.ui.editor;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.ui.ISummaryInfo;


/**
 * Info section for mission queries
 * @author Emily
 *
 */
public class MissionInfoSection implements ISummaryInfo {

	private Label lblNumResults;
	
	@Override
	public void createControls(Composite parent, FormToolkit toolkit) {
		GridLayout layout = new GridLayout(7,false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		parent.setLayout(layout);
		
		toolkit.createLabel(parent,  Messages.MissionInfoSection_NumMissions);
		lblNumResults = toolkit.createLabel(parent, Messages.MissionInfoSection_NA);
	}

	@Override
	public void updateControls(IPagedQueryResultSet resultSet) {
		lblNumResults.setText(Messages.MissionTrackInfoSection_NA);
		if (resultSet != null ){
			lblNumResults.setText(String.valueOf(resultSet.getItemCount()));
		}
		lblNumResults.getParent().getParent().layout();
	}


}
