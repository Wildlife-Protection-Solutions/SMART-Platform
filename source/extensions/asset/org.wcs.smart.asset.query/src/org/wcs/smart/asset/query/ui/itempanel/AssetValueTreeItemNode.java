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
package org.wcs.smart.asset.query.ui.itempanel;

import java.util.Locale;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetValueOption;
import org.wcs.smart.asset.ui.IQueryAssetLabelProvider;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;

/**
 * Tree node provided for asset deployment summaries
 * @author Emily
 *
 */
public class AssetValueTreeItemNode implements IItemTreeNode{

	public static final String KEY = "assetvalue"; //$NON-NLS-1$

	private ILabelProvider pp = new LabelProvider() {
		@Override
		public String getText(Object element) {
			return SmartContext.INSTANCE.getClass(IQueryAssetLabelProvider.class).getLabel(element, Locale.getDefault());
		}
		
		@Override
		public Image getImage(Object element) {
			return null;
		}
	};
	
	private ITreeContentProvider provider = new ITreeContentProvider() {

		@Override
		public Object[] getElements(Object inputElement) {
			return AssetValueOption.values();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}};
		
	
	@Override
	public String getName() {
		return Messages.AssetValueTreeItemNode_TreeNodeName;
	}

	@Override
	public Image getImage() {
		return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_ASSET);
	}

	@Override
	public ITreeContentProvider getContentProvider() {
		return provider;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		return pp;
	}

	@Override
	public String getKey() {
		return KEY;
	}
	
}
