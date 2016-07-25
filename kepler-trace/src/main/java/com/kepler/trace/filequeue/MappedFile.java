package com.kepler.trace.filequeue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MappedFile {

	public static final int FILE_SIZE = 1024 * 1024 * 128;
	
	private static final Log LOGGER = LogFactory.getLog(MappedFile.class);
	
	private final RandomAccessFile randomAccessfile;
	
	private final File file;
	
	private final MappedByteBuffer buffer;
	
	private final String fileName;
	
	private AtomicBoolean isCleaned = new AtomicBoolean(false);
	
	public MappedFile(File f) throws IOException {
		this.randomAccessfile = new RandomAccessFile(f, "rw");
		this.file = f;
		this.fileName = f.getName();
		this.buffer = this.getFile().getChannel().map(MapMode.READ_WRITE, 0, MappedFile.FILE_SIZE);
		this.buffer.position(0);
	}

	public RandomAccessFile getFile() {
		return randomAccessfile;
	}

	public MappedByteBuffer getBuffer() {
		return buffer;
	}
	
	public long getOffset() {
		return Long.parseLong(fileName);
	}
	
	public void destroy() {
		if (isCleaned.compareAndSet(false, true)) {
			try {
				MMapUtils.unmap(this.buffer);
				this.randomAccessfile.close();
				this.file.delete();
			} catch (IOException e) {
				LOGGER.error("[Close Error]Failed closing the mapped file: " + this.fileName, e);
				e.printStackTrace();
			}
		}
	}
	
	public void flush() {
		this.buffer.force();
	}

}
