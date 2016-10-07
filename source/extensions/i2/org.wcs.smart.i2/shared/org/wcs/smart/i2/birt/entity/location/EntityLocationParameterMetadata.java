package org.wcs.smart.i2.birt.entity.location;

import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.i2.birt.datasource.DataSourceParameter;
import org.wcs.smart.i2.birt.datasource.IntelBirtConnection;
import org.wcs.smart.i2.birt.entity.EntityDataset;

public class EntityLocationParameterMetadata implements IParameterMetaData{
	private static DataSourceParameter[] parameters = new DataSourceParameter[]{
		null,
		DataSourceParameter.ENTITY_UUID,
		DataSourceParameter.START_DATE,
		DataSourceParameter.END_DATE,
	};
	
	public int findParameterIndex(String parameterName){
		for (int i = 1; i < parameters.length; i ++){
			if (parameters[i].getName().equalsIgnoreCase(parameterName)){
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public int getParameterCount() throws OdaException {
		return parameters.length-1;
	}

	@Override
	public int getParameterMode(int param) throws OdaException {
		return parameters[param].getParameterMode();
	}

	@Override
	public String getParameterName(int param) throws OdaException {
		return parameters[param].getName();
	}

	@Override
	public int getParameterType(int param) throws OdaException {
		return parameters[param].getType();
	}

	@Override
	public String getParameterTypeName(int param) throws OdaException {
		int nativeTypeCode = getParameterType(param);
		return IntelBirtConnection.getNativeDataTypeName(nativeTypeCode, EntityDataset.DATASET_TYPE);
	}

	@Override
	public int getPrecision(int param) throws OdaException {
		return -1;
	}

	@Override
	public int getScale(int param) throws OdaException {
		return -1;
	}

	@Override
	public int isNullable(int param) throws OdaException {
		if (param == 1) return parameterNoNulls;
		return parameterNullableUnknown;
	}
}
