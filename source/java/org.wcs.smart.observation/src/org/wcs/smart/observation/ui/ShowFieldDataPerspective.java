package org.wcs.smart.observation.ui;

import javax.inject.Named;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.ui.ShowPerspectiveHandler;

public class ShowFieldDataPerspective {

	public static final String FOCUS_VIEW = "org.wcs.smart.observation.perspective.show.focusview";
	
	@Execute
	public void execute(@Optional @Named(FOCUS_VIEW) String focusView,
			EModelService mService, EPartService pService){

		(new ShowPerspectiveHandler()).execute(FieldDataPerspective.ID, mService, pService.getActivePart().getContext().get(MWindow.class));
		
		if (focusView == null) return;
		MPart activate = pService.findPart(focusView);
		if (activate == null) return;
		pService.bringToTop(activate);
	}
	
	public static class ShowFieldDataPerspectiveWrapper extends AbstractHandler {

		private ShowFieldDataPerspective component;

		public ShowFieldDataPerspectiveWrapper() {
			IEclipseContext context = getActiveContext();
			component = ContextInjectionFactory.make(ShowFieldDataPerspective.class, context);
		}

		private static IEclipseContext getActiveContext() {
			IEclipseContext parentContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			return parentContext.getActiveLeaf();
		}
		
		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			//Default di handler does not add parameters into context
			IEclipseContext ctx = getActiveContext();
			ctx.set(FOCUS_VIEW, event.getParameter(FOCUS_VIEW));
			return ContextInjectionFactory.invoke(component, Execute.class, ctx);
		}

	}
}
