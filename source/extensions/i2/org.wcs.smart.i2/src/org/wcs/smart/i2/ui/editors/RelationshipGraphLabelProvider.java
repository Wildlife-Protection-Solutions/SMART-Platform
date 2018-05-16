/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors;

import java.util.Map;

import org.eclipse.gef.zest.fx.jface.IGraphAttributesProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.ui.Thumbnail;

/**
 * Label provider for graph that displays {@link IntelEntity} relationships.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipGraphLabelProvider extends LabelProvider implements IGraphAttributesProvider {
	
	@Override
	public String getText(Object element) {
		if (element instanceof IntelEntity) {
			IntelEntity e = (IntelEntity) element;
			return e.getIdAttributeAsText();
		}
		return super.getText(element);
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof IntelEntity) {
			IntelEntity e = (IntelEntity) element;
			Thumbnail thum = new Thumbnail(e.getPrimaryAttachment());
			return thum.getImage();
		}
		return super.getImage(element);
	}

	@Override
	public Map<String, Object> getNodeAttributes(Object node) {
		return null;
	}
	
	@Override
	public Map<String, Object> getEdgeAttributes(Object sourceNode, Object targetNode) {
		return null;
	}

	@Override
	public Map<String, Object> getGraphAttributes() {
		return null;
	}

	@Override
	public Map<String, Object> getNestedGraphAttributes(Object nestingNode) {
		return null;
	}

}
