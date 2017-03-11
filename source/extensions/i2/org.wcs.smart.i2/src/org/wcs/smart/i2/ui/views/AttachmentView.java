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

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttachment;

/**
 * Simple view for displaying an image attachment
 * http://www.eclipse.org/articles/Article-Image-Viewer/Image_viewer.html
 * 
 * @author Emily
 *
 */
public class AttachmentView {

	public static final String ID = "org.wcs.smart.i2.ui.views.AttachmentView"; //$NON-NLS-1$

	private Canvas draw;
	private Image rawImage;
	private Image screenImage;
	private AffineTransform transform = null;
	
	@Inject
	private IEclipseContext context;

	@PostConstruct
	public void createPartControl(Composite parent) {
		IntelAttachment attachment = context.get(IntelAttachment.class);
		if (attachment == null) {
			return;
		}
		parent = new Composite(parent, SWT.NONE);
		
		ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		toolbar.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		ToolItem btnZoomIn = new ToolItem(toolbar, SWT.PUSH);
		btnZoomIn.addListener(SWT.Selection, e->zoomIn());
		btnZoomIn.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_IMG_ZOOMIN));
		btnZoomIn.setToolTipText(Messages.AttachmentView_zoomintooltip);
		
		ToolItem btnZoomOut = new ToolItem(toolbar, SWT.PUSH);
		btnZoomOut.addListener(SWT.Selection, e->zoomOut());
		btnZoomOut.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_IMG_ZOOMOUT));
		btnZoomOut.setToolTipText(Messages.AttachmentView_zoomouttooltip);
		
		ToolItem btnRefresh = new ToolItem(toolbar, SWT.PUSH);
		btnRefresh.addListener(SWT.Selection, e->{
			transform = null;
			draw.redraw();
		});
		btnRefresh.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_REFRESH));
		btnRefresh.setToolTipText(Messages.AttachmentView_resettooltip);
		
		Point tbsize = toolbar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		toolbar.setBounds(5, 5, tbsize.x, tbsize.y);
		
		draw = new Canvas(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.NO_BACKGROUND);
		draw.addListener(SWT.Resize, e -> syncScrollBars());

		Composite thisParent = parent;
		parent.addListener(SWT.Resize, e-> draw.setBounds(thisParent.getClientArea()));
		
		rawImage = context.get(Image.class);
		
		draw.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		draw.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				Rectangle clientRect = draw.getClientArea();
				if (rawImage != null) {
					if (transform == null){
						//initial zoom
						transform = new AffineTransform();
						Rectangle rect = draw.getClientArea();
						int w = rect.width, h = rect.height;
						/* zooming center */
						double dx = ((double) w) / 2;
						double dy = ((double) h) / 2;
						
						double xscale = (draw.getClientArea().width * 1.0) / rawImage.getBounds().width;
						double yscale = (draw.getClientArea().height * 1.0) / rawImage.getBounds().height;
						
						centerZoom(dx, dy, Math.min(xscale, yscale), transform);
						return;
					}
					Rectangle imageRect = inverseTransformRect(transform, clientRect);

					int gap = 2;
					imageRect.x -= gap;
					imageRect.y -= gap;
					imageRect.width += 2 * gap;
					imageRect.height += 2 * gap;

					Rectangle imageBound = rawImage.getBounds();
					imageRect = imageRect.intersection(imageBound);
					Rectangle destRect = transformRect(transform, imageRect);

					if (screenImage != null) {
						screenImage.dispose();
					}
					screenImage = new Image(draw.getDisplay(), clientRect.width, clientRect.height);
					GC newGC = new GC(screenImage);
					newGC.setClipping(clientRect);
					newGC.drawImage(rawImage, imageRect.x, imageRect.y,
							imageRect.width, imageRect.height, destRect.x,
							destRect.y, destRect.width, destRect.height);
					newGC.dispose();

					e.gc.drawImage(screenImage, 0, 0);
				} else {
					e.gc.setClipping(clientRect);
					e.gc.fillRectangle(clientRect);
					initScrollBars();
				}
			}
		});
		
		
		Listener mouseMove = new Listener(){

			private Point mouseDown;
			private int downButton;
			
			@Override
			public void handleEvent(Event event) {
				if (rawImage == null) return;
				
				if (event.type == SWT.MouseDown){
					downButton = event.button;
				}else if (event.type == SWT.MouseUp){
					downButton = -1;
				}
				
				if (downButton == 1){//left
					if (!draw.getHorizontalBar().isEnabled() && !draw.getVerticalBar().isEnabled()) return;
					if (event.type == SWT.MouseDown){
						mouseDown = new Point(event.x, event.y);
					}else if (event.type == SWT.MouseUp || event.type == SWT.MouseMove){
						if (mouseDown == null) return;
						Point down = mouseDown;
						
						Point up = new Point(event.x, event.y);
						if (event.type == SWT.MouseUp){
							mouseDown = null;
						}else{
							mouseDown = up;
						}
						int panHor = up.x - down.x;
						int panVer = up.y - down.y;
						
						AffineTransform af = transform;
						af.preConcatenate(AffineTransform.getTranslateInstance(panHor, panVer));
						transform = af;
						syncScrollBars();
					}
				}
			}
			
		};
		draw.addListener(SWT.MouseDown, mouseMove);
		draw.addListener(SWT.MouseUp, mouseMove);
		draw.addListener(SWT.MouseMove, mouseMove);
		
		draw.addMouseWheelListener(new MouseWheelListener() {
			
			@Override
			public void mouseScrolled(MouseEvent e) {
				int count = e.count;
				if (count > 0){
					zoomIn();
				}else{
					zoomOut();
				}
			}
		});
		draw.addListener(SWT.Dispose, d -> {
			if (rawImage != null){
				rawImage.dispose();
			}
			if (screenImage != null)
				screenImage.dispose();
		});

		initScrollBars();
	}

	

	private void syncScrollBars() {
		if (rawImage == null) {
			draw.redraw();
			return;
		}
		if (transform == null) return;
		
		AffineTransform af = transform;
		double sx = af.getScaleX(), sy = af.getScaleY();
		double tx = af.getTranslateX(), ty = af.getTranslateY();
		if (tx > 0) tx = 0;
		if (ty > 0) ty = 0;

		ScrollBar horizontal = draw.getHorizontalBar();
		horizontal.setIncrement((int) (draw.getClientArea().width / 100));
		horizontal.setPageIncrement(draw.getClientArea().width);
		Rectangle imageBound = rawImage.getBounds();
		int cw = draw.getClientArea().width;
		int ch = draw.getClientArea().height;
		if (imageBound.width * sx > cw) { /* image is wider than client area */
			horizontal.setMaximum((int) (imageBound.width * sx));
			horizontal.setEnabled(true);
			if (((int) - tx) > horizontal.getMaximum() - cw)
				tx = -horizontal.getMaximum() + cw;
		} else { /* image is narrower than client area */
			horizontal.setEnabled(false);
			tx = (cw - imageBound.width * sx) / 2; //center if too small.
		}
		horizontal.setSelection((int) (-tx));
		horizontal.setThumb((int) (draw.getClientArea().width));

		ScrollBar vertical = draw.getVerticalBar();
		vertical.setIncrement((int) (draw.getClientArea().height / 100));
		vertical.setPageIncrement((int) (draw.getClientArea().height));
		if (imageBound.height * sy > ch) { /* image is higher than client area */
			vertical.setMaximum((int) (imageBound.height * sy));
			vertical.setEnabled(true);
			if (((int) - ty) > vertical.getMaximum() - ch)
				ty = -vertical.getMaximum() + ch;
		} else { /* image is less higher than client area */
			vertical.setEnabled(false);
			ty = (ch - imageBound.height * sy) / 2; //center if too small.
		}
		vertical.setSelection((int) (-ty));
		vertical.setThumb((int) (draw.getClientArea().height));

		/* update transform. */
		af = AffineTransform.getScaleInstance(sx, sy);
		af.preConcatenate(AffineTransform.getTranslateInstance(tx, ty));
		transform = af;
		draw.redraw();
	}

	private void initScrollBars() {
		ScrollBar horizontal = draw.getHorizontalBar();
		horizontal.setEnabled(false);
		horizontal.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				scrollHorizontally((ScrollBar) event.widget);
			}
		});
		ScrollBar vertical = draw.getVerticalBar();
		vertical.setEnabled(false);
		vertical.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				scrollVertically((ScrollBar) event.widget);
			}
		});
	}
	
	/* Scroll horizontally */
	private void scrollHorizontally(ScrollBar scrollBar) {
		if (rawImage == null)
			return;

		AffineTransform af = transform;
		double tx = af.getTranslateX();
		double select = -scrollBar.getSelection();
		af.preConcatenate(AffineTransform.getTranslateInstance(select - tx, 0));
		transform = af;
		syncScrollBars();
	}

	/* Scroll vertically */
	private void scrollVertically(ScrollBar scrollBar) {
		if (rawImage == null)
			return;

		AffineTransform af = transform;
		double ty = af.getTranslateY();
		double select = -scrollBar.getSelection();
		af.preConcatenate(AffineTransform.getTranslateInstance(0, select - ty));
		transform = af;
		syncScrollBars();
	}

	private void centerZoom(
			double dx,
			double dy,
			double scale,
			AffineTransform af) {
			af.preConcatenate(AffineTransform.getTranslateInstance(-dx, -dy));
			af.preConcatenate(AffineTransform.getScaleInstance(scale, scale));
			af.preConcatenate(AffineTransform.getTranslateInstance(dx, dy));
			transform = af;
			syncScrollBars();
		}

	private void zoomIn() {
		if (rawImage == null)
			return;
		Rectangle rect = draw.getClientArea();
		int w = rect.width, h = rect.height;
		/* zooming center */
		double dx = ((double) w) / 2;
		double dy = ((double) h) / 2;
		centerZoom(dx, dy, 1.1f, transform);
	}

	private void zoomOut() {
		if (rawImage == null)
			return;
		Rectangle rect = draw.getClientArea();
		int w = rect.width, h = rect.height;
		/* zooming center */
		double dx = ((double) w) / 2;
		double dy = ((double) h) / 2;
		centerZoom(dx, dy, 0.9f,transform);
	}
	
	@Focus
	public void setFocus() {
		if (draw != null)
			draw.setFocus();
	}

	@PreDestroy
	public void dispose() {
	}

	/**
	 * Given an arbitrary rectangle, get the rectangle with the given transform.
	 * The result rectangle is positive width and positive height.
	 * 
	 * @param af
	 *            AffineTransform
	 * @param src
	 *            source rectangle
	 * @return rectangle after transform with positive width and height
	 */
	private static Rectangle transformRect(AffineTransform af, Rectangle src) {
		Rectangle dest = new Rectangle(0, 0, 0, 0);
		src = absRect(src);
		Point p1 = new Point(src.x, src.y);
		p1 = transformPoint(af, p1);
		dest.x = p1.x;
		dest.y = p1.y;
		dest.width = (int) (src.width * af.getScaleX());
		dest.height = (int) (src.height * af.getScaleY());
		return dest;
	}

	/**
	 * Given an arbitrary rectangle, get the rectangle with the inverse given
	 * transform. The result rectangle is positive width and positive height.
	 * 
	 * @param af
	 *            AffineTransform
	 * @param src
	 *            source rectangle
	 * @return rectangle after transform with positive width and height
	 */
	private static Rectangle inverseTransformRect(AffineTransform af,
			Rectangle src) {
		Rectangle dest = new Rectangle(0, 0, 0, 0);
		src = absRect(src);
		Point p1 = new Point(src.x, src.y);
		p1 = inverseTransformPoint(af, p1);
		dest.x = p1.x;
		dest.y = p1.y;
		dest.width = (int) (src.width / af.getScaleX());
		dest.height = (int) (src.height / af.getScaleY());
		return dest;
	}

	/**
	 * Given an arbitrary point, get the point with the given transform.
	 * 
	 * @param af
	 *            affine transform
	 * @param pt
	 *            point to be transformed
	 * @return point after tranform
	 */
	private static Point transformPoint(AffineTransform af, Point pt) {
		Point2D src = new Point2D.Float(pt.x, pt.y);
		Point2D dest = af.transform(src, null);
		Point point = new Point((int) Math.floor(dest.getX()),
				(int) Math.floor(dest.getY()));
		return point;
	}

	/**
	 * Given an arbitrary point, get the point with the inverse given transform.
	 * 
	 * @param af
	 *            AffineTransform
	 * @param pt
	 *            source point
	 * @return point after transform
	 */
	private static Point inverseTransformPoint(AffineTransform af, Point pt) {
		Point2D src = new Point2D.Float(pt.x, pt.y);
		try {
			Point2D dest = af.inverseTransform(src, null);
			return new Point((int) Math.floor(dest.getX()),
					(int) Math.floor(dest.getY()));
		} catch (Exception e) {
			e.printStackTrace();
			return new Point(0, 0);
		}
	}

	/**
	 * Given arbitrary rectangle, return a rectangle with upper-left start and
	 * positive width and height.
	 * 
	 * @param src
	 *            source rectangle
	 * @return result rectangle with positive width and height
	 */
	public static Rectangle absRect(Rectangle src) {
		Rectangle dest = new Rectangle(0, 0, 0, 0);
		if (src.width < 0) {
			dest.x = src.x + src.width + 1;
			dest.width = -src.width;
		} else {
			dest.x = src.x;
			dest.width = src.width;
		}
		if (src.height < 0) {
			dest.y = src.y + src.height + 1;
			dest.height = -src.height;
		} else {
			dest.y = src.y;
			dest.height = src.height;
		}
		return dest;
	}

	public static class AttachmentViewWrapper extends
			DIViewPart<AttachmentView> {
		public AttachmentViewWrapper() {
			super(AttachmentView.class);
		}
	}
}
