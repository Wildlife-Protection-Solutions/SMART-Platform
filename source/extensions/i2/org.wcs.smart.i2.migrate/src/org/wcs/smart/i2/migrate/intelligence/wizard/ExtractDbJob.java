/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.i2.migrate.intelligence.wizard;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.migrate.MigratePlugin;
import org.wcs.smart.i2.migrate.intelligence.Smart6Database;
import org.wcs.smart.i2.migrate.internal.Messages;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Job for extracting the SMART6 database from backup file.
 * 
 * @author Emily
 *
 */
public class ExtractDbJob implements IRunnableWithProgress{

	private String filename;
	
	private Smart6Database smart6db;
	private List<ConservationArea> toProcess;
	
	public ExtractDbJob(String filename) {
		this.filename = filename;
	}
	
	public Smart6Database getDatabase() {
		return this.smart6db;
	}
	
	public List<ConservationArea> getConservationAreas(){
		return this.toProcess;
	}
	
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

		SubMonitor task = SubMonitor.convert(monitor);
		task.beginTask(Messages.ExtractDbJob_TaskName, 2);
		
		Path p = Paths.get(filename);
		if (!Files.exists(p)) {
			throw new InvocationTargetException(new Exception(Messages.ExtractDbJob_FileNotFound));
		}
		
		task.split(1).beginTask(Messages.ExtractDbJob_subtask1, 1);
		Path dir = null;
		try {
			//delete when done->this gets done with closing the smart6db
			dir = ZipUtil.unzip(p);
		}catch (Exception ex) {
			throw new InvocationTargetException(ex);
		}
		
		task.split(1).beginTask(Messages.ExtractDbJob_subtask2, 1);
		try {
			Smart6Database smart6db = new Smart6Database(dir);
			try {
				if (!smart6db.validateIntelligenceVersion()){
					throw new Exception(Messages.ExtractDbJob_invalidVersion);
				}
				
				task.split(1).beginTask(Messages.ExtractDbJob_subtask3, 1);

				List<ConservationArea> s6 = smart6db.getConservationAreasWithData();
				Set<UUID> uuids6 = s6.stream().map(e->e.getUuid()).collect(Collectors.toSet());
				
				toProcess= new ArrayList<>();
				try(Session session = HibernateManager.openSession()){
					List<ConservationArea> cas = QueryFactory.buildQuery(session, ConservationArea.class).list();
					for (ConservationArea c : cas) {
						if (uuids6.contains(c.getUuid())) {
							toProcess.add(c);
						}
					}
				}
				
				
				this.smart6db = smart6db;
			}catch (Exception ex) {
				smart6db.close();
				if (dir != null && Files.exists(dir)) {
					try {
						SmartUtils.deleteDirectory(dir);
					}catch (IOException io) {
						MigratePlugin.log(io.getMessage(), io);
					}
				}
				throw ex;
			}
			
		}catch (Exception ex) {
			throw new InvocationTargetException(ex);
		}
	}

}
