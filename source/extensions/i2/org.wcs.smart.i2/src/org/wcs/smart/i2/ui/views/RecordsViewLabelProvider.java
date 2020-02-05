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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.search.IntelRecordSearchResultItem;
import org.wcs.smart.i2.ui.RecordLabelProvider;
import org.wcs.smart.i2.ui.Resources;

/**
 * Label provider for records list view and various trees/tables
 * @author Emily
 *
 */
public class RecordsViewLabelProvider extends ColumnLabelProvider {
	
	private Font boldFont;
	
	private RecordLabelProvider labelProvider;

	//combined images for case where sourceAndStatusImg is true
	private HashMap<String, Image> images = new HashMap<String,Image>();
	private List<Image> todispose = new ArrayList<>();
	private Image NULL_IMAGE;
	
	private IEventBroker eventBroker;
	private EventHandler refreshImages = new EventHandler() {
		@Override
		public void handleEvent(Event event) {
			for (Image i : todispose) i.dispose();
			images.clear();
		}
	};
	
	private boolean sourceImg;
	private boolean statusImg;
	private boolean profileImg;
		
	public RecordsViewLabelProvider(IEclipseContext context){
		this(context, false, false, false);
	}
	
	public RecordsViewLabelProvider(IEclipseContext context, boolean sourceImg, boolean statusImg, boolean profileImg){
		this.sourceImg = sourceImg;
		this.statusImg = statusImg;
		this.profileImg = profileImg;
		
		FontData fd = Display.getDefault().getActiveShell().getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(Display.getCurrent(), fd);
		labelProvider = new RecordLabelProvider();
		
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
		//dispose images
		todispose.forEach(w->w.dispose());
		
		eventBroker.unsubscribe(refreshImages);
		
		NULL_IMAGE.dispose();
		
		images.clear();
		NULL_IMAGE = null;
	}
	
