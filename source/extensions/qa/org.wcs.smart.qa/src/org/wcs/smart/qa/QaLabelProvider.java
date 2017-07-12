package org.wcs.smart.qa;

import java.util.Locale;

import org.wcs.smart.qa.internal.Messages;

public class QaLabelProvider extends ILabelProvider {

	@Override
	public String getString(Key key, Locale l) {
		switch(key){
		case IgnoreAction_Name:
			return Messages.QaLabelProvider_IngoreActionName;
		case LocationRoutineType_AreaParamDescription:
			return Messages.QaLabelProvider_LocationAreaParamLabel;
		case LocationRoutineType_Description:
			return Messages.QaLabelProvider_LocationRoutineDescription;
		case LocationRoutineType_Error:
			return Messages.QaLabelProvider_LocationRoutineErrorLabel;
		case LocationRoutineType_FileParamDescription:
			return Messages.QaLabelProvider_LocationFileParamLabel;
		case LocationRoutineType_LoadingDataMsg:
			return Messages.QaLabelProvider_LoadingDataTaskName;
		case LocationRoutineType_Name:
			return Messages.QaLabelProvider_LocationRoutineName;
		case LocationRoutineType_NoGeomFound:
			return Messages.QaLabelProvider_ValidGeometryNotFound;
		case LocationRoutineType_TrackOutsideArea:
			return Messages.QaLabelProvider_LocationTrackOutsideArea;
		case LocationRoutineType_TrackOutsideArea2:
			return Messages.QaLabelProvider_LocationTrackOutsideArea2;
		case LocationRoutineType_ValidatingDataTaskName:
			return Messages.QaLabelProvider_ValidationTaskName;
		case LocationRoutineType_WktParamDescription:
			return Messages.QaLabelProvider_LocationWktParamLabel;
		case LocationRoutineType_WpOutsideArea:
			return Messages.QaLabelProvider_LocationWpOutsideArea;
		case LocationRoutineType_WpOutsideArea2:
			return Messages.QaLabelProvider_LocationWpOutsideArea2;
		case QaErrorGeoResourceInfo_Description:
			return Messages.QaLabelProvider_GeoResourceDescription;
		case QaErrorGeoResourceInfo_Name:
			return Messages.QaLabelProvider_GeoResourceName;
		case QaErrorService_Description:
			return Messages.QaLabelProvider_QaServiceDescription;
		case QaErrorService_Name:
			return Messages.QaLabelProvider_QaServiceName;
		case ValidationEngine_SubTaskName:
			return Messages.QaLabelProvider_ValidationSubTask;
		case ValidationEngine_TaskName:
			return Messages.QaLabelProvider_ValidationTaskName;
		case QaError_Status_New:
			return Messages.QaLabelProvider_StatusNew;
		case QaError_Status_Ignored:
			return Messages.QaLabelProvider_StatusIgnored;
		case QaError_Status_Deleted:
			return Messages.QaLabelProvider_StatusDeleted;
		case QaError_Status_Error:
			return Messages.QaLabelProvider_StatusError;
		case QaError_Status_Fixed:
			return Messages.QaLabelProvider_StatusFixed;
		default:
			break;
		
		}
		return ""; //$NON-NLS-1$
	}

}
