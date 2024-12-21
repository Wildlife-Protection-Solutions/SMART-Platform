package org.wcs.smart.connect.qa;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.qa.ILabelProvider;


public class QaLabelProvider extends ILabelProvider {

	@Override
	public String getString(Key key, Locale l) {
		switch(key){
		case IgnoreAction_Name:
			return Messages.getString("QaLabelProvider.IgnoreActionName", l); //$NON-NLS-1$
		case LocationRoutineType_AreaParamDescription:
			return Messages.getString("QaLabelProvider.LocationAreaParamDesc", l); //$NON-NLS-1$
		case LocationRoutineType_Description:
			return Messages.getString("QaLabelProvider.LocationDesc", l); //$NON-NLS-1$
		case LocationRoutineType_Error:
			return Messages.getString("QaLabelProvider.LocationErrorLabel", l); //$NON-NLS-1$
		case LocationRoutineType_FileParamDescription:
			return Messages.getString("QaLabelProvider.LocationFileDesc", l); //$NON-NLS-1$
		case LocationRoutineType_LoadingDataMsg:
			return Messages.getString("QaLabelProvider.LocationTaskName", l); //$NON-NLS-1$
		case LocationRoutineType_Name:
			return Messages.getString("QaLabelProvider.LocationRoutineName", l); //$NON-NLS-1$
		case LocationRoutineType_NoGeomFound:
			return Messages.getString("QaLabelProvider.LocationNoGeomFound", l); //$NON-NLS-1$
		case LocationRoutineType_TrackOutsideArea:
			return Messages.getString("QaLabelProvider.LocationTrackOutside1", l); //$NON-NLS-1$
		case LocationRoutineType_TrackOutsideArea2:
			return Messages.getString("QaLabelProvider.LocationTrackOutside2", l); //$NON-NLS-1$
		case LocationRoutineType_ValidatingDataTaskName:
			return Messages.getString("QaLabelProvider.Location_DataTaskName", l); //$NON-NLS-1$
		case LocationRoutineType_WktParamDescription:
			return Messages.getString("QaLabelProvider.LocationWktParamDesc", l); //$NON-NLS-1$
		case LocationRoutineType_WpOutsideArea:
			return Messages.getString("QaLabelProvider.LocationWaypointOutside", l); //$NON-NLS-1$
		case LocationRoutineType_WpOutsideArea2:
			return Messages.getString("QaLabelProvider.LocationWaypointOutside1", l); //$NON-NLS-1$
		case LocationRoutineType_PrjWpOutsideArea:
			return Messages.getString("QaLabelProvider.PrjLocationWaypointOutside1", l); //$NON-NLS-1$
		case QaErrorGeoResourceInfo_Description:
			return Messages.getString("QaLabelProvider.GeoResourceDesc", l); //$NON-NLS-1$
		case QaErrorGeoResourceInfo_Name:
			return Messages.getString("QaLabelProvider.GeoResourceName", l); //$NON-NLS-1$
		case QaErrorService_Description:
			return Messages.getString("QaLabelProvider.ServiceDesc", l); //$NON-NLS-1$
		case QaErrorService_Name:
			return Messages.getString("QaLabelProvider.ServiceName", l); //$NON-NLS-1$
		case ValidationEngine_SubTaskName:
			return Messages.getString("QaLabelProvider.ValidationTaskName", l); //$NON-NLS-1$
		case ValidationEngine_TaskName:
			return Messages.getString("QaLabelProvider.ValidationSubTaskName", l); //$NON-NLS-1$
		case QaError_Status_New:
			return Messages.getString("QaLabelProvider.StatusNew", l); //$NON-NLS-1$
		case QaError_Status_Ignored:
			return Messages.getString("QaLabelProvider.StatusIgnore", l); //$NON-NLS-1$
		case QaError_Status_Deleted:
			return Messages.getString("QaLabelProvider.StatusDelete", l); //$NON-NLS-1$
		case QaError_Status_Error:
			return Messages.getString("QaLabelProvider.StatusError", l); //$NON-NLS-1$
		case QaError_Status_Fixed:
			return Messages.getString("QaLabelProvider.StatusFixed", l); //$NON-NLS-1$		
		case QaError_Status_Unknown:
			return Messages.getString("QaLabelProvider.StatusUnknown", l); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

}
