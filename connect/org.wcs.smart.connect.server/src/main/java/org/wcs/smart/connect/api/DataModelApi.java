/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.api;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModelMergeAndUpdater;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.AttachmentInterceptor;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.CmSmartToXml;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelToXmlConverter;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelToXmlConverter.IconOption;
import org.wcs.smart.internal.ca.datamodel.xml.XmlDataModelImporter;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v11.DataModel;
import org.wcs.smart.util.UuidUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;


/**
 * Data model API.  Currently provides the ability
 * to merge new items from a datamodel xml file into one
 * or more Conservation Area datamodels and viewing data model xml.
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR )
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class DataModelApi extends HttpServlet{

	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(DataModelApi.class.getName());
	
	@Context private ServletContext context;
	@Context private HttpHeaders headers;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;
	
	@SuppressWarnings("unchecked")
	@GET
	@Operation(description = "Gets the information about the datamodel for a specific Conservation Area.")
	@Path("/metadata/datamodel/{cauuid}/info")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getDataModelInfo(
			@Parameter(description="uuid of the conservation area to get the data model info for") @PathParam("cauuid") String uuid) {
		
		UUID caUuid = parseUuid(uuid);
		
		String lastmodified = null;
		ConservationArea ca = null;
		try(Session s = HibernateManager.getSession(context)){
			s.beginTransaction();
			try {
				if (!SecurityManager.INSTANCE.canAccess(s, 
						request.getUserPrincipal().getName(), 
						CaAction.VIEWCA_KEY,
						caUuid)){
					logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to view ca."); //$NON-NLS-1$ //$NON-NLS-2$
					throw new SmartConnectException(Response.Status.UNAUTHORIZED);
				}
				
				
				ca = s.get(ConservationArea.class, caUuid);
				if (ca == null) throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("DataModelApi.CaNotFound", request.getLocale())); //$NON-NLS-1$
				
				ConservationAreaProperty prop = QueryFactory.buildQuery(s, ConservationAreaProperty.class, 
						new Object[] {"conservationArea", ca}, //$NON-NLS-1$
						new Object[] {"key", ConservationAreaProperty.CA_DM_LAST_MODIFIED_KEY}).uniqueResult(); //$NON-NLS-1$
				
				if (prop == null) {
					lastmodified = Instant.ofEpochMilli(0).toString();
				}else {
					lastmodified = prop.getValue();
				}
				
				
			}finally {
				s.getTransaction().rollback();
			}
			
		}
		JSONObject obj = new JSONObject();
		obj.put("ca_uuid", ca.getUuid().toString()); //$NON-NLS-1$
		obj.put("last_modified", lastmodified); //$NON-NLS-1$
		
		return Response.status(Response.Status.OK)
				.entity(obj.toJSONString())
				.build();
	}
	
	@GET
	@Path("/metadata/datamodel/{cauuid}")
	@Operation(description = "Gets the datamodel xml for a given conservation area.")
	@Produces({ MediaType.APPLICATION_XML })
	public Response getDataModel(
			@Parameter(description="uuid of the conservation area to get the data model for") @PathParam("cauuid") String uuid) {
		
		UUID caUuid = parseUuid(uuid);
		
		try(Session s = HibernateManager.getSession(context)){
			s.beginTransaction();
			try {
				if (!SecurityManager.INSTANCE.canAccess(s, 
						request.getUserPrincipal().getName(), 
						CaAction.VIEWCA_KEY,
						caUuid)){
					logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to view ca."); //$NON-NLS-1$ //$NON-NLS-2$
					throw new SmartConnectException(Response.Status.UNAUTHORIZED);
				}
				
				
				ConservationArea ca = s.get(ConservationArea.class, caUuid);
				if (ca == null) throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("DataModelApi.CaNotFound", request.getLocale())); //$NON-NLS-1$
					
				
				List<Attribute> attributes = QueryFactory.buildQuery(s, Attribute.class, 
						new Object[] {"conservationArea",ca}).list(); //$NON-NLS-1$
				
				List<Category> roots = QueryFactory.buildQuery(s, Category.class, 
						new Object[] {"conservationArea", ca}, //$NON-NLS-1$
						new Object[] {"parent", null}).list(); //$NON-NLS-1$
						
				SimpleDataModel caDataModel = new SimpleDataModel(ca, roots, attributes);
				
				DataModelToXmlConverter converter = new DataModelToXmlConverter(IconOption.NONE);
				DataModel xml = converter.convert(caDataModel);
				
				StreamingOutput stream = new StreamingOutput() {
					@Override
					public void write(OutputStream output) throws IOException {
						try {
							DataModelToXmlConverter.writeDataModel(xml, output);
						} catch (JAXBException e) {
							throw new IOException(e);
						}
					}
			    };
				
			    String filename = "datamodel." + uuid + ".xml"; //$NON-NLS-1$ //$NON-NLS-2$
			    
				return Response.status(Response.Status.PARTIAL_CONTENT)
						.entity(stream)
						.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"") //$NON-NLS-1$ //$NON-NLS-2$
						.build();
			}finally {
				s.getTransaction().commit();
			}
		}
		
	}
	
	private UUID parseUuid(String uuid) throws SmartConnectException{
		UUID itemUuid = null;
		try{
			itemUuid= UuidUtils.stringToUuid(uuid);
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Invalid uuid: " + uuid + ". " + ex.getMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid UUID", ex); //$NON-NLS-1$
		}
		return itemUuid;
	}
	
	
	/**
	 * <p>Uploads data to server</p>
	 * <p>
	 * URL: .../server/api/ca/datamodel<br>
	 * Call Type: POST<br>
	 * 
	 * @param input	MultipartFormDataInput containing a "dm_file" component which is
	 * the data model xml file in utf-8 encoding, and a "conservation_areas" component which is
	 * a comma delimited list of Conservation Area Uuids to update.
	 * 
	 * @return Response
	 * @throws Exception FileNotFound is possible if the upload fails for some reason.
	 */
	@POST
	@Path("/ca/datamodel")
	@Consumes("multipart/form-data")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<String> updateFilePost(MultipartFormDataInput input) throws Exception{
		Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
		List<InputPart> dataModelFilesParts = uploadForm.get("dm_file"); //$NON-NLS-1$
		List<InputPart> conservationAreaParts = uploadForm.get("conservation_areas"); //$NON-NLS-1$

		if (dataModelFilesParts.size() != 1) {
			throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("DataModelApi_DataModelFileRequest", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		if (conservationAreaParts.size() != 1) {
			throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("DataModelApi_CaRequired", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}

		//read data model as utf-8 file
		List<String> allWarnings = new ArrayList<>();
		
		java.nio.file.Path workingDirectory = Files.createTempDirectory("smartdm"); //$NON-NLS-1$
		try {
			//write input stream to local file
			String extension = ".xml"; //$NON-NLS-1$
			if (dataModelFilesParts.get(0).getMediaType().toString().contains("zip")) { //$NON-NLS-1$
				extension = ".zip"; //$NON-NLS-1$
			}
			java.nio.file.Path dmfile = workingDirectory.resolve("dmfile" + extension); //$NON-NLS-1$
			try(InputStream inputStream = dataModelFilesParts.get(0).getBody(InputStream.class,null)){
				Files.copy(inputStream, dmfile);	
			}
			
			String conservationAreas = conservationAreaParts.get(0).getBodyAsString();
			String[] cabits = conservationAreas.split(","); //$NON-NLS-1$
			
			
			SimpleDataModel sdm = null;
			
			try(Session session = HibernateManager.getSession(context, request.getLocale(), new AttachmentInterceptor())){
				session.beginTransaction();
				try {
					for (String strUuid : cabits) {
						UUID caUuid = UuidUtils.stringToUuid(strUuid);
					
						ConservationArea ca = session.get(ConservationArea.class, caUuid);
						if (ca == null) {
							throw new SmartConnectException(Status.BAD_REQUEST, MessageFormat.format(Messages.getString("DataModelApi_CaIdError", SmartUtils.getRequestLocale(request)), ca)); //$NON-NLS-1$
						}
	
						List<Icon> cmIcons = QueryFactory.buildQuery(session, Icon.class, new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
						List<IconSet> cmSets = QueryFactory.buildQuery(session, IconSet.class, new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
						
						java.nio.file.Path localTemp = workingDirectory.resolve(ca.getUuid().toString());
						XmlDataModelImporter importer = new XmlDataModelImporter(cmIcons, cmSets,  request.getLocale(), localTemp);
						importer.processFile(dmfile);
						sdm = importer.getImportedDataModel();
										
						CriteriaBuilder cb = session.getCriteriaBuilder();
						CriteriaQuery<Category> c = cb.createQuery(Category.class);
						Root<Category> root = c.from(Category.class);
						c.where(cb.and(
								cb.equal(root.get("conservationArea"), ca), //$NON-NLS-1$
								cb.isNull(root.get("parent")) //$NON-NLS-1$
								));
						c.orderBy(cb.asc(root.get("categoryOrder"))); //$NON-NLS-1$
						List<Category> rootCategories = session.createQuery(c).getResultList();
									
						List<Attribute> attribute = QueryFactory.buildQuery(session, Attribute.class, 
								new Object[] {"conservationArea", ca}).getResultList(); //$NON-NLS-1$
						
						//we need to load and cache all attribute details here otherwise
						//auto flush flushes data when loading which causes hibernate errors
						rootCategories.forEach(rc->rc.accept(cat->{
							cat.getNames().size();
							return true;
						}));
						attribute.forEach(ra->{
							ra.getNames().size();
							if (ra.getAttributeList() != null) ra.getAttributeList().forEach(li->li.getNames().size());
							if (ra.getTree() != null) {
								ra.getTree().forEach(node->
									node.accept(v->{
										v.getNames().size();
										return true;
									})
								);
							}
						});
						
						SimpleDataModel targetDm = new SimpleDataModel(ca, rootCategories, attribute);
						
						DataModelMergeAndUpdater merger = new DataModelMergeAndUpdater(targetDm, sdm, SmartUtils.getRequestLocale(request));
						List<String> warnings = merger.merge(session, new NullProgressMonitor());
						allWarnings.addAll(warnings);
						
						//add any new objects that are not saved via relationships
						for (Attribute a : targetDm.getAttributes()){
							updateAndSaveIcon(a, a.getConservationArea(), session);
							if (a.getAttributeList() != null) a.getAttributeList().forEach(li->updateAndSaveIcon(li, a.getConservationArea(), session));
							if (a.getTree() != null)a.getTree().forEach(node->node.accept(v->{
								updateAndSaveIcon(v, a.getConservationArea(), session);
								return true;
							}));
							if (a.getUuid() == null){
								session.save(a);
							}
						}
						for (Category cat : targetDm.getCategories()){
							cat.accept(cd->{
								updateAndSaveIcon(cd, cd.getConservationArea(), session);
								return true;
							});
							if (cat.getUuid() == null){
								session.save(cat);
							}
						}
						
						//update the last modified date
						ConservationAreaProperty prop = QueryFactory.buildQuery(session, ConservationAreaProperty.class, 
								new Object[] {"conservationArea", ca}, //$NON-NLS-1$
								new Object[] {"key", ConservationAreaProperty.CA_DM_LAST_MODIFIED_KEY}).uniqueResult(); //$NON-NLS-1$
						
						if (prop == null) {
							prop = new ConservationAreaProperty();
							prop.setConservationArea(ca);
							prop.setKey(ConservationAreaProperty.CA_DM_LAST_MODIFIED_KEY);
							
							session.persist(prop);
						}
						
						prop.setValue(Instant.now().toString());
					}
					session.getTransaction().commit();
				}catch (Exception ex) {
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					if (session.getTransaction().isActive()) session.getTransaction().rollback();
					throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, Messages.getString("DataModelApi_MergeError", SmartUtils.getRequestLocale(request)) +ex.getMessage(), ex); //$NON-NLS-1$
				}
			}
		}finally{
			if (Files.exists(workingDirectory)) FileUtils.deleteDirectory(workingDirectory.toFile());
		}
		return allWarnings;
	}
	
	private void updateAndSaveIcon(DmObject object, ConservationArea ca, Session session) {
		if (object.getIcon() == null) return;
		object.getIcon().setConservationArea(ca);
		if (object.getIcon().getUuid() == null) {
			session.save(object.getIcon());
		}
		
	}
	
	
	@GET
	@Path("/metadata/configurablemodel/{modeluuid}")
	@Produces({ MediaType.APPLICATION_XML })
	public Response getConfigurableModel(
			@Parameter(description="the configurable model uuid") @PathParam("modeluuid") String uuid,
			@Parameter(description="if supplied format can be one of xml (default - returns only xml file) or zip (includes icons in zip file) (optional)") @QueryParam("format") String format) {
	
		UUID cmUuid = parseUuid(uuid);
		
		if (format != null) {
			format = format.toLowerCase();
		}else {
			format = "xml"; //$NON-NLS-1$
		}
				
		try(Session s = HibernateManager.getSession(context)){
			s.beginTransaction();
			try {
				ConfigurableModel model = s.get(ConfigurableModel.class, cmUuid);
				if (model == null) throw new SmartConnectException(Response.Status.NOT_FOUND, "Invalid configurable model uuid."); //$NON-NLS-1$
					
				if (!SecurityManager.INSTANCE.canAccess(s, 
						request.getUserPrincipal().getName(), 
						CaAction.VIEWCA_KEY,
						model.getConservationArea().getUuid())){
					logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to view ca."); //$NON-NLS-1$ //$NON-NLS-2$
					throw new SmartConnectException(Response.Status.UNAUTHORIZED);
				}
				
				CmSmartToXml converter = new CmSmartToXml(s);
				
				converter.convert(model, new NullProgressMonitor());
				if (format.equals("zip")) { //$NON-NLS-1$
					java.nio.file.Path zipFile = Files.createTempFile("configurablemodel", "zip"); //$NON-NLS-1$ //$NON-NLS-2$
	
					try (ZipArchiveOutputStream tOut = new ZipArchiveOutputStream(
							new BufferedOutputStream(Files.newOutputStream(zipFile)))) {
						ZipArchiveEntry zipEntry = new ZipArchiveEntry("configurablemodel.xml"); //$NON-NLS-1$
						tOut.putArchiveEntry(zipEntry);
						CmXmlManager.writeDataModel(converter.getXmlModel(), tOut);
						tOut.closeArchiveEntry();
	
						for (Entry<String, java.nio.file.Path> include : converter.getReferencedFiles().entrySet()) {
							zipEntry = new ZipArchiveEntry(include.getKey());
							tOut.putArchiveEntry(zipEntry);
							try (InputStream is = Files.newInputStream(include.getValue())) {
								IOUtils.copy(is, tOut);
							}
							tOut.closeArchiveEntry();
						}
	
					} catch (Exception ex) {
						throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, ex);
					}
	
					StreamingOutput stream = new StreamingOutput() {
						@Override
						public void write(OutputStream output) throws IOException {
							IOUtils.copy(Files.newInputStream(zipFile), output);
	
							Files.delete(zipFile);
	
						}
					};
					String filename = "cm." + uuid + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$
	
					return Response.status(Response.Status.PARTIAL_CONTENT).entity(stream)
							.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
							.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"") //$NON-NLS-1$ //$NON-NLS-2$
							.build();
				}
				
				StreamingOutput stream = new StreamingOutput() {
					@Override
					public void write(OutputStream output) throws IOException {
						try {
							CmXmlManager.writeDataModel(converter.getXmlModel(), output);
						} catch (JAXBException e) {
							throw new IOException(e);
						}
					}
				};
				String filename = "cm." + uuid + ".xml"; //$NON-NLS-1$ //$NON-NLS-2$

				return Response.status(Response.Status.PARTIAL_CONTENT).entity(stream)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"") //$NON-NLS-1$ //$NON-NLS-2$
					.build();
				    
				
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
				throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, e);
			}finally {
				s.getTransaction().commit();
			}
		}
		
	}
	
	/**
	 * returns a list of configurable models in the system
	 * for all Conservation Areas which the user has view access to or for the
	 * specific conservation area provided in the query
	 * 
	 * @param uuid
	 * @return
	 */
	@GET
	@Path("/metadata/configurablemodel")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<ConfigurableModelProxy> getConfigurableModels(			
			@Parameter(description="query by conservation area (optional)") @QueryParam("ca_uuid") String querycauuid) {
	
		UUID caUuid = null;
		if (querycauuid != null) {
			caUuid = parseUuid(querycauuid);
		}
		
		try(Session s = HibernateManager.getSession(context)){
			s.beginTransaction();
			try {
				List<UUID> read = new ArrayList<>();
				if (caUuid != null) {
					if (SecurityManager.INSTANCE.canAccess(s, 
							request.getUserPrincipal().getName(), CaAction.VIEWCA_KEY, caUuid)){
						read.add(caUuid);
					}
				}else {
					List<ConservationArea> cas = QueryFactory.buildQuery(s, ConservationArea.class).list();
					
					for (ConservationArea ca : cas) {
						if (SecurityManager.INSTANCE.canAccess(s, 
								request.getUserPrincipal().getName(), CaAction.VIEWCA_KEY, ca.getUuid())){
							read.add(ca.getUuid());
						}
					}
				}
				if (read.isEmpty()) return Collections.emptyList();
				
				List<ConfigurableModel> cms = s.createQuery("FROM ConfigurableModel WHERE conservationArea.uuid IN (:cauuids)", ConfigurableModel.class) //$NON-NLS-1$
						.setParameterList("cauuids", read) //$NON-NLS-1$
						.list();
				
				List<ConfigurableModelProxy> values = new ArrayList<>();
				for (ConfigurableModel cm : cms) {
					values.add(new ConfigurableModelProxy(cm));
				}
				return values;
				
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
				throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, e);
			}finally {
				s.getTransaction().commit();
			}
		}
		
	}
	
	class ConfigurableModelProxy {
		UUID uuid;
		UUID caUuid;
		String caName;
		String caId;
		String name;
		boolean useEarthRanger;
		
		List<Translation> translations;
		
		public ConfigurableModelProxy(ConfigurableModel cm) {
			this.uuid = cm.getUuid();
			this.caUuid = cm.getConservationArea().getUuid();
			this.caName = cm.getConservationArea().getName();
			this.caId = cm.getConservationArea().getId();
			this.name = cm.getName();
			this.useEarthRanger = cm.getUseEarthRanger();
			this.translations= new ArrayList<>();
			for (Label l : cm.getNames()) {
				this.translations.add(new Translation(l));
			}
			
		}
		
		public UUID getUuid() {
			return this.uuid;
		}
		public String getName() {
			return this.name;
		}
		
		@JsonProperty("ca_uuid")
		public UUID getConservationAreaUuid() {
			return this.caUuid;
		}
		@JsonProperty("ca_name")
		public String getConservationAreaName() {
			return this.caName;
		}
		@JsonProperty("ca_id")
		public String getConservationAreaId() {
			return this.caId;
		}
		
		@JsonProperty("use_with_earth_ranger")
		public Boolean getUseEarthRanger() {
			return this.useEarthRanger;
		}
		public List<Translation> getTranslations(){
			return this.translations;
		}
		
		class Translation{
			
			String languageCode;
			String value;
			
			public Translation(Label label) {
				this.languageCode = label.getLanguage().getCode();
				this.value = label.getValue();
			}
			
			@JsonProperty("language_code")
			public String getLanguageCode() {
				return this.languageCode;
			}
			public String getValue() {
				return this.value;
			}
		}
	}
	

}
