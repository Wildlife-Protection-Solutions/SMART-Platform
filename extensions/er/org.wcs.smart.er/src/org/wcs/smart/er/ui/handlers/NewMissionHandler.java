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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.SurveyListTreeNode;
import org.wcs.smart.er.ui.SurveyListTreeNode.Type;
import org.wcs.smart.er.ui.mision.wizard.NewMissionWizard;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;

/**
 * Handler for creating new missions.
 * 
 * @author Emily
 *
 */
public class NewMissionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		
		//search for a parent
		byte[] parentDesign = null;
		byte[] parentSurvey = null;
		ISelection selection = HandlerUtil.getCurrentSelection(event);
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
		
	
		newMission(HandlerUtil.getActiveShell(event), parentDesign, parentSurvey);
		return null;
	}
	
	public static void newMission(Shell parent, byte[] parentDesign, byte[] parentSurvey){
		WizardDialog dialog = new WizardDialog(parent, 
				new NewMissionWizard(parentDesign, parentSurvey));
		dialog.open();
	}
}