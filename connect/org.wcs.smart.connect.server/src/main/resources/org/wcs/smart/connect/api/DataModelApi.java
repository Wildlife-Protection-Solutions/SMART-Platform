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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

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
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelXmlToSimpleDataModelConverter;
import org.wcs.smart.util.UuidUtils;

@Path(ConnectRESTApplication.PATH_SEPERATOR + DataModelApi.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
/**
 * Data model maintenance API.  Currently provides the ability
 * to merge new items from a datamodel xml file into one
 * or more Conservation Area datamodels.
 * 
 * @author Emily
 *
 */
public class DataModelApi extends HttpServlet{

	public static final String PATH = "ca/datamodel"; //$NON-NLS-1$
	
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(DataModelApi.class.getName());
	
	@Context private ServletContext context;
	@Context private HttpHeaders headers;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;
	
	
	/**
	 * Uploads data to server via POST
	 * URL: .../server/uploader/{uploaduuid}
	 * 
	 * @param uploaduuid	provided in the URL, uuid of the workItem this file upload belongs to.
	 * @param input	MultipartFormDataInput containing a "dm_file" component which is
	 * the data model xml file in utf-8 encoding, and a "conservation_areas" component which is
	 * a comma delimited list of Conservation Area Uuids to update.
	 * 
	 * @return Response
	 * @throws Exception FileNotFound is possible if the upload fails for some reason.
	 */
	@POST
	@Path("/")
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
		
		DataModelXmlToSimpleDataModelConverter cc = new DataModelXmlToSimpleDataModelConverter();
		
		SimpleDataModel sdm = null;
		try(InputStream stream = new ByteArrayInputStream(dmXml.getBytes(StandardCharsets.UTF_8.name()))){
			sdm = cc.convert(stream, Locale.getDefault());
		}catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Status.BAD_REQUEST, MessageFormat.format(Messages.getString("DataModelApi_ReadError", SmartUtils.getRequestLocale(request)), ex.getMessage()), ex) ; //$NON-NLS-1$
		}
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
}
