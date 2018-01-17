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
package org.wcs.smart.asset.ui.views.map;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.ToolTip;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;

/**
 * Status composite for displaying the status table in asset overview maps.
 * 
 * 
 * @author Emily
 *
 */
public class StatusCanvas extends Composite{

	private LocalDate cStart, cEnd;
	private long numDays = 0;

	final Point origin = new Point (0, 0);
	
	private Image mainImage;
	private Image topImage;
	private Image leftImage;
	
	private Canvas cMain;
	private Canvas cTop;
	private Canvas cLeft;
	
	private int cellWidth;
	private int cellHeight;
	
	private List<Object> headerData;
	
	public StatusCanvas(Composite parent) {
		super(parent, SWT.NONE);
		
		cMain = new Canvas(this, SWT.V_SCROLL | SWT.H_SCROLL );
		cTop = new Canvas(this, SWT.NONE);
		cLeft = new Canvas(this, SWT.NONE);
		
		cMain.addListener(SWT.Paint, e->{
			e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
			e.gc.fillRectangle(0, 0, cMain.getBounds().width, cMain.getBounds().height);
			e.gc.drawImage (mainImage, origin.x, origin.y);
		});
		
		cTop.addListener(SWT.Paint, e->{
			e.gc.drawImage (topImage, origin.x, 0);
		});
		
		cLeft.addListener(SWT.Paint, e->{
			e.gc.drawImage (leftImage, 0, origin.y);
		});
		
		final ToolTip dateTooltip = new ToolTip(getShell(), SWT.NONE);
		dateTooltip.setAutoHide(true);
		
		final ToolTip statusTooltip = new ToolTip(getShell(), SWT.NONE);
		statusTooltip.setAutoHide(true);
		
		cTop.addListener(SWT.MouseHover, e->{
			int date =  (int)Math.floor( ( e.x - origin.x ) / ((double)cellWidth) );
			LocalDate currentDate = cStart.plus(date,ChronoUnit.DAYS);
			dateTooltip.setVisible(false);
			dateTooltip.setMessage(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(currentDate));
			dateTooltip.setLocation(cTop.toDisplay(e.x,e.y + 20));
			dateTooltip.setVisible(true);
		});
		cTop.addListener(SWT.MouseExit, e->dateTooltip.setVisible(false));
		
		cMain.addListener(SWT.MouseHover, e->{
			int date =  (int)Math.floor( ( e.x - origin.x ) / ((double)cellWidth) );
			LocalDate currentDate = cStart.plus(date,ChronoUnit.DAYS);
			
			int id =  (int)Math.floor( ( e.y - origin.y ) / ((double)cellHeight) );
			Object x = headerData.get(id);
			statusTooltip.setVisible(false);
			statusTooltip.setMessage(getId(x) + "\n" + DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(currentDate));
			statusTooltip.setLocation(cMain.toDisplay(e.x,e.y + 20));
			statusTooltip.setVisible(true);
			
		});
		cMain.addListener(SWT.MouseExit, e->statusTooltip.setVisible(false));
		
		addListener(SWT.Dispose, e->{
			if (mainImage != null) { mainImage.dispose(); mainImage = null; }
			if (topImage != null) { topImage.dispose(); topImage = null; }
			if (leftImage != null) { leftImage.dispose(); leftImage = null; }
			dateTooltip.dispose();
			statusTooltip.dispose();
		});
		
		final ScrollBar hBar = cMain.getHorizontalBar ();
		hBar.addListener (SWT.Selection, e -> {
			int hSelection = hBar.getSelection ();
			int destX = -hSelection - origin.x;
			Rectangle rect = mainImage.getBounds ();
			
			cMain.scroll (destX, 0, 0, 0, rect.width, rect.height, false);
			cTop.scroll (destX, 0, 0, 0, rect.width, rect.height, false);
			origin.x = -hSelection;
		});
		final ScrollBar vBar = cMain.getVerticalBar ();
		vBar.addListener (SWT.Selection, e -> {
			int vSelection = vBar.getSelection ();
			int destY = -vSelection - origin.y;
			Rectangle rect = mainImage.getBounds ();
			cMain.scroll (0, destY, 0, 0, rect.width, rect.height, false);
			cLeft.scroll (0, destY, 0, 0, rect.width, rect.height, false);
			origin.y = -vSelection;
		});
		
		addListener (SWT.Resize,  e -> {
			resizeItems();
		});
	}
	
