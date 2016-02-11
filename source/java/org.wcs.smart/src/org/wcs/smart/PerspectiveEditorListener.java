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

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PerspectiveAdapter;
import org.wcs.smart.util.E3Utils;

/**
 * Perspective listener that hides editors not associated
 * with the active perspective and shows editors that are. 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PerspectiveEditorListener extends PerspectiveAdapter {

	private EPartService partService;
	
	private PerspectiveEditorTracker tracker;
	
	private HashMap<String, MPart> lastActive = new HashMap<String, MPart>();
	/**
	 * Create new listener
	 * @param tracker the perspective editor tracker
	 */
	public PerspectiveEditorListener( PerspectiveEditorTracker tracker, EPartService partService) {
		this.tracker = tracker;
		this.partService = partService;
	}

	
	@Override
	public void perspectiveActivated(IWorkbenchPage page,
			IPerspectiveDescriptor perspectiveDescriptor) {
		//super.perspectiveActivated(page, perspectiveDescriptor);
			
		Collection<MPart> allParts  = null;
		try{
			allParts = partService.getParts();
		}catch (Exception ex){
			return;
		}
		for (MPart p : allParts){
			if (E3Utils.isCompatibilityEditor(p) ||   
					p.getTags().contains(PerspectiveEditorTracker.EDITOR_TAG)){
				if (p.getTags().contains(perspectiveDescriptor.getId())){
					//this is set to make the close others/close all/close menu work
					p.setCloseable(true);	
					p.setVisible(true);
				}else{
					p.setVisible(false);
					p.setCloseable(false);
				}
			}
		}
		tracker.selectStackElement(lastActive.get(perspectiveDescriptor.getId()));
	}

	@Override
	public void perspectiveDeactivated(IWorkbenchPage page,
			IPerspectiveDescriptor perspective) {
		MStackElement ele = tracker.getActivePart();
		if (ele == null){
			lastActive.put(perspective.getId(), null);
		}else if (ele instanceof MPart){
			lastActive.put(perspective.getId(), (MPart)ele);
		}
	}
}
