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
package org.wcs.smart.shared.labels;

import java.util.Locale;

import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.IGridQueryColumnLabelProvider;

public class GridQueryColumnLabelProvider implements IGridQueryColumnLabelProvider {

	@Override
	public String getLabel(Object key, Locale l) {
		if (key instanceof GridQueryColumn.GridColumns){
			switch((GridQueryColumn.GridColumns)key){
				case TILE_X:return "Tile X ID";
				case TILE_Y:return "Tile Y ID";
				case VALUE:return "Value";
			}
		}
		if (key.equals(GRID_TO_BIG_KEY)){
			return "Grid too large.  Increase the grid size."	;
		}
		return null;
	}
}
