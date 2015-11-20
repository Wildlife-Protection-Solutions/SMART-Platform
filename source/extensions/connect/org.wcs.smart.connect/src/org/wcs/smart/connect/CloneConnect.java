/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.connect.model.ConnectServerOption.Option;

/**
 * Clones connect data when cloning a conservation area.  This clones:
 * 1) connect server details and certificate
 * 2) connect server options
 * 
 * @author Emily
 *
 */
public class CloneConnect implements IConservationAreaTemplateCloner {

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
		monitor.beginTask("Cloning connect server details", 1);
		try{
			Session s = engine.getSession();
			//clone server
			ConnectServer server = ConnectHibernateManager.getConnectServer(s, engine.getTemplateCa());
			if (server != null){
				ConnectServer clone = new ConnectServer();
				clone.setConservationArea(engine.getNewCa());
				clone.setServerUrl(server.getServerUrl());
				if (server.getCertificateFileName() != null){
					clone.setCertificateFile(server.getLocalCertificateFile());
				}
				//clone options
				Map<Option, ConnectServerOption> options = server.getOptions();
				clone.setOptions(new HashMap<Option, ConnectServerOption>());
				for (Entry<Option, ConnectServerOption> op : options.entrySet()){
					clone.setOption(op.getKey(), op.getValue().getValue());
				}
				s.save(clone);
				
			}
			s.flush();
		}finally{
			monitor.done();
		}
	}

}