	public void setData(Set<Entry<Object,Set<Long>>> data, LocalDate startDate, LocalDate endDate) {
		cStart = LocalDate.from(startDate);
		cEnd = LocalDate.from(endDate);
		numDays = ChronoUnit.DAYS.between(cStart, cEnd)+1;
		
		
		if (mainImage != null) mainImage.dispose();
		if (leftImage != null) leftImage.dispose();
		if (topImage != null) topImage.dispose();
		
		mainImage = new Image(Display.getDefault(), 1, 1);
		GC gc = new GC(mainImage);
		int idColWidth = 0;
		int charheight = 0;
		for (Entry<Object, Set<Long>> d : data) {
			String id = getId(d.getKey());
			Point charsize = gc.stringExtent(id);
			if (charsize.x > idColWidth) {
				idColWidth = charsize.x;
			}
			if (charsize.y > charheight) {
				charheight = charsize.y;
			}
		}
		gc.dispose();
		mainImage.dispose();
		
		int idcolwidth = idColWidth + 5;
		int cellheight = charheight + 5 + 2;
		int cellwidth = cellheight + 2;
		this.cellWidth = cellwidth;
		this.cellHeight = cellheight;
		
		topImage = new Image(Display.getDefault(), (int)(cellwidth * numDays), (int)(cellheight));
		leftImage = new Image(Display.getDefault(), (int)(idcolwidth), (int)(cellheight * data.size()));
		mainImage = new Image(Display.getDefault(), (int)(cellwidth * numDays), (int)(cellheight * data.size()));
		
		//top
		gc = new GC(topImage);
		int y = 0;
		int x = 0;
		LocalDate tmp = LocalDate.from(cStart);
		while(!tmp.isAfter(cEnd)) {		
			String text = String.valueOf(tmp.getDayOfMonth());
			Point swidth = gc.stringExtent(text);
			int xoff = (int)Math.floor( (cellwidth - 2 - swidth.x) / 2.0 );
			gc.drawText(text, x + xoff, y);			
			x+= cellwidth;
			tmp = tmp.plus(1, ChronoUnit.DAYS);
		}
		gc.dispose();
		
		//header
		headerData = new ArrayList<>();
		gc = new GC(leftImage);
		y = 0;
		x = 0;
		for (Entry<?,Set<Long>> ee : data) {
			gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
			gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
			
			String text = getId(ee.getKey());
			Point swidth = gc.stringExtent(text);
			int yoff = (int)Math.floor( (cellheight - 2 - swidth.y) / 2.0 );
			gc.drawText(text, x, y+yoff);
			y+= cellheight;
			headerData.add(ee.getKey());
		};
		gc.dispose();
		
		//main
		x = 0;
		y = 0;
		gc = new GC(mainImage);
		for (Entry<?,Set<Long>> ee : data) {
			tmp = LocalDate.from(cStart);
			x = 0;
			while(!tmp.isAfter(cEnd)) {
				if (ee.getValue().contains(tmp.toEpochDay())) {
					gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
				}else {
					gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
				}
				gc.fillRectangle(new Rectangle(x+1,y+1,cellwidth-2,cellheight-2));
				x+= cellwidth;
				tmp = tmp.plus(1, ChronoUnit.DAYS);
			}	
			y+= cellheight;
		}
		gc.dispose();
		resizeItems();
		layout(true);
		
		cMain.redraw();
		cLeft.redraw();
		cTop.redraw();
	}
	
	private void resizeItems() {
		if (cLeft == null || cTop == null || cMain == null) return;
		if (leftImage == null || mainImage == null || topImage == null) return;
		
		int mainWidth = getClientArea().width - leftImage.getBounds().width;
		int mainHeigth = getClientArea().height - topImage.getBounds().height;
		
		cMain.setBounds(leftImage.getBounds().width, topImage.getBounds().height, mainWidth, mainHeigth);
		
		cLeft.setBounds(0, topImage.getBounds().height, mainWidth, mainHeigth - cMain.getHorizontalBar().getThumbBounds().height);
		cTop.setBounds(leftImage.getBounds().width, 0, mainWidth - cMain.getVerticalBar().getThumbBounds().width, topImage.getBounds().height);
		
		final ScrollBar vBar = cMain.getVerticalBar ();
		final ScrollBar hBar = cMain.getHorizontalBar ();
		
		Rectangle rect = mainImage.getBounds ();
		Rectangle client = cMain.getClientArea ();
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
	}
	

	public String getId(Object o) {
		if (o instanceof AssetStation) return ((AssetStation) o).getId();
		if (o instanceof AssetStationLocation) return ((AssetStationLocation) o).getId();
		return "";
	}
	
}
