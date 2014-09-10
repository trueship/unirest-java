package com.mashape.unirest.http.options;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;

import com.mashape.unirest.http.async.utils.AsyncIdleConnectionMonitorThread;
import com.mashape.unirest.http.utils.SyncIdleConnectionMonitorThread;

public class Options {

	public static final long CONNECTION_TIMEOUT = 10000;
	private static final long SOCKET_TIMEOUT = 60000;
	
	private static Map<Option, Object> options = new HashMap<Option, Object>();
	
	public static void setOption(Option option, Object value) {
		options.put(option, value);
	}
	
	public static Object getOption(Option option) {
		return options.get(option);
	}

	static {
		refresh();
	}
	
	public static void refresh() {
		// Load timeouts
		Object connectionTimeout = Options.getOption(Option.CONNECTION_TIMEOUT);
		if (connectionTimeout == null) connectionTimeout = CONNECTION_TIMEOUT;
		Object socketTimeout = Options.getOption(Option.SOCKET_TIMEOUT);
		if (socketTimeout == null) socketTimeout = SOCKET_TIMEOUT;
		
		// Create common default configuration
		RequestConfig clientConfig = RequestConfig.custom().setConnectTimeout(((Long) connectionTimeout).intValue()).setSocketTimeout(((Long) socketTimeout).intValue()).setConnectionRequestTimeout(((Long)socketTimeout).intValue()).build();
		
		PoolingHttpClientConnectionManager syncConnectionManager = new PoolingHttpClientConnectionManager();
		syncConnectionManager.setMaxTotal(Integer.MAX_VALUE);
		syncConnectionManager.setDefaultMaxPerRoute(Integer.MAX_VALUE);
		
		// Create clients
		setOption(Option.HTTPCLIENT, HttpClientBuilder.create().setDefaultRequestConfig(clientConfig).setConnectionManager(syncConnectionManager).build());
		SyncIdleConnectionMonitorThread syncIdleConnectionMonitorThread = new SyncIdleConnectionMonitorThread(syncConnectionManager);
		setOption(Option.SYNC_MONITOR, syncIdleConnectionMonitorThread);
		syncIdleConnectionMonitorThread.start();
		
		DefaultConnectingIOReactor ioreactor;
		PoolingNHttpClientConnectionManager asyncConnectionManager;
		try {
			ioreactor = new DefaultConnectingIOReactor();
			asyncConnectionManager = new PoolingNHttpClientConnectionManager(ioreactor);
			asyncConnectionManager.setMaxTotal(Integer.MAX_VALUE);
			asyncConnectionManager.setDefaultMaxPerRoute(Integer.MAX_VALUE);
		} catch (IOReactorException e) {
			throw new RuntimeException(e);
		}
		
		CloseableHttpAsyncClient asyncClient = HttpAsyncClientBuilder.create().setDefaultRequestConfig(clientConfig).setConnectionManager(asyncConnectionManager).build();
		setOption(Option.ASYNCHTTPCLIENT, asyncClient);
		setOption(Option.ASYNC_MONITOR, new AsyncIdleConnectionMonitorThread(asyncConnectionManager));
	}
	
}
