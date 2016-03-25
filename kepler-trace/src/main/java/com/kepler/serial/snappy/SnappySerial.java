package com.kepler.serial.snappy;

import java.io.InputStream;
import java.io.OutputStream;

import org.xerial.snappy.Snappy;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;

import com.kepler.serial.SerialInput;
import com.kepler.serial.SerialOutput;
import com.kepler.serial.jackson.JacksonSerial;

/**
 * 
 * @author zhangjiehao
 *
 * 2016年3月10日
 */
public class SnappySerial extends JacksonSerial implements SerialInput, SerialOutput {


	public static final String NAME = "snappy";

	private static final byte SERIAL = 5;

	@Override
	public byte serial() {
		return SnappySerial.SERIAL;
	}

	@Override
	public String name() {
		return SnappySerial.NAME;
	}

	@Override
	public byte[] output(Object data, Class<?> clazz) throws Exception {
		return Snappy.compress(super.output(data, clazz));
	}

	@Override
	public OutputStream output(Object data, Class<?> clazz, OutputStream stream, int buffer) throws Exception {
		super.output(data, clazz, new SnappyOutputStream(stream), buffer);
		return stream;
	}

	@Override
	public <T> T input(byte[] data, Class<T> clazz) throws Exception {
		return super.input(Snappy.uncompress(data), clazz);
	}

	@Override
	public <T> T input(InputStream input, int buffer, Class<T> clazz) throws Exception {
		return super.input(new SnappyInputStream(input), buffer, clazz);
	}

}
