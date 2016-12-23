package org.wcs.smart.i2.ui.views.query;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute.IAttributeType;
import org.wcs.smart.i2.ui.AttributeLabelProvider;

public class FilterTreeLabelProvider extends LabelProvider {

	private AttributeLabelProvider attributeInstance = new AttributeLabelProvider();
	
	private Map<Object, Image> toDispose = new HashMap<Object, Image>();
	
	public void dispose(){
		attributeInstance.dispose();
		toDispose.values().forEach(e->e.dispose());
	}
	public String getText(Object element){
		if (element instanceof FilterItem) return ((FilterItem) element).getName();
		return super.getText(element);
	}
	
	@Override
	public Image getImage(Object element){
		Image img = toDispose.get(element);
		if (img != null) return img;
		if (element instanceof AttributeFilterItem){
			return attributeInstance.getImage(((AttributeFilterItem) element).getType());
		}
		if (element instanceof EntityTypeFilterItem){
			if (((EntityTypeFilterItem) element).getImage() != null){
				img = ((EntityTypeFilterItem) element).getImage().createImage();
				if (img != null){
					toDispose.put(element, img);
					return img;
				}
			}
			return null;
		}
		if (element instanceof AttributeHeaderFilterItem){
			if (((AttributeHeaderFilterItem) element).isGroup()){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON);
			}
			return IAttributeType.NUMERIC.getImage();
		}
		
		if (element instanceof AreaFilterItem){
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_AREA);
		}
		if (element instanceof DataModelFilterItem){
			if (((DataModelFilterItem) element).getType() == null){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON); 
			}else{
				return DataModel.getAttributeImage(((DataModelFilterItem) element).getType());
			}
			
		}
		if (element instanceof BasicFilterItem){
			if (((BasicFilterItem) element).getImage() != null){
				img = ((BasicFilterItem) element).getImage().createImage();
				if (img != null){
					toDispose.put(element, img);
					return img;
				}
			}
		}
		return null;
	}
}
