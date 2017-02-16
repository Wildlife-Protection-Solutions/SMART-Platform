package org.wcs.smart.i2.birt.entity;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.OdaException;

public class EntityLocationAttributeDatasetMetadata extends EntityDatasetMetadata{

	public EntityLocationAttributeDatasetMetadata(IConnection connection) {
		super(connection);
	}
	
	
	@Override
	public String getDataSourceProductName() throws OdaException {
		return "Intelligence Entity Location Attributes";
	}

}
