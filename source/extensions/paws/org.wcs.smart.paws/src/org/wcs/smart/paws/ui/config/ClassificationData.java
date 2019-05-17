package org.wcs.smart.paws.ui.config;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.paws.model.AbstractPawsClass;
import org.wcs.smart.paws.model.PawsQueryClass;
import org.wcs.smart.paws.model.PawsSimpleClass;

public class ClassificationData {
	
	private AbstractPawsClass op1;
	private String label;

	public ClassificationData(AbstractPawsClass op1, String label) {
		this.op1 = op1;
		this.label = label;
	}
	
	public AbstractPawsClass getPawsClass(){
		return this.op1;
	}
	public String getClassification() {
		if (op1 instanceof PawsSimpleClass) return op1.getClassification();
		if (op1 instanceof PawsQueryClass) return op1.getClassification();
		return "";
	}
	
	public String getDataSource() {
		if (op1 instanceof PawsSimpleClass) return "Data Model";
		if (op1 instanceof PawsQueryClass) return "Query";
		return "unknown";
	}
	
	public String getDetails() {
		return label;
	}

	public static String createLabel(Category c, Attribute a, DmObject listortree){
		if (listortree == null) return c.getFullCategoryName();
		
		StringBuilder sb = new StringBuilder();
		sb.append(listortree.getName());
		sb.append( " (" + a.getName() + ") ");
		sb.append(c.getFullCategoryName());
		return sb.toString();
	}
}