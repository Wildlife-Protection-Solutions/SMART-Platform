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
package org.wcs.smart.i2.ui;

import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * Class for displaying an animated gif in canvas
 * @author Emily
 *
 */
public class AnimatedGif extends Canvas {
	
	public static final String ICON_PROGRESS = "images/icons/obj16/search_progress.gif";

	private ImageData[] data;
	private Image[] images;
	private int index = 0;
	
	private Thread animate;
	
	public AnimatedGif(Composite parent, InputStream resource) {
		super(parent, SWT.NONE);

		data = (new ImageLoader()).load(resource);
		images = new Image[data.length];
		index = 0;
		
		draw();
		
		animate = new Thread() {			
			@Override
			public void run() {
				while(!AnimatedGif.this.isDisposed()){
					try {
						Thread.sleep(400);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					draw();
				}
			}
		};
		animate.setDaemon(true);
		animate.start();
	}

	/**
	 * 
	 * @return the size of the image
	 */
	public Point getImageSize(){
		return new Point(data[0].width, data[0].height);
	}
	
	@Override
	public void dispose(){
		super.dispose();
		if (images != null){
			for (Image i : images){
				if (i != null) i.dispose();
			}
		}
	}

	private void draw(){
		Display.getDefault().asyncExec(drawRunnable);
	}
	
	private Runnable drawRunnable = new Runnable(){

		@Override
		public void run() {
			if (AnimatedGif.this.isDisposed()) return;
			GC gc = new GC(AnimatedGif.this);
		    
			Image img = images[index];
			if (img == null){
				ImageData idata = data[index];
				img = new Image(getDisplay(), idata);
				images[index] = img;
			}
	        gc.drawImage(img, data[index].x, data[index].y);
	        
			index ++;
			if (index >= data.length) index = 0;
		}
		
	};
}
