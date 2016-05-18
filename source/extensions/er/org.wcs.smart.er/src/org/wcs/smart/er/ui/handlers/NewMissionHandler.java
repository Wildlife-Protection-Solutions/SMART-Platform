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
package org.wcs.smart.er.ui.handlers;

import java.util.Iterator;
import java.util.UUID;

import javax.inject.Named;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.SurveyDesignListView;
import org.wcs.smart.er.ui.SurveyListTreeNode;
import org.wcs.smart.er.ui.SurveyListTreeNode.Type;
import org.wcs.smart.er.ui.mision.wizard.NewMissionWizard;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;

/**
 * Handler for creating new missions.
 * 
 * @author Emily
 *
 */
public class NewMissionHandler {

	public static final String STARTPAGE_ID_PARAM = "org.wcs.smart.er.mission.new.parameter.startpage"; //$NON-NLS-1$
	
	//EG: See DeleteSurveyElementHandler for use of ESelectionService over ActiveSelection
	@Execute
	public void execute(@Optional @Named(STARTPAGE_ID_PARAM) String startPage,
			ESelectionService selectionService, 
			IEclipseContext ctx,
			Shell activeShell){
		Object selection = selectionService.getSelection();
		
		//search for a parent
		UUID parentDesign = null;
		UUID parentSurvey = null;
		if (selection != null && selection instanceof StructuredSelection){
			IStructuredSelection sselection = (IStructuredSelection)selection;
			
			for (Iterator<?> iterator = sselection.iterator(); iterator.hasNext();) {
				Object item = (Object) iterator.next();
				if (item instanceof SurveyDesign){
					parentDesign = ((SurveyDesign) item).getUuid();
					break;
				}else if (item instanceof Survey){
					parentSurvey = ((Survey) item).getUuid();
				}else if (item instanceof SurveyListTreeNode &&
						((SurveyListTreeNode)item).getType() == Type.SURVEY){
					parentSurvey = ((SurveyListTreeNode)item).getUuid();
					break;
				}else if (item instanceof SurveyListTreeNode && 
					((SurveyListTreeNode)item).getType() == Type.MISSION){
					parentSurvey = ((SurveyListTreeNode)item).getParent().getUuid();
				}else if (item instanceof SurveyDesignEditorInput){
					parentDesign = ((SurveyDesignEditorInput) item).getUuid();
				}
			}
		}
		
		NewMissionWizard.StartPage initPage = null;
		if (startPage != null){
			if (startPage.equalsIgnoreCase("mission")){ //$NON-NLS-1$
				initPage = NewMissionWizard.StartPage.MISSION;
			}else if (startPage.equalsIgnoreCase("survey")){ //$NON-NLS-1$
				initPage = NewMissionWizard.StartPage.SURVEY;
			}
		}
	
		Mission m = newMission(activeShell, parentDesign, parentSurvey, initPage);
		if (m == null) {
			return ;
		}
		
		ctx.set(ShowFieldDataPerspective.FOCUS_VIEW, SurveyDesignListView.ID);
		ContextInjectionFactory.invoke(new ShowFieldDataPerspective(), Execute.class, ctx);
		
		EditSurveyElementHandler.editMission(activeShell, m.getUuid(), m.getId());
	}
	
	/**
	 * Opens the new mission wizard at a specific start page
	 * @param parent
	 * @param parentDesign
	 * @param parentSurvey
	 * @param startPage - can be null; the initial wizard page to display
	 * @return
	 */
	public static Mission newMission(Shell parent, UUID parentDesign, UUID parentSurvey, NewMissionWizard.StartPage startPage){
		NewMissionWizard newWizard = new NewMissionWizard(parentDesign, parentSurvey);
		newWizard.setStartPage(startPage);
		WizardDialog wd = new WizardDialog(parent, newWizard);
		if (wd.open() == WizardDialog.OK){
			return newWizard.getNewMission();
		}else{
			return null;
		}
	}
	
	public static class NewMissionHandlerWrapper extends AbstractHandler{
		private NewMissionHandler component;
		
		public NewMissionHandlerWrapper(){
			IEclipseContext context = getActiveContext();
			component = ContextInjectionFactory.make(NewMissionHandler.class, context);
		}
		
		private static IEclipseContext getActiveContext() {
			IEclipseContext parentContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			return parentContext.getActiveLeaf();
		}
		
		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			//Default di handler does not add parameters into context
			IEclipseContext ctx = getActiveContext();
			ctx.set(STARTPAGE_ID_PARAM, event.getParameter(STARTPAGE_ID_PARAM));
			return ContextInjectionFactory.invoke(component, Execute.class, ctx);
		}
	}
}