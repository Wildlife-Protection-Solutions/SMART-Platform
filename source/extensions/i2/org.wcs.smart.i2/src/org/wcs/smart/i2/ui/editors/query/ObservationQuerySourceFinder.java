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
package org.wcs.smart.i2.ui.editors.query;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.engine.IntelObservationResultItem;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;

/**
 * Opens the record that is identified as the source for the result item.
 * 
 * @author Emily
 *
 */
public class ObservationQuerySourceFinder implements IQuerySourceFinder {

	public static ObservationQuerySourceFinder INSTANCE = new ObservationQuerySourceFinder();
	
	private ObservationQuerySourceFinder(){}
	
	public void runAction(IResultItem item){
		if (!(item instanceof IntelObservationResultItem)) return;	
		IntelObservationResultItem i = (IntelObservationResultItem)item;
		//TODO: add record source to query results?
		(new OpenRecordHandler()).openRecord(new RecordEditorInput(i.getRecordTitle(), i.getRecordUuid(), null, null, null), false);
	}

	@Override
	public Image getImage() {
		return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RECORD);
	}

	@Override
	public String getName() {
		return "Open Record...";
	}
}
