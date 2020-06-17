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
import java.util.HashMap;
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
import org.locationtech.udig.legend.ui.LegendLocationStyle;
import org.locationtech.udig.legend.ui.LegendLocationStyleContent;
import org.locationtech.udig.legend.ui.LegendStyle;
import org.locationtech.udig.legend.ui.LegendStyleContent;
import org.locationtech.udig.mapgraphic.MapGraphic;
import org.locationtech.udig.mapgraphic.MapGraphicContext;
import org.locationtech.udig.mapgraphic.internal.MapGraphicResource;
import org.locationtech.udig.mapgraphic.style.FontStyle;
import org.locationtech.udig.mapgraphic.style.FontStyleContent;
import org.locationtech.udig.project.IBlackboard;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.locationtech.udig.ui.graphics.SLDs;
import org.locationtech.udig.ui.graphics.ViewportGraphics;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.feature.type.Name;
import org.wcs.smart.udig.legend.style.LegendLayerStyle;
import org.wcs.smart.udig.legend.style.LegendLayerStyleContent;

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
	private int horizontalSpacing; // distance between columns
	private int imageSpacing; //distance between glyph and text
	private int numCols; //number of columns
	private int indentSize; //indent for substyles	
	private Color backgroundColour;
	private int imageWidth;
	private int imageHeight; // size of glyph image
	private boolean drawBorder;
	
	private double scale;	//scale based on dpi
	
	public void draw(MapGraphicContext context) {

		scale = context.getMapDisplay().getDPI() / 96.0;
		
		IBlackboard blackboard = context.getLayer().getStyleBlackboard();
		LegendStyle legendStyle = (LegendStyle) blackboard.get(LegendStyleContent.ID);
		if (legendStyle == null) {
			legendStyle = LegendStyleContent.createDefault();
			blackboard.put(LegendStyleContent.ID, legendStyle);
		}

		LegendLocationStyle locationStyle = (LegendLocationStyle) blackboard.get(LegendLocationStyleContent.ID);
		if (locationStyle == null) {
			locationStyle = LegendLocationStyleContent.createDefaultStyle();
			blackboard.put(LegendLocationStyleContent.ID, locationStyle);
		}

		FontStyle fontStyle = (FontStyle) blackboard.get(FontStyleContent.ID);
		if (fontStyle == null) {
			fontStyle = new FontStyle();
			blackboard.put(FontStyleContent.ID, fontStyle);
		}

		this.backgroundColour = legendStyle.backgroundColour;
		this.horizontalMargin = (int)(legendStyle.horizontalMargin * scale);
		this.verticalMargin = (int)(legendStyle.verticalMargin * scale);
		this.horizontalSpacing = (int)(legendStyle.horizontalSpacing * scale);
		this.verticalSpacing = (int)(legendStyle.verticalSpacing * scale);
		this.indentSize = (int)(legendStyle.indentSize * scale);
		this.numCols = legendStyle.numCols;
		this.imageSpacing = (int)(legendStyle.imageSpacing * scale);
		drawBorder = legendStyle.drawBorder;
		this.imageHeight = (int)(legendStyle.imageHeight * scale);
		this.imageWidth = (int)(legendStyle.imageWidth * scale);

		final ViewportGraphics graphics = context.getGraphics();

		if (fontStyle.getFont() != null) {
			graphics.setFont(fontStyle.getFont());
		}

		List<Map<ILayer, LegendEntry[]>> layers = new ArrayList<Map<ILayer, LegendEntry[]>>();

		int numberOfEntries = 0;
		int numberOfLayers = 0;
		/*
		 * Set up the layers that we want to draw so we can operate just on
		 * those ones. Layers at index 0 are on the bottom of the map, so we
		 * must iterate in reverse.
		 * 
		 * While we are doing this, determine the longest row so we can properly
		 * draw the graphic's border.
		 */
		if (imageWidth < 0) imageWidth = 0;
		if (imageHeight < 0) imageHeight = 0;
		
		Dimension imageSize = new Dimension(imageWidth,imageHeight);
		Dimension textSize = new Dimension(0, graphics.getFontHeight());
		
		for (int i = context.getMapLayers().size() - 1; i >= 0; i--) {
			ILayer layer = context.getMapLayers().get(i);

			LegendLayerStyle ll = (LegendLayerStyle) layer.getStyleBlackboard().get(LegendLayerStyleContent.ID);
			if (ll != null && !ll.isVisible) continue;
					
			
			if (!(layer.getGeoResource() instanceof MapGraphicResource)
					&& layer.isVisible()) {

				if (layer.hasResource(MapGraphic.class)) {
					// don't include mapgraphics
					continue;
				}
				numberOfLayers++;
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
				numberOfEntries += entries.length;
			}
		}

		if (numberOfEntries == 0) {
			// nothing to draw!
			return;
		}
		if (numCols > numberOfLayers) {
			numCols = numberOfLayers;
		}
		if (numCols < 1) {
			numCols = 1;
		}
		int entriesPerCol = (int)Math.ceil( numberOfEntries / (float)numCols );
		numCols = (int)Math.ceil(numberOfEntries / entriesPerCol);
		
		//for each column get the layer and maxwidth
		List<LegendColumn> columns = new ArrayList<>();
		
		int cnt = 0; 
		LegendColumn current = new LegendColumn();
		columns.add(current);
		for (int i = 0; i < layers.size(); i++) {
			if (cnt >= entriesPerCol) {
				current = new LegendColumn();
				columns.add(current);	
				cnt= 0;
			}
			
			Map<ILayer, LegendEntry[]> map = layers.get(i);
			final ILayer layer = map.keySet().iterator().next();
			final LegendEntry[] entries = map.values().iterator().next();
			cnt+= entries.length;
			current.layer.add(layer);
			current.entries.put(layer, entries);
		}
			
		
		final int rowHeight = Math.max(imageHeight, graphics.getFontHeight()); 
		
		// we want to grow to whatever size we need
		int width = horizontalMargin;
		for (LegendColumn c : columns) {
			width += c.getMaxLength(graphics) + horizontalSpacing;
		}
		width = width + horizontalMargin - horizontalSpacing;
	
		int height = 0;
		for (LegendColumn c : columns) {
			int temp = c.computHeight(rowHeight);
			if (temp > height) height = temp;
		}
		
		int rawx = 0;
		int rawy = 0;
		
		// ensure box within the display
		Dimension displaySize = context.getMapDisplay().getDisplaySize();
		int xpad = (int)(locationStyle.xpadding * scale);
		int ypad = (int)(locationStyle.ypadding * scale);
		if (locationStyle.xposition == LegendLocationStyle.FixedPosition.LEFT.value) {
			rawx = xpad;
		}else if (locationStyle.xposition == LegendLocationStyle.FixedPosition.MIDDLE.value) {
			rawx = (displaySize.width / 2) - (width / 2);
			rawx += xpad;
		}else if (locationStyle.xposition == LegendLocationStyle.FixedPosition.RIGHT.value) {
			rawx = displaySize.width - width - xpad;
		}else {
			rawx = locationStyle.xposition;
		}
		
		if (locationStyle.yposition == LegendLocationStyle.FixedPosition.TOP.value) {
			rawy = ypad;
		}else if (locationStyle.yposition == LegendLocationStyle.FixedPosition.MIDDLE.value) {
			rawy = (displaySize.height / 2) - (height / 2);
			rawy += ypad;
		}else if (locationStyle.yposition == LegendLocationStyle.FixedPosition.BOTTOM.value) {
			rawy = displaySize.height- height - ypad;
		}else {
			rawy = locationStyle.yposition;
		}
		
		Rectangle bounds = new Rectangle(rawx, rawy, width, height);
		graphics.setClip(bounds);

		//Draw the box containing the layers/icons
		drawOutline(graphics, context, bounds, fontStyle);

		// Draw the layer names/icons
		int x = rawx + horizontalMargin;
		int y = rawy + verticalMargin;

		if (fontStyle.getFont() != null) {
			graphics.setFont(fontStyle.getFont());
		}

		ImageGenerator imageGenerator = new ImageGenerator(imageWidth);
		
		for (LegendColumn column : columns) {
		
			for (ILayer layer : column.layer) {
				final LegendEntry[] entries = column.entries.get(layer);
	
				try {
					layer.getGeoResources().get(0).getInfo(null);
				} catch (Exception ex) {
				}
	
	
				for (int k = 0; k < entries.length; k++) {
					BufferedImage awtIcon = null;
					if (entries[k].getRule() != null) {
						// generate icon from use
						ImageDescriptor descriptor = imageGenerator.generateStyledIcon(layer, entries[k].getRule());
						if (descriptor == null) {
							descriptor = imageGenerator.generateIcon((Layer) layer);
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
						ImageDescriptor descriptor = imageGenerator.generateIcon((Layer) layer);
						if (descriptor != null) {
							awtIcon = AWTSWTImageUtils.convertToAWT(descriptor.getImageData());
						}
					}
					drawRow(graphics, x, y, awtIcon, entries[k].getText(), k != 0, entries[k].getTextPosition());
					y += rowHeight;
					
					if (entries[k].getSpacingAfter() != null){
						y += entries[k].getSpacingAfter();
					}else{
						y += verticalSpacing;
					}
				}
			}
			
			x += column.getMaxLength(graphics) + horizontalSpacing;
			y = rawy + verticalMargin;
			
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

		int height = (int)graphics.getStringBounds(text[0] == null ? "W" : text[0]).getHeight(); //$NON-NLS-1$
		/*
		 * Center the smaller item (text or icon) according to the taller one.
		 */
		int textVerticalOffset = 0;
		int iconVerticalOffset = 0;

		if ((position | SWT.CENTER) == position) {
			if (imageHeight == height) {
				// items are the same height; do nothing.
			} else if (imageHeight > height) {
				int difference = imageHeight - height;
				textVerticalOffset = difference / 2;
			} else if (imageHeight < height) {
				int difference = height - imageHeight;
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

		if (text != null && text[0] != null && text[0].length() != 0) {
			graphics.drawString(
					text[0],
					x + imageSpacing,
					y + textVerticalOffset
					- (int) (graphics.getFontHeight() - graphics.getFontAscent()),
					ViewportGraphics.ALIGN_LEFT, ViewportGraphics.ALIGN_TOP);
		}

		if (text != null && text.length > 1 && text[text.length - 1] != null) {
			//draw last label at bottom of range
			String end = text[text.length - 1];

			graphics.drawString(end, x + imageSpacing, y + imageHeight
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
		
		if (drawBorder) {
			outline = new Rectangle(locationStyle.x, locationStyle.y,
					locationStyle.width-1, locationStyle.height-1);
			graphics.draw(outline);
		}
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
    
    class LegendColumn{
    	
    	List<ILayer> layer = new ArrayList<>();
    	HashMap<ILayer, LegendEntry[]> entries = new HashMap<>();
    	
    	public int getMaxLength(ViewportGraphics graphics) {
    		int longestRow = 0;
    		for (LegendEntry[] entries : entries.values()) {
		    	for (int j = 0; j < entries.length; j++) {
					StringBuilder sb = new StringBuilder();
					for (int k = 0; k < entries[j].getText().length; k++){
						sb.append(entries[j].getText()[k]);
					}
					Rectangle2D bounds = graphics.getStringBounds(sb.toString());
					
					int length = (int)bounds.getWidth() + imageSpacing + imageWidth;
					if (j != 0) length += indentSize;
		
					if (length > longestRow) {
						longestRow = length;
					}
				}
    		}
    		return longestRow;
    	}
    	
    	public int computHeight(int rowHeight) {
    		int numitems = 0;
    		for (LegendEntry[] e : entries.values()) {
    			numitems += e.length;
    		}
    		int height = rowHeight  * numitems + verticalMargin * 2;
    		
			
    		for (LegendEntry[] entries : entries.values()) {
    			for (int j = 0; j < entries.length; j ++){
    				if (entries[j].getSpacingAfter() == null){
    					height += verticalSpacing;
    				}else{
    					height += entries[j].getSpacingAfter();
    				}
				}
			}
			
			return height - verticalSpacing;
    	}
    }
}
