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
import java.util.Iterator;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PerspectiveAdapter;

/**
 * Perspective listener that hides editors not associated
 * with the active perspective and shows editors that are. 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PerspectiveEditorListener extends PerspectiveAdapter {

	private PerspectiveEditorTracker tracker;

	/**
	 * Create new listener
	 * @param tracker the perspective editor tracker
	 */
	public PerspectiveEditorListener(
			PerspectiveEditorTracker tracker) {
		this.tracker = tracker;
	}

	@Override
	public void perspectiveActivated(IWorkbenchPage page,
			IPerspectiveDescriptor perspectiveDescriptor) {
		
		super.perspectiveActivated(page, perspectiveDescriptor);
		// Hide all the editors
		IEditorReference[] editors = page.getEditorReferences();
		for (int i = 0; i < editors.length; i++) {
			page.hideEditor(editors[i]);
		}

		// Show the editors associated with this perspective
		ArrayList<IEditorReference> editorRefs = tracker.getEditorForPerspective(perspectiveDescriptor.getId());
		if (editorRefs != null) {
			for (Iterator<IEditorReference> it = editorRefs.iterator(); it.hasNext();) {
				IEditorReference editorInput = it.next();
				page.showEditor(editorInput);
			}

			// Send the last active editor to the top
			IEditorReference lastActiveRef = tracker.getLastActiveEditor(perspectiveDescriptor.getId());
			if (lastActiveRef != null){
				page.bringToTop(lastActiveRef.getPart(true));
			}
		}
	}

	public void perspectiveDeactivated(IWorkbenchPage page,
			IPerspectiveDescriptor perspective) {
		IEditorPart activeEditor = page.getActiveEditor();
		if (activeEditor != null) {
			// Find the editor reference that relates to this editor input
			IEditorReference[] editorRefs = page.findEditors(
					activeEditor.getEditorInput(), null,
					IWorkbenchPage.MATCH_INPUT);
			if (editorRefs.length > 0) {
				tracker.setLastActiveEditor(
						perspective.getId(), editorRefs[0]);
			}
		}
	}
}
