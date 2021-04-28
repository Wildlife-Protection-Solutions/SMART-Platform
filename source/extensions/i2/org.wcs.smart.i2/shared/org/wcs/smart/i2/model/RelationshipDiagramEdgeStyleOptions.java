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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.wcs.smart.util.ColorUtil;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Style options for relationship diagram edge.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipDiagramEdgeStyleOptions {

	private static final String SHOW_LABEL_KEY = "showLabel"; //$NON-NLS-1$
	private static final String COLOR_KEY = "color"; //$NON-NLS-1$
	private static final String STYLE_KEY = "style"; //$NON-NLS-1$
	
	public enum EdgeStyle {
		LINE,
		ARROW;
	}
	
	private JsonObject json;

	public RelationshipDiagramEdgeStyleOptions(String options) {
		json = new Gson().fromJson(options, JsonObject.class);
	}

	protected RelationshipDiagramEdgeStyleOptions(JsonObject json) {
		this.json = json;
	}
	
	protected JsonObject getJson() {
		return json;
	}

	public Color getColor() {
		String hex = json.get(COLOR_KEY).getAsString();
		Color color = ColorUtil.hex2Color(hex);
		return color;
	}
	
	public String getColorAsString() {
		return json.get(COLOR_KEY).getAsString();
	}
	
	public void setColor(RGB color) {
		json.addProperty(COLOR_KEY, ColorUtil.color2HexStr(color));
	}

	public boolean isShowLabel() {
		return json.get(SHOW_LABEL_KEY).getAsBoolean();
	}
	
	public void setShowLabel(boolean showLabel) {
		json.addProperty(SHOW_LABEL_KEY, showLabel);
	}
	
	public EdgeStyle getStyle() {
		JsonElement e = json.get(STYLE_KEY);
		return e != null ? EdgeStyle.valueOf(e.getAsString()) : EdgeStyle.values()[0];
	}
	
	public void setStyle(EdgeStyle style) {
		json.addProperty(STYLE_KEY, style.name());
	}

}
