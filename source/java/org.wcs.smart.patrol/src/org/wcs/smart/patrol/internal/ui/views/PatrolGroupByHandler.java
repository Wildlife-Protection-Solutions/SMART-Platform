package org.wcs.smart.patrol.internal.ui.views;

import javax.inject.Named;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class PatrolGroupByHandler  {

	public static final String GROUPBY_OP_PARAM = "org.eclipse.ui.commands.radioStateParameter"; //$NON-NLS-1$
	
	@Execute
	public void execute(@Optional @Named(GROUPBY_OP_PARAM) String groupByOption, IEventBroker broker){
		if (groupByOption == null) return;
		broker.send(PatrolListView.GROUP_BY_EVENT, groupByOption);
	}

	//E3
	public static class PatrolGroupByHandlerWrapper extends AbstractHandler {

		private PatrolGroupByHandler component;

		public PatrolGroupByHandlerWrapper() {
			IEclipseContext context = getActiveContext();
			component = ContextInjectionFactory.make(PatrolGroupByHandler.class, context);
		}

		private static IEclipseContext getActiveContext() {
			IEclipseContext parentContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			return parentContext.getActiveLeaf();
		}
		
		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			if (HandlerUtil.matchesRadioState(event)) return null;
			 HandlerUtil.updateRadioState(event.getCommand(),  event.getParameter(GROUPBY_OP_PARAM));
			//Default di handler does not add parameters into context
			IEclipseContext ctx = getActiveContext();
			ctx.set(GROUPBY_OP_PARAM, event.getParameter(GROUPBY_OP_PARAM));
			return ContextInjectionFactory.invoke(component, Execute.class, ctx);
		}
	}
}

