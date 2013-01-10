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
package org.wcs.smart.intelligence.ui.wizard;

import java.util.Calendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.util.SmartUtils;

/**
 * Intelligence Wizard page for collecting the intelligence received date information
 * 
 * @author elitvin
 *
 */
public class IntelligenceReceivedWizardPage extends IntelligenceWizardPage {

    private DateTime dtReceivedDate;

    /**
     * @param pageName
     */
    public IntelligenceReceivedWizardPage() {
        super(Messages.IntelligenceReceivedWizardPage_PageTitle);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {
        Composite center = new Composite(parent, SWT.NONE);
        center.setLayout(new GridLayout(2, false));
        center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        
        Label lbl = new Label(center, SWT.NONE);
        lbl.setText(Messages.IntelligenceReceivedWizardPage_ReceivedDate_Label);
        lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        dtReceivedDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
        dtReceivedDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        ((GridData)dtReceivedDate.getLayoutData()).horizontalIndent = 10;
        
        setControl(center);
        setMessage(Messages.IntelligenceReceivedWizardPage_Message);
    }

    @Override
    protected boolean updateModel(Intelligence intelligence) {
        intelligence.setRecievedDate(SmartUtils.getDate(dtReceivedDate));
        return true;
    }

    @Override
    void initModel(Intelligence intelligence, Session session) {
        if (intelligence.getRecievedDate() != null){
            Calendar cal = SmartUtils.convertDate(intelligence.getRecievedDate());
            dtReceivedDate.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
        }
    }

}
