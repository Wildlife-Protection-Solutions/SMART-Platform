package org.wcs.smart.i2.birt.datasource.ui;

import org.wcs.smart.i2.birt.record.location.RecordLocationDataset;

public class IntelRecordLocationWizardPage extends IntelRecordDetailsWizardPage {


	public IntelRecordLocationWizardPage(String pageName) {
		super(pageName);
	}

	@Override
	public String getDatasetType() {
		return RecordLocationDataset.DATASET_TYPE;
	}

}
