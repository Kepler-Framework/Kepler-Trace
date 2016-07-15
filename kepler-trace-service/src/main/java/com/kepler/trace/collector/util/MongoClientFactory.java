package com.kepler.trace.collector.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.FactoryBean;

import com.kepler.org.apache.commons.lang.StringUtils;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

public class MongoClientFactory implements FactoryBean<MongoDatabase> {

	// 配置项开始
	private String host;
	
	private int port;
	
	private String username;
	
	private String password;
	
	private String db;
	// 配置项结束

	private MongoClient client;

	@Override
	public MongoDatabase getObject() throws Exception {
		String[] hosts = StringUtils.split(host, ",");
		List<ServerAddress> serverAddress = new ArrayList<>();
		for (String h : hosts) {
			serverAddress.add(new ServerAddress(h, port));
		}
		if (useAuthentication()) {
			MongoCredential credential = MongoCredential.createCredential(username, db, password.toCharArray());
			client = new MongoClient(serverAddress, Arrays.asList(credential));
		} else {
			client = new MongoClient(serverAddress);
		}
		return client.getDatabase(db);
	}

	public void destroy() {
		client.close();
	}

	private boolean useAuthentication() {
		return StringUtils.isNotEmpty(this.username);
	}

	@Override
	public Class<MongoDatabase> getObjectType() {
		return MongoDatabase.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDb() {
		return db;
	}

	public void setDb(String db) {
		this.db = db;
	}
}
