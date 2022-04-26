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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.IConnectionStyleProvider;
import org.eclipse.zest.core.viewers.IEntityStyleProvider;
import org.eclipse.zest.core.viewers.IFigureProvider;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.RelationshipDiagramEdgeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramEdgeStyleOptions.EdgeStyle;
import org.wcs.smart.i2.model.RelationshipDiagramEntityTypeStyle;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions.ImageSizeOption;
import org.wcs.smart.i2.model.RelationshipDiagramRelationshipTypeStyle;
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
public class RelationshipGraphLabelProvider extends LabelProvider implements IFigureProvider, 
	IConnectionStyleProvider, IEntityStyleProvider,  IFontProvider {
	
	private RelationshipGraphContentProvider graphContentProvider;
	private RelationshipDiagramStyle style;
	
	private Map<RelationshipDiagramNodeStyleOptions, Font> fonts = new HashMap<>();
	private Map<IntelEntity, Image> images = new HashMap<>();
	
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
		}else if (element instanceof EntityConnectionData) {
			EntityConnectionData data = (EntityConnectionData)element;
			IntelEntityRelationship r = getRelationship(data.source, data.dest);
			
			if (r != null) {
				if (style == null) return r.getRelationshipType().getName();
				if (getEdgeOptions(r).isShowLabel()) return r.getRelationshipType().getName();
				return null;
			}
		}
		return super.getText(element);
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof IntelEntity) {
			IntelEntity e = (IntelEntity) element;
			if (images.containsKey(e)) return images.get(e);
			
			int size = style != null ? getNodeOptions(element).getImageSize().getSize() : ImageSizeOption.DEFAULT_IMAGE_SIZE_OPTION.getSize();
			Thumbnail thum = new Thumbnail(e.getPrimaryAttachment(), size);
			Image i = thum.getImage();
			images.put(e, i);
			return i;
		}
		return super.getImage(element);
	}

//	@Override
//	public Color getForeground(Object element) {
//		if (style != null) {
//			return getNodeOptions(element).getForegroundColor();
//		}
//		return null;
//	}
//
//	@Override
//	public Color getBackground(Object element) {
//		if (style != null) {
//			return getNodeOptions(element).getBackgroudColor();
//		}
//		return null;
//	}

	
	
	@Override
	public Font getFont(Object element) {
		if (style != null) {
			RelationshipDiagramNodeStyleOptions ops = getNodeOptions(element);
			if (fonts.containsKey(ops)) return fonts.get(ops);
		
			FontData fd = ops.getFontData();
			if (fd == null) return null;
			Font f = new Font(Display.getCurrent(), fd);
			fonts.put(ops, f);
			return f;
		}
		return null;
	}

	@Override
	public Color getNodeHighlightColor(Object element) {
		return Display.getDefault().getSystemColor(SWT.COLOR_RED);
	}

	@Override
	public Color getBorderColor(Object entity) {
		return null;
	}

	@Override
	public Color getBorderHighlightColor(Object entity) {
		return Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
	}

	@Override
	public int getBorderWidth(Object entity) {
		return 0;
	}

	@Override
	public Color getBackgroundColour(Object entity) {
		if (style != null) {
			return getNodeOptions(entity).getBackgroudColor();
		}
		return null;
	}

	@Override
	public Color getForegroundColour(Object entity) {
		if (style != null) {
			return getNodeOptions(entity).getForegroundColor();
		}
		return null;
	}

	@Override
	public IFigure getTooltip(Object element) {
		if (element instanceof IntelEntity) {
			IntelEntity ie = (IntelEntity)element;
			
			String lbl = MessageFormat.format(Messages.RelationshipGraphLabelProvider_EntityTooltip, 
					ie.getIdAttributeAsText(), "\n", ie.getEntityType().getName(), //$NON-NLS-1$
					"\n", ie.getProfile().getName()); //$NON-NLS-1$
			Label l = new Label(lbl);
			return l;
			
		}else if (element instanceof EntityConnectionData) {
			EntityConnectionData cd = (EntityConnectionData)element;
			IntelEntityRelationship r = getRelationship(cd.source, cd.dest);
			if (r != null) {
				String lbl = MessageFormat.format(Messages.RelationshipGraphLabelProvider_RelationshipTooltip,
						r.getRelationshipType().getName(), "\n", //$NON-NLS-1$
						r.getSourceEntity().getIdAttributeAsText(), "\n", //$NON-NLS-1$
						r.getTargetEntity().getIdAttributeAsText());
				return new Label(lbl);
			}
		}
		return null;
	}

	@Override
	public boolean fisheyeNode(Object entity) {
		return false;
	}

	@Override
	public int getConnectionStyle(Object object) {
		if (style == null) return 0;
		if (object instanceof EntityConnectionData) {
			IntelEntityRelationship r = getRelationship(((EntityConnectionData)object).source, ((EntityConnectionData)object).dest);
			RelationshipDiagramEdgeStyleOptions options = getEdgeOptions(r);
			if (options != null) {
				if (options.getStyle() == EdgeStyle.ARROW) return ZestStyles.CONNECTIONS_DIRECTED;
						
			}
		}
		return 0;
	}

	@Override
	public Color getColor(Object object) {
		if (style == null) return null;
		if (object instanceof EntityConnectionData) {
			IntelEntityRelationship r = getRelationship(((EntityConnectionData)object).source, ((EntityConnectionData)object).dest);
			RelationshipDiagramEdgeStyleOptions options = getEdgeOptions(r);
			if (options != null) {
				return options.getColor();
			}
		}
		return null;
	}

	@Override
	public Color getHighlightColor(Object rel) {
		return Display.getDefault().getSystemColor(SWT.COLOR_RED);
	}

	@Override
	public int getLineWidth(Object object) {
		return 1;
	}

	@Override
	public IFigure getFigure(Object element) {
		if (element instanceof IntelEntity) {
			String text = getText(element);
			if (text == null || text.isEmpty()) text = " "; //$NON-NLS-1$
			IFigure figure = new EntityFigure(text, getImage(element), true);
			Font f = getFont(element);
			figure.setForegroundColor(getForegroundColour(element));
			if (f != null) figure.setFont(f);
			figure.setToolTip(getTooltip(element));
			return figure;
		}
		return null;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		
		fonts.values().forEach(f->f.dispose());
		fonts.clear();
		
		images.values().forEach(e->e.dispose());
		images.clear();
	}

}
