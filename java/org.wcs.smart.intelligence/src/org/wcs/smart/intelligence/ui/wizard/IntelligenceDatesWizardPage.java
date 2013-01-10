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
 * Intelligence Wizard page for collecting the intelligence relevant date(s) information
 *  
 * @author elitvin
 *
 */
public class IntelligenceDatesWizardPage extends IntelligenceWizardPage {

    private DateTime dtFromDate;
    private DateTime dtToDate;
    
    /**
     * @param pageName
     */
    public IntelligenceDatesWizardPage() {
        super(Messages.IntelligenceDatesWizardPage_PageTitle);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {
        Composite center = new Composite(parent, SWT.NONE);
        center.setLayout(new GridLayout(2, false));
        center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        
        Label label = new Label(center, SWT.NONE);
        label.setText(Messages.IntelligenceDatesWizardPage_From_Label);
        label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        dtFromDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
        dtFromDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        ((GridData)dtFromDate.getLayoutData()).horizontalIndent = 10;
        
        label = new Label(center, SWT.NONE);
        label.setText(Messages.IntelligenceDatesWizardPage_To_Label);
        label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        dtToDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
        dtToDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        ((GridData)dtToDate.getLayoutData()).horizontalIndent = 10;
        
        setControl(center);
        setMessage(Messages.IntelligenceDatesWizardPage_Message);
    }

    /* (non-Javadoc)
     * @see org.wcs.smart.intelligence.ui.wizard.IntelligenceWizardPage#updateModel(org.wcs.smart.intelligence.model.Intelligence)
     */
    @Override
    protected boolean updateModel(Intelligence intelligence) {
        intelligence.setFromDate(SmartUtils.getDate(dtFromDate));
        intelligence.setToDate(SmartUtils.getDate(dtToDate));
        return true;
    }

    /* (non-Javadoc)
     * @see org.wcs.smart.intelligence.ui.wizard.IntelligenceWizardPage#initModel(org.wcs.smart.intelligence.model.Intelligence, org.hibernate.Session)
     */
    @Override
    void initModel(Intelligence intelligence, Session session) {
        if (intelligence.getFromDate() != null){
            Calendar cal = SmartUtils.convertDate(intelligence.getFromDate());
            dtFromDate.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
        }
        if (intelligence.getToDate() != null){
            Calendar cal = SmartUtils.convertDate(intelligence.getToDate());
            dtToDate.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
        }
    }

}
