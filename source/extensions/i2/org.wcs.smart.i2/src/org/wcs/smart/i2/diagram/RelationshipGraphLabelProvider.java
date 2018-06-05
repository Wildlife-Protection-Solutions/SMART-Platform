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
package org.wcs.smart.i2.diagram;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.gef.zest.fx.ZestProperties;
import org.eclipse.gef.zest.fx.jface.IGraphAttributesProvider;
import org.eclipse.gef.zest.fx.jface.ZestContentViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.RelationshipDiagramEdgeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions.ImageSizeOption;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;
import org.wcs.smart.i2.model.RelationshipDiagramStyleOptions;
import org.wcs.smart.ui.Thumbnail;

/**
 * Label provider for graph that displays {@link IntelEntity} relationships.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipGraphLabelProvider extends LabelProvider implements IGraphAttributesProvider, IColorProvider {
	
	private ZestContentViewer graphViewer;
	private RelationshipDiagramStyle style;
	
	public RelationshipGraphLabelProvider(ZestContentViewer graphViewer) {
		this.graphViewer = graphViewer; 
	}

	private boolean isRootNode(Object node) {
		Object input = graphViewer.getInput();
		if (input instanceof Collection<?>) {
			return ((Collection<?>)input).contains(node);
		}
		return node.equals(input);
	}
	
	private IntelEntityRelationship getRelationship(Object source, Object target) {
		IContentProvider cp = graphViewer.getContentProvider();
		if (cp instanceof RelationshipGraphContentProvider) {
			return ((RelationshipGraphContentProvider)cp).getRelationship(source, target);
		}
		return null;
	}
	
	public void setStyle(RelationshipDiagramStyle style) {
		this.style = style;
	}
	
	private RelationshipDiagramNodeStyleOptions getNodeOptions(Object element) {
		RelationshipDiagramStyleOptions styleOptions = style.getStyleOptions();
		if (isRootNode(element) && styleOptions.getRootNodeStyle() != null) {
			return styleOptions.getRootNodeStyle();
		}
		//TODO: ZZZZZZZ fetch for specific node
		return styleOptions.getDefaultNodeStyle();
		
	}

	private RelationshipDiagramEdgeStyleOptions getEdgeOptions(Object element) {
		//TODO: ZZZZZZZ fetch for specific node
		return style.getStyleOptions().getDefaultEdgeStyle();
		
	}
	
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
			int size = style != null ? getNodeOptions(element).getImageSize().getSize() : ImageSizeOption.DEFAULT_IMAGE_SIZE_OPTION.getSize();
			Thumbnail thum = new Thumbnail(e.getPrimaryAttachment(), size);
			return thum.getImage();
		}
		return super.getImage(element);
	}

	@Override
	public Map<String, Object> getNodeAttributes(Object node) {
		if (node instanceof IntelEntity) {
			IntelEntity e = (IntelEntity) node;
			Map<String, Object> attributes = new HashMap<>();
			attributes.put(ZestProperties.TOOLTIP__N, "Tooltip: " + e.getIdAttributeAsText()); //TODO: ZZZZZZZ need proper tooltip!!!
			return attributes;
		}
		return null;
	}
	
	@Override
	public Map<String, Object> getEdgeAttributes(Object sourceNode, Object targetNode) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(ZestProperties.TARGET_DECORATION__E, new javafx.scene.shape.Polygon(0, 0, 10, 3, 10, -3));
		IntelEntityRelationship r = getRelationship(sourceNode, targetNode);
		if (r != null && style != null) {
			RelationshipDiagramEdgeStyleOptions options = getEdgeOptions(r);
			String color = options.getColorAsString();
			attributes.put(ZestProperties.TARGET_DECORATION_CSS_STYLE__E, "-fx-stroke: "+color+"; -fx-fill: "+color+";"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			attributes.put(ZestProperties.CURVE_CSS_STYLE__E, "-fx-stroke: "+color+";"); //$NON-NLS-1$ //$NON-NLS-2$
			if (options.isShowLabel()) {
				attributes.put(ZestProperties.LABEL__NE, r.getRelationshipType().getName());
			}
		}
		
		return attributes;
	}

	@Override
	public Map<String, Object> getGraphAttributes() {
		return null;
	}

	@Override
	public Map<String, Object> getNestedGraphAttributes(Object nestingNode) {
		return null;
	}

	@Override
	public Color getForeground(Object element) {
		if (style != null) {
			return getNodeOptions(element).getForegroundColor();
		}
		return null;
	}

	@Override
	public Color getBackground(Object element) {
		if (style != null) {
			return getNodeOptions(element).getBackgroudColor();
		}
		return null;
	}

}
