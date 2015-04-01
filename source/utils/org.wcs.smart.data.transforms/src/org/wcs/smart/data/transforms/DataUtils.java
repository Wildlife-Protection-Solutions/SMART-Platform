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

public class DataUtils {
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
	
	public static void processConfiguration(String args[], IDataProcessor processor){
		processConfiguration(args, processor, true);
	}
	
	public static void processConfiguration(String args[], IDataProcessor processor, boolean printInfo){
		if (args.length < 2){
			System.out.println("Invalid usage.  Must provide both input and output file");
			System.exit(1);
		}
		File f1 = new File(args[0]);
		if (!f1.exists()){
			System.out.println("File does not exist. " + f1.toString());
			System.exit(1);
		}
		File f2 = new File(args[1]);
		
		if(f1.isFile() && f2.isDirectory()){
			System.out.println("Input is file, output is not a file.");
			System.exit(1);
		}
		if (f1.isDirectory() && f2.isFile()){
			System.out.println("Input is directory; output is not a directory");
			System.exit(1);
		}
		
		if (f1.isFile()){
			try{
				processor.processFile(f1, f2);
				if (printInfo){
					System.out.println("Processed file wrote results to :" +f2.toString());
				}
			}catch (Exception ex){
				ex.printStackTrace();
				System.out.println("Failed to process file: " + f2.toString() + ". " + ex.getMessage());
			}
		}else if (f1.isDirectory()){
			int processed = 0;
			for (File file : f1.listFiles()){
				if (file.isFile()){
					try{
						File output = new File(f2, file.getName());
						processor.processFile(file, output);
						processed++;
						if (printInfo){
							System.out.println("Processed file " + file.getName() + " - wrote results to " + output.toString());
						}
					}catch (Exception ex){
						ex.printStackTrace();
						System.out.println("Failed to process file: " + f2.toString() + ". " + ex.getMessage());
					}
				}
			}
			System.out.println("Processed " + processed + " of " + f1.listFiles().length + " files.");
		}
		
	}
}
