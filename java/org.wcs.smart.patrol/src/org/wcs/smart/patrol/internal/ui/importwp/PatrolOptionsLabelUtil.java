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
package org.wcs.smart.patrol.internal.ui.importwp;

import java.text.DateFormat;
import java.text.MessageFormat;

import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.common.importwp.ImportOptionsWizardPage;
import org.wcs.smart.patrol.internal.Messages;

/**
 * Util class to update labels for patrol options page.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class PatrolOptionsLabelUtil {

	public static void updateLabels(ImportOptionsWizardPage page, PatrolImportGpsDataWizard w) {
		if (w.getCurrentDay().getPatrolLeg().getPatrol().getLegs().size() > 1) {
			page.setWarningMessage(MessageFormat.format(Messages.ImportOptionsComposite_ImportAllWarning , new Object[]{w.getType().guiName}));
			ImportOption[] validOptions = new ImportOption[]{ImportOption.ALL, ImportOption.DATE, ImportOption.SELECT};
			String op = DateFormat.getDateInstance(DateFormat.MEDIUM).format(w.getCurrentDay().getDate());
			op += " (" + Messages.ImportOptionsComposite_LegPrefix + ": " + w.getCurrentDay().getPatrolLeg().getId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String[] labels = new String[]{Messages.ImportGpxWizardPage_ImportAllOp + "*", //$NON-NLS-1$
					MessageFormat.format(Messages.ImportGpxWizardPage_ImportSingleDayOp, new Object[]{w.getType().guiName.toLowerCase(), op}),
					MessageFormat.format(Messages.ImportGpxWizardPage_ImportSelectionOp, new Object[]{w.getType().guiName.toLowerCase(), op})
			};
			page.setOptions(validOptions, labels);
		}
	}
	
}
