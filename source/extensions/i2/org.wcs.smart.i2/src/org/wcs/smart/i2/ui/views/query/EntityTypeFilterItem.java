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
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;

public class EntityTypeFilterItem extends DeferredFilterItem {

	private Object LOCK = new Object();
	private UUID typeUuid;
	
	
	public EntityTypeFilterItem(IntelEntityType type) {
		super("Entities");
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
	public List<FilterItem> getChildren() {
		if (kids == null ){
			synchronized (LOCK) {
				if (kids == null){
					System.out.println("loading kidss:" + getName());
					Session s = HibernateManager.openSession();
					try{
						List<IntelEntity> entities = s.createCriteria(IntelEntity.class).add(Restrictions.eq("entityType.uuid", typeUuid)).list();
						ArrayList<FilterItem> temp = new ArrayList<>();
						for (IntelEntity e : entities){
							temp.add(new EntityFilterItem(e));
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
