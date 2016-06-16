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
package org.wcs.smart.connect.replication;

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
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.changetracking.IFileStoreWatcher;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.internal.server.replication.ChangeLogTableManager;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.connect.model.ChangeLogItem.Source;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartHibernateManager;
import org.wcs.smart.util.UuidUtils;

/**
 * Watcher for managing filestore changes that need to 
 * be replicated to connect. 
 * 
 * @author Emily
 *
 */
public class FileStoreWatcher implements Runnable, IFileStoreWatcher{

    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;
    private final Set<Path> ignorePaths;
	
    public FileStoreWatcher() throws IOException{
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
    @Override
    public void addIgnorePath(Path ignorePath){
    	ignorePaths.add(ignorePath);
    }
    
    /**
     * Register the given directory with the WatchService
     */
    private void registerDirectory(Path dir) throws IOException {
    	if (ignorePaths.contains(dir)) return;
        WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        keys.put(key, dir);
    }
    
    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    @Override
    public void register(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            	registerDirectory(dir);
                return FileVisitResult.CONTINUE;
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

    	Session s = HibernateManager.openSession();
    	try{
    		if (DerbyReplicationManager.INSTANCE.isReplicationEnabled(caUuid, s)){
    			SmartHibernateManager.lockDatabase(s);
    			//this ensures nobody else can be writing to the change log table
    			//which may cause deadlock issues in derby
    			try{
    				s.beginTransaction();
    			
	    			ChangeLogItem item = new ChangeLogItem();
	    	    	item.setAction(type);
	    	    	item.setConservationArea(caUuid);
	    	    	item.setFileName(relativeFileName);
	    	    	item.setSource(Source.LOCAL);
	    	    	
	    			ChangeLogTableManager.INSTANCE.addItem(s, item);
	    			s.getTransaction().commit();
    			}finally{
    				SmartHibernateManager.unlockDatabase();
    			}
    		}
    	}catch (Exception ex){
    		ConnectPlugIn.displayLog(MessageFormat.format(Messages.FileStoreWatcher_FilestoreLogError, relativeFileName, ex.getMessage()), ex);
    	}finally{
    		s.close();
    	}
    }

	@SuppressWarnings("unchecked")
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
				ConnectPlugIn.log(Messages.FileStoreWatcher_Error, null);
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				Kind<?> kind = event.kind();

				// TODO: - provide example of how OVERFLOW event is handled
				if (kind == StandardWatchEventKinds.OVERFLOW) {
					ConnectPlugIn.log(Messages.FileStoreWatcher_Overflow, null);
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
							register(child);
						}
					} catch (IOException x) {
						// ignore to keep sample readbale
					}
				}
				try {
					processEvent(child, kind);
				} catch (Throwable t) {
					ConnectPlugIn.log(Messages.FileStoreWatcher_Error2, t);
				}
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
		}
	}
}
