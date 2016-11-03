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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.common.attachment.ISmartAttachment;

/**
 * Pop out an attachment in shell dialog.
 * 
 * @author Emily
 *
 */
public class AttachmentPopoutShell extends SmartShellDialog {

	private ISmartAttachment attachment;
	
	public AttachmentPopoutShell(Shell parentShell, ISmartAttachment attachment) {
		super(parentShell, SWT.RESIZE | SWT.TITLE);
		this.attachment = attachment;
		shell.setSize(400,400);
	}

	@Override
	public void createContents(Composite parent) {
		final Canvas draw = new Canvas(parent, SWT.BORDER);
		final Image rawImage = new Image(Display.getDefault(), attachment.getAttachmentFile().getAbsolutePath());
		draw.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		draw.addPaintListener(new PaintListener() {
			
			@Override
			public void paintControl(PaintEvent e) {
				Rectangle dst = draw.getClientArea();
				Rectangle r = rawImage.getBounds();
				
				double ratio = (r.height*1.0) / r.width;
				int height = dst.height;
				int width = dst.width;
				int x = 0;
				int y = 0;
				if (dst.width < dst.height){
					height = (int)(dst.width * ratio);
				}else{
					width = (int)(dst.height / ratio);
				}
				x = (dst.width - width) / 2;
				y = (dst.height - height) / 2;
				e.gc.drawImage(rawImage, 0,0,r.width, r.height, x, y, width, height);
			}
		});
		draw.addListener(SWT.Dispose, d -> rawImage.dispose());	
	}

}
