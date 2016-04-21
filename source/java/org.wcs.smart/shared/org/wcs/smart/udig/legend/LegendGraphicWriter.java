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
package org.wcs.smart.udig.legend;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.locationtech.udig.legend.ui.LegendEntry;
import org.locationtech.udig.legend.ui.LegendStyle;
import org.locationtech.udig.legend.ui.LegendStyleContent;
import org.locationtech.udig.mapgraphic.MapGraphic;
import org.locationtech.udig.mapgraphic.MapGraphicContext;
import org.locationtech.udig.mapgraphic.internal.MapGraphicResource;
import org.locationtech.udig.mapgraphic.style.FontStyle;
import org.locationtech.udig.mapgraphic.style.FontStyleContent;
import org.locationtech.udig.mapgraphic.style.LocationStyleContent;
import org.locationtech.udig.project.IBlackboard;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.locationtech.udig.ui.graphics.SLDs;
import org.locationtech.udig.ui.graphics.ViewportGraphics;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.feature.type.Name;

/**
 * A legend graphic writer that does not require a display thread to
 * render graphics.  Modelled after the uDig MapGraphics class.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class LegendGraphicWriter  {

	private int verticalMargin; // distance between border and icons/text
	private int horizontalMargin; // distance between border and icons/text
	private int verticalSpacing; // distance between layers
	private int horizontalSpacing; // space between image and text
	
	private Color backgroundColour;
	private int indentSize;

	private int imageWidth;
	private int imageHeight; // size of glyph image

	public void draw(MapGraphicContext context) {

		IBlackboard blackboard = context.getLayer().getStyleBlackboard();
		LegendStyle legendStyle = (LegendStyle) blackboard.get(LegendStyleContent.ID);
		if (legendStyle == null) {
			legendStyle = LegendStyleContent.createDefault();
			blackboard.put(LegendStyleContent.ID, legendStyle);
		}

		Rectangle locationStyle = (Rectangle) blackboard.get(LocationStyleContent.ID);
		if (locationStyle == null) {
			locationStyle = new Rectangle(-1, -1, -1, -1);
			blackboard.put(LocationStyleContent.ID, locationStyle);
		}

		FontStyle fontStyle = (FontStyle) blackboard.get(FontStyleContent.ID);
		if (fontStyle == null) {
			fontStyle = new FontStyle();
			blackboard.put(FontStyleContent.ID, fontStyle);
		}

		this.backgroundColour = legendStyle.backgroundColour;
		this.horizontalMargin = legendStyle.horizontalMargin;
		this.verticalMargin = legendStyle.verticalMargin;
		this.horizontalSpacing = legendStyle.horizontalSpacing;
		this.verticalSpacing = legendStyle.verticalSpacing;
		this.indentSize = legendStyle.indentSize;
		this.imageHeight = legendStyle.imageHeight;
		this.imageWidth = legendStyle.imageWidth;

		final ViewportGraphics graphics = context.getGraphics();

		if (fontStyle.getFont() != null) {
			graphics.setFont(fontStyle.getFont());
		}

		List<Map<ILayer, LegendEntry[]>> layers = new ArrayList<Map<ILayer, LegendEntry[]>>();

		int longestRow = 0; // used to calculate the width of the graphic
		final int[] numberOfEntries = new int[1]; // total number of entries to
													// draw
		numberOfEntries[0] = 0;

		/*
		 * Set up the layers that we want to draw so we can operate just on
		 * those ones. Layers at index 0 are on the bottom of the map, so we
		 * must iterate in reverse.
		 * 
		 * While we are doing this, determine the longest row so we can properly
		 * draw the graphic's border.
		 */
		Dimension imageSize = new Dimension(imageWidth,imageHeight);
		Dimension textSize = new Dimension(0, graphics.getFontHeight());
		
		for (int i = context.getMapLayers().size() - 1; i >= 0; i--) {
			ILayer layer = context.getMapLayers().get(i);

			if (!(layer.getGeoResource() instanceof MapGraphicResource)
					&& layer.isVisible()) {

				if (layer.hasResource(MapGraphic.class)) {
					// don't include mapgraphics
					continue;
				}
				String layerName = layer.getName();
				if (layerName == null) {
					layerName = null;
				}
				LegendEntry layerEntry = new LegendEntry(layerName);

				FeatureTypeStyle[] styles = locateStyle(layer);
				LegendEntry[] entries = null;
				if (styles == null) {
					// we should have a label but no style
					entries = new LegendEntry[] { layerEntry };
				} else {
					List<Rule> rules = rules(styles);
					int ruleCount = rules.size();

					if (ruleCount == 1
							&& layer.getGeoResource().canResolve(
									GridCoverage.class)) {
						// grid coverage with single rule; lets see if it is a
						// theming style
						List<LegendEntry> cmEntries = ColorMapLegendCreatorAWT.findEntries(styles, imageSize, textSize);
						if (cmEntries != null) {
							cmEntries.add(0, layerEntry);
							entries = cmEntries.toArray(new LegendEntry[cmEntries.size()]);
						}
					}
					if (entries == null) {
						List<LegendEntry> localEntries = new ArrayList<LegendEntry>();
						if (ruleCount == 1) {
							layerEntry.setRule(rules.get(0));
						}
						localEntries.add(layerEntry); // add layer legend entry

						if (ruleCount > 1) {
							// we have more than one rule so there is likely
							// some themeing going on; add each of these rules
							for (Rule rule : rules) {
								LegendEntry rentry = new LegendEntry(rule);
								localEntries.add(rentry);
							}
						}
						entries = localEntries.toArray(new LegendEntry[localEntries.size()]);
					}
				}
				layers.add(Collections.singletonMap(layer, entries));

				// compute maximum length for each entry
				for (int j = 0; j < entries.length; j++) {
					StringBuilder sb = new StringBuilder();
					for (int k = 0; k < entries[j].getText().length; k++){
						sb.append(entries[j].getText()[k]);
					}
					Rectangle2D bounds = graphics.getStringBounds(sb.toString());
					int length = indentSize + imageWidth + horizontalSpacing
							+ (int) bounds.getWidth();

					if (length > longestRow) {
						longestRow = length;
					}
					numberOfEntries[0]++;
				}
			}
		}

		if (numberOfEntries[0] == 0) {
			// nothing to draw!
			return;
		}

		final int rowHeight = Math.max(imageHeight, graphics.getFontHeight()); 
		if (locationStyle.width == 0 || locationStyle.height == 0) {
			// we want to change the location style as needed
			// but not change the saved one so we create a copy here
			locationStyle = new Rectangle(locationStyle);
			if (locationStyle.width == 0) {
				// we want to grow to whatever size we need
				int width = longestRow + horizontalMargin * 2;
				locationStyle.width = width;
			}
			if (locationStyle.height == 0) {
				// we want to grow to whatever size we need
				int height = rowHeight * numberOfEntries[0] + verticalMargin * 2;
				for (int i = 0; i < layers.size(); i++) {
					Map<ILayer, LegendEntry[]> map = layers.get(i);
					final LegendEntry[] entries = map.values().iterator().next();
					for (int j = 0; j < entries.length; j ++){
						if (entries[j].getSpacingAfter() == null){
							height += verticalSpacing;
						}else{
							height += entries[j].getSpacingAfter();
						}
					}
				}
				locationStyle.height = height- verticalSpacing;
			}
		}

		// ensure box within the display
		Dimension displaySize = context.getMapDisplay().getDisplaySize();
		if (locationStyle.x < 0) {
			locationStyle.x = displaySize.width - locationStyle.width
					+ locationStyle.x;
		}
		if ((locationStyle.x + locationStyle.width + 6) > displaySize.width) {
			locationStyle.x = displaySize.width - locationStyle.width - 5;
		}

		if (locationStyle.y < 0) {
			locationStyle.y = displaySize.height - locationStyle.height - 5
					+ locationStyle.y;
		}
		if ((locationStyle.y + locationStyle.height + 6) > displaySize.height) {
			locationStyle.y = displaySize.height - locationStyle.height - 5;
		}

		graphics.setClip(new Rectangle(locationStyle.x, locationStyle.y,
				locationStyle.width + 1, locationStyle.height + 1));

		/*
		 * Draw the box containing the layers/icons
		 */
		drawOutline(graphics, context, locationStyle, fontStyle);

		/*
		 * Draw the layer names/icons
		 */
		int rowsDrawn = 0;
		int x = locationStyle.x + horizontalMargin;
		int y = locationStyle.y + verticalMargin;

		if (fontStyle.getFont() != null) {
			graphics.setFont(fontStyle.getFont());
		}

		for (int i = 0; i < layers.size(); i++) {
			Map<ILayer, LegendEntry[]> map = layers.get(i);
			final ILayer layer = map.keySet().iterator().next();
			final LegendEntry[] entries = map.values().iterator().next();

			try {
				layer.getGeoResources().get(0).getInfo(null);
			} catch (Exception ex) {
			}


			for (int k = 0; k < entries.length; k++) {
				BufferedImage awtIcon = null;
				if (entries[k].getRule() != null) {
					// generate icon from use
					ImageDescriptor descriptor = ImageGenerator.generateStyledIcon(layer, entries[k].getRule());
					if (descriptor == null) {
						descriptor = ImageGenerator.generateIcon((Layer) layer);
					}
					if (descriptor != null) {
						awtIcon = AWTSWTImageUtils
								.convertToAWT(descriptor.getImageData());
						}
				} else if (entries[k].getIcon() != null) {
					// use set icon
					awtIcon = AWTSWTImageUtils.convertToAWT(entries[k]
								.getIcon().getImageData());
				} else {
					// no rule, no icon, try default for layer
					ImageDescriptor descriptor = ImageGenerator.generateIcon((Layer) layer);
					if (descriptor != null) {
						awtIcon = AWTSWTImageUtils.convertToAWT(descriptor.getImageData());
					}
				}
				drawRow(graphics, x, y, awtIcon, entries[k].getText(), k != 0, entries[k].getTextPosition());
				y += rowHeight;
				
				if ((rowsDrawn + 1) < numberOfEntries[0]) {
					if (entries[k].getSpacingAfter() != null){
						y += entries[k].getSpacingAfter();
					}else{
						y += verticalSpacing;
					}
				}
				rowsDrawn++;
			}
		}
		// clear the clip so we don't affect other rendering processes
		graphics.setClip(null);
	}

	private List<Rule> rules(FeatureTypeStyle[] styles) {
		List<Rule> rules = new ArrayList<Rule>();
		for (FeatureTypeStyle featureTypeStyle : styles) {
			rules.addAll(featureTypeStyle.rules());
		}
		return rules;
	}

	private void drawRow(ViewportGraphics graphics, int x, int y,
			RenderedImage icon, String[] text, boolean indent, int position) {

		if (text.length == 0){
			return;
		}
		Rectangle2D stringBounds = graphics.getStringBounds(text[0]);

		/*
		 * Center the smaller item (text or icon) according to the taller one.
		 */
		int textVerticalOffset = 0;
		int iconVerticalOffset = 0;

		if ((position | SWT.CENTER) == position) {
			if (imageHeight == (int) stringBounds.getHeight()) {
				// items are the same height; do nothing.
			} else if (imageHeight > (int) stringBounds.getHeight()) {
				int difference = imageHeight - (int) stringBounds.getHeight();
				textVerticalOffset = difference / 2;
			} else if (imageHeight < (int) stringBounds.getHeight()) {
				int difference = (int) stringBounds.getHeight() - imageHeight;
				iconVerticalOffset = difference / 2;
			}
		} else if ((position | SWT.TOP) == position) {
			// do nothing; position everything at top
			textVerticalOffset = (int) (graphics.getFontAscent() * -0.6);
		} 

		if (indent) {
			x += indentSize;
		}

		if (icon != null) {
			graphics.drawImage(icon, x, y + iconVerticalOffset);
			x += imageWidth;
		}

		if (text != null && text[0].length() != 0) {
			graphics.drawString(
					text[0],
					x + horizontalMargin,
					y + textVerticalOffset
					- (int) (graphics.getFontHeight() - graphics.getFontAscent()),
					ViewportGraphics.ALIGN_LEFT, ViewportGraphics.ALIGN_TOP);
		}

		if (text != null && text.length > 1) {
			//draw last label at bottom of range
			String end = text[text.length - 1];

			graphics.drawString(end, x + horizontalMargin, y + imageHeight
					+ (int) (graphics.getFontAscent() * 0.3),
					ViewportGraphics.ALIGN_LEFT, ViewportGraphics.ALIGN_BOTTOM);
		}

	}

	private FeatureTypeStyle[] locateStyle(ILayer layer) {
		StyleBlackboard blackboard = (StyleBlackboard) layer
				.getStyleBlackboard();
		if (blackboard == null) {
			return null;
		}

		Style sld = (Style) blackboard.lookup(Style.class);
		if (sld == null) {
			return null;
		}

		List<FeatureTypeStyle> styles = new ArrayList<FeatureTypeStyle>();
		String layerTypeName = null;
		if (layer.getSchema() != null
				&& layer.getSchema().getTypeName() != null) {
			layerTypeName = layer.getSchema().getTypeName();
		}
		for (FeatureTypeStyle style : sld.featureTypeStyles()) {
			Set<Name> names = style.featureTypeNames();
			if (names.size() == 0) {
				styles.add(style);
			} else {
				for (Name name : names) {
					if (name.getLocalPart().equals(
							SLDs.GENERIC_FEATURE_TYPENAME)
							|| (layerTypeName != null && layerTypeName
									.equals(name.getLocalPart()))) {
						styles.add(style);
						break;
					}
				}
			}
		}
		return styles.toArray(new FeatureTypeStyle[0]);
	}

	private void drawOutline(ViewportGraphics graphics,
			MapGraphicContext context, Rectangle locationStyle, FontStyle fs) {
		Rectangle outline = new Rectangle(locationStyle.x, locationStyle.y,
				locationStyle.width, locationStyle.height);

		// reserve this area free of labels!
		context.getLabelPainter().put(outline);

		graphics.setColor(backgroundColour);
		graphics.fill(outline);

		graphics.setColor(fs.getColor());
		graphics.setBackground(backgroundColour);
		graphics.draw(outline);
	}
	

    public static ImageData convertToSWT(BufferedImage bufferedImage) {
        if (bufferedImage.getColorModel() instanceof DirectColorModel) {
            DirectColorModel colorModel
                    = (DirectColorModel) bufferedImage.getColorModel();
            PaletteData palette = new PaletteData(colorModel.getRedMask(),
                    colorModel.getGreenMask(), colorModel.getBlueMask());
            ImageData data = new ImageData(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), colorModel.getPixelSize(),
                    palette);
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = colorModel.getComponentSize();
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    int pixel = palette.getPixel(new RGB(pixelArray[0],
                            pixelArray[1], pixelArray[2]));
                    data.setPixel(x, y, pixel);
                    if (colorModel.hasAlpha()){
                    	data.setPixel(x, y, pixelArray[3]);	
                    }
                }
            }
            return data;
        }
        else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
            IndexColorModel colorModel = (IndexColorModel)
                    bufferedImage.getColorModel();
            int size = colorModel.getMapSize();
            byte[] reds = new byte[size];
            byte[] greens = new byte[size];
            byte[] blues = new byte[size];
            colorModel.getReds(reds);
            colorModel.getGreens(greens);
            colorModel.getBlues(blues);
            RGB[] rgbs = new RGB[size];
            for (int i = 0; i < rgbs.length; i++) {
                rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF,
                        blues[i] & 0xFF);
            }
            PaletteData palette = new PaletteData(rgbs);
            ImageData data = new ImageData(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), colorModel.getPixelSize(),
                    palette);
            data.transparentPixel = colorModel.getTransparentPixel();
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[1];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    data.setPixel(x, y, pixelArray[0]);
                }
            }
            return data;
        }else if (bufferedImage.getColorModel() instanceof ComponentColorModel){
        	ComponentColorModel colorModel = (ComponentColorModel)bufferedImage.getColorModel();
       	    PaletteData palette = new PaletteData(0x0000FF, 0x00FF00,0xFF0000);
       	    ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
        	data.transparentPixel = -1;
        	WritableRaster raster = bufferedImage.getRaster();
        	int[] pixelArray = colorModel.getComponentSize();
        	for (int y = 0; y < data.height; y++) {
        		for (int x = 0; x < data.width; x++) {
        			raster.getPixel(x, y, pixelArray);
        	        int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
        	        data.setPixel(x, y, pixel);
        	        if (colorModel.hasAlpha()){
        	        	data.setAlpha(x, y, pixelArray[3]);
        	        }
        		}
        	}
        	return data;
        }
        return null;
    }
}
