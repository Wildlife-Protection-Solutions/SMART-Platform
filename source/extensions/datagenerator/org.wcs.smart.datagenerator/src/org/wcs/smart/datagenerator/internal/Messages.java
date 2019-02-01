/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.datagenerator.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.datagenerator.internal.messages"; //$NON-NLS-1$
	public static String DataGenerator_AttributeTypeNotSupported;
	public static String DataGenerator_TaskName;
	public static String DataGeneratorPlugIn_ErrordialogTitle;
	public static String DataGeneratorRunnable_CanceledMessage;
	public static String DataGeneratorRunnable_CanceledTitle;
	public static String DataGeneratorRunnable_CompleteMsg;
	public static String DataGeneratorRunnable_CompleteTitle;
	public static String DataGeneratorRunnable_ErrorMsg;
	public static String DataGeneratorView_AddObsButton;
	public static String DataGeneratorView_AllFile;
	public static String DataGeneratorView_AttributeLbl;
	public static String DataGeneratorView_BboxErrorMsg;
	public static String DataGeneratorView_BBoxLbl;
	public static String DataGeneratorView_CategoryLbl;
	public static String DataGeneratorView_CompleteTitle;
	public static String DataGeneratorView_CurrentRange;
	public static String DataGeneratorView_CustomArea;
	public static String DataGeneratorView_DataTab;
	public static String DataGeneratorView_DataTabDescription;
	public static String DataGeneratorView_DateRangeLbl;
	public static String DataGeneratorView_DateRangeTo;
	public static String DataGeneratorView_DaysPerPatrolLbl;
	public static String DataGeneratorView_EmployeesPerPatrolLbl;
	public static String DataGeneratorView_EndDateError;
	public static String DataGeneratorView_ErrorTitle;
	public static String DataGeneratorView_ExportBtn;
	public static String DataGeneratorView_ExportComplete;
	public static String DataGeneratorView_ExportError;
	public static String DataGeneratorView_FileNotFound;
	public static String DataGeneratorView_GenerateBtn;
	public static String DataGeneratorView_ImportBtn;
	public static String DataGeneratorView_ImportTitle;
	public static String DataGeneratorView_IntegerRequired1;
	public static String DataGeneratorView_IntegerRequired2;
	public static String DataGeneratorView_IntegerRequired3;
	public static String DataGeneratorView_IntegerRequired4;
	public static String DataGeneratorView_IntegerRequired5;
	public static String DataGeneratorView_IntegerRequired7;
	public static String DataGeneratorView_InvalidUsername;
	public static String DataGeneratorView_LoadFromShapefile;
	public static String DataGeneratorView_MinMaxInvalid;
	public static String DataGeneratorView_NewRange;
	public static String DataGeneratorView_NumPatrolsLlb;
	public static String DataGeneratorView_ObsColumn;
	public static String DataGeneratorView_ObservationsSection;
	public static String DataGeneratorView_ObsPerWapointLbl;
	public static String DataGeneratorView_OverwriteMsg;
	public static String DataGeneratorView_OverwriteTitle;
	public static String DataGeneratorView_PatrolDetailsSection;
	public static String DataGeneratorView_PostiveNumberRequired;
	public static String DataGeneratorView_ReadError;
	public static String DataGeneratorView_SampleBboxValue;
	public static String DataGeneratorView_ShiftButton;
	public static String DataGeneratorView_ShiftLabel;
	public static String DataGeneratorView_SpatialShiftTab;
	public static String DataGeneratorView_TimeShiftMessage;
	public static String DataGeneratorView_TimeShiftTab;
	public static String DataGeneratorView_ToLbl;
	public static String DataGeneratorView_UserNameMsg;
	public static String DataGeneratorView_UserNameTitle;
	public static String DataGeneratorView_WaypointsPerDaylbl;
	public static String DataGeneratorView_WeightColumn;
	public static String DataGeneratorView_XmlFile;
	public static String LayerSelectionDialog_Message;
	public static String LayerSelectionDialog_NotSelected;
	public static String LayerSelectionDialog_SelectionRequire;
	public static String LayerSelectionDialog_ShpFile;
	public static String LayerSelectionDialog_SmartLayer;
	public static String LayerSelectionDialog_Title;
	public static String LayerSelectionDialog_Title2;
	public static String SpatialShiftComposite_allFiles;
	public static String SpatialShiftComposite_CurrentBBox;
	public static String SpatialShiftComposite_FromLabel;
	public static String SpatialShiftComposite_invalidScale;
	public static String SpatialShiftComposite_invalidX;
	public static String SpatialShiftComposite_invalidY;
	public static String SpatialShiftComposite_MapName;
	public static String SpatialShiftComposite_NewBbox;
	public static String SpatialShiftComposite_NotFoundMsg;
	public static String SpatialShiftComposite_notFoundTitle;
	public static String SpatialShiftComposite_ReadErrorMsg;
	public static String SpatialShiftComposite_ReadErrorTitle;
	public static String SpatialShiftComposite_refreshLink;
	public static String SpatialShiftComposite_ResourcesNotFound;
	public static String SpatialShiftComposite_scaleLabel;
	public static String SpatialShiftComposite_Shapefile;
	public static String SpatialShiftComposite_ShapefileOp;
	public static String SpatialShiftComposite_ShiftButton;
	public static String SpatialShiftComposite_ShiftText;
	public static String SpatialShiftComposite_ToLabel;
	public static String SpatialShiftComposite_xLabel;
	public static String SpatialShiftComposite_yLabel;
	public static String TimeShiftComposite_refreshlink;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
