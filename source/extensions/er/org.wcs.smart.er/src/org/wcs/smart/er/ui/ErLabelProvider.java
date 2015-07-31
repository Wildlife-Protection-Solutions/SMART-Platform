package org.wcs.smart.er.ui;

import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.IErLabelProvider;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;

public class ErLabelProvider implements IErLabelProvider{

	@Override
	public String getLabel(Object item, Locale l) {
		if (item == SamplingUnit.GeometryType.PLOT){
			return Messages.SamplingUnit_PointGeomType;
		}
		if (item == SamplingUnit.GeometryType.TRANSECT){
			return Messages.SamplingUnit_LinearGeomType; 
		} 
		if (item == SamplingUnit.State.ACTIVE){
			return Messages.SamplingUnit_ActiveState;
		}
		if (item == SamplingUnit.State.INACTIVE){
			return Messages.SamplingUnit_InActiveState;
		}
		if (item == SurveyDesign.State.ACTIVE){
			return Messages.SurveyDesign_ActiveStateLabel;
		}
		if (item == SurveyDesign.State.INACTIVE){
			return Messages.SurveyDesign_InActiveStateLabel;
		}
		if (item == MissionTrack.TrackType.TRACK){
			return Messages.MissionTrack_TrackGuiName;
		}
		if (item == MissionTrack.TrackType.SAMPLING_UNIT){
			return Messages.MissionTrack_SuTrackGuiName;
		}
		return null;
	}

	public static Image getImage(Object item){
		if (item == SamplingUnit.GeometryType.PLOT){
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_TRANSECT_ICON);
		}
		if (item == SamplingUnit.GeometryType.TRANSECT){
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_PLOT_ICON); 
		}
		return null;
	}
}
