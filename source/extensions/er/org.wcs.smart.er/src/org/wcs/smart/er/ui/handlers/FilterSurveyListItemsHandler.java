package org.wcs.smart.er.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.er.ui.SurveyDesignListView;

public class FilterSurveyListItemsHandler  extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if (!(part instanceof SurveyDesignListView)){
			return null;
			
		}
		SurveyDesignListView view = (SurveyDesignListView) part;
		view.showFilterDialog();
		
		return null;
	}

}
