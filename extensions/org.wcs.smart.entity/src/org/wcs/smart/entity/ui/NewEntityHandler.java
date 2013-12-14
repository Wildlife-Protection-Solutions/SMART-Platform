package org.wcs.smart.entity.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.wcs.smart.entity.ui.typelist.EntityTypeListView;
import org.wcs.smart.observation.ui.FieldDataPerspective;

public class NewEntityHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		FieldDataPerspective.openPerspective(EntityTypeListView.ID);
		return null;
	}

}
