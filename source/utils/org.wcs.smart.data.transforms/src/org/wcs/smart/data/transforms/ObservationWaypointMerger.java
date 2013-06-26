package org.wcs.smart.data.transforms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.wcs.smart.patrol.xml.model.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolType;
import org.wcs.smart.patrol.xml.model.WaypointType;

public class ObservationWaypointMerger {

	
	private static void mergeObservation(PatrolType patrol){
		
		for (PatrolLegType leg : patrol.getLegs()){
			for (PatrolLegDayType day : leg.getDays()){
				List<WaypointType> newWaypoints = new ArrayList<WaypointType>();
				
				WaypointType lastWaypoint = null;
				
				for (WaypointType wp : day.getWaypoints()){
					if (lastWaypoint != null){
						if (wp.getX().equals(lastWaypoint.getX()) &&
							wp.getY().equals(lastWaypoint.getY()) &&
							wp.getTime().equals(lastWaypoint.getTime())){
						
							//add all of this waypoints observations to the last
							//waypoint and ignore it
							lastWaypoint.getObservations().addAll(wp.getObservations());
						}else{
							//add the waypoint to list of new aypoints
							newWaypoints.add(lastWaypoint);
							lastWaypoint = wp;
						}
					}else{
						lastWaypoint = wp;
					}
				}
				newWaypoints.add(lastWaypoint);
				
				day.getWaypoints().clear();
				day.getWaypoints().addAll(newWaypoints);
			}
		}
		
	}
	
	public static void processFile(File in, File out) throws JAXBException{
		PatrolType pt = XmlUtils.readPatrol(in);
		mergeObservation(pt);
		XmlUtils.writePatrol(out, pt);
	}
	
	
	public static void main(String args[]){
		if (args.length != 2){
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
				ObservationWaypointMerger.processFile(f1, f2);
				System.out.println("Processed file wrote results to :" +f2.toString());
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
						ObservationWaypointMerger.processFile(file, output);
						System.out.println("Processed file " + file.getName() + " - wrote results to " + output.toString());
						processed++;
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
