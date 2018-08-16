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
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.dataentry.DataentryPlugIn;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.IImageAssociatedObject;
import org.wcs.smart.util.SmartUtils;

/**
 * Control that allows to select a single image for provided object.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ImageSelectionControl extends Composite {

	//preference for last image filter used
	private static final String IMG_FILTER_PREFKEY = "org.wcs.smart.dataentry.dialog.composite.ImageSelectionControl.imagefilter"; //$NON-NLS-1$
	
	private IImageContentProvider contentProvider;
	private Canvas canvas;
	private Label lblWarnText;
	private Composite warningArea;
	
	public ImageSelectionControl(Composite parent, IImageContentProvider contentProvider) {
		super(parent, SWT.NONE);
		this.contentProvider = contentProvider;
		initControls();
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		super.setEnabled(isEnabled);
		
		List<Control> kids = new ArrayList<>();
		for (Control c : this.getChildren()) kids.add(c);
		
		while(kids.size() > 0) {
			Control c = kids.remove(0);
			c.setEnabled(isEnabled);
			if (c instanceof Composite) {
				for (Control kk : ((Composite)c).getChildren()) {
					kids.add(kk);
				}
			}
		}
	}
	
	public void updateImage() {
		canvas.redraw();
		warningArea.setVisible(false);
		lblWarnText.setText(""); //$NON-NLS-1$

		File file = contentProvider.getImageFile();
		if (file != null && file.exists() && file.isFile()) {
			
			if (file.getName().endsWith(".svg") || file.getName().endsWith(".png")) { //$NON-NLS-1$ //$NON-NLS-2$
				lblWarnText.setText(Messages.ImageSelectionControl_BetaCtFormat);
				warningArea.setVisible(true);
			}
		}
		warningArea.getParent().getParent().layout(true);
		
		canvas.setToolTipText(file == null ? "" : file.getName()); //$NON-NLS-1$
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

		int h = 100;
		int w = h;
		
		canvas = new Canvas(this, SWT.BORDER);
		GridData canvasLayoutData = new GridData(w, h);
		
		canvas.setLayoutData(canvasLayoutData);
		canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				
				File file = contentProvider.getImageFile();
				if (file != null && file.exists() && file.isFile()) {
					Image image = null;
					try {
						image = new Image(Display.getDefault(), file.getAbsolutePath());
					}catch (Exception ex) {
						try {
							image = SmartUtils.readSvg(getDisplay(), file.toPath()); 
						}catch (Exception ex2) {
						}
					}
					if (image == null) return;
					try {
						double scale = 1;
						if (image.getBounds().width > image.getBounds().height) {
							scale = (double)image.getBounds().width / w;
						}else {
							scale = (double)image.getBounds().height / h;
						}
						int width = (int)( image.getBounds().width/scale);
						int height = (int) (image.getBounds().height/scale);
						
						int xoffset = 0;
						int yoffset = 0;
						if (width < w) {
							xoffset = (w - width)/2;
						}
						if (height < h) {
							yoffset = (h - height)/2;
						}
						e.gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, xoffset, yoffset, width, height);
					}finally {
						image.dispose();
					}
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
		btnCmp.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		
		
		Button buttonLoad = new Button(btnCmp, SWT.PUSH);
		buttonLoad.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		buttonLoad.setText(Messages.ImageSelectionControl_Button_Load);
		buttonLoad.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setFilterExtensions(new String[] {
						"*.bmp;*.jpg;*.jpeg", //$NON-NLS-1$
						"*.bmp", //$NON-NLS-1$
						"*.jpg;*.jpeg", //$NON-NLS-1$
						"*.png;*.svg", //$NON-NLS-1$
						"*.png", //$NON-NLS-1$
						"*.svg", //$NON-NLS-1$
						
				});
				fd.setFilterNames(new String[] {
						Messages.ImageSelectionControl_AllImages,
						Messages.ImageSelectionControl_BitmapFiles,
						Messages.ImageSelectionControl_JpegFiles,
						Messages.ImageSelectionControl_pngsvg,
						Messages.ImageSelectionControl_png,
						Messages.ImageSelectionControl_svg
				});
				
				int lastIndex = DataentryPlugIn.getDefault().getPreferenceStore().getInt(IMG_FILTER_PREFKEY);
				fd.setFilterIndex(lastIndex);
				
				String f = fd.open();
				if (f != null) {
					contentProvider.setImageFile(new File(f));
					updateImage();
					
					//save preference
					int filterIndex = fd.getFilterIndex();
					DataentryPlugIn.getDefault().getPreferenceStore().setValue(IMG_FILTER_PREFKEY, filterIndex);
				}
			}
		});

		Button buttonClear = new Button(btnCmp, SWT.PUSH);
		buttonClear.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		buttonClear.setText(Messages.ImageSelectionControl_Button_Clear);
		buttonClear.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				contentProvider.setImageFile(IImageAssociatedObject.NULL_FILE); //we cannot use null, as null indicates that we need to load a value
				updateImage();
			}
		});
		
		warningArea = new Composite(this, SWT.NONE);
		warningArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		warningArea.setLayout(new GridLayout(2, false));
		
		Label lblWarnImg = new Label(warningArea, SWT.NONE);
		lblWarnImg.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		lblWarnImg.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		lblWarnText = new Label(warningArea, SWT.WRAP);
		lblWarnText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblWarnText.getLayoutData()).widthHint = w;
		lblWarnText.setText(""); //$NON-NLS-1$
		
		warningArea.setVisible(false);
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
