package org.wcs.smart.query.compound.engine;

import java.sql.SQLException;

import org.hibernate.Session;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.CompoundMapQueryLayer;

public class CompoundQueryResult implements IQueryResult{

	private boolean isDisposed;
	
	public CompoundQueryResult(){
	}
	

	@Override
	public void dispose(Session session) throws SQLException {
		isDisposed = true;
	}

	@Override
	public boolean isDisposed() {
		return isDisposed;
	}
}
