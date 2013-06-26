package org.wcs.smart.data.transforms;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import org.wcs.smart.patrol.xml.model.ObjectFactory;
import org.wcs.smart.patrol.xml.model.PatrolType;

public class XmlUtils {
	private static final String METADATA_CLASSES_PACKAGE = "org.wcs.smart.patrol.xml.model"; //$NON-NLS-1$
	
	
	public static PatrolType readPatrol(File xmlFile) throws JAXBException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Unmarshaller un = context.createUnmarshaller();	
		JAXBElement<PatrolType> o = (JAXBElement<PatrolType>) un.unmarshal(xmlFile);
		PatrolType x = o.getValue();
		return x;
	}
	
	public static void writePatrol(File xmlFile, PatrolType patrol) throws JAXBException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		
		ObjectFactory objFactor = new ObjectFactory();
		
		JAXBElement<PatrolType> element = objFactor.createPatrol(patrol);
		marshaller.marshal(element, xmlFile);
	}
}
