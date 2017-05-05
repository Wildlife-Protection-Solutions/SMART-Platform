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
package org.wcs.smart.entity.query.ui.itempanel;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;

/**
 * Entity Type tree node for queries.  Supports filter and summary
 * query types.
 * @author Emily
 *
 */
public class EntityTypeTreeNode implements IItemTreeNode {
	public static final String KEY = "entitytype"; //$NON-NLS-1$

	private ITreeContentProvider provider;
	private LabelProvider labelprovider;

	private String name;

	public enum Type {
		FILTER, GROUPBY
	};

	/**
	 * type of node
	 * 
	 * @param type
	 */
	public EntityTypeTreeNode(Type type) {
		if (type == Type.FILTER) {
			provider = new EntityTypeFilterContentProvider();
			labelprovider = EntityTypeFilterContentProvider.lblProvider;
			name = Messages.EntityTypeTreeNode_FilterLabel;
		} else if (type == Type.GROUPBY) {
			provider = new EntityTypeSummaryContentProvider();
			labelprovider = ((EntityTypeSummaryContentProvider) provider)
					.getLabelProvider();
			name = Messages.EntityTypeTreeNode_GroupByLabel;
		}

	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ITreeContentProvider getContentProvider() {
		return provider;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		return labelprovider;
	}

	@Override
	public Image getImage() {
		return EntityPlugIn.getDefault().getImageRegistry()
				.get(EntityPlugIn.ENTITY_TYPE_ICON);
	}

	@Override
	public String getKey() {
		return KEY;
	}

}
