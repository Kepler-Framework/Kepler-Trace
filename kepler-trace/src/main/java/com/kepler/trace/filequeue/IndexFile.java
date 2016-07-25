package com.kepler.trace.filequeue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IndexFile {

	public static final int FILE_SIZE = 1024 * 4;
	
	private final RandomAccessFile randomAccessfile;
	
	private final File file;
	
	private final MappedByteBuffer buffer;
	
	private AtomicBoolean isCleaned = new AtomicBoolean(false);
	
	private static final Log LOGGER = LogFactory.getLog(IndexFile.class);
	
	private static final int FILE_NO_POS = 0;
	
	private static final int OFFSET_POS = Integer.SIZE;
	
	public IndexFile(File f) throws IOException {
		this.randomAccessfile = new RandomAccessFile(f, "rw");
		boolean needCreate = this.randomAccessfile.length() == 0;
		this.file = f;
		this.buffer = this.randomAccessfile.getChannel().map(MapMode.READ_WRITE, 0, FILE_SIZE);
		if (needCreate) {
			writeFileNo(0);
			writeOffset(0);
		}
	}
	
	public long readFileNo() {
		return this.buffer.getLong(FILE_NO_POS);
	}

	public long readFileOffset() {
		return this.buffer.getLong(OFFSET_POS);
	}
	
	public void writeFileNo(long fileNo) {
		this.buffer.putLong(FILE_NO_POS, fileNo);
	}
	
	public void writeOffset(long offset) {
		this.buffer.putLong(OFFSET_POS, offset);
	}
	
	public void destroy() {
		if (isCleaned.compareAndSet(false, true)) {
			try {
				MMapUtils.unmap(this.buffer);
				this.randomAccessfile.close();
				this.file.delete();
			} catch (IOException e) {
				LOGGER.error("[Close Error]Failed closing the index file", e);
			}
		}
	}
	
	public void flush() {
		this.buffer.force();
	}

}
