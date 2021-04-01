package org.wcs.smart.i2.diagram;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.ScaledGraphics;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/*
 * used GraphLabel as example
 */
public class EntityFigure extends Label {
	
	private Color borderColor;
	private int borderWidth;
	private int arcWidth;

	private boolean painting = false;
	
	public EntityFigure(String text, Image i, boolean cacheLabel) {
		super(text, i);
		initLabel();
	}
	
	protected void initLabel() {
		super.setFont(Display.getDefault().getSystemFont());
		this.borderColor = ColorConstants.black;
		this.borderWidth = 0;
		this.arcWidth = 5;
		this.setLayoutManager(new StackLayout());
		this.setBorder(new MarginBorder(1));
	}
	
	public void setFont(Font f) {
		super.setFont(f);
		adjustBoundsToFit();
	}
	/**
	 * Adjust the bounds to make the text fit without truncation.
	 */
	protected void adjustBoundsToFit() {
		String text = getText();
		int safeBorderWidth = borderWidth > 0 ? borderWidth : 1;
		if ((text != null)) {
			Font font = getFont();
			if (font != null) {
				Dimension minSize = FigureUtilities.getTextExtents(text, font);
				if (getIcon() != null) {
					org.eclipse.swt.graphics.Rectangle imageRect = getIcon().getBounds();
					int expandHeight = Math.max(imageRect.height - minSize.height, 0);
					minSize.expand(imageRect.width + 4, expandHeight);
				}
				minSize.expand(10 + (2 * safeBorderWidth), 4 + (2 * safeBorderWidth));
				setBounds(new Rectangle(getLocation(), minSize));
			}
		}
	}
	@Override
	public void paint(Graphics graphics) {
		graphics.setBackgroundColor(getBackgroundColor());

		int safeBorderWidth = borderWidth > 0 ? borderWidth : 1;
		graphics.pushState();
		double scale = 1;

		if (graphics instanceof ScaledGraphics) {
			scale = ((ScaledGraphics) graphics).getAbsoluteScale();
		}
		// Top part inside the border (as fillGradient does not allow to fill a rectangle with round corners).
		Rectangle rect = getBounds().getCopy();
		graphics.setForegroundColor(getBackgroundColor());
		graphics.setBackgroundColor(getBackgroundColor());
		graphics.fillRoundRectangle(rect, arcWidth * safeBorderWidth, arcWidth * 2 * safeBorderWidth);
		
		// Paint the border
		if (borderWidth > 0) {
			rect = getBounds().getCopy();
			rect.x += safeBorderWidth / 2;
			rect.y += safeBorderWidth / 2;
			rect.width -= safeBorderWidth;
			rect.height -= safeBorderWidth;
			graphics.setForegroundColor(borderColor);
			graphics.setBackgroundColor(borderColor);
			graphics.setLineWidth((int) (safeBorderWidth * scale));
			graphics.drawRoundRectangle(rect, arcWidth, arcWidth);
		}

		graphics.popState();
		
		if (getLocalBackgroundColor() != null)
			graphics.setBackgroundColor(getLocalBackgroundColor());
		if (getLocalForegroundColor() != null)
			graphics.setForegroundColor(getLocalForegroundColor());
		if (getLocalFont() != null)
			graphics.setFont(getLocalFont());

		graphics.pushState();
		try {
			paintFigure(graphics);
			graphics.restoreState();
			paintClientArea(graphics);
			paintBorder(graphics);
		} finally {
			graphics.popState();
		}
	}

	protected Color getBackgroundTextColor() {
		return getBackgroundColor();
	}

	/**
	 * This method is overridden to ensure that it doesn't get called while the
	 * super.paintFigure() is being called. Otherwise NullPointerExceptions can
	 * occur because the icon or text locations are cleared *after* they were
	 * calculated.
	 * 
	 * @see org.eclipse.draw2d.Label#invalidate()
	 */
	public void invalidate() {
		if (!painting) {
			super.invalidate();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.draw2d.Label#setText(java.lang.String)
	 */
	public void setText(String s) {
		if (!s.equals("")) { //$NON-NLS-1$
			super.setText(s);

		} else {
			super.setText(""); //$NON-NLS-1$
		}
		adjustBoundsToFit();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.draw2d.Label#setIcon(org.eclipse.swt.graphics.Image)
	 */
	public void setIcon(Image image) {
		super.setIcon(image);
		//adjustBoundsToFit();
	}

	public Color getBorderColor() {
		return borderColor;
	}

	public void setBorderColor(Color c) {
		this.borderColor = c;
	}

	public int getBorderWidth() {
		return borderWidth;
	}

	public void setBorderWidth(int width) {
		this.borderWidth = width;
	}

	public int getArcWidth() {
		return arcWidth;
	}

	public void setArcWidth(int arcWidth) {
		this.arcWidth = arcWidth;
	}

	public void setBounds(Rectangle rect) {
		super.setBounds(rect);
	}
}
