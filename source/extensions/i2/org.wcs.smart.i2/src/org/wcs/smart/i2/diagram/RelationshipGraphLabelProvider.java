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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.gef.zest.fx.ZestProperties;
import org.eclipse.gef.zest.fx.jface.IGraphAttributesProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.RelationshipDiagramEdgeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramEntityTypeStyle;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions.ImageSizeOption;
import org.wcs.smart.i2.model.RelationshipDiagramRelationshipTypeStyle;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;
import org.wcs.smart.i2.model.RelationshipDiagramStyleOptions;
import org.wcs.smart.i2.ui.AttributeValueLabelProvider;
import org.wcs.smart.ui.Thumbnail;

/**
 * Label provider for graph that displays {@link IntelEntity} relationships.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipGraphLabelProvider extends LabelProvider implements IGraphAttributesProvider, IColorProvider, IFontProvider {
	
	private static final int TOOLTIP_TRUNCATE_LENGTH = 100;
	
	private RelationshipGraphContentProvider graphContentProvider;
	private RelationshipDiagramStyle style;
	
	private AttributeValueLabelProvider attributeLabelProvider = new AttributeValueLabelProvider();
	
	public RelationshipGraphLabelProvider(RelationshipGraphContentProvider graphContentProvider) {
		this.graphContentProvider = graphContentProvider; 
	}

	private boolean isRootNode(Object node) {
		return graphContentProvider.isRootNode(node);
	}
	
	private IntelEntityRelationship getRelationship(Object source, Object target) {
		return graphContentProvider.getRelationship(source, target);
	}
	
	public void setStyle(RelationshipDiagramStyle style) {
		this.style = style;
	}
	
	private RelationshipDiagramNodeStyleOptions getNodeOptions(Object element) {
		RelationshipDiagramStyleOptions styleOptions = style.getStyleOptions();
		if (isRootNode(element) && styleOptions.getRootNodeStyle() != null) {
			return styleOptions.getRootNodeStyle();
		}
		if (element instanceof IntelEntity) {
			IntelEntity e = (IntelEntity) element;
			RelationshipDiagramEntityTypeStyle etStyle = style.getEntityTypeStyles().get(e.getEntityType());
			if (etStyle != null) {
				return etStyle.getStyleOptions();
			}
		}
		return styleOptions.getDefaultNodeStyle();
		
	}

	private RelationshipDiagramEdgeStyleOptions getEdgeOptions(IntelEntityRelationship r) {
		RelationshipDiagramRelationshipTypeStyle rtStyle = style.getRelationshipTypeStyles().get(r.getRelationshipType());
		if (rtStyle != null) {
			return rtStyle.getStyleOptions();
		}
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
			attributes.put(ZestProperties.TOOLTIP__N, buildTooltip(e));
			return attributes;
		}
		return null;
	}
	
	private String buildTooltip(IntelEntity e) {
		StringBuilder sb = new StringBuilder();
		for (IntelEntityTypeAttribute a : e.getEntityType().getAttributes()) {
			if (sb.length() > 0) {
				sb.append("\n"); //$NON-NLS-1$
			}
			sb.append(a.getAttribute().getName()).append(": "); //$NON-NLS-1$
			String text = ""; //$NON-NLS-1$
			for (IntelEntityAttributeValue v : e.getAttributes()) {
				if (v.getAttribute().equals(a.getAttribute())){
					text = attributeLabelProvider.getText(v);
					break;
				}
			}
			//truncate long values
			if (text.length() > TOOLTIP_TRUNCATE_LENGTH + 5) {
				text = text.substring(0, TOOLTIP_TRUNCATE_LENGTH) + "..."; //$NON-NLS-1$
			}
			sb.append(text);
		}
		return sb.toString();
	}
	
	@Override
	public Map<String, Object> getEdgeAttributes(Object sourceNode, Object targetNode) {
		Map<String, Object> attributes = new HashMap<>();
		IntelEntityRelationship r = getRelationship(sourceNode, targetNode);
		if (r != null && style != null) {
			RelationshipDiagramEdgeStyleOptions options = getEdgeOptions(r);

			switch (options.getStyle()) {
			case LINE: break;
			case ARROW:
				attributes.put(ZestProperties.TARGET_DECORATION__E, 
						new javafx.scene.shape.Polygon(0, 0, 10, 3, 10, -3));
				break;
			}
			
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

	@Override
	public Font getFont(Object element) {
		if (style != null) {
			FontData fd = getNodeOptions(element).getFontData();
			return fd != null ? new Font(Display.getCurrent(), fd) : null;
		}
		return null;
	}

}
