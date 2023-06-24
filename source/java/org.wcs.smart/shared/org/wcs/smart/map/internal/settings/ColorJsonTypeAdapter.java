/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.map.internal.settings;

import java.awt.Color;
import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Color type adapter for decoding map json
 * 
 * @author Emily
 *
 */
public class ColorJsonTypeAdapter extends TypeAdapter<Color>{
	
	private static String VALUE_NAME = "value"; //$NON-NLS-1$
	private static String FALPHA_NAME = "falpha"; //$NON-NLS-1$
		
	@Override
	public Color read(JsonReader in) throws IOException {
		JsonToken peek = in.peek();
		if (peek == JsonToken.NULL) {
			in.nextNull();
			return null;
		}else {
			in.beginObject();
			
			Integer value = null;
			Integer falpha = null;
			for (int i = 0; i < 2; i ++) {
				String field = in.nextName();
				if (field.equalsIgnoreCase(VALUE_NAME)) {
					value = in.nextInt();
				}else if (field.equalsIgnoreCase(FALPHA_NAME)) {
					falpha = in.nextInt();
				}else {
					throw new IOException("invalid field for color"); //$NON-NLS-1$
				}
			}
			in.endObject();
			return new Color(value, falpha==1);
		}
	}

	@Override
	public void write(JsonWriter out, Color c) throws IOException {
		if (c == null) {
			out.nullValue();
		}else {
			out.beginObject();
			out.name(VALUE_NAME);
			out.value(c.getRGB());
			out.name(FALPHA_NAME);
			out.value(c.getAlpha() == 255 ? 0 : 1);
			out.endObject();
		}
		
	}
}
