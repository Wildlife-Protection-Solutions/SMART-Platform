package org.wcs.smart.entity.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.wcs.smart.entity.xml.model.EntityType;
import org.wcs.smart.entity.xml.model.ObjectFactory;

public class EntityTypeXmlManager {
	
	private static final String METADATA_CLASSES_PACKAGE = "org.wcs.smart.entity.xml.model"; //$NON-NLS-1$
		
	/**
	 * Reads patrol data from an xml file.
	 * <p>
	 * User is required to close input stream.
	 * </p>
	 * 
	 * @param file input stream to read patrol data from
	 * @return
	 * @throws JAXBException
	 */
	public static EntityType readDataModel(InputStream file) throws JAXBException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Unmarshaller un = context.createUnmarshaller();	
		@SuppressWarnings("unchecked")
		JAXBElement<EntityType> o = (JAXBElement<EntityType>) un.unmarshal(file);
		EntityType x = o.getValue();
		return x;
	}
	
	/**
	 * Writes a xml patrol object to a file.
	 * <p>
	 * User is required to close output stream.
	 * </p>
	 * @param patrol xml patrol to write
	 * @param file output stream 
	 * @throws JAXBException
	 * @throws IOException
	 */
	public static void writeDataModel(EntityType entityType, OutputStream file) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		
		ObjectFactory objFactor = new ObjectFactory();
		
		JAXBElement<EntityType> element = objFactor.createEntityType(entityType);
		marshaller.marshal(element, file);
	}
}
