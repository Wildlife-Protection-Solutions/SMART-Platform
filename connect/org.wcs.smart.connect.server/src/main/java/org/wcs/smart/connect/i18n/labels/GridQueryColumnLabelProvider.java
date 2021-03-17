/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.IGridQueryColumnLabelProvider;

/**
 * Grid query column label provider
 * 
 * @author Emily
 *
 */
public class GridQueryColumnLabelProvider implements IGridQueryColumnLabelProvider {

	@Override
	public String getLabel(Object key, Locale l) {
		if (key instanceof GridQueryColumn.GridColumns){
			switch((GridQueryColumn.GridColumns)key){
				case TILE_X:return Messages.getString("GridQueryColumnLabelProvider.XId", l); //$NON-NLS-1$
				case TILE_Y:return Messages.getString("GridQueryColumnLabelProvider.YId", l); //$NON-NLS-1$
				case VALUE:return Messages.getString("GridQueryColumnLabelProvider.Value", l); //$NON-NLS-1$
			}
		}
		if (key.equals(GRID_TO_BIG_KEY)){
			return Messages.getString("GridQueryColumnLabelProvider.GridTooBig", l); //$NON-NLS-1$
		}
		return null;
	}
}
