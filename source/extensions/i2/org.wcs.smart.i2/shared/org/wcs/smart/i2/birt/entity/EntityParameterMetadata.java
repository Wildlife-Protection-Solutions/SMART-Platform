package org.wcs.smart.i2.birt.entity;

import java.util.UUID;

import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

public class EntityParameterMetadata implements IParameterMetaData{

	public enum EntityParameter{
		UUID(1, "Entity UUID", parameterModeIn, java.sql.Types.VARCHAR);
		
		public int index;
		public String name;
		public int parameterMode;
		public int type;
		
		EntityParameter(int index, String name, int parameterMode, int type){
			this.index = index;
			this.name = name;
			this.parameterMode = parameterMode;
			this.type = type;
		}
	}
	
	
	@Override
	public int getParameterCount() throws OdaException {
		return EntityParameter.values().length;
	}

	@Override
	public int getParameterMode(int param) throws OdaException {
		for (EntityParameter p : EntityParameter.values()){
			if (p.index == param) return p.parameterMode;
		}
		return parameterModeUnknown;
	}

	@Override
	public String getParameterName(int param) throws OdaException {
		for (EntityParameter p : EntityParameter.values()){
			if (p.index == param) return p.name;
		}
		return null;
	}

	@Override
	public int getParameterType(int param) throws OdaException {
		for (EntityParameter p : EntityParameter.values()){
			if (p.index == param) return p.type;
		}
		return 0;
	}

	@Override
	public String getParameterTypeName(int param) throws OdaException {
		if (param == EntityParameter.UUID.index) return UUID.class.getCanonicalName();	
		return null;
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
