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
package org.wcs.smart.i2.birt.datasource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.impl.EngineTask;
import org.eclipse.birt.report.engine.api.impl.RunAndRenderTask;
import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDataSetMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.util.manifest.DataTypeMapping;
import org.eclipse.datatools.connectivity.oda.util.manifest.ExtensionManifest;
import org.eclipse.datatools.connectivity.oda.util.manifest.ManifestExplorer;
import org.hibernate.Session;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ProjectionUtils;
import org.wcs.smart.SmartContext;
import org.wcs.smart.birt.BirtConstants;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.datamodel.CcaaDataModel;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.i2.birt.entity.EntityDataset;
import org.wcs.smart.i2.birt.entity.EntityDatasetMetadata;
import org.wcs.smart.i2.birt.entity.EntityLocationAttributeDataset;
import org.wcs.smart.i2.birt.entity.EntityLocationAttributeDatasetMetadata;
import org.wcs.smart.i2.birt.entity.attachment.EntityAttachmentDataset;
import org.wcs.smart.i2.birt.entity.attachment.EntityAttachmentDatasetMetadata;
import org.wcs.smart.i2.birt.entity.location.EntityLocationDataset;
import org.wcs.smart.i2.birt.entity.location.EntityLocationDatasetMetadata;
import org.wcs.smart.i2.birt.entity.records.EntityRecordDataset;
import org.wcs.smart.i2.birt.entity.records.EntityRecordDatasetMetadata;
import org.wcs.smart.i2.birt.entity.relation.EntityRelationDataset;
import org.wcs.smart.i2.birt.entity.relation.EntityRelationDatasetMetadata;
import org.wcs.smart.i2.birt.entity.search.EntitySearchDataset;
import org.wcs.smart.i2.birt.query.IntelQueryDataset;
import org.wcs.smart.i2.birt.record.RecordAttributeDataset;
import org.wcs.smart.i2.birt.record.RecordDataset;
import org.wcs.smart.i2.birt.record.RecordMetadata;
import org.wcs.smart.i2.birt.record.attachment.RecordAttachmentDataset;
import org.wcs.smart.i2.birt.record.entities.RecordEntityDataset;
import org.wcs.smart.i2.birt.record.location.RecordLocationDataset;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.util.GeometryUtils;

import com.ibm.icu.util.ULocale;

/**
 * BIRT Connection for intelligence datasets 
 * @author Emily
 *
 */
public abstract class AbstractIntelBirtConnection implements IConnection {
	
	/**
	 * App context locale variable
	 */
	public static final String LOCALE_CONTEXT_VAR = "org.wcs.smart.report.locale"; //$NON-NLS-1$
	/**
	 * App context projection provider variable
	 */
	public static final String PROJECTION_PROVIDER_CONTEXT_VAR = "org.wcs.smart.report.crs"; //$NON-NLS-1$

	public static enum Permission{
		ENTITY,
		RECORD,
		QUERY,
	}
	
	protected boolean m_isOpen = false;
	protected Session localSession;
	protected Map<Object,Object> appContext;
	protected List<Path> attachmentFiles;
	protected boolean skipSecurityCheck = false;
	
	private final static IProjectionProvider defaultProjectionProvider = new IProjectionProvider() {
		private Projection p; 
		@Override
		public Projection getProjection() {
			if (p == null){
				p = new Projection();
				p.setParsedCoordinateReferenceSystem(GeometryUtils.SMART_CRS);
				p.setName(GeometryUtils.SMART_CRS.getName().toString());
			}
			return p;
		}
	};
	
	public abstract CcaaDataModel getDataModel() ;
	/**
	 * Decrypt attachment to a file uri which is returned.  The uri will
	 * be absolute if the file is located within the SMART filestore or the
	 * output emitter is not html.  For HTML outside the SMART filestore we return
	 * a relative url, this enables the html to be passed around with the files.
	 * 
	 * If the output type is NOT HTML all files are deleted once the connection is closed.
	 * 
	 * @param attachment
	 * @return
	 */
	public String decryptAttachment(ISmartAttachment attachment) {
		if (attachment == null) return null;
		
		Path imageOutDir = getImageOutputDirectory();
		Path imageFile = null;
		try {
			if (imageOutDir == null) {
				imageFile = EncryptUtils.decryptAttachment(attachment);
			}else {
				imageFile = imageOutDir.resolve(attachment.getFilename());
				imageFile = EncryptUtils.uniqueFile(imageFile);
				EncryptUtils.decryptAttachment(attachment, imageFile);
			}
			attachmentFiles.add(imageFile);
			
			if (imageFile.startsWith(Paths.get(SmartContext.INSTANCE.getFilestoreLocation()))) {
				return imageFile.toAbsolutePath().toUri().toString();
			}else {
				if (isHtml()) {
					//relatize
					return imageOutDir.relativize(imageFile).toString();
				}else {
					return imageFile.toAbsolutePath().toUri().toString();	
				}
			}
		} catch (Exception e) {
			//we are going to eat this; likely an issue decrypting the attachment 
		}
		return null;
	}

	/**
	 * True if security checks should be skipped when loading entities. 
	 * This is used when configuring templates.
	 * @return
	 */
	public boolean skipSecurityCheck() {
		return skipSecurityCheck;
	}
	public void setSkipSecurityCheck(boolean skipSecurityCheck) {
		this.skipSecurityCheck = skipSecurityCheck;
	}
	private Path getWorkingDirectory() {
		Object x = appContext.get(BirtConstants.WORKING_DIRECTORY);
		if (x instanceof Path) return (Path)x;
		return null;
	}
	/**
	 * 
	 * @return the image output directory supplied in the app context.  Will
	 * return null if not specified
	 */
	protected Path getImageOutputDirectory() {
		Object randrtask = appContext.get("EngineTask"); //$NON-NLS-1$
		if (randrtask == null || !(randrtask instanceof EngineTask)) return getWorkingDirectory();
		EngineTask task = (EngineTask)randrtask;
		if (task.getRenderOption() == null) return getWorkingDirectory();
		Object x = task.getRenderOption().getOption(HTMLRenderOption.IMAGE_DIRECTROY);
		if (x instanceof File) {
			return ((File) x).toPath();
		}else if (x instanceof String) {
			return Paths.get((String)x);
		}else if (x instanceof Path) {
			return (Path)x;
		}
		return getWorkingDirectory();
		
	}
	
	/**
	 * If the output type is html
	 * @return
	 */
	protected boolean isHtml() {
		if (appContext == null) return false;
		Object randrtask = appContext.get("EngineTask"); //$NON-NLS-1$
		if (randrtask == null || !(randrtask instanceof RunAndRenderTask)) {
			return false;
		}else {
			RunAndRenderTask task = (RunAndRenderTask)randrtask;
			if (task.getRenderOption()!= null && task.getRenderOption().getOutputFormat().equalsIgnoreCase(HTMLRenderOption.HTML)) {
				return true;
			}else {
				return false;
			}
		}
	}
	
