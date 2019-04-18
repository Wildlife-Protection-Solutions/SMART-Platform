package org.wcs.smart.observation.ui;

import java.util.List;

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
import org.eclipse.e4.ui.model.application.ui.menu.MHandledItem;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.wcs.smart.ui.ShowPerspectiveHandler;

public class ShowFieldDataPerspective {

	public static final String FOCUS_VIEW = "org.wcs.smart.observation.perspective.show.focusview"; //$NON-NLS-1$
	
	public static String LASTVIEW = "";
	public static void enableToolbarItem(String focusView, IEclipseContext context) {
		EModelService mService = context.get(EModelService.class);
		List<MHandledItem> elements = mService.findElements(context.get(MWindow.class), null, MHandledItem.class );
		elements.forEach(f->{
			if (f.getCommand() != null) {
				f.setSelected(false);
				f.getParameters().forEach(p->{
					if (p.getName().equals(FOCUS_VIEW) && p.getValue().equalsIgnoreCase(focusView)) {
						f.setSelected(true);
					}
				});
			}
		});
		
		LASTVIEW = focusView;
		context.get(IEvaluationService.class).requestEvaluation("org.wcs.smart.fielddata.focusview"); //$NON-NLS-1$
		//Fire event to refresh menus
	}
	
	@Execute
	public void execute(@Optional @Named(FOCUS_VIEW) String focusView,
			MWindow currentWindow){

		//show field data perspective
		(new ShowPerspectiveHandler()).execute(FieldDataPerspective.ID, currentWindow);
		
		if (focusView == null) return;
		
		//active requested part
		EPartService pService = ((EPartService)currentWindow.getContext().get(EPartService.class));
		MPart activate = pService.findPart(focusView);
		if (activate == null) return;
		pService.bringToTop(activate);
		enableToolbarItem(focusView, currentWindow.getContext());
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
