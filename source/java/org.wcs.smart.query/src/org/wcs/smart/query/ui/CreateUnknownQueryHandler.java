/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.query.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.common.model.CompoundMapQueryLayer;
import org.wcs.smart.query.compound.ui.CompoundQueryEditor;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.model.IMappableQueryType;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.newwizard.NewQueryWizard;
import org.wcs.smart.query.ui.querylist.OpenQueryHandler;
import org.wcs.smart.ui.ShowPerspectiveHandler;

/**
 * Query handler that prompt the user for the type of query they
 * want to create before creating it.
 * @author egouge
 *
 */
public class CreateUnknownQueryHandler {

	private static final String QUERY_TYPE_KEY = "org.wcs.smart.query.type"; //$NON-NLS-1$
	
	@Execute
	public void execute(@Optional @Named(QUERY_TYPE_KEY) String queryType, Shell activeShell, 
			IEclipseContext context){
		
		(new ShowPerspectiveHandler()).execute(QueryPlugIn.getActivePerspectiveId(), 
				context.get(MWindow.class));
		
		if (queryType != null){
			IQueryType type = QueryTypeManager.INSTANCE.findQueryType(queryType);
			if (type != null){
				
				if (type.getKey().equalsIgnoreCase(CompoundMapQuery.TYPE_KEY)){
				
					List<QueryEditorInput> queries = new ArrayList<QueryEditorInput>();
					ESelectionService service = context.get(ESelectionService.class);
					if (service.getSelection() instanceof StructuredSelection){
						StructuredSelection sel = (StructuredSelection)service.getSelection();
						for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
							Object select = (Object) iterator.next();
							if (select instanceof QueryEditorInput &&
									((QueryEditorInput)select).getType() instanceof IMappableQueryType){
								queries.add((QueryEditorInput)select);
							}
							
						}
					}
					if (queries.size() > 0){
						IEditorPart part = createQuery(type);
						if (part instanceof CompoundQueryEditor){
							CompoundMapQuery query = (CompoundMapQuery) ((CompoundQueryEditor) part).getQueryProxy().getQuery();
							if (query.getLayers() == null) query.setLayers(new ArrayList<CompoundMapQueryLayer>());
							int order = 0;
							for (QueryEditorInput in : queries){
								CompoundMapQueryLayer layer = new CompoundMapQueryLayer();
								layer.setMapQuery(query);
								layer.setOrder(order++);
								layer.setQueryType(in.getType().getKey());
								layer.setQueryUuid(in.getUuid());
								query.getLayers().add(layer);
							}
							((CompoundQueryEditor)part).reparseQuery();
							return;
						}
					}
				}
				
				createQuery(type);
				return;
			}
		}
		createUnknownQuery(activeShell);
	}
	
	private IEditorPart createQuery(IQueryType qtype){
		return (new OpenQueryHandler()).execute(new StructuredSelection(new QueryEditorInput(qtype)));
	}

	
	private void createUnknownQuery(Shell activeShell){
		NewQueryWizard w = new NewQueryWizard();
		WizardDialog d = new WizardDialog(activeShell, w);
		d.setMinimumPageSize(650,200);
		
		if (d.open() != IDialogConstants.OK_ID){
			return;
		}
		IQueryType type = w.getSelectedQueryType();
		if (type == null){
			return ;
		}
		createQuery(type);
	}
	
	public static class CreateUnknownQueryHandlerWrapper extends AbstractHandler {

		private CreateUnknownQueryHandler component;

		public CreateUnknownQueryHandlerWrapper() {
			IEclipseContext context = getActiveContext();
			component = ContextInjectionFactory.make(CreateUnknownQueryHandler.class, context);
		}

		private static IEclipseContext getActiveContext() {
			IEclipseContext parentContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			return parentContext.getActiveLeaf();
		}
		
		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			//Default di handler does not add parameters into context
			IEclipseContext ctx = getActiveContext();
			ctx.set(QUERY_TYPE_KEY, event.getParameter(QUERY_TYPE_KEY));
			return ContextInjectionFactory.invoke(component, Execute.class, ctx);
		}

	}
}

