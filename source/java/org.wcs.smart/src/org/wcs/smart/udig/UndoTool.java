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
package org.wcs.smart.udig;

import org.locationtech.udig.project.ui.tool.AbstractActionTool;

/**
 * Undo tool for undoing map edits.  Makes use of the IEditPointManager on 
 * the map blackboard for undoing edito.
 * 
 * @author Emily
 *
 */
public class UndoTool extends AbstractActionTool{
	
	public static final String ID = "org.wcs.smart.map.tool.edit.undo"; //$NON-NLS-1$
   
	public UndoTool() {
		super();
	}
	
	@Override
	public void run() {
		IMapEditManager manager = getEditManager();
		if (manager == null) return;
		manager.undo();
	}

	@Override
	public void dispose() {
	}
	
	private IMapEditManager getEditManager(){
		Object x = getContext().getMap().getBlackboard().get(IMapEditManager.BLACKBOARD_KEY);
		if (x instanceof IMapEditManager) return (IMapEditManager)x;
		return null;
	}

}
