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
package org.wcs.smart.i2.ui.views.query;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextDropItem;

/**
 * Filter item for entity types; children include attributes and specific entities
 * 
 * @author Emily
 *
 */
public class RecordSourceFilterItem extends DeferredTreeFilterItem {

	private String typeKey;
	
	
	public RecordSourceFilterItem(IntelRecordSource source) {
		super(source.getName());
		typeKey = source.getKeyId();
	
		final byte[] icon = source.getIcon();
		if (icon != null){
			setImageDescriptor(new ImageDescriptor() {
				@Override
				public ImageData getImageData() {
					try(ByteArrayInputStream in = new ByteArrayInputStream(icon)){
						BufferedImage image = ImageIO.read(in);
						if (image != null){
							return AWTSWTImageUtils.convertToSWTImage(image).getImageData();
						}
					}catch (Exception ex){
						
					}
					return null;
				}
			});	
		}
	}

	
	@Override
	public DropItem[] asDropItem() {
		return new DropItem[]{new TextDropItem(this.getName(), "recordsource:" + typeKey)}; //$NON-NLS-1$
	}


}
