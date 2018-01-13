package org.wcs.smart.asset.ui.views.map;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;

public class StatusCanvas extends Canvas{

	public StatusCanvas(Composite parent) {
		super(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
//		setBounds(0, 0, 100, 100);	
//		setSize(100, 100);
		
		addListener(SWT.Paint, e->paint(e));
		addListener(SWT.Dispose, e->{
			if (image != null) {
				image.dispose();
				image = null;
			}
		});
		
		final ScrollBar hBar = getHorizontalBar ();
		hBar.addListener (SWT.Selection, e -> {
			int hSelection = hBar.getSelection ();
			int destX = -hSelection - origin.x;
			Rectangle rect = image.getBounds ();
			scroll (destX, 0, 0, 0, rect.width, rect.height, false);
			origin.x = -hSelection;
		});
		final ScrollBar vBar = getVerticalBar ();
		vBar.addListener (SWT.Selection, e -> {
			int vSelection = vBar.getSelection ();
			int destY = -vSelection - origin.y;
			Rectangle rect = image.getBounds ();
			scroll (0, destY, 0, 0, rect.width, rect.height, false);
			origin.y = -vSelection;
		});
		
		addListener (SWT.Resize,  e -> {
			Rectangle rect = image.getBounds ();
			Rectangle client = getClientArea ();
			hBar.setMaximum (rect.width);
			vBar.setMaximum (rect.height);
			hBar.setThumb (Math.min (rect.width, client.width));
			vBar.setThumb (Math.min (rect.height, client.height));
			int hPage = rect.width - client.width;
			int vPage = rect.height - client.height;
			int hSelection = hBar.getSelection ();
			int vSelection = vBar.getSelection ();
			if (hSelection >= hPage) {
				if (hPage <= 0) hSelection = 0;
				origin.x = -hSelection;
			}
			if (vSelection >= vPage) {
				if (vPage <= 0) vSelection = 0;
				origin.y = -vSelection;
			}
			redraw ();
		});
	}
	
	private Collection<Entry<Object,Set<Long>>> data;
	private int maxLength = 0;
	
	private LocalDate cStart, cEnd;
	private long numDays = 0;
	
	private Image image;
	
	public void setData(Set<Entry<Object,Set<Long>>> data, LocalDate startDate, LocalDate endDate) {
		this.data = data;
		maxLength = computeMaxIdLength();
		
		cStart = LocalDate.from(startDate);
		cEnd = LocalDate.from(endDate);
		numDays = ChronoUnit.DAYS.between(cStart, cEnd);
	
		if (image != null) image.dispose();
		
		image = new Image(Display.getDefault(), 1, 1);
		GC gc = new GC(image);
		Point charsize =  gc.stringExtent("W");
		gc.dispose();
		image.dispose();
		
		int idcolwidth = charsize.x * maxLength;
		
		int cellheight = charsize.y + 5;
		int cellwidth = cellheight;
		
		int dayswidth = (int)(numDays * cellwidth);
		int width = idcolwidth + dayswidth;
		int height = (data.size() + 1) * cellheight;
		
		image = new Image(Display.getDefault(), width, height);
		gc = new GC(image);

		int y = 0;
		
		//draw dates
		int x = idcolwidth;
		LocalDate tmp = LocalDate.from(cStart);
		while(!tmp.isAfter(cEnd)) {
			
			gc.drawText(String.valueOf(tmp.getDayOfMonth()), x, y);
			
			x+= cellwidth;
			tmp = tmp.plus(1, ChronoUnit.DAYS);
		}
		
		y+= cellheight;
		
		for (Entry<?,Set<Long>> ee : data) {
			x = 0;
			gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
			gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
			gc.drawText(getId(ee.getKey()), x, y);
			x += idcolwidth;
			tmp = LocalDate.from(cStart);
			while(!tmp.isAfter(cEnd)) {
				if (ee.getValue().contains(tmp.toEpochDay())) {
//					e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
					gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
				}else {
//					e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
					gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
				}
				gc.fillRectangle(new Rectangle(x,y,cellwidth,charsize.y));
//				e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
				gc.drawLine(x, y, x, y + charsize.y);
				x+= cellwidth;
				tmp = tmp.plus(1, ChronoUnit.DAYS);
			}	
			y+= cellheight;
		}
		gc.dispose();
		
	}
	
	final Point origin = new Point (0, 0);
	public void paint(Event e) {
//		e.gc.draw
		System.out.println("paint");
		GC gc = e.gc;
		gc.drawImage (image, origin.x, origin.y);
		Rectangle rect = image.getBounds ();
		Rectangle client = getClientArea ();
		int marginWidth = client.width - rect.width;
		if (marginWidth > 0) {
			gc.fillRectangle (rect.width, 0, marginWidth, client.height);
		}
		int marginHeight = client.height - rect.height;
		if (marginHeight > 0) {
			gc.fillRectangle (0, rect.height, client.width, marginHeight);
		}
		
	}

	public String getId(Object o) {
		if (o instanceof AssetStation) return ((AssetStation) o).getId();
		if (o instanceof AssetStationLocation) return ((AssetStationLocation) o).getId();
		return "";
	}
	
	private int computeMaxIdLength() {
		int size = 0;
		for (Entry<?,Set<Long>> d : data) {
			Object key = d.getKey();
			int s = getId(key).length();
			if (s > size) size = s;
		}
		return size;
	}
}
