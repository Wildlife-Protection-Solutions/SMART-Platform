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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModelMergeAndUpdater;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.CmSmartToXml;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelToXmlConverter;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelXmlToSimpleDataModelConverter;
import org.wcs.smart.internal.ca.datamodel.xml.XmlSmartDataModelManager;
import org.wcs.smart.util.UuidUtils;

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
	
	
	@GET
	@Path("/metadata/datamodel/{cauuid}")
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
				
				DataModelToXmlConverter converter = new DataModelToXmlConverter();
				org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml = converter.convert(caDataModel);
				
				StreamingOutput stream = new StreamingOutput() {
					@Override
					public void write(OutputStream output) throws IOException {
						try {
							XmlSmartDataModelManager.writeDataModel(xml, output);
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
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid Conservation Area UUID", ex); //$NON-NLS-1$
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
		InputStream inputStream = dataModelFilesParts.get(0).getBody(InputStream.class,null);
		byte [] bytes = IOUtils.toByteArray(inputStream);
		String dmXml = new String(bytes, StandardCharsets.UTF_8);
		
		String conservationAreas = conservationAreaParts.get(0).getBodyAsString();
		String[] cabits = conservationAreas.split(","); //$NON-NLS-1$
		
		
		SimpleDataModel sdm = null;

		List<String> allWarnings = new ArrayList<>();
		
		try(Session session = HibernateManager.getSession(context)){
			session.beginTransaction();
			try {
				for (String strUuid : cabits) {
					UUID caUuid = UuidUtils.stringToUuid(strUuid);
				
					ConservationArea ca = session.get(ConservationArea.class, caUuid);
					if (ca == null) {
						throw new SmartConnectException(Status.BAD_REQUEST, MessageFormat.format(Messages.getString("DataModelApi_CaIdError", SmartUtils.getRequestLocale(request)), ca)); //$NON-NLS-1$
					}

					List<Icon> cmIcons = QueryFactory.buildQuery(session, Icon.class, new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
					try(InputStream stream = new ByteArrayInputStream(dmXml.getBytes(StandardCharsets.UTF_8.name()))){
						DataModelXmlToSimpleDataModelConverter cc = new DataModelXmlToSimpleDataModelConverter();
						sdm = cc.convert(stream, cmIcons, Locale.getDefault());
					}catch (Exception ex) {
						logger.log(Level.SEVERE, ex.getMessage(), ex);
						throw new SmartConnectException(Status.BAD_REQUEST, MessageFormat.format(Messages.getString("DataModelApi_ReadError", SmartUtils.getRequestLocale(request)), ex.getMessage()), ex) ; //$NON-NLS-1$
					}
					
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
					
					SimpleDataModel targetDm = new SimpleDataModel(ca, rootCategories, attribute);
					
					
					DataModelMergeAndUpdater merger = new DataModelMergeAndUpdater(targetDm, sdm, SmartUtils.getRequestLocale(request));
					List<String> warnings = merger.merge(new NullProgressMonitor());
					allWarnings.addAll(warnings);
					
					//add any new objects that are not saved via relationships
					for (Attribute a : targetDm.getAttributes()){
						if (a.getUuid() == null){
							session.save(a);
						}
					}
					for (Category cat : targetDm.getCategories()){
						if (cat.getUuid() == null){
							session.save(cat);
						}
					}
					session.flush();
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, Messages.getString("DataModelApi_MergeError", SmartUtils.getRequestLocale(request)) +ex.getMessage(), ex); //$NON-NLS-1$
			}
		}
		return allWarnings;
	}
	
	
	
	@GET
	@Path("/metadata/configurablemodel/{modeluuid}")
	@Produces({ MediaType.APPLICATION_XML })
	public Response getConfigurableModel(
			@Parameter(description="the configurable model uuid") @PathParam("modeluuid") String uuid) {
	
		UUID cmUuid = parseUuid(uuid);
		
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
				    
				
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
				throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, e);
			}finally {
				s.getTransaction().commit();
			}
		}
		
	}
}
