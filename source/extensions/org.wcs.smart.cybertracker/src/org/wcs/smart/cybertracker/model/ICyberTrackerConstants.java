/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.model;

/**
 * Place holder for CyberTracker constants
 * 
 * @author elitvin
 * @since 1.0.0
 */
public interface ICyberTrackerConstants {

	/**
	 * The version number to use for checking against the CT
	 * registry version number
	 */
	public static final String INSTALL_MIN_VERSION  = "3.00.0435"; //$NON-NLS-1$
	/**
	 * The version number to display if the current
	 * version CT is not compatible 
	 */
	/*
	 * We noticed that CT version 3.345 will install a version
	 * number of 3.00.0345 in the windows registry.  This is
	 * not reflective of the versioning of CT on the downloads page
	 * (or what the user sees) so rather than displaying the extra
	 * 0's we use this constant to display something different. 
	 * 
	 * Q: It looks like in the registry the version is something like 
	 * 3.00.0366  but in the application the about shows just 3.366
	 * 
	 * A: (From Justin) I think this is just an artifact of the installer. 
	 * I don't use those digits.  
	 * 
	 */
	public static final String DISPLAY_MIN_VERSION  = "3.435"; //$NON-NLS-1$
	
	public static final String SMART_CTX_FILENEME = "smart.ctx"; //$NON-NLS-1$
	public static final String SMART_CTX_DOWNLOAD_FOLDER = "CyberTracker"; //$NON-NLS-1$
	public static final String SMART_CTX_STORAGE_FOLDER = "storage"; //$NON-NLS-1$
	
	public static final String XML_SCREENS = "Screens.xml"; //$NON-NLS-1$
	public static final String XML_ELEMENTS = "Elements.xml"; //$NON-NLS-1$
	public static final String XML_REPORTS = "Reports.xml"; //$NON-NLS-1$
	
	public static final String REG_KEY_PATH = "Software\\CyberTracker3"; //$NON-NLS-1$
	public static final String REG_KEY_NAME = "Path"; //$NON-NLS-1$
	public static final String REG_KEY_SMART = "SMART"; //$NON-NLS-1$
	public static final String REG_KEY_EXPORT_MEDIA = "ExportMedia"; //$NON-NLS-1$
	public static final String REG_KEY_VERSION = "Version"; //$NON-NLS-1$

	public static final String COMMAND_CREATE = "/createctx"; //$NON-NLS-1$
	public static final String COMMAND_UPLOAD = "/uploadpda"; //$NON-NLS-1$
	public static final String COMMAND_DOWNLOAD = "/downloadpda"; //$NON-NLS-1$
	public static final String COMMAND_DATAFILE = "/datafile"; //$NON-NLS-1$
	public static final String COMMAND_EXPORT = "/exportxml"; //$NON-NLS-1$
	public static final String COMMAND_SILENT = "/silent"; //$NON-NLS-1$

	
	public static final int UPLOAD_CODE_CONNECT_FAIL = 100; //database connection failure
	public static final int UPLOAD_CODE_IMPORT_FAIL = 101; //database import failure
	public static final int UPLOAD_CODE_APP_NOT_FOUND = 110; //No applications found
	public static final int UPLOAD_CODE_SYNC_FAIL = 120; //Failed to Sync
	public static final int UPLOAD_CODE_CT_NOT_INSTALLED = 201; //CyberTracker was not installed on the device, so an install was triggered
	public static final int UPLOAD_CODE_SUCCESS = 200; //The application was successfully installed on the device

	public static final int DOWNLOAD_CODE_NO_CONNECTION = 300; //No connections made
	public static final int DOWNLOAD_CODE_NO_DATA = 301; //Connections, but no data
	public static final int DOWNLOAD_CODE_SUCCESS = 302; //Connections and data downloaded
	
	public static final String CT_DATE_FORMAT = "MM/dd/yy"; //$NON-NLS-1$

	public static final String STR_TRUE = "True"; //$NON-NLS-1$
	public static final String STR_FALSE = "False"; //$NON-NLS-1$
	
	public static final int MAX_TEXT_ATTRIBUTE_LENGTH = 1024;

