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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.ui.SurveyListTreeNode;
import org.wcs.smart.er.ui.SurveyListTreeNode.Type;
import org.wcs.smart.er.ui.mision.editor.MissionEditor;
import org.wcs.smart.er.ui.mision.editor.MissionEditorInput;
import org.wcs.smart.er.ui.survey.EditSurveyDialog;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditor;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;

/**
 * Edit survey handler
 * 
 * @author Emily
 *
 */
public class EditSurveyElementHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection a = HandlerUtil.getCurrentSelection(event);
		if (!(a instanceof StructuredSelection)){
			return null;
		}
		
		Shell parent = HandlerUtil.getActiveShell(event);
		
		StructuredSelection selection = (StructuredSelection)a;
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object node = (Object) iterator.next();
			if (node instanceof SurveyListTreeNode){
				SurveyListTreeNode treeNode = (SurveyListTreeNode) node;
				if (treeNode.getType() == Type.MISSION){
					editMission(parent, treeNode.getUuid(), treeNode.getLabel());
				}else if (treeNode.getType() == Type.SURVEY){
					editSurvey(parent, treeNode.getUuid());
				}				
			}else if (node instanceof SurveyDesignEditorInput){
				editSurveyDesign(parent, (SurveyDesignEditorInput)node);
			}
		}
		
		return null;
	}

	/**
	 * Opens the edit survey dialog for the given survey 
	 * @param parentShell
	 * @param surveyUuid
	 */
	public static final void editSurvey(Shell parentShell, Survey survey){
		editSurvey(parentShell, survey.getUuid());
	}
	
	/**
	 * Opens the edit survey dialog for the given survey 
	 * @param parentShell
	 * @param surveyUuid
	 */
	public static final void editSurvey(Shell parentShell, byte[] surveyUuid){
		EditSurveyDialog dialog = new EditSurveyDialog(parentShell, surveyUuid);
		dialog.open();
	}
	
	/**
	 * Opens the mission editor for the given mission
	 * @param parentShell
	 * @param missionUuid
	 * @param name
	 */
	public static final void editMission(Shell parentShell, byte[] missionUuid, String name){
		MissionEditorInput in = new MissionEditorInput(name, missionUuid);
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(in, MissionEditor.ID);
		} catch (PartInitException e) {
			EcologicalRecordsPlugIn.displayLog(Messages.EditSurveyElementHandler_MissionError + "\n\n" + e.getMessage(), e); //$NON-NLS-1$
		}
	}


	/**
	 * Opens the survey design editor for the given survey design
	 * @param parentShell
	 * @param in
	 */
	public static final void editSurveyDesign(Shell parentShell, SurveyDesignEditorInput in){
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(in, SurveyDesignEditor.ID);
		} catch (PartInitException e) {
			EcologicalRecordsPlugIn.displayLog(Messages.EditSurveyElementHandler_DesignError + "\n\n" + e.getMessage(), e); //$NON-NLS-1$
		}
	}
}
