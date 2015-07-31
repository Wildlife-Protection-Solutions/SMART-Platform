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
package org.wcs.smart.patrol.query.ui.itempanel;

import java.util.Locale;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.IExtensionOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.ui.PatrolQueryLabelProvider;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;

/**
 * Patrol filter items.
 * 
 * @author Emily
 *
 */
public class PatrolFilterTreeItem implements IItemTreeNode{

	public static final String KEY = "patrolfilter"; //$NON-NLS-1$
	
	private PatrolOptionContentProvider provider;
	
	@Override
	public String getName() {
		return Messages.PatrolFilterTreeItem_PatrolFiltersTreeItem;
	}

	@Override
	public Image getImage() {
		return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON);
	}

	@Override
	public ITreeContentProvider getContentProvider() {
		if (provider == null){
			provider = new PatrolOptionContentProvider();
		}
		return provider;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		return lblProvider;
	}

	@Override
	public String getKey() {
		return KEY;
	}
	
	private static final LabelProvider lblProvider = new LabelProvider(){
		
		public String getText(Object element){
			if (element instanceof PatrolQueryOption){
				return ((PatrolQueryOption) element).getGuiName(Locale.getDefault());
			}else if (element instanceof IExtensionOption){
				return ((IExtensionOption)element).getName();
			}
			return super.getText(element);
		}
		public Image getImage(Object element){
			if (element instanceof PatrolQueryOption){
				return PatrolQueryLabelProvider.getImage((PatrolQueryOption)element);
			}else if (element instanceof IExtensionOption){
				return ((IExtensionOption)element).getImage();
			}
			return super.getImage(element);
		}
	};

}
