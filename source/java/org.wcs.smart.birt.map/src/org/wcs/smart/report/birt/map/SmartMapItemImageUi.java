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
package org.wcs.smart.report.birt.map;

import org.eclipse.birt.report.designer.ui.extensions.IReportItemImageProvider;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.report.birt.map.internal.Messages;

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
			int iwidth = BirtMapUtils.getWidthInPx(handle, Display.getCurrent().getDPI().x);
			int iheight = BirtMapUtils.getHeightInPx(handle, Display.getCurrent().getDPI().y);
			
			
			Image img = new Image(Display.getCurrent(), iwidth, iheight);
			gc = new GC(img);
			
			Image mapImage = SmartMapItemPlugIn.getDefault().getImageRegistry().get(SmartMapItemPlugIn.SMART_MAP_ICON_64);
			int x = (int)((iwidth - mapImage.getBounds().width) * 0.5);
			int y = (int)((iheight - mapImage.getBounds().height) * 0.5);
			gc.drawImage(mapImage, x, y);

			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
			gc.setLineWidth(2);
			gc.drawLine(0, 0, 0, iheight);
			gc.drawLine(0, iheight, iwidth, iheight);
			gc.drawLine(iwidth, iheight, iwidth, 0);
			gc.drawLine(iwidth, 0, 0,0);
			

			return img;

		} catch (Exception ex) {
			SmartMapItemPlugIn.displayLog(Messages.SmartMapItemImageUi_ErrorCreatingMapItem + ex.getMessage(), ex);
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
