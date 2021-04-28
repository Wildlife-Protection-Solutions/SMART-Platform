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
package org.wcs.smart.i2.model;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.wcs.smart.util.ColorUtil;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Style options for relationship diagram node.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipDiagramNodeStyleOptions {

	private static final String FG_COLOR_KEY = "foregroundColor"; //$NON-NLS-1$
	private static final String BG_COLOR_KEY = "backgroundColor"; //$NON-NLS-1$
	private static final String IMAGE_SIZE_KEY = "imageSize"; //$NON-NLS-1$
	private static final String FONT_DATA_KEY = "fontData"; //$NON-NLS-1$
	
	/**
	 * The supported image sizes.
	 * 
	 * @author elitvin
	 * @since 6.0.0
	 */
	public enum ImageSizeOption {
		SMALL  (50),
		MEDIUM (100),
		LARGE  (150);

		private final int size;
		
		public static final ImageSizeOption DEFAULT_IMAGE_SIZE_OPTION = SMALL;

		private ImageSizeOption(int size) {
			this.size = size;
		}

		public int getSize() {
			return size;
		}
	}
	
	private static final FontData DEFAULT_FONT_DATA = new FontData("Segoe UI", 10, SWT.NORMAL); //$NON-NLS-1$

	private JsonObject json;

	public RelationshipDiagramNodeStyleOptions(String options) {
		json = new Gson().fromJson(options, JsonObject.class);
	}

	protected RelationshipDiagramNodeStyleOptions(JsonObject json) {
		this.json = json;
	}
	
	protected JsonObject getJson() {
		return json;
	}
	
	public ImageSizeOption getImageSize() {
		String strSize = json.get(IMAGE_SIZE_KEY).getAsString();
		if (strSize == null || strSize.isEmpty()) {
			return ImageSizeOption.DEFAULT_IMAGE_SIZE_OPTION;
		}
		ImageSizeOption sizeOp = ImageSizeOption.valueOf(strSize);
		return sizeOp;
	}
	public void setImageSize(ImageSizeOption size) {
		if (size != null) {
			json.addProperty(IMAGE_SIZE_KEY, size.name());
		}
	}
	
	public Color getBackgroudColor() {
		String hex = json.get(BG_COLOR_KEY).getAsString();
		Color color = ColorUtil.hex2Color(hex);
		return color;
		
	}
	public void setBackgroudColor(RGB color) {
		json.addProperty(BG_COLOR_KEY, ColorUtil.color2HexStr(color));
	}

	public Color getForegroundColor() {
		String hex = json.get(FG_COLOR_KEY).getAsString();
		Color color = ColorUtil.hex2Color(hex);
		return color;
		
	}
	public void setForegroundColor(RGB color) {
		json.addProperty(FG_COLOR_KEY, ColorUtil.color2HexStr(color));
	}
	
	public FontData getFontData() {
		JsonElement el = json.get(FONT_DATA_KEY);
		if (el != null) {
			String fdStr = el.getAsString();
			FontData fd = new FontData(fdStr);
			return fd;
		}
		return DEFAULT_FONT_DATA;
	}
	public void setFontData(FontData fontData) {
		json.addProperty(FONT_DATA_KEY, fontData.toString());
	}

}
