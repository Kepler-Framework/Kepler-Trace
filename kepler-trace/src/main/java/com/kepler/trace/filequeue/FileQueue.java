package com.kepler.trace.filequeue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.CRC32;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kepler.org.apache.commons.lang.math.NumberUtils;

public class FileQueue {

	private volatile List<MappedFile> files = new ArrayList<MappedFile>();

	private static final Log LOGGER = LogFactory.getLog(FileQueue.class);

	private static final int EOF = -1;

	private final String baseDir;

	private final String dataDir;

	private final String commitDir;

	private IndexFile indexFile;

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private final Lock offerLock = new ReentrantLock();
	
	private final Lock pollLock = new ReentrantLock();

	private final Condition canPoll = pollLock.newCondition();

	private volatile long writePos = 0;

	private volatile long readPos = 0;

	private volatile long writeCommitPos = 0;

	private volatile long readFileNo = 0;

	private volatile boolean shutdown = false;

	private volatile MappedFile readFile;

	private AtomicBoolean isCleaning = new AtomicBoolean(false);

	private AtomicBoolean isCommiting = new AtomicBoolean(false);

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	
	public FileQueue(String dir) {
		this.baseDir = dir;
		this.dataDir = baseDir + File.separator + "data";
		this.commitDir = baseDir + File.separator + "commit";
	}

