package org.wcs.smart.i2.birt.datasource.ui;

import java.util.Properties;

import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSourceWizardPage;
import org.eclipse.datatools.connectivity.ui.PingJob;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

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
		lbl.setText("The intelligence data source is not configurable");
		setTitle("SMART Intelligence Data Source");
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