	/**
	 * Will clean up set of attachment files if the report system should clean them
	 * up after running.  If emitting to HTML these files should not be removed otherwise
	 * the files should be deleted after the report is generated.
	 * 
	 * @param files
	 */
	private void cleanUpAttachmentFiles(List<Path> files) {
		boolean doCleanUp = !isHtml();		
		if (!doCleanUp) return;
		for (Path p : files) {
			try {
				if (p != null ) {
					Files.delete(p);
				}
			}catch (Exception ex) {}
		}
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#open(java.util.Properties)
	 */
	public void open(Properties connProperties) throws OdaException{
		openSession();
		m_isOpen = true;
		attachmentFiles = new ArrayList<>();
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#close()
	 */
	public void close() throws OdaException {
		closeSession();
		m_isOpen = false;
		//we cannot cleanup attachments until after the report is rendered
		//if we run BIRT as separate run and render tasks in html
		if (appContext == null) return;
		Object randrtask = appContext.get("EngineTask"); //$NON-NLS-1$
		if (randrtask == null || !(randrtask instanceof EngineTask)) return;
		EngineTask task = (EngineTask)randrtask;
		if (task.getRenderOption() == null) return;
		Object x = task.getRenderOption().getOption(HTMLRenderOption.IMAGE_DIRECTROY);
		if ( x == null ) cleanUpAttachmentFiles(attachmentFiles);
			
	}

	/**
	 * Find/open hibernate database session
	 */
	public abstract void openSession();
	
	/**
	 * Close hibernate database session
	 */
	public abstract void closeSession();

	/**
	 * Get all conservation areas that should be used in the queries 
	 * 
	 * @return
	 */
	public abstract Collection<ConservationArea> getConservationAreas();

	/**
	 * Returns a set of profiles the current user has the given
	 * permission for or empty if none
	 * 
	 * @param permission
	 * @return true if user has access to permission; false otherwise
	 */
	public abstract Set<IntelProfile> hasPermission(Permission permission);
	
	
	/**
	 * Returns a set of addition parameters to supply to the query engine;
	 * 
	 */
	public Map<String,String> getAdditionalQueryParameters(){
		return Collections.emptyMap();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void setAppContext(Object context) throws OdaException {
		if (context instanceof Map){
			this.appContext = (Map<Object,Object>)context;
			this.appContext.put(AbstractIntelBirtConnection.class.getCanonicalName(), this);
		}
	}
	
	/**
	 * Gets the context locale from the appContext 
	 * variable LOCAL_CONTEXT_VAR or the default 
	 * context.
	 * @return
	 */
	public Locale getCurrentLocale(){
		if (appContext == null) return Locale.getDefault();
		Locale l = (Locale) appContext.get(LOCALE_CONTEXT_VAR);
		if (l == null) return Locale.getDefault();
		return l;
	}
	
	/**
	 * 
	 * @return the projection provider for the current app context
	 * @throws Exception
	 */
	public IProjectionProvider getProjectionProvider() throws Exception{
		IProjectionProvider value = defaultProjectionProvider;
		if (appContext != null){
			value = (IProjectionProvider) appContext.get(PROJECTION_PROVIDER_CONTEXT_VAR);	
			if (value == null){
				ConservationArea reportca = (ConservationArea) appContext.get(BirtConstants.CA_PARAM);
				if (reportca != null){
					value = ProjectionUtils.INSTANCE.createProjectionProvider(getSession(), reportca);
					appContext.put(PROJECTION_PROVIDER_CONTEXT_VAR, value);
				}
			}
		}
		return value;		
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#isOpen()
	 */
	public boolean isOpen() throws OdaException {
		return m_isOpen;
	}
	
	public Session getSession(){
		return this.localSession;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#getMetaData(java.lang.String)
	 */
	public IDataSetMetaData getMetaData(String dataSetType) throws OdaException {
		if (dataSetType.equals(EntityDataset.DATASET_TYPE)) {
			return new EntityDatasetMetadata(this);
		}else if (dataSetType.equals(EntityLocationAttributeDataset.DATASET_TYPE)) {
			return new EntityLocationAttributeDatasetMetadata(this);
		}else if (dataSetType.equals(EntityLocationDataset.DATASET_TYPE)) {
			return new EntityLocationDatasetMetadata(this);
		}else if (dataSetType.equals(EntityRecordDataset.DATASET_TYPE)) {
			return new EntityRecordDatasetMetadata(this);
		}else if (dataSetType.equals(EntityAttachmentDataset.DATASET_TYPE)){
			return new EntityAttachmentDatasetMetadata(this);
		}else if (dataSetType.equals(EntityRelationDataset.DATASET_TYPE)){
			return new EntityRelationDatasetMetadata(this);
		}else if (dataSetType.equals(RecordDataset.DATASET_TYPE)){
			return new RecordMetadata(this, dataSetType);
		}else if (dataSetType.equals(RecordAttributeDataset.DATASET_TYPE)){
			return new RecordMetadata(this, dataSetType);
		}else if (dataSetType.equals(RecordEntityDataset.DATASET_TYPE)){
			return new RecordMetadata(this, dataSetType);
		}else if (dataSetType.equals(RecordLocationDataset.DATASET_TYPE)){
			return new RecordMetadata(this, dataSetType);
		}else if (dataSetType.equals(RecordAttachmentDataset.DATASET_TYPE)){
			return new RecordMetadata(this, dataSetType);
		}else if (dataSetType.equals(IntelQueryDataset.DATASET_TYPE)){
			return new RecordMetadata(this, dataSetType);
		}else if (dataSetType.equals(EntitySearchDataset.DATASET_TYPE)){
			return new RecordMetadata(this, dataSetType);
		}
		throw new OdaException(
				MessageFormat.format("Dataset {0} not supported by SMART", //$NON-NLS-1$
						new Object[]{dataSetType}));
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#newQuery(java.lang.String)
	 */
	public IQuery newQuery(String dataSetType) throws OdaException {
		try {
			if (dataSetType.equals(EntityDataset.DATASET_TYPE)) {
				return new EntityDataset(this);
			}else if (dataSetType.equals(EntityLocationAttributeDataset.DATASET_TYPE)) {
				return new EntityLocationAttributeDataset(this);
			}else if (dataSetType.equals(EntityLocationDataset.DATASET_TYPE)) {
				return new EntityLocationDataset(this);
			}else if (dataSetType.equals(EntityRecordDataset.DATASET_TYPE)) {
				return new EntityRecordDataset(this);
			}else if (dataSetType.equals(EntityAttachmentDataset.DATASET_TYPE)){
				return new EntityAttachmentDataset(this);
			}else if (dataSetType.equals(EntityRelationDataset.DATASET_TYPE)){
				return new EntityRelationDataset(this);
			}else if (dataSetType.equals(RecordDataset.DATASET_TYPE)){
				return new RecordDataset(this);
			}else if (dataSetType.equals(RecordAttributeDataset.DATASET_TYPE)){
				return new RecordAttributeDataset(this);
			}else if (dataSetType.equals(RecordEntityDataset.DATASET_TYPE)){
				return new RecordEntityDataset(this);
			}else if (dataSetType.equals(RecordLocationDataset.DATASET_TYPE)){
				return new RecordLocationDataset(this);
			}else if (dataSetType.equals(RecordAttachmentDataset.DATASET_TYPE)){
				return new RecordAttachmentDataset(this);
			}else if (dataSetType.equals(IntelQueryDataset.DATASET_TYPE)) {
				return new IntelQueryDataset(this);
			}else if (dataSetType.equals(EntitySearchDataset.DATASET_TYPE)){
				return new EntitySearchDataset(this);
			}
			throw new OdaException(
					MessageFormat.format("Dataset {0} not supported by SMART", //$NON-NLS-1$
							new Object[]{dataSetType}));
		} catch (Exception e) {
			throw new OdaException(e);
		}
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#getMaxQueries()
	 */
	public int getMaxQueries() throws OdaException {
		return 0; // no limit
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#commit()
	 */
	public void commit() throws OdaException {
		// do nothing; assumes no transaction support needed
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#rollback()
	 */
	public void rollback() throws OdaException {
		// do nothing; assumes no transaction support needed
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#setLocale(com.ibm.icu.util.ULocale)
	 */
	public void setLocale(ULocale locale) throws OdaException {
		// do nothing; assumes no locale support
	}

	/**
	 * Returns the object that represents this extension's manifest.
	 * 
	 * @throws OdaException
	 */
	public static ExtensionManifest getManifest() throws OdaException {
		return ManifestExplorer.getInstance().getExtensionManifest(IntelBirtDataSource.ODA_DATA_SOURCE_ID);
	}

	/**
	 * Returns the native data type name of the specified code, as defined in
	 * this data source extension's manifest.
	 * 
	 * @param nativeTypeCode
	 *            the native data type code
	 * @return corresponding native data type name
	 * @throws OdaException
	 *             if lookup fails
	 */
	public static String getNativeDataTypeName(int nativeDataTypeCode, String dataSetType)
			throws OdaException {
		DataTypeMapping typeMapping = getManifest().getDataSetType(dataSetType)
				.getDataTypeMapping(nativeDataTypeCode);
		if (typeMapping != null)
			return typeMapping.getNativeType();
		return "Undefined mapping type"; //$NON-NLS-1$
	}

}