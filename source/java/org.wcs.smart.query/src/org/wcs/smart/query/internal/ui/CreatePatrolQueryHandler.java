package org.wcs.smart.query.internal.ui;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.wcs.smart.query.model.Query.QueryType;

public class CreatePatrolQueryHandler extends CreateHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		super.execute(event);
		super.createQuery(QueryType.PATROL);
		return null;
	}

}
