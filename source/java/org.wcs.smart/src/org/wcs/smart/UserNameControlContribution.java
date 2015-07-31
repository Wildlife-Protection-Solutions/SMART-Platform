/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004-2008, Refractions Research Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package org.wcs.smart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.wcs.smart.ca.LabelConstants;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * Contribution item to display the current
 * logged in user name in upper right.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class UserNameControlContribution extends
		WorkbenchWindowControlContribution {

	/**
	 * 
	 */
	public UserNameControlContribution() {
	}

	/**
	 * @param id
	 */
	public UserNameControlContribution(String id) {
		super(id);
	}

	/**
	 * @see org.eclipse.jface.action.ControlContribution#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.marginHeight =  gl.horizontalSpacing = gl.verticalSpacing = 0;
		gl.marginWidth = 5;
		comp.setLayout(gl);
		Label lbl = new Label(comp, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lbl.setText(LabelConstants.getShortLabel(SmartDB.getCurrentEmployee()));
		lbl.setToolTipText(Messages.UserNameControlContribution_LoggedInLabel_ToolTip + LabelConstants.getFullLabel(SmartDB.getCurrentEmployee()));
		return comp;
	}

}
