package org.wcs.smart.paws;

import org.wcs.smart.ILoginHandler;
import org.wcs.smart.paws.engine.PawsStartUpJob;

/**
 * Conservation Area login handler that runs the PAWS start up
 * job to validate the state of all PAWS analysis.
 * 
 * @author Emily
 *
 */
public class LoginHandler implements ILoginHandler {

	@Override
	public void onLogin() throws Exception {
		(new PawsStartUpJob()).schedule();
	}

}
