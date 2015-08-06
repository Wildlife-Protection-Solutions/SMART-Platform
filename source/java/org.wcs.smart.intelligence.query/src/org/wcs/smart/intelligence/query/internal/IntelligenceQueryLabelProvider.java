package org.wcs.smart.intelligence.query.internal;

import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.query.IIntelligenceQueryLabelProvider;
import org.wcs.smart.intelligence.query.RecievedDateFilter;
import org.wcs.smart.intelligence.query.filter.IntelligenceFilterOption;
import org.wcs.smart.intelligence.query.model.FixedQueryColumn;
import org.wcs.smart.patrol.SmartPatrolPlugIn;

public class IntelligenceQueryLabelProvider implements
		IIntelligenceQueryLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item == FixedQueryColumn.FixedColumns.CA_ID) return Messages.FixedQueryColumn_CaIdCol;
		if (item == FixedQueryColumn.FixedColumns.CA_NAME) return Messages.FixedQueryColumn_CaNameCol;
		if (item == FixedQueryColumn.FixedColumns.INTEL_NAME) return Messages.FixedQueryColumn_NameCol;
		if (item == FixedQueryColumn.FixedColumns.INTEL_DATE_RECIEVED) return Messages.FixedQueryColumn_ReceivedDateCol;
		if (item == FixedQueryColumn.FixedColumns.INTEL_DATE_FROM) return Messages.FixedQueryColumn_FromDateColumn;
		if (item == FixedQueryColumn.FixedColumns.INTEL_DATE_TO) return Messages.FixedQueryColumn_ToDateColumn;
		if (item == FixedQueryColumn.FixedColumns.INTEL_SOURCE) return Messages.FixedQueryColumn_SourceColumn;
		if (item == FixedQueryColumn.FixedColumns.INTEL_PATROL_SOURCE) return Messages.FixedQueryColumn_PatrolColumn;
		if (item == FixedQueryColumn.FixedColumns.INTEL_INFORMANT_ID) return Messages.FixedQueryColumn_InformantCol;
		if (item == FixedQueryColumn.FixedColumns.INTEL_DESCRIPTION) return Messages.FixedQueryColumn_DescriptionCol;
		
		
		if (item == IntelligenceFilterOption.NAME) return Messages.IntelligenceFilterOption_NameOption;
		if (item == IntelligenceFilterOption.SOURCE) return Messages.IntelligenceFilterOption_SoureOption;
		if (item == IntelligenceFilterOption.PATROLID) return Messages.IntelligenceFilterOption_PatrolIdOption;
		if (item == IntelligenceFilterOption.DESCRIPTION) return Messages.IntelligenceFilterOption_DescriptionOption;
		if (item == IntelligenceFilterOption.INFORMANTID) return Messages.IntelligenceFilterOption_InformationIdOption;
		
		if (item instanceof RecievedDateFilter) return Messages.RecievedDateFilter_ReceivedDateFilterName;
		return null;
	}

	public static Image getImage(IntelligenceFilterOption option){
		if (option == IntelligenceFilterOption.PATROLID) return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON);
		if (option == IntelligenceFilterOption.INFORMANTID) return IntelligencePlugIn.getDefault().getImageRegistry().get(IntelligencePlugIn.INFORMANT_ICON);
		return null;
	}
}
