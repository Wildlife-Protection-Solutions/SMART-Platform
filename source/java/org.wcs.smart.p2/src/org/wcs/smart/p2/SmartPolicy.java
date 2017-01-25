/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
package org.wcs.smart.p2;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.p2.internal.Messages;
import org.wcs.smart.user.UserLevelManager;

/**
 * SMARTPolicy defines the SMART UI policies for the
 * p2 UI.  The policy is declared as an OSGi service in
 * the policy_component.xml file.
 * 
 * Similar to the SDKPolicy class found in
 * org.eclipse.equinox.internal.p2.ui.sdk.
 * 
 * @since 3.6
 */
public class SmartPolicy extends Policy {

	public SmartPolicy() {
		// initialize for our values
		setVisibleAvailableIUQuery(QueryUtil.createIUGroupQuery());
		// If this ever changes, we must change AutomaticUpdateSchedule.getProfileQuery()
		setVisibleInstalledIUQuery(new UserVisibleRootQuery());
		setRepositoryPreferencePageId("org.wcs.smart.p2.ui.SitesPreferencePage"); //$NON-NLS-1$		
		setRepositoryPreferencePageName(Messages.SmartPolicy_PreferencePageName);
		if (SmartDB.getCurrentEmployee() != null){
			setRepositoriesVisible(UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ADMIN));
		}
		Activator.getDefault().updateWithPreferences(this);
	}

	public IStatus getNoProfileChosenStatus() {
		return Activator.getNoSelfProfileStatus();
	}
	
	

	public boolean continueWorkingOperation(ProfileChangeOperation operation, Shell shell) {
		// don't continue if superclass has already identified problem scenarios
		boolean ok = super.continueWorkingWithOperation(operation, shell);
		if (!ok)
			return false;

		IProvisioningPlan plan = operation.getProvisioningPlan();
		if (plan == null)
			return false;

		// Check the preference to see whether to continue.
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		String openPlan = prefs.getString(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN);
		if (MessageDialogWithToggle.ALWAYS.equals(openPlan)) {
			return true;
		}
		if (MessageDialogWithToggle.NEVER.equals(openPlan)) {
			StatusManager.getManager().handle(plan.getStatus(), StatusManager.SHOW | StatusManager.LOG);
			return false;
		}
		MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoCancelQuestion(shell, Messages.SmartPolicy_Question, Messages.SmartPolicy_InstallQuestion, null, false, prefs, PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN);

		// Any answer but yes will stop the performance of the plan, but NO is interpreted to mean, show me the error.
		if (dialog.getReturnCode() == IDialogConstants.NO_ID)
			StatusManager.getManager().handle(plan.getStatus(), StatusManager.SHOW | StatusManager.LOG);
		return dialog.getReturnCode() == IDialogConstants.YES_ID;
	}
}
