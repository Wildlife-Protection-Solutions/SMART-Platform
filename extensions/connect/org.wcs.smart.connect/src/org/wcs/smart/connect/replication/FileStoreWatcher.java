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
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem;
import org.wcs.smart.connect.replication.changelog.ChangeLogTableManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

/**
 * Watcher for managing filestore changes that need to 
 * be replicated to connect. 
 * 
 * @author Emily
 *
 */
public class FileStoreWatcher implements Runnable{

    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;
	
    public FileStoreWatcher() throws IOException{
    	keys = new HashMap<WatchKey, Path>();
    	watcher = FileSystems.getDefault().newWatchService();
    }
	
    /**
     * Register the given directory with the WatchService
     */
    private void registerDirectory(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        keys.put(key, dir);
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
            	registerDirectory(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * Deregister all listeners and closes the watcher
     */
    public void deregister() throws IOException {
    	for (WatchKey key : keys.keySet()){
    		key.cancel();
    	}
    	watcher.close();
    }

    
    private void processEvent(Path p, Kind<?> kind){
    	
    	Path relativePath = FileSystems.getDefault()
    			.getPath(SmartContext.INSTANCE.getFilestoreLocation())
    			.relativize(p);
    	if (!relativePath.getName(0).toString().matches(UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid() ))){
    		//not in the current conservation area folder.  We just
    		//don't care.
    		//TODO: document this 
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
    	
    	final String relativeFileName = FileSystems.getDefault()
    			.getPath(SmartContext.INSTANCE.getFilestoreLocation())
    			.relativize(p)
    			.toString();
    	
    	ChangeLogItem item = new ChangeLogItem();
    	item.setAction(type);
    	item.setConservationArea(SmartDB.getCurrentConservationArea().getUuid());
    	item.setFileName(relativeFileName);

    	Session s = HibernateManager.openSession();
    	try{
    		s.beginTransaction();
    		ChangeLogTableManager.INSTANCE.addItem(s, item);
    		s.getTransaction().commit();
    	}finally{
    		s.close();
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
			}

			Path dir = keys.get(key);
			if (dir == null) {
				System.err.println("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				Kind<?> kind = event.kind();

				// TODO: - provide example of how OVERFLOW event is handled
				if (kind == StandardWatchEventKinds.OVERFLOW) {
					ConnectPlugIn.log("OVERFLOW FILE SYSTEM EVENTS", null);
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
					ConnectPlugIn.log("Error processing filestore event", t);
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
