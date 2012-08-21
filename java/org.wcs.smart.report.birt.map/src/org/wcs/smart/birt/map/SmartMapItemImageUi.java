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
package org.wcs.smart.birt.map;

import org.eclipse.birt.report.designer.ui.extensions.IReportItemImageProvider;
import org.eclipse.birt.report.model.api.DimensionHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.eclipse.birt.report.model.api.util.DimensionUtil;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * The implementation of the smart map item that appears in the report editor.
 * 
 * @author Emily
 * 
 */
public class SmartMapItemImageUi implements IReportItemImageProvider {

	public SmartMapItemImageUi() {
	}

	/**
	 * @see org.eclipse.birt.report.designer.ui.extensions.IReportItemImageProvider#getImage(org.eclipse.birt.report.model.api.ExtendedItemHandle)
	 */
	@Override
	public Image getImage(ExtendedItemHandle handle) {
		GC gc = null;
		try {
			DimensionHandle width = handle.getWidth();
			DimensionHandle height = handle.getHeight();
			Double w1 = DimensionUtil.convertTo(width.getDisplayValue(),
					width.getDefaultUnit(), DesignChoiceConstants.UNITS_IN)
					.getMeasure();
			Double h1 = DimensionUtil.convertTo(height.getDisplayValue(),
					height.getDefaultUnit(), DesignChoiceConstants.UNITS_IN)
					.getMeasure();
			int iwidth = (int) (w1 * Display.getCurrent().getDPI().x);
			int iheight = (int) (h1 * Display.getCurrent().getDPI().y);
			
			Image img = new Image(Display.getCurrent(), iwidth, iheight);
			gc = new GC(img);
			gc.drawLine(0, 0, iwidth, iheight);
			gc.drawLine(0, iheight, iwidth, 0);
			return img;

		} catch (Exception ex) {
			SmartMapItemPlugIn.displayLog("Could not create report map item: "
					+ ex.getMessage(), ex);
		} finally {
			if (gc != null && !gc.isDisposed()) {
				gc.dispose();
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.birt.report.designer.ui.extensions.IReportItemImageProvider#disposeImage(org.eclipse.birt.report.model.api.ExtendedItemHandle,
	 *      org.eclipse.swt.graphics.Image)
	 */
	@Override
	public void disposeImage(ExtendedItemHandle handle, Image image) {
		if (image != null && !image.isDisposed()) {
			image.dispose();
		}

	}

}
