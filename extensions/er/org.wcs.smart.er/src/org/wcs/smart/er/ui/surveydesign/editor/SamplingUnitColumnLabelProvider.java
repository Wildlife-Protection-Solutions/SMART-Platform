package org.wcs.smart.er.ui.surveydesign.editor;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;

public class SamplingUnitColumnLabelProvider extends ColumnLabelProvider {

	public enum FixedColumns{TYPE, ID, BUFFER, LENGTH, STATE};
	
	private String key;
	
	public SamplingUnitColumnLabelProvider(String key){
		this.key = key;
	}
	
	public String getText(Object element){
		if (element instanceof SamplingUnit){
			SamplingUnit su = (SamplingUnit) element;
			
			if (key.equals(FixedColumns.TYPE.name())){
				return su.getType().getGuiName();
			}else if(key.equals(FixedColumns.ID.name())){
				return su.getId();
			}else if (key.equals(FixedColumns.ID.BUFFER.name())){
				if (su.getBuffer() == null){
					return "";
				}else{
					return su.getBuffer().toString();
				}
			}else if (key.equals(FixedColumns.ID.LENGTH.name())){
				if (su.getGeometryLengthKm() == null){
					return "";
				}else{
					return su.getGeometryLengthKm().toString();
				}
			}else if (key.equals(FixedColumns.ID.STATE.name())){
				return su.getState().getGuiName();
			}else{
				//search attributes
				for (SamplingUnitAttributeValue sua : su.getAttributes()){
					if (sua.getSamplingUnitAttribute().getKeyId().equals(key)){
						if (sua.getSamplingUnitAttribute().getType() == AttributeType.TEXT){
							if (sua.getStringValue() == null){
								return "";
							}else{
								return sua.getStringValue();
							}
						}else if (sua.getSamplingUnitAttribute().getType() == AttributeType.NUMERIC){
							if (sua.getDoubleValue() == null){
								return "";
							}else{
								return sua.getDoubleValue().toString();
							}
						}
					}
				}
				return "";
				
			}
			
		}
		
		return super.getText(element);
	}
}
