/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa;

import org.eclipse.jface.resource.ImageDescriptor;
import org.wcs.smart.qa.model.IQaAction;
import org.wcs.smart.qa.routine.IgnoreAction;

/**
 * Wrapper around an action that include the action image provided in the
 * extension point
 * @author Emily
 *
 */
public class QaActionInfo {

	public final static QaActionInfo IGNORE_INSTANCE = new QaActionInfo(IgnoreAction.INSTANCE, null);
	
	private IQaAction action;
	private ImageDescriptor actionImage;
	
	public QaActionInfo(IQaAction action, ImageDescriptor actionImage) {
		this.action = action;
		this.actionImage = actionImage;
	}
	
	public IQaAction getAction() {
		return this.action;
	}
	
	public ImageDescriptor getImage() {
		return this.actionImage;
	}
}
