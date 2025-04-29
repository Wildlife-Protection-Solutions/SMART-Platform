/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.QrCodeManager;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;

/**
 * @since 8.1.1
 */
public class QrCodeLabel extends Composite{

	private Label lbl;
	private String url;
	
	public QrCodeLabel(Composite parent, int style) {
		super(parent, style);
		create();
	}

	private Image getImage() {
		return lbl.getImage();
	}
	
	private void create() {
		setLayout(new GridLayout());
		
		lbl = new Label(this, SWT.NONE);
		
		addListener(SWT.Dispose,e->{
			if (getImage() != null && !getImage().isDisposed()) {
				getImage().dispose();
			}
		});
		lbl.setToolTipText(Messages.QrCodeLabel_viewlargeversion);
		
		lbl.addListener(SWT.MouseUp, e->{
			if (this.url == null) return;
			
			Shell largeShell = new Shell(getShell(), SWT.NO_TRIM);
			largeShell.setBackground(largeShell.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			largeShell.setLayout(new GridLayout());
			
			Label image = new Label(largeShell, SWT.NONE);
			image.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			image.setBackground(largeShell.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			Image img = null;
			try {
				img = QrCodeManager.INSTANCE.generateQRCode(url, 400);					
				image.addDisposeListener(xx->{						
					if (image.getImage() != null && !image.getImage().isDisposed()) {
						image.getImage().dispose();
					}
				});
				image.setImage(img);
			} catch (Exception e1) {
				SmartPlugIn.log(e1.getMessage(), e1);
				largeShell.dispose();
				return;
			}
			image.addListener(SWT.MouseUp, evt->largeShell.dispose());
			largeShell.addListener(SWT.Deactivate, event -> largeShell.close());
			int size = img.getBounds().width + ((GridLayout)largeShell.getLayout()).marginHeight*2;
			largeShell.setSize(size, size);
			Rectangle r = getShell().getMonitor().getBounds();
			largeShell.setLocation(r.x + (r.width/2 - size/2), r.y + (r.height/2 - size/2));
			largeShell.open();
		});
	}
	
	public void setUrl(String url) {
		this.url = url;
		
		Image img = getImage();
		if (img != null && !img.isDisposed()) {
			img.dispose();
		}
		if (url != null) {
			try {
				lbl.setImage(QrCodeManager.INSTANCE.generateQRCode(this.url));
			}catch (Exception ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
			}
		}
	}
}