	public void load() throws IOException {
		Files.createDirectories(Paths.get(dataDir));
		Files.createDirectories(Paths.get(commitDir));
		this.indexFile = new IndexFile(new File(this.commitDir, "commit"));
		loadData();
		loadCommit();
		clean();

		executor.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				try {
					if (!FileQueue.this.shutdown) {
						FileQueue.this.commit();
						FileQueue.this.clean();
					}
				} catch (Exception e) {
					LOGGER.error("Fail flush to disk.", e);
				}
			}

		}, 20, 20, TimeUnit.MILLISECONDS);
		
	}

	public void destroy() throws InterruptedException {
		this.shutdown = true;
		this.executor.shutdown();
		this.executor.awaitTermination(500, TimeUnit.MILLISECONDS);
		commit();
	}

	private void clean() {
		if (this.files.isEmpty())
			return;
		if (this.files.get(0).getOffset() >= this.readFileNo) 
			return;
		if (isCleaning.compareAndSet(false, true)) {
			readWriteLock.writeLock().lock();
			try {
				long writeFileNo = this.readFileNo;
				List<MappedFile> removedFiles = new ArrayList<>();
				for (MappedFile f : this.files) {
					if (f.getOffset() < writeFileNo) {
						f.destroy();
						removedFiles.add(f);
					}
				}
				this.files.removeAll(removedFiles);
			} finally {
				readWriteLock.writeLock().unlock();
				isCleaning.set(false);
			}
		}
	}

	private void loadData() throws IOException {
		LOGGER.debug("Loading data from disk.");
		File[] listFiles = new File(this.dataDir).listFiles();
		Arrays.sort(listFiles);
		for (File dataFile : listFiles) {
			if (NumberUtils.isNumber(dataFile.getName())) {
				MappedFile mappedFile = new MappedFile(dataFile);
				this.files.add(mappedFile);
			}
		}
		if (this.files.isEmpty()) {
			LOGGER.debug("Data loaded. [writePos]0[writeCommitPos]0");
			newFile(0);
			return;
		}
		MappedFile lastFile = getLastMappedFile();
		long offset = getLastMappedFile().getOffset();
		this.writePos = offset;

		CRC32 crc32 = new CRC32();
		ByteBuffer buffer = lastFile.getBuffer().duplicate();
		buffer.position(0);
		try {
			while (buffer.hasRemaining() && (this.writePos - offset < MappedFile.FILE_SIZE)) {
				int length = buffer.getInt();
				if (length == 0 || length == -1) {
					break;
				}
				byte[] content = new byte[length];
				buffer.get(content);
				crc32.update(content);
				long checksum = buffer.getLong();
				if (checksum != crc32.getValue()) {
					break;
				}
				crc32.reset();
				this.writePos += (Integer.SIZE / Byte.SIZE + length + Long.SIZE / Byte.SIZE);
			}
		} catch (BufferUnderflowException e) {

		}
		this.writeCommitPos = this.writePos;
		LOGGER.debug(String.format("Data loaded. [writePos]%d[writeCommitPos]%d", this.writePos, this.writeCommitPos));
	}

	private void loadCommit() throws FileNotFoundException, IOException {
		LOGGER.debug("Loading commit from disk.");
		this.readFileNo = this.indexFile.readFileNo();
		this.readPos = this.indexFile.readFileOffset();
		for (MappedFile f : this.files) {
			if (f.getOffset() == this.readFileNo) {
				this.readFile = f;
				break;
			}
		}
		LOGGER.debug(String.format("Commit loaded. [readFileNo]%d[readPos]%d", this.readFileNo, this.readPos));
	}

	private void newFile(long offset) throws IOException {
		File f = new File(this.dataDir, String.format("%020d", offset));
		MappedFile newFile = new MappedFile(f);
		readWriteLock.writeLock().lock();
		try {
			this.files.add(newFile);
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	public boolean offer(byte[] content) throws IOException {
		offerLock.lock();
		try {
			int length = content.length;
			CRC32 crc32 = new CRC32();
			crc32.update(content);
			long checksum = crc32.getValue();
			int msgSize = Integer.SIZE / Byte.SIZE + content.length + Long.SIZE / Byte.SIZE;
			if (this.files.isEmpty()) {
				newFile(0);
			}
			MappedFile lastFile = getLastMappedFile();
			if (this.writePos - lastFile.getOffset() + msgSize >= MappedFile.FILE_SIZE - 1) {
				putEof(lastFile);
				newFile(this.writePos);
				lastFile = getLastMappedFile();
			}
			ByteBuffer slice = lastFile.getBuffer().duplicate();
			slice.position((int) (this.writePos - lastFile.getOffset()));
			slice.putInt(length).put(content).putLong(checksum);
			this.writePos += msgSize;
		} finally {
			offerLock.unlock();
		}
		pollLock.lock();
		try {
			canPoll.signalAll();
		} finally {
			pollLock.unlock();
		}
		return true;
	}

	private void putEof(MappedFile lastFile) {
		ByteBuffer slice = lastFile.getBuffer().duplicate();
		slice.position((int) (this.writePos - lastFile.getOffset()));
		slice.putInt(-1);
	}

	private MappedFile getLastMappedFile() {
		readWriteLock.readLock().lock();
		try {
			return this.files.get(this.files.size() - 1);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	public void commit() {
		if (isCommiting.compareAndSet(false, true)) {
			try {
				commitDataFile();
				commitIndexFile(this.readFileNo, this.readPos);
			} finally {
				isCommiting.set(false);
			}
		}
	}

	private void commitDataFile() {
		if (this.writeCommitPos >= this.writePos)
			return;
		long lastCommitPos = this.writeCommitPos;
		this.writeCommitPos = this.writePos;
		this.readWriteLock.readLock().lock();
		try {
			if (this.files.isEmpty())
				return;
			int targetIdx = 0;
			for (int fileIdx = 0; fileIdx < this.files.size(); fileIdx++) {
				if (this.files.get(fileIdx).getOffset() < lastCommitPos) {
					targetIdx = fileIdx;
				} else {
					break;
				}
			}
			for (int fileIdx = targetIdx; fileIdx < this.files.size(); fileIdx++) {
				this.files.get(fileIdx).flush();
				LOGGER.debug(String.format("Flush data file %020d", this.files.get(fileIdx).getOffset()));
			}
		} finally {
			this.readWriteLock.readLock().unlock();
		}
	}

	private void commitIndexFile(long fileNo, long offset) {
		if (this.indexFile.readFileNo() == fileNo && this.indexFile.readFileOffset() == offset) {
			return;
		}
		this.indexFile.writeFileNo(fileNo);
		this.indexFile.writeOffset(offset);
		this.indexFile.flush();
		LOGGER.debug(String.format("Flush commit file. [fileNo]%d[offset]%d", fileNo, offset));
	}

	public <T> T peek(MessageReader<T> reader) throws IOException, DecodeException {
		pollLock.lock();
		try {
			if (!(this.readPos < this.writePos)) {
				return null;
			}
			return doPoll(reader, this.readFile, true);
		} finally {
			pollLock.unlock();
		}
	}

	public <T> T poll(MessageReader<T> reader) throws DecodeException, InterruptedException {
		pollLock.lockInterruptibly();
		try {
			while (!(this.readPos < this.writePos)) {
				canPoll.await();
			}
			return doPoll(reader, this.readFile, false);
		} finally {
			pollLock.unlock();
		}
	}

	private <T> T doPoll(MessageReader<T> reader, MappedFile mappedFile, boolean isPeek) throws DecodeException {
		int offset = (int) (this.readPos - mappedFile.getOffset());
		ByteBuffer buffer = null;
		if (offset < MappedFile.FILE_SIZE) {
			buffer = mappedFile.getBuffer().duplicate();
			buffer.position(offset);
			int length = buffer.getInt();
			if (length == EOF) {
				return rollAndReadNext(reader, mappedFile, isPeek);
			}
			if (this.readPos + Integer.SIZE / Byte.SIZE + length < this.writePos) {
				buffer.position(offset + Integer.SIZE / Byte.SIZE);
				buffer.limit(offset + Integer.SIZE / Byte.SIZE + length);
				if (!isPeek) {
					this.readPos += Integer.SIZE / Byte.SIZE + length + Long.SIZE / Byte.SIZE;
				}
				return reader.readMessage(buffer);
			}
			return null;
		} else {
			LOGGER.warn("Code should not go here except the fs is corrupted.");
			return rollAndReadNext(reader, mappedFile, isPeek);
		}
	}

	private <T> T rollAndReadNext(MessageReader<T> reader, MappedFile mappedFile, boolean isPeek) throws DecodeException {
		this.readWriteLock.readLock().lock();
		boolean rotate = false;
		MappedFile readFile = mappedFile;
		try {
			int readFileIdx = this.files.indexOf(mappedFile);
			rotate = readFileIdx + 1 < this.files.size();
			if (rotate) {
				readFile = this.files.get(readFileIdx + 1);
				if (!isPeek) {
					this.readFile = readFile;
					this.readFileNo = this.readFile.getOffset();
				}
			}
		} finally {
			this.readWriteLock.readLock().unlock();
		}
		if (rotate) {
			if (!isPeek) {
				this.commit();
//				this.clean();
			}
			return doPoll(reader, readFile, isPeek);
		} else {
			return null;
		}
	}

}
