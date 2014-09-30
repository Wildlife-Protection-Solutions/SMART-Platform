package org.wcs.smart.er.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.wcs.smart.er.xml.model.surveyDesign.ObjectFactory;

public class SurveyDesignXMLManager {
	
	private static final String METADATA_CLASSES_PACKAGE = "org.wcs.smart.er.xml.model.surveyDesign"; //$NON-NLS-1$
		
	/**
	 * Reads surverydesign data from an xml file.
	 * <p>
	 * User is required to close input stream.
	 * </p>
	 * 
	 * @param file input stream to read patrol data from
	 * @return
	 * @throws JAXBException
	 */
	public static org.wcs.smart.er.xml.model.surveyDesign.SurveyDesign readDataModel(InputStream file) throws JAXBException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Unmarshaller un = context.createUnmarshaller();	
		@SuppressWarnings("unchecked")
		JAXBElement<org.wcs.smart.er.xml.model.surveyDesign.SurveyDesign> o = (JAXBElement<org.wcs.smart.er.xml.model.surveyDesign.SurveyDesign>) un.unmarshal(file);
		org.wcs.smart.er.xml.model.surveyDesign.SurveyDesign x = o.getValue();
		return x;
	}
	
	/**
	 * Writes a xml surveyDesign object to a file.
	 * <p>
	 * User is required to close output stream.
	 * </p>
	 * @param surveryDesign xml surveyDesign to write
	 * @param file output stream 
	 * @throws JAXBException
	 * @throws IOException
	 */
	public static void writeDataModel(org.wcs.smart.er.xml.model.surveyDesign.SurveyDesign surveydesign, OutputStream file) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		
		ObjectFactory objFactor = new ObjectFactory();
		
		JAXBElement<org.wcs.smart.er.xml.model.surveyDesign.SurveyDesign> element = objFactor.createSurveyDesign(surveydesign);
		marshaller.marshal(element, file);
	}
}