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
package org.wcs.smart;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.EditorPart;

/**
 * A Editor/Perspective tracker.
 * <p>This maps editors to perspectives.<p>
 * <p>Editors are mapped to the active perspective
 * when the editor is opened.</p>
 * <p>Also tracks the current active editor for the
 * given perspective.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PerspectiveEditorTracker implements IPartListener {

	private HashMap<String, ArrayList<IEditorReference>> perspectiveEditors = new HashMap<String, ArrayList<IEditorReference>>();
	private HashMap<String, IEditorReference> lastActiveEditors = new HashMap<String, IEditorReference>();
	
	/**
	 * Finds all editors associated with the given perspective 
	 * @param perspectiveId the perspective 
	 * @return list of editor references associated with the given perspective
	 */
	public ArrayList<IEditorReference> getEditorForPerspective(String perspectiveId){
		return perspectiveEditors.get(perspectiveId);
	}
	
	/**
	 * @param perspectiveId
	 * @return the last adtive editor for the given perspective
	 */
	public IEditorReference getLastActiveEditor(String perspectiveId){
		return lastActiveEditors.get(perspectiveId);
	}
	
	/**
	 * Sets the last active editor for the given perspective 
	 * @param perspectiveId the perspective id
	 * @param editor the editor reference
	 */
	public void setLastActiveEditor(String perspectiveId, IEditorReference editor){
		lastActiveEditors.put(perspectiveId, editor);
	}
	
	public void partActivated(IWorkbenchPart part) {
	}

	public void partBroughtToTop(IWorkbenchPart part) {
	}

	public void partClosed(IWorkbenchPart part) {
		if (part instanceof EditorPart) {
			EditorPart editor = (EditorPart) part;
			IWorkbenchPage page = part.getSite().getPage();
			IEditorInput editorInput = editor.getEditorInput();
			IPerspectiveDescriptor activePerspective = page.getPerspective();

			ArrayList<IEditorReference> editors = perspectiveEditors.get(activePerspective.getId());
			
			if (editors != null) {
				for (IEditorReference ref : editors){
					if (ref.getPart(false) == part){
						editors.remove(ref);
						break;
					}
				}
			}
		}
	}

	public void partDeactivated(IWorkbenchPart part) {
	}

	public void partOpened(IWorkbenchPart part) {
		if (part instanceof EditorPart) {
			EditorPart editor = (EditorPart) part;
			IWorkbenchPage page = part.getSite().getPage();
			IEditorInput editorInput = editor.getEditorInput();
			IPerspectiveDescriptor activePerspective = page.getPerspective();

			// Find the editor reference that relates to this editor input
			IEditorReference[] editorRefs = page.findEditors(editorInput, null, IWorkbenchPage.MATCH_INPUT);

			if (editorRefs.length > 0) {
				ArrayList<IEditorReference> editors = perspectiveEditors.get(activePerspective.getId());
				if (editors == null){
					editors = new ArrayList<IEditorReference>();
					perspectiveEditors.put(activePerspective.getId(), editors);
				}
				editors.add(editorRefs[0]);
			}
		}
	}

}
