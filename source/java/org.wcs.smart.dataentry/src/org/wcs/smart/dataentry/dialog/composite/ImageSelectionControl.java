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
package org.wcs.smart.dataentry.dialog.composite;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.wcs.smart.dataentry.dialog.AssociatedImageInterceptor;
import org.wcs.smart.dataentry.model.IImageAssociatedObject;

/**
 * Control that allows to select a single image for provided object.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ImageSelectionControl extends Composite {
	
	private static final int HEIGHT = 90;
	private static final int WIDTH  = 150;

	private IImageContentProvider contentProvider;
	private Canvas canvas;
	
	public ImageSelectionControl(Composite parent, IImageContentProvider contentProvider) {
		super(parent, SWT.NONE);
		this.contentProvider = contentProvider;
		initControls();
	}

	private void initControls() {
		GridLayout gd = new GridLayout(2, false);
		gd.marginBottom=0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		this.setLayout(gd);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		int h = HEIGHT;
		int w = WIDTH;
		canvas = new Canvas(this, SWT.NONE);
		GridData canvasLayoutData = new GridData(w, h);
//		canvasLayoutData.horizontalSpan = 1;
		canvas.setLayoutData(canvasLayoutData);
		canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				File file = contentProvider.getImageFile();
				if (file != null && file.exists() && file.isFile()) {
					Image image = new Image(Display.getDefault(), file.getAbsolutePath());
					e.gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, 0, 0, w, h);
					image.dispose();
				}
			}
		});
		
		Composite btnCmp = new Composite(this, SWT.NONE);
		GridLayout bgd = new GridLayout(1, false);
		bgd.marginBottom=0;
		bgd.marginHeight = 0;
		bgd.marginLeft = 0;
		bgd.marginRight = 0;
		bgd.marginTop = 0;
		bgd.marginWidth = 0;
		btnCmp.setLayout(bgd);
		btnCmp.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
		
		
		Button buttonLoad = new Button(btnCmp, SWT.PUSH);
		buttonLoad.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		buttonLoad.setText("Load...");
		buttonLoad.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setFilterExtensions(new String[] {
						"*.bmp;*.jpg;*.jpeg", //$NON-NLS-1$
						"*.bmp", //$NON-NLS-1$
						"*.jpg;*.jpeg", //$NON-NLS-1$
				});
				fd.setFilterNames(new String[] {
						"All Images (*.bmp;*.jpg;*.jpeg)",
						"Bitmap Files (*.bmp)",
						"JPEG Files (*.jpg;*.jpeg)"
				});
				String f = fd.open();
				if (f != null) {
					contentProvider.setImageFile(new File(f));
					redrawCanvas();
				}
			}
		});

		Button buttonClear = new Button(btnCmp, SWT.PUSH);
		buttonClear.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		buttonClear.setText("Clear");
		buttonClear.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				contentProvider.setImageFile(IImageAssociatedObject.NULL_FILE); //we cannot use null, as null indicates that we need to load a value
				redrawCanvas();
			}
		});
	}
	
	public void redrawCanvas() {
		canvas.redraw();
	}

	/**
	 * Interface should be implemented by any object that want to use this control.
	 * It provides data to display and is called when new image was selected.
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	public static interface IImageContentProvider {
		
		/**
		 * @return an image that is supposed to be displayed.
		 */
		public File getImageFile();
		
		/**
		 * Called when new image was selected.
		 * @param file
		 */
		public void setImageFile(File file);
	}
}