	private Image combineImage(String key, Image img1, Image img2, Image img3){
		if (images.containsKey(key)) return images.get(key);
		
		int width = 0;
		int height = 0;
		int depth = 1;
		PaletteData palette = null;
		
		if (img1 == null) {
			width = 16;
		}else {
			width = img1.getBounds().width;
			height = img1.getBounds().height;
			depth = img1.getImageData().depth;
			palette = img1.getImageData().palette;
		}
		
		if (img2 == null) {
			width += 16;
		}else {
			width += img2.getBounds().width;
			height = Math.max(height,  img2.getBounds().height);
			depth = Math.max(depth,  img2.getImageData().depth);
			if (palette == null) palette = img2.getImageData().palette;
		}
	
		if (img3 != null) {
			width += img3.getBounds().width;
			height = Math.max(height,  img3.getBounds().height);
			depth = Math.max(depth,  img3.getImageData().depth);
			if (palette == null) palette = img3.getImageData().palette;
		}
		
		
		ImageData newData = new ImageData(width, height, depth, palette);
		
		int w1 = img1 == null ? 16 : img1.getBounds().width;
		int w2 = img2 == null ? 16 : img2.getBounds().width;
		
		for (int x = 0; x < w1; x ++) {
			for (int y = 0; y < height; y ++) {
				if (img1 != null) {
					RGB b = img1.getImageData().palette.getRGB(img1.getImageData().getPixel(x, y));
					newData.setPixel(x, y, newData.palette.getPixel(b));
					newData.setAlpha(x, y, img1.getImageData().getAlpha(x, y));
				}else {
					newData.setAlpha(x, y, 0);
				}
				
				if (img2 != null) {
					RGB b = img2.getImageData().palette.getRGB(img2.getImageData().getPixel(x, y));
					newData.setPixel(x+w1, y, newData.palette.getPixel(b));
					newData.setAlpha(x+w1, y, img2.getImageData().getAlpha(x, y));
				}else {
					newData.setAlpha(x+w1, y, 0);
				}
				
				if (img3 != null) {
					RGB b = img3.getImageData().palette.getRGB(img3.getImageData().getPixel(x, y));
					newData.setPixel(x+w1+w2, y, newData.palette.getPixel(b));
					newData.setAlpha(x+w1+w2, y, img3.getImageData().getAlpha(x, y));
				}
			}
		}
		Image combined = new Image(Display.getDefault(), newData);
		images.put(key, combined);
		return combined;
		
	}
	
	
	private Image getImage(IntelRecordSource source, IntelProfile profile, IntelRecord.Status status) {
		if (!sourceImg && !profileImg && !statusImg) return null;
		
		if (sourceImg && !profileImg && !statusImg) return Resources.INSTANCE.getImage(source);
		if (!sourceImg && !profileImg && statusImg)  return Resources.INSTANCE.getImage(status);
		if (!sourceImg && profileImg && !statusImg)  return Resources.INSTANCE.getImage(profile);
		
		String srckey = Messages.RecordsViewLabelProvider_NoneLabel;
		
		if (source != null && sourceImg) {
			srckey = source.getKeyId();
		}
		
		if (sourceImg && profileImg && !statusImg) return combineImage(srckey + "_" + profile.getKeyId(), Resources.INSTANCE.getImage(source), Resources.INSTANCE.getImage(profile), null); //$NON-NLS-1$
		if (!sourceImg && profileImg && statusImg) return combineImage(profile.getKeyId() + "_" + status.name(), Resources.INSTANCE.getImage(status), Resources.INSTANCE.getImage(profile), null); //$NON-NLS-1$
		if (sourceImg && !profileImg && statusImg) return combineImage(srckey + "_" + status.name(), Resources.INSTANCE.getImage(status), Resources.INSTANCE.getImage(source), null); //$NON-NLS-1$
		
		if (sourceImg && profileImg && statusImg) return combineImage(srckey + "_" + profile.getKeyId() + "_" + status.name(), Resources.INSTANCE.getImage(status), Resources.INSTANCE.getImage(source), Resources.INSTANCE.getImage(profile)); //$NON-NLS-1$ //$NON-NLS-2$
		
		return null;
	}
	
	
	@Override
	public Image getImage(Object element){
		if (element instanceof IntelRecordProxy) {
			IntelRecordProxy proxy = (IntelRecordProxy)element;
			return getImage(proxy.getRecordSource(), proxy.getProfile(), proxy.getStatus());
		}else if (element instanceof IntelRecordSearchResultItem) {
			IntelRecordSearchResultItem item = (IntelRecordSearchResultItem)element;
			return getImage(item.getRecordSource(), item.getProfile(), item.getStatus());
		}else if (element instanceof IntelRecordSource) {
			return Resources.INSTANCE.getImage((IntelRecordSource)element);
		}else if (element instanceof IntelRecord.Status) {
			return Resources.INSTANCE.getImage((Status) element);
		}else if (element == RecordsViewContentProvider.NONE_SOURCE) {
			if (sourceImg && profileImg && statusImg) {
				//required for down arrow to display if children
				return NULL_IMAGE;
			}
		}else if (element instanceof Date) {
			if (sourceImg && profileImg && statusImg) return NULL_IMAGE;
		}
		return super.getImage(element);
	}
	
	@Override
	public String getText(Object element) {
		if (element instanceof IntelRecordProxy) {
			return MessageFormat.format("{0} ({1})",((IntelRecordProxy)element).getTitle(),  DateFormat.getDateInstance().format(((IntelRecordProxy)element).getDate())); //$NON-NLS-1$
		}else if (element instanceof IntelRecordSource) {
			return ((IntelRecordSource)element).getName();
		}else if (element instanceof IntelRecord.Status) {
			return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(element, Locale.getDefault());
		}else if (element instanceof Date) {
			return  (new SimpleDateFormat("MMMM, yyyy")).format((Date)element); //$NON-NLS-1$
		}else if (element instanceof IntelRecordSearchResultItem) {
			return ((IntelRecordSearchResultItem)element).getTitle();
		}else if (element instanceof IntelProfile) {
			return ((IntelProfile)element).getName();
		}
			
		return super.getText(element);
	}
	
	@Override
	public Color getBackground(Object element) {
		return null;
	}

	@Override
	public Color getForeground(Object element) {
//		if (!(element instanceof IntelRecordProxy)){
//			return Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
//		}
		return null;
	}
	
	@Override
	public Font getFont(Object element) {
//		if (!(element instanceof IntelRecordProxy)){
//			return boldFont;
//		}
		return null;
	}
}
