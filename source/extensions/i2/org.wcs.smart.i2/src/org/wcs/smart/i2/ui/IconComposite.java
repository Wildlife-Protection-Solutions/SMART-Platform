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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;

/**
 * Simple composite that allows users to select an icon.  The image is 
 * resized to 16x16.
 * 
 * @author Emily
 *
 */
public class IconComposite extends Composite {

	private Label image;
	private Button btnBrowse;
	
	private String lastDir = null;
	private List<ModifyListener> listeners = new ArrayList<ModifyListener>();
	
	private byte[] imageData;
	
	public IconComposite(Composite parent) {
		super(parent, SWT.NONE);
		createContent();
	}
	
	public void addModifyListener(ModifyListener listener){
		listeners.add(listener);
	}

	private void fireChange(){
		for (ModifyListener l : listeners){
			Event evt = new Event();
			evt.display = getDisplay();
			evt.widget = image;
			l.modifyText(new ModifyEvent(evt));
		}
	}
	
	private void createContent(){
		setLayout(new GridLayout(2, false));
		
		image = new Label(this, SWT.NONE);
		image.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)image.getLayoutData()).widthHint = 16;
		((GridData)image.getLayoutData()).heightHint = 16;
		
		btnBrowse = new Button(this, SWT.PUSH);
		btnBrowse.setText("..."); //$NON-NLS-1$
		btnBrowse.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileChooser = new FileDialog(getShell(), SWT.OPEN);
		        fileChooser.setText(Messages.IconComposite_FileChooserMsg);
		        fileChooser.setFilterPath(lastDir);
		        fileChooser.setFilterExtensions(new String[] { "*.gif;*.jpg;*.png;*.ico;*.bmp;*.jpeg", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
		        fileChooser.setFilterNames(new String[] { Messages.IconComposite_imageslabel + " (gif, jpg, jpeg, png, ico, bmp)", Messages.IconComposite_AllFiles }); //$NON-NLS-1$
		        String filename = fileChooser.open();
		        if (filename != null){
		        	loadImage(filename);
		        	lastDir = fileChooser.getFilterPath();
		        }
			}
		});
	}
	
	private void loadImage(String filename){
		if (image.getImage() != null && !image.getImage().isDisposed()){
			image.getImage().dispose();
			image.setImage(null);
		}
		try{
			BufferedImage img = ImageIO.read(new File(filename));
			
			if (img.getWidth() !=  16 || img.getHeight() != 16){
				Image tmp = img.getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
				img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
				img.getGraphics().drawImage(tmp,  0,  0,  null);
				img.getGraphics().dispose();
			}
		
			try(ByteArrayOutputStream stream = new ByteArrayOutputStream()){
				ImageIO.write(img, "png", stream); //$NON-NLS-1$
				imageData = stream.toByteArray();
			}
			image.setImage(AWTSWTImageUtils.convertToSWTImage(img));
			fireChange();
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(Messages.IconComposite_InvalidFile + ex.getMessage(), ex);
		}
	}
	
	public byte[] getImage() {
		return imageData;
	}
	
	public void setImage(byte[] img){
		if (img == null){
			imageData = null;
			image.setImage(null);
			return;
		}
		imageData = img;
		try(ByteArrayInputStream in = new ByteArrayInputStream(img)){
			BufferedImage swtImage = ImageIO.read(in);
			image.setImage(AWTSWTImageUtils.convertToSWTImage(swtImage));
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(Messages.IconComposite_ReadError + ex.getMessage(), ex);
			image.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		}
	}
}
