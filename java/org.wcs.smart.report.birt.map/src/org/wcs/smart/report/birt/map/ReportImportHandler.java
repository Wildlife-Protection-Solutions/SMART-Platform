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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SlotHandle;
import org.eclipse.birt.report.model.core.DesignElement;
import org.wcs.smart.report.in.IReportImportHandler;

/**
 * Handler that updates the map report item
 * query links when report is imported
 * 
 * @author egouge
 *
 */
public class ReportImportHandler implements IReportImportHandler {

	/**
	 * Updates the map report item
	 * query links.
	 * 
	 * @see org.wcs.smart.report.in.IReportImportHandler#reportImported(org.eclipse.birt.report.model.api.ReportDesignHandle, java.util.HashMap)
	 */
	@Override
	public void reportImported(ReportDesignHandle handle,
			HashMap<String, String> oldToNewQuery) throws Exception {

		for (Iterator<SlotHandle> iterator = handle.slotsIterator(); iterator.hasNext();) {
			SlotHandle type = (SlotHandle) iterator.next();
			List<DesignElement> elements = type.getSlot().getContents();
			for (DesignElement element : elements){
				Object x = element.getHandle(handle.getModule());
				if (x instanceof ExtendedItemHandle && ((ExtendedItemHandle) x).getReportItem() instanceof SmartMapItem){
					SmartMapItem item = (SmartMapItem)((ExtendedItemHandle)x).getReportItem();
					ArrayList<String> newLayers = new ArrayList<String>();
					if (item.getLayers() != null){
						for (String layer : item.getLayers()){
							if (oldToNewQuery.get(layer) != null){
								newLayers.add(oldToNewQuery.get(layer));
							}else{
								newLayers.add(layer);
							}
						}
					}
					item.setLayers(newLayers);
				}
			}
		}
	}

}
