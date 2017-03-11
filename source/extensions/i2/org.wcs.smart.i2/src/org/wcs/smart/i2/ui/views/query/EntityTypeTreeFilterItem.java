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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;

/**
 * Filter item for entity types; children include attributes and specific entities
 * 
 * @author Emily
 *
 */
public class EntityTypeTreeFilterItem extends DeferredTreeFilterItem {

	private Object LOCK = new Object();
	private UUID typeUuid;
	
	
	public EntityTypeTreeFilterItem(IntelEntityType type) {
		super(Messages.EntityTypeTreeFilterItem_EntitiesLabel);
		typeUuid = type.getUuid();
	
		final byte[] icon = type.getIcon();
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
	

	@SuppressWarnings("unchecked")
	@Override
	public List<FilterTreeItem> getChildren() {
		if (kids == null ){
			synchronized (LOCK) {
				if (kids == null){
					Session s = HibernateManager.openSession();
					try{
						List<IntelEntity> entities = s.createCriteria(IntelEntity.class).add(Restrictions.eq("entityType.uuid", typeUuid)).list(); //$NON-NLS-1$
						ArrayList<FilterTreeItem> temp = new ArrayList<>();
						for (IntelEntity e : entities){
							temp.add(new EntityTreeFilterItem(e));
						}
						kids = temp;
					}finally{
						s.close();
					}
				}
			}
			
		}
		if (kids == null) return null;
		return Collections.unmodifiableList(kids);
	}

}