	public static final String ATTRIBUTE_DEFAULT_VALUES_SEPATATOR = ";"; //$NON-NLS-1$
	
//    <E I="{4764F5E6-15A1-48BF-808A-F673ED7CDCDA}" N="Date" Static="1"/>
//    <E I="{EB86279A-E032-43D2-B4A8-8B8B2892B10E}" N="Time" Static="1"/>
//    <E I="{F6636A2D-2E9A-474D-A77D-2E1BAE1EF857}" N="Latitude" Static="1"/>
//    <E I="{D93E86A8-2629-44FE-AFC4-2EC510FB1820}" N="Longitude" Static="1"/>
//    <E I="{25265B91-E12F-42EC-9159-A4BF133313E9}" N="Original latitude" Static="1"/>
//    <E I="{43C87308-BA10-4BA9-B603-94A2A01D466A}" N="Original longitude" Static="1"/>
//    <E I="{3E2DF3A5-4D01-47B9-8EC6-A31083A3BC18}" N="Altitude" Static="1"/>
//    <E I="{4FB2C6B9-08DD-47B9-93F1-AF5FA8EE63C4}" N="Accuracy" Static="1"/>
//    <E I="{F5E5552F-7B23-4F0D-8154-77B5FA49E96C}" N="Speed" Static="1"/>
//    <E I="{ED2AA133-5768-46F7-A5A9-C3A16FBC79FA}" N="Heading" Static="1"/>
//    <E I="{82D16C8E-776E-4E8B-A459-6EBF62E50076}" N="Photo" Static="1"/>
//    <E I="{BA3E0B6B-7E07-47DB-AC2D-FC46831CD8A7}" N="Sound" Static="1"/>
//    <E I="{7662336F-D7B2-4962-BC7A-9CC237DA2EAA}" N="User" Static="1"/>
//    <E I="{E3948BC5-B998-47F0-AE0E-DD954B503F81}" N="RangeFinder id" Static="1"/>
//    <E I="{FF625BA3-316A-490B-81A6-D961C68EA971}" N="Range" Static="1"/>
//    <E I="{F643AE74-BDB1-4B5C-A76F-CB7951C89DFC}" N="Range units" Static="1"/>
//    <E I="{1680E1E3-442A-4DFF-9E72-0D3E166F8127}" N="Bearing" Static="1"/>
//    <E I="{D69380AB-AFF5-488B-9BC1-4C4C5D8E96E4}" N="Bearing mode" Static="1"/>
//    <E I="{013923D9-958D-43FD-9808-7683DA6ABF7B}" N="Polar inclination" Static="1"/>
//    <E I="{8D5DDC83-F1EC-45F1-9E21-DEEF2A2C985E}" N="Polar roll" Static="1"/>
//    <E I="{FBDF7B37-4D9C-4C42-AA2A-6301E1BCF850}" N="Azimuth" Static="1"/>
//    <E I="{457A5D9D-0A09-44B2-8711-A3AF82684E1B}" N="Inclination" Static="1"/>
//    <E I="{E0A63989-B292-43BC-81FC-FB73B7288BDC}" N="Status" Static="1"/>
//    <E I="{52D68278-9D2E-4D23-A4C6-FA5437182D9B}" N="Id" Static="1"/>
//    <E I="{A0114ECC-3B0E-4FAE-9866-2381F8764D6E}" N="DeviceId" Static="1"/>
//    <E I="{96F17B05-7881-4910-90CC-1B53CEEA091F}" N="Transect length" Static="1"/>
//    <E I="{782B44A8-B33B-4471-9134-320584E4EFD3}" N="Transect time" Static="1"/>
//    <E I="{6B9C0CCF-BA3B-494E-9EEA-AAA3533E1E30}" N="Transect perpendicular distance" Static="1"/>
//    <E I="{A8A8BA54-D663-43ED-A330-8B6E675B8EAA}" N="Transect marker" Static="1"/>
//    <E I="{F6A9AB47-A1B9-45D3-8775-D162476195E0}" N="Source" Static="1"/>
	

	public static final String ID = "{52D68278-9D2E-4D23-A4C6-FA5437182D9B}"; //$NON-NLS-1$
	public static final String DEVICE_ID = "{A0114ECC-3B0E-4FAE-9866-2381F8764D6E}"; //$NON-NLS-1$
	public static final String DATE = "{4764F5E6-15A1-48BF-808A-F673ED7CDCDA}"; //$NON-NLS-1$
	public static final String TIME = "{EB86279A-E032-43D2-B4A8-8B8B2892B10E}"; //$NON-NLS-1$
	public static final String LATITUDE = "{F6636A2D-2E9A-474D-A77D-2E1BAE1EF857}"; //$NON-NLS-1$
	public static final String LONGITUDE = "{D93E86A8-2629-44FE-AFC4-2EC510FB1820}"; //$NON-NLS-1$
	public static final String PHOTO = "{82D16C8E-776E-4E8B-A459-6EBF62E50076}"; //$NON-NLS-1$
}
