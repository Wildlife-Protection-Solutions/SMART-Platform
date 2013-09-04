package org.wcs.smart.plan.report;

import java.io.File;

import org.eclipse.birt.report.designer.internal.ui.editors.ReportEditorInput;

public class ReportPlanEditorInput extends ReportEditorInput{

	public ReportPlanEditorInput(File file) {
		super(file);
	}
	
	public boolean equals(Object other){
		if (this == other) return true;
		if (other instanceof ReportPlanEditorInput){
			return  ((ReportPlanEditorInput)other).getFile().equals(getFile());
		}
		return false;
	}
	
	public int hashCode(){
		return getFile().hashCode();
	}

}
