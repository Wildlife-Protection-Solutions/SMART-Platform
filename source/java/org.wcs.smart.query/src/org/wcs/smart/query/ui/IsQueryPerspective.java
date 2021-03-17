 
package org.wcs.smart.query.ui;

import org.eclipse.e4.core.di.annotations.Evaluate;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

public class IsQueryPerspective {
	@Evaluate
	public boolean evaluate(EModelService service, MWindow window) {
		
		String pid = service.getActivePerspective(window).getElementId();
		System.out.println(pid);
		return ( pid.equals(QueryPerspective.ID) || pid.equals(MultiCaQueryPerspective.ID));
		
	}
}
