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
package org.wcs.smart.i2.ui.views;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.search.IntelRecordSearchResultItem;
import org.wcs.smart.i2.ui.RecordLabelProvider;
import org.wcs.smart.i2.ui.RecordSourceLabelProvider;

/**
 * Label provider for records list view and various trees/tables
 * @author Emily
 *
 */
public class RecordsViewLabelProvider extends ColumnLabelProvider {
	
	private Font boldFont;
	
	private RecordLabelProvider labelProvider;
	private RecordSourceLabelProvider srcProvider;
	private boolean sourceAndStatusImg = false;
	//combined images for case where sourceAndStatusImg is true
	private HashMap<String, Image> images = new HashMap<String,Image>();
	private Image NULL_IMAGE;
	
	private IEventBroker eventBroker;
	private EventHandler refreshImages = new EventHandler() {
		@Override
		public void handleEvent(Event event) {
			srcProvider.disposeImages();
			for (Image i : images.values()) i.dispose();
			images.clear();
		}
	};
	
	public RecordsViewLabelProvider(IEclipseContext context){
		this(false, context);
	}
	
	public RecordsViewLabelProvider(boolean sourceAndStatusImg, IEclipseContext context){
		this.sourceAndStatusImg = sourceAndStatusImg;
		FontData fd = Display.getDefault().getActiveShell().getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(Display.getCurrent(), fd);
		labelProvider = new RecordLabelProvider();
		srcProvider = new RecordSourceLabelProvider();
		
		ImageData newData = new ImageData(1, 1, 1, new PaletteData(new RGB[] {new RGB(0, 0, 0)}));
		newData.setAlpha(0, 0, 0);
		NULL_IMAGE = new Image(Display.getDefault(), newData);
		
		
		eventBroker = context.get(IEventBroker.class);
		eventBroker.subscribe(IntelEvents.RECORD_SOURCE_ALL, refreshImages);
		eventBroker.subscribe(SmartPlugIn.E4_DATABASE_CHANGED_EVENT, refreshImages);
	}
	
	@Override
	public void dispose(){
		boldFont.dispose();
		labelProvider.dispose();
		srcProvider.dispose();
		//dispose images
		images.values().forEach(w->w.dispose());
		
		eventBroker.unsubscribe(refreshImages);
	}
	
	private Image combineImage(IntelRecord.Status status, IntelRecordSource source) {
		String key = ""; //$NON-NLS-1$
		IntelRecordSource rsrc = source;
		if (rsrc == null) {
			key += "NONE"; //$NON-NLS-1$
		}else {
			key += rsrc.getKeyId();
		}
		key += "_" + status.name(); //$NON-NLS-1$
		if (images.containsKey(key)) return images.get(key);

		//merge images
		Image img1 = RecordLabelProvider.getRecordStatusImage(status);
		Image img2 = srcProvider.getImage(source);
		if (img1 == null && img2 == null) return null;
		if (img1 == null) {
			images.put(key, img2);
			return img2;
		}
		if (img2 == null) {
			images.put(key, img1);
			return img1;
		}

		int w1 = img1.getBounds().width;
		int width = w1+ img2.getBounds().width;
		int height = Math.max(img1.getBounds().height,  img2.getBounds().height);
		int depth = Math.max(img1.getImageData().depth,  img2.getImageData().depth);
		PaletteData palette = img1.getImageData().palette;
		
		ImageData newData = new ImageData(width, height, depth, palette);
		
		for (int x = 0; x < w1; x ++) {
			for (int y = 0; y < height; y ++) {
				RGB b = img1.getImageData().palette.getRGB(img1.getImageData().getPixel(x, y));
				newData.setPixel(x, y, newData.palette.getPixel(b));
				newData.setAlpha(x, y, img1.getImageData().getAlpha(x, y));
				
				b = img2.getImageData().palette.getRGB(img2.getImageData().getPixel(x, y));
				newData.setPixel(x+w1, y, newData.palette.getPixel(b));
				newData.setAlpha(x+w1, y, img2.getImageData().getAlpha(x, y));
			}
		}
		Image combined = new Image(Display.getDefault(), newData);
		images.put(key, combined);
		return combined;
	}
	
	@Override
	public Image getImage(Object element){
		if (element instanceof IntelRecordProxy) {
			IntelRecordProxy proxy = (IntelRecordProxy)element;
			if (!sourceAndStatusImg) {
				//only display source image
				return srcProvider.getImage(proxy.getRecordSource());
			}
			return combineImage(proxy.getStatus(), proxy.getRecordSource());
		}else if (element instanceof IntelRecordSearchResultItem) {
			IntelRecordSearchResultItem item = (IntelRecordSearchResultItem)element;
			if (!sourceAndStatusImg) {
				//only display source image
				return srcProvider.getImage(item.getRecordSource());
			}
			return combineImage(item.getStatus(), item.getRecordSource());
		}else if (element instanceof IntelRecordSource) {
			return srcProvider.getImage(element);
		}else if (element instanceof IntelRecord.Status) {
			return RecordLabelProvider.getRecordStatusImage((Status) element);
		}else if (element == RecordsViewContentProvider.NONE_SOURCE) {
			if (sourceAndStatusImg) {
				//required for down arrow to display if children
				return NULL_IMAGE;
			}
		}else if (element instanceof Date) {
			if (sourceAndStatusImg) return NULL_IMAGE;
		}
		return super.getImage(element);
	}
	
	@Override
	public String getText(Object element) {
		if (element instanceof IntelRecordProxy) {
			return MessageFormat.format("{0} ({1})",((IntelRecordProxy)element).getTitle(),  DateFormat.getDateInstance().format(((IntelRecordProxy)element).getDate())); //$NON-NLS-1$
		}else if (element instanceof IntelRecordSource) {
			return srcProvider.getText(element);
		}else if (element instanceof IntelRecord.Status) {
			return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(element, Locale.getDefault());
		}else if (element instanceof Date) {
			return  (new SimpleDateFormat("MMMM, yyyy")).format((Date)element); //$NON-NLS-1$
		}else if (element instanceof IntelRecordSearchResultItem) {
			return ((IntelRecordSearchResultItem)element).getTitle();
		}
			
		return super.getText(element);
	}
	
	@Override
	public Color getBackground(Object element) {
		return null;
	}

	@Override
	public Color getForeground(Object element) {
		if (!(element instanceof IntelRecordProxy)){
			return Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
		}
		return null;
	}
	
	@Override
	public Font getFont(Object element) {
		if (!(element instanceof IntelRecordProxy)){
			return boldFont;
		}
		return null;
	}
}
