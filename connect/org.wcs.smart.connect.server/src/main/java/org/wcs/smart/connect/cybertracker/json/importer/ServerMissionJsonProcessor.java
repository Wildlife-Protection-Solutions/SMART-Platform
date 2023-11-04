package org.wcs.smart.connect.cybertracker.json.importer;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.survey.json.MissionJsonProcessor;

public class ServerMissionJsonProcessor extends MissionJsonProcessor {

	private final Logger logger = Logger.getLogger(ServerMissionJsonProcessor.class.getName());

	public ServerMissionJsonProcessor(ConservationArea ca) {
		super(ca);
	}

	@Override
	protected void logException(String message, Exception ex) {
		logger.log(Level.SEVERE, message, ex.getMessage());		
	}

	
}
