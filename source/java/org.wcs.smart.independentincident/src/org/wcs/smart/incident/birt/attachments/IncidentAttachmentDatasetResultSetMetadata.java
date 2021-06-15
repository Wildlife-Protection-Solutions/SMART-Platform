/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.incident.birt.attachments;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.impl.Blob;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.birt.SmartIncidentDriver;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;

/**
 * Incident attachment results set metadata
 * 
 * @author Emily
 *
 */
public class IncidentAttachmentDatasetResultSetMetadata implements IResultSetMetaData {

	public enum Column {

		UUID (Messages.IncidentAttachmentDatasetResultSetMetadata_attachmentuuidcolumnname, "attachment:uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		WP_UUID(Messages.IncidentAttachmentDatasetResultSetMetadata_waypointuuidcolumnname, "wp:uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		OBS_UUID(Messages.IncidentAttachmentDatasetResultSetMetadata_obsuuidcolumnname, "obs:uuid", java.sql.Types.VARCHAR),  //$NON-NLS-1$
		SIGNATURETYPEKEY(Messages.IncidentAttachmentDatasetResultSetMetadata_signaturekeycolumnname, "signaturetype:key", java.sql.Types.VARCHAR), //$NON-NLS-1$
		SIGNATURETYPENAME(Messages.IncidentAttachmentDatasetResultSetMetadata_signaturenamecolumnname, "signaturetype:name", java.sql.Types.VARCHAR), //$NON-NLS-1$
		FILENAME(Messages.IncidentAttachmentDatasetResultSetMetadata_filenamecolumnname, "attachment:filename", java.sql.Types.VARCHAR), //$NON-NLS-1$
		DATA(Messages.IncidentAttachmentDatasetResultSetMetadata_datacolumnname, "attachment:data", java.sql.Types.BLOB); //$NON-NLS-1$
		
		public String name;
		public String key;
		public int type;
		
		private Column(String name, String key, int type){
			this.name = name;
			this.key = key;
			this.type = type;
		}
		
		public Object getValue(ISmartAttachment wa){
			if (wa instanceof WaypointAttachment) return getValueInternal ((WaypointAttachment)wa);
			if (wa instanceof ObservationAttachment) return getValueInternal ((ObservationAttachment)wa);
			return null;
		}
		
		private Object getValueInternal(WaypointAttachment wa){
			switch(this){
			case DATA: return getData(wa);
			case FILENAME: return wa.getAttachmentFile().getFileName().toString();
			case OBS_UUID: return null;
			case SIGNATURETYPEKEY: 
				if (wa.getSignatureType() == null) return null;
				return wa.getSignatureType().getKeyId();
			case SIGNATURETYPENAME:
				if (wa.getSignatureType() == null) return null;
				return wa.getSignatureType().getName();
			case UUID: return String.valueOf(wa.getUuid());
			case WP_UUID: return String.valueOf(wa.getWaypoint().getUuid());			
			}
			return null;
		}
		
		private Object getValueInternal(ObservationAttachment wa){
			switch(this){
			case DATA: return getData(wa);
			case FILENAME: return wa.getAttachmentFile().getFileName().toString();
			case OBS_UUID: return String.valueOf(wa.getObservation().getUuid());
			case SIGNATURETYPEKEY: return null;
			case SIGNATURETYPENAME: return null;
			case UUID: return String.valueOf(wa.getUuid());
			case WP_UUID: return String.valueOf(wa.getObservation().getWaypoint().getUuid());
			}
			return null;
		}
		
		public Object getData(ISmartAttachment att) {
			try {
				Path tempImage = EncryptUtils.decryptAttachment(att);
				
				try(InputStream is = Files.newInputStream(tempImage)){
					return new Blob(is.readAllBytes());
				}finally {
					Files.delete(tempImage);
				}
			}catch (Exception ex) {
				IncidentPlugIn.displayLog(ex.getMessage(), ex);
				return null;
			}
		}
	}
	
	public IncidentAttachmentDatasetResultSetMetadata(){
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		return Column.values().length;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnDisplayLength(int)
	 */
	@Override
	public int getColumnDisplayLength(int arg0) throws OdaException {
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnLabel(int)
	 */
	@Override
	public String getColumnLabel(int index) throws OdaException {
		return Column.values()[index-1].name;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		return Column.values()[index-1].key;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType(int)
	 */
	@Override
	public int getColumnType(int index) throws OdaException {
		return Column.values()[index-1].type;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	@Override
	public String getColumnTypeName(int index) throws OdaException {
		 int nativeTypeCode = getColumnType( index );
	     return SmartIncidentDriver.getNativeDataTypeName( nativeTypeCode, IncidentAttachmentDataset.DATASET_TYPE );
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getPrecision(int)
	 */
	@Override
	public int getPrecision(int arg0) throws OdaException {
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getScale(int)
	 */
	@Override
	public int getScale(int arg0) throws OdaException {
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#isNullable(int)
	 */
	@Override
	public int isNullable(int arg0) throws OdaException {
		return columnNullableUnknown;
	}

}