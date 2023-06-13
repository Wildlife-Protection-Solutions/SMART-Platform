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
package org.wcs.smart.connect.datastore;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.connect.model.ChangeLogItem.Source;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.uploader.sync.ChangeLogManager;
import org.wcs.smart.util.UuidUtils;

/**
 * Watcher for managing filestore changes that need to 
 * be replicated. 
 * 
 *  ASSUMPTION:
 * We only log changes made to files in directories that whose root directory 
 * (relative to the filestore directory)
 * can be converted to a uuid.  We then assume this uuid represents the Conservation Area UUID.  
 * Plugins should NEVER make a folder in the root datastore directory that is named with
 * a value that can be converted to a UUID.  Data outside a Conservation Area directory
 * is not replicated. 
 * @author Emily
 *
 */
public class FileStoreWatcher implements Runnable {

    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;
    private final Set<Path> ignorePaths;
	
    private SessionFactory sessionFactory;
    private Set<UUID> casToIgnore = Collections.synchronizedSet(new HashSet<UUID>());
	private final Logger logger = Logger.getLogger(FileStoreWatcher.class.getName());
	
    public FileStoreWatcher(SessionFactory sessionFactory) throws IOException{
    	this.sessionFactory = sessionFactory;
    	keys = Collections.synchronizedMap(new HashMap<WatchKey, Path>());
    	watcher = FileSystems.getDefault().newWatchService();
    	ignorePaths = new HashSet<Path>();
    }
	
    /**
     * Adds a path to ignore. Can be a specific file to ignore
     * or a directory. Needs to be called before register() as it
     * will not deregister directories, if added after.
     * 
     */
    public void addIgnorePath(Path ignorePath){
    	ignorePaths.add(ignorePath);
    }

    /**
     * Register the given directory with the WatchService
     */
    private boolean registerDirectory(Path dir) throws IOException {
    	if (ignorePaths.contains(dir)) {
    		return false;
    	}
    	
    	//find ca_uuid in path; if we cannot find it then don't register this path
    	Path relativePath = FileSystems.getDefault()
    			.getPath(SmartContext.INSTANCE.getFilestoreLocation())
    			.relativize(dir);
    	try{
    		UuidUtils.stringToUuid(relativePath.getName(0).toString());
    	}catch (Exception ex){
    		//not in a ca directory so we do not replicate 
    		//System.out.println("FileStoreWatcher: Process Event: Error cannot determine CA from '" + relativePath.getName(0).toString() + "', directory not being watched"); //$NON-NLS-1$ //$NON-NLS-2$
    		logger.log(Level.INFO, "FileStoreWatcher: Process Event: Error cannot determine CA from '" + dir.toString() + "', directory not being watched"); //$NON-NLS-1$ //$NON-NLS-2$
    		return true; //we do want to check sub-directories
    	}
    	//logger.log(Level.SEVERE, "Watching directory: " + dir.toString());
        WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        keys.put(key, dir);
        return true;
    }
    
    public void ignoreCa(ConservationAreaInfo ca) {
    	casToIgnore.add(ca.getUuid());
    }
    
    public void addCa(ConservationAreaInfo ca) {
    	casToIgnore.remove(ca.getUuid());
    }
    
    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    public void register(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            	if (registerDirectory(dir)) return FileVisitResult.CONTINUE;
            	return FileVisitResult.SKIP_SUBTREE;
            }
        });
    }
    
    /**
     * Deregister all listeners and closes the watcher
     */
    public void deregister() throws IOException {
    	synchronized (keys) {
    		for (WatchKey key : keys.keySet()){
        		key.cancel();
        	}	
		}
    	watcher.close();
    }

    
    private void processEvent(Path p, Kind<?> kind){
    	if (ignorePaths.contains(p)) return;
    	
    	Path relativePath = FileSystems.getDefault()
    			.getPath(SmartContext.INSTANCE.getFilestoreLocation())
    			.relativize(p);
    	
    	UUID caUuid = null;
    	try{
    		caUuid = UuidUtils.stringToUuid(relativePath.getName(0).toString());
    	}catch (Exception ex){
    		//no in a ca directory so we do not replication
    		return;
    	}
    	if (casToIgnore.contains(caUuid)) return;
    	
    	ChangeLogItem.Action type = ChangeLogItem.Action.FS_INSERT;
    	if (kind == StandardWatchEventKinds.ENTRY_CREATE){
    		type = ChangeLogItem.Action.FS_INSERT;
    	}else if (kind == StandardWatchEventKinds.ENTRY_MODIFY){
    		type = ChangeLogItem.Action.FS_UPDATE;
    	}else if(kind == StandardWatchEventKinds.ENTRY_DELETE){
    		type = ChangeLogItem.Action.FS_DELETE;
    	}
    	
    	if (type == ChangeLogItem.Action.FS_UPDATE){
    		if (!Files.exists(p)){
    			//path has been deleted by next event skip
    			return;
    		}
    		if (Files.isDirectory(p)){
    			//don't want to log changes to directories
    			return;
    		}
    	}
    	final String relativeFileName = FilenameUtils.separatorsToUnix(FileSystems.getDefault()
    			.getPath(SmartContext.INSTANCE.getFilestoreLocation())
    			.relativize(p)
    			.toString());

    	
    	try(Session s = sessionFactory.openSession()){
			ChangeLogItem item = new ChangeLogItem();
			item.setUuid(ChangeLogManager.INSTANCE.generateUuid(s, item));
			item.setAction(type);
			item.setConservationArea(caUuid);
			item.setFileName(relativeFileName);
			item.setSource(Source.LOCAL);

			s.beginTransaction();
			try {
				ChangeLogManager.INSTANCE.insertItem(s, item);
				s.getTransaction().commit();
			}catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
    	}catch (Exception ex){
    		logger.log(Level.WARNING, "Could not register filestore change event.  File will not be replicated to other systems. " + ex.getMessage(), ex); //$NON-NLS-1$
    	}
    }

	@Override
	public void run() {
		for (;;) {
			// wait for key to be signalled
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			} catch (ClosedWatchServiceException x){
				//we have disabled replication so don't worry about
				//this event
				return;
			}

			Path dir = keys.get(key);
			if (dir == null) {
	    		logger.log(Level.WARNING, "Watchkey not recongized!."); //$NON-NLS-1$
				continue;
			}

			//track any new directories created; sometimes if a file
			//is created in this directory a race condition is created and
			//the file doesn't get tracked; so at the end we visit
			//all files in the directory and add them too
			List<Path> newdirs = new ArrayList<>();
			
			//events to process; store these up and process them at the end
			List<Object[]> events = new ArrayList<>();
			
			for (WatchEvent<?> event : key.pollEvents()) {
				Kind<?> kind = event.kind();
				
				// TODO: - provide example of how OVERFLOW event is handled
				if (kind == StandardWatchEventKinds.OVERFLOW) {
					logger.log(Level.WARNING, "OVERFLOW FILE SYSTEM EVENTS not supported.  Files may not be replicated as expected."); //$NON-NLS-1$
					continue;
				}

				// Context for directory entry event is the file name of entry
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path name = ev.context();
				Path child = dir.resolve(name);

				// if directory is created, and watching recursively, then
				// register it and its sub-directories
				if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
					try {
						if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
							newdirs.add(child);
							register(child);		
						}
					} catch (IOException x) {
						// ignore to keep sample readbale
					}
				}
				if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
					//delete folders; find and remove key as we don't want these registered anymore
					for (Entry<WatchKey,Path> keyset : keys.entrySet()) {
						if (keyset.getValue().equals(child)) {
							keyset.getKey().cancel();
							break;
						}
					}
				}
				events.add(new Object[] {child, kind});
			}

			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);

				// all directories are inaccessible
				if (keys.isEmpty()) {
					break;
				}
			}
			
			Set<Path> toProcess = new HashSet<>();
			for (Path dirx : newdirs) {
				try {
					try(Stream<Path> stream = Files.walk(dirx)){
						stream.forEach(p->toProcess.add(p));
					}
				}catch (IOException ex) {
					logger.log(Level.SEVERE, ex.getMessage(), ex);
				}
			}

			for (Object[] x : events) {
				try {
					processEvent((Path)x[0], (Kind<?>)x[1]);
					toProcess.remove((Path)x[0]);
				} catch (Throwable t) {
					logger.log(Level.WARNING, "Error processing filestore event. Files may not be replicated as expected. " + t.getMessage(), t); //$NON-NLS-1$
				}
			}
			
			//it seems files within the dir may not 
			//get events fired - race condition where file is created before
			//registered so log a create event for any file in this directory
			//see: #3466
			for (Path p : toProcess) {
				processEvent(p, StandardWatchEventKinds.ENTRY_CREATE);
			}
		}
	}
}
