package org.wcs.smart.connect.replication;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartContext;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

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
    	String type ="FS_INSERT";
    	if (kind == StandardWatchEventKinds.ENTRY_CREATE){
    		type = "FS_INSERT";
    	}else if (kind == StandardWatchEventKinds.ENTRY_MODIFY){
    		type = "FS_UPDATE";
    	}else if(kind == StandardWatchEventKinds.ENTRY_DELETE){
    		type = "FS_DELETE";
    	}
    	final String ftype = type;
    	
    	
    	Path filestore = (new File(SmartContext.INSTANCE.getFilestoreLocation())).toPath();
    	Path relativeFile = filestore.relativize(p);
    	final String fRelative = relativeFile.toString();
    	
    	System.out.println("DO:" + ftype + ":" + fRelative);
    	
    	Session s = HibernateManager.openSession();
    	s.doWork(new Work(){

			@Override
			public void execute(Connection connection) throws SQLException {
				// TODO deal with exceptions
				String sql = "INSERT INTO smart.connect_change_log (action, filename, ca_uuid) values (?, ?, ?)";
				PreparedStatement ps = connection.prepareStatement(sql);
				ps.setString(1, ftype);
				ps.setString(2, fRelative);
				UUID value = SmartDB.getCurrentConservationArea().getUuid();
				byte[] uuid = 
						ByteBuffer.allocate(16).putLong(((UUID)value).getMostSignificantBits())
						.putLong(((UUID)value).getLeastSignificantBits()).array();
				
				ps.setObject(3,uuid);
				ps.executeUpdate();
				
				connection.commit();
    
			}
    		
    	});
    	s.close();
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

	            for (WatchEvent<?> event: key.pollEvents()) {
	                Kind<?> kind = event.kind();

	                // TBD - provide example of how OVERFLOW event is handled
	                if (kind == StandardWatchEventKinds.OVERFLOW) {
	                    continue;
	                }

	                // Context for directory entry event is the file name of entry
	                WatchEvent<Path> ev = (WatchEvent<Path>) event;
	                Path name = ev.context();
	                Path child = dir.resolve(name);

	                // print out event
//	                System.out.format("%s: %s\n", event.kind().name(), child);

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
	                }catch (Throwable t){
	                	//TODO: deal with this
	                	t.printStackTrace();
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
