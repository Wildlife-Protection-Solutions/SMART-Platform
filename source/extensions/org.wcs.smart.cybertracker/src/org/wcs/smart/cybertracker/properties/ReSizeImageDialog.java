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
package org.wcs.smart.cybertracker.properties;

import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption.ImageSizeOption;

/**
 * dialog for collecting details about how a given image should be resized
 * @author Emily
 *
 */
public class ReSizeImageDialog extends TitleAreaDialog{

	private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.00"); //$NON-NLS-1$
	
	private static final String RESIZE_KEY = "org.wcs.smart.cybertracker.properties.ReSizeImageDialog.resize"; //$NON-NLS-1$
	private static final String SIZEOP_KEY = "org.wcs.smart.cybertracker.properties.ReSizeImageDialog.sizeop"; //$NON-NLS-1$
	private static final String SIZE_WIDTH_KEY = "org.wcs.smart.cybertracker.properties.ReSizeImageDialog.width"; //$NON-NLS-1$
	private static final String SIZE_HEIGHT_KEY = "org.wcs.smart.cybertracker.properties.ReSizeImageDialog.height"; //$NON-NLS-1$
	
	private ResizeOptionComposite sizeComp;
	
	private ISmartAttachment attachment;
	private BufferedImage image;
	private Button btnResize;
	
	private int width = -1;
	private int height = -1;
	
	public ReSizeImageDialog(Shell parentShell, ISmartAttachment attachment, BufferedImage img) {
		super(parentShell);
		this.attachment = attachment;
		this.image = img;
	}
	
	@Override
	public void createButtonsForButtonBar(Composite parent){
		createButton(parent, IDialogConstants.YES_ID, Messages.ReSizeImageDialog_ApplyButton, true);
		createButton(parent, IDialogConstants.YES_TO_ALL_ID, Messages.ReSizeImageDialog_ApplyAllButton, false);
	}
	
	public void buttonPressed(int buttonId){
		setReturnCode(buttonId);
		
		CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(RESIZE_KEY, btnResize.getSelection());
		if (btnResize.getSelection()){
			ImageSizeOption option = sizeComp.getResizeOption();
			CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(SIZEOP_KEY, option.name());
			if (option == ImageSizeOption.CUSTOM){	
				try{
					width = Integer.parseInt(sizeComp.getWidth());
					height = Integer.parseInt(sizeComp.getHeight());
					if (width < 0 || height < 0) throw new Exception();
				}catch (Exception ex){
					setErrorMessage(Messages.ReSizeImageDialog_InvalidWidthHeight);
				}
			}else{
				width = option.width;
				height = option.height;
			}
			CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(SIZE_WIDTH_KEY, width);
			CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(SIZE_HEIGHT_KEY, height);
		}else{
			width = -1;
			height = -1;
			CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(SIZE_WIDTH_KEY, ""); //$NON-NLS-1$
			CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(SIZE_HEIGHT_KEY, ""); //$NON-NLS-1$
		}
		super.close();
	}
	
	/**
	 * will return a point with values of -1 if image
	 * is not to be resized
	 * @return
	 */
	public Point getImageSize(){
		return new Point(width, height);
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite left = new Composite(main, SWT.NONE);
		left.setLayout(new GridLayout());
		
		Canvas img = new Canvas(left, SWT.BORDER);
		img.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		img.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		((GridData)img.getLayoutData()).widthHint = 200;
		((GridData)img.getLayoutData()).heightHint = 200;
		
		Image i1 = AWTSWTImageUtils.createSWTImage(image);
		Image i2 = null;
		try{
			double ratio = Math.min(200.0/(double)i1.getBounds().width, 200.0/(double)i1.getBounds().height);
			i2 = new Image(getShell().getDisplay(), i1.getImageData().scaledTo((int)(i1.getBounds().width*ratio), (int)(i1.getBounds().height*ratio)));
		}finally{
			i1.dispose();
		}
		if (i2 != null){
			final Image i3 = i2;
			img.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					int x = (int)Math.floor((e.width - i3.getBounds().width) / 2.0);
					int y = (int)Math.floor((e.height- i3.getBounds().height) / 2.0);
					e.gc.drawImage(i3, x, y);
				}
			});
			img.addListener(SWT.Dispose, e->i3.dispose());
		}
		
		
		Label l = new Label(left, SWT.NONE);
		l.setText(attachment.getFilename());
		
		l = new Label(left, SWT.NONE);
		l.setText(String.valueOf(image.getWidth()) + " x " + String.valueOf(image.getHeight()) ); //$NON-NLS-1$
		
		l = new Label(left, SWT.NONE);
		l.setText( NUMBER_FORMAT.format(attachment.getCopyFromLocation().length() / (1048576.0)) + "MB" ); //$NON-NLS-1$
		
		Composite right = new Composite(main, SWT.NONE);
		right.setLayout(new GridLayout());
		
		btnResize = new Button(right, SWT.CHECK);
		btnResize.setText(Messages.ReSizeImageDialog_ResizeOpt);
		btnResize.setSelection(true);
		
		btnResize.addListener(SWT.Selection, e->{
			sizeComp.setEnabled(btnResize.getSelection());
		});
		sizeComp = new ResizeOptionComposite(right);
		sizeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		boolean resize = CyberTrackerPlugIn.getDefault().getPreferenceStore().getBoolean(RESIZE_KEY);
		btnResize.setSelection(resize);
		Object x = CyberTrackerPlugIn.getDefault().getPreferenceStore().getInt(SIZE_HEIGHT_KEY);
		if (x != null && x instanceof Integer){
			sizeComp.setHeight(String.valueOf((Integer)x));
		}
		x = CyberTrackerPlugIn.getDefault().getPreferenceStore().getInt(SIZE_WIDTH_KEY);
		if (x != null && x instanceof Integer){
			sizeComp.setWidth(String.valueOf((Integer)x));
		}
		
		x = CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(SIZEOP_KEY);
		if (x != null && x instanceof String){
			for (ImageSizeOption op : CyberTrackerPropertiesOption.ImageSizeOption.values()){
				if (op.name().equalsIgnoreCase((String)x)){
					sizeComp.setResizeOption(op);
					break;
				}
			}
		}
		sizeComp.setEnabled(btnResize.getSelection());
		
		setTitle(Messages.ReSizeImageDialog_Title);
		getShell().setText(Messages.ReSizeImageDialog_Title);
		setMessage(Messages.ReSizeImageDialog_Message);
		return parent;
	}

	
	@Override
	public boolean isResizable(){
		return true;
	}
}
