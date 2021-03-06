package com.ctrip.xpipe.redis.core.protocal.cmd;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import com.ctrip.xpipe.api.pool.SimpleObjectPool;
import com.ctrip.xpipe.netty.commands.NettyClient;
import com.ctrip.xpipe.redis.core.protocal.pojo.Sentinel;
import com.ctrip.xpipe.redis.core.protocal.protocal.RequestStringParser;
import com.ctrip.xpipe.utils.StringUtil;

import io.netty.buffer.ByteBuf;

/**
 * @author wenchao.meng
 *
 * Dec 9, 2016
 */
public abstract class AbstractSentinelCommand<T> extends AbstractRedisCommand<T>{
	
	public static String SENTINEL = "sentinel";

	public AbstractSentinelCommand(SimpleObjectPool<NettyClient> clientPool, ScheduledExecutorService scheduled) {
		super(clientPool, scheduled);
	}

	public static class Sentinels extends AbstractSentinelCommand<List<Sentinel>>{
		
		public static String SENTINELS = "sentinels";
		
		private String masterName;

		public Sentinels(SimpleObjectPool<NettyClient> clientPool, String masterName, ScheduledExecutorService scheduled) {
			super(clientPool, scheduled);
			this.masterName = masterName;
		}
		
		@Override
		protected List<Sentinel> format(Object payload) {
			
			if(!(payload instanceof Object[])){
				throw new IllegalStateException("expected Object[], but:" + payload + "," + payload.getClass());
			}
			return doFormat((Object[])payload);			
		}

		private List<Sentinel> doFormat(Object[] payload) {
			
			List<Sentinel> sentinels = new LinkedList<>();
			for(Object sentinel : payload){
				if(!(sentinel instanceof Object[])){
					throw new IllegalStateException("expected Object[], but:" + sentinel + "," + sentinel.getClass());
				}
				sentinels.add(objectToSentinel((Object[])sentinel));
			}
			return sentinels;
		}

		private Sentinel objectToSentinel(Object[] sentinel) {
			
			if(sentinel.length < 6){
				throw new IllegalStateException("expected arg len >=6, but:" + sentinel.length + "," + StringUtil.join(",", sentinel));
			}
			return new Sentinel(payloadToString(sentinel[1]), 
					payloadToString(sentinel[3]), payloadToInteger(sentinel[5]));
		}

		@Override
		public ByteBuf getRequest() {
			return new RequestStringParser(SENTINEL, SENTINELS, masterName).format();
		}

		@Override
		public String toString() {
			return String.format("%s %s %s", SENTINEL, SENTINELS, masterName);
		}
	}
	
	
	public static class SentinelAdd extends AbstractSentinelCommand<String>{
		
		public static String MONITOR = "monitor";
		
		private String masterIp;
		private int masterPort;
		private int quorum;
		private String masterName;
		
		public SentinelAdd(SimpleObjectPool<NettyClient> clientPool, String masterName, String masterIp, int masterPort, int quorum, ScheduledExecutorService scheduled) {
			super(clientPool, scheduled);
			this.masterIp = masterIp;
			this.masterPort = masterPort;
			this.quorum = quorum;
			this.masterName = masterName;
		}

		@Override
		protected String format(Object payload) {
			return payloadToString(payload);
		}

		@Override
		public ByteBuf getRequest() {
			return new RequestStringParser(SENTINEL, MONITOR, masterName, masterIp, String.valueOf(masterPort), String.valueOf(quorum)).format();
		}
		
		@Override
		public String toString() {
			return String.format("%s %s %s %s %d %d", SENTINEL, MONITOR, masterName, masterIp, masterPort, quorum);
		}
	}
	
	public static class SentinelRemove extends AbstractSentinelCommand<String>{
		
		public static String REMOVE = "remove";
		
		private String masterName;

		public SentinelRemove(SimpleObjectPool<NettyClient> clientPool, String masterName, ScheduledExecutorService scheduled) {
			super(clientPool, scheduled);
			this.masterName = masterName;
		}

		@Override
		protected String format(Object payload) {
			return payloadToString(payload);
		}

		@Override
		public ByteBuf getRequest() {
			return new RequestStringParser(SENTINEL, REMOVE, masterName).format();
		}
		
		@Override
		public String toString() {
			return String.format("%s %s %s", SENTINEL, REMOVE, masterName);
		}
	}
}
