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
package org.wcs.smart.i2.birt.datasource.ui;

import java.util.Properties;

import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSourceWizardPage;
import org.eclipse.datatools.connectivity.ui.PingJob;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.i2.internal.Messages;

/**
 * Intelligence data source wizard page
 * 
 * @author Emily
 *
 */
public class IntelligenceDataSourceWizardPage extends DataSourceWizardPage {

	private Properties p = new Properties();
	
	public IntelligenceDataSourceWizardPage(String pageName) {
		super(pageName);
	}

	@Override
	public Properties collectCustomProperties() {
		return p;
	}

	@Override
	public void createPageCustomControl(Composite arg0) {
		Label lbl = new Label(arg0, SWT.NONE);
		lbl.setText(Messages.IntelligenceDataSourceWizardPage_NotConfigurable);
		setTitle(Messages.IntelligenceDataSourceWizardPage_Title);
	}

	@Override
	public void setInitialProperties(Properties prop) {
		this.p = prop;

	}
	
	@Override
    protected Runnable createTestConnectionRunnable( final IConnectionProfile profile )
    {
        return new Runnable() 
        {
            public void run() 
            {
                PingJob.PingUIJob.showTestConnectionMessage( getShell(), null );
            }
        };
    }

}
