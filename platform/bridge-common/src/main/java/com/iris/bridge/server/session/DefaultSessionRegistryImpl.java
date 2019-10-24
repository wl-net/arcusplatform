/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.bridge.server.session;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.metrics.IrisMetrics;

@Singleton
public class DefaultSessionRegistryImpl implements SessionRegistry  {
   private static final Logger logger = LoggerFactory.getLogger(DefaultSessionRegistryImpl.class);

   private final Set<SessionListener> listeners;
   private final ClientFactory clientFactory;
   protected Map<ClientToken, Session> sessionMap = new ConcurrentHashMap<ClientToken, Session>();
   // unmodifiable view of the values in the sessionMap
   // also concurrent because sessionMap is concurrent
   private Iterable<Session> sessions = Collections.unmodifiableCollection(sessionMap.values());         

   @Inject
   public DefaultSessionRegistryImpl(ClientFactory clientFactory, Set<SessionListener> listeners) {
      this.listeners = ImmutableSet.copyOf(listeners);
      this.clientFactory = clientFactory;
   }
      

   public int sessionCount() {
      return sessionMap.size();
   }

   @Override
   public Iterable<Session> getSessions() {
      return sessions;
   }

   @Override
   public Session getSession(ClientToken ct) {
      return sessionMap.get(ct);
   }

   @Override
   public void putSession(Session session) {
      ClientToken ct = session.getClientToken();
      SessionMetrics.incrementSessions();
      sessionMap.put(ct, session);
      logger.debug("Session registered [{}]", ct);
      logger.debug("put n={}", sessionMap.size());

   }

   /***
    * Register a client other than the default against with this session.
    * This is useful when an IPCD device would like to host act as a hub and provide additional devices.
    * @param ct
    * @param session
    */
   public void putSession(ClientToken ct, Session session) {
      logger.info("setting up additional client token [{}] for session", ct);
      if (ct == session.getClientToken()) {
         logger.debug("putSession(ClientToken, Session) is intended to be used with a sub-device");
         // TODO: throw an exception, this is only used for special cases, and is not intended to be used normally.
      }
      // more than one client per session, so this isn't a new session, and isn't in the metrics.
      sessionMap.put(ct, session);
      logger.debug("put n={}", sessionMap.size());
      logger.debug("Registered alternative session for [" + session.getClientToken() + "]. Now operating in 'hub' mode");
   }

   @Override
   public void destroySession(Session session) {
      logger.debug("Bridge session.destroySession is called [{}]");
      ClientToken ct = session.getClientToken();
      Session s = sessionMap.remove(ct);
      if(s != null && s instanceof DefaultSessionImpl) {
    	 Client curClient = session.getClient();
         logger.debug("Session destroyed [{}], session id [{}]", ct, curClient!=null?curClient.getSessionId():"");
         SessionMetrics.decrementSessions();                 
         fireListeners(session);   
         ((DefaultSessionImpl) s).destroy0(); 	         
      }
   }

   @Override
   public ClientFactory getClientFactory() {
      return clientFactory;
   }

   private void fireListeners(Session session) {
      listeners.forEach((l) -> l.onSessionDestroyed(session));
   }
   
   
   private static class SessionMetrics {
      private static final SessionMetrics INSTANCE;
      
      static {
         INSTANCE = new SessionMetrics();
         IrisMetrics.metrics("bridge.sessions").gauge("active", SessionMetrics::getSessionCount, INSTANCE);
      }
      
      public static void incrementSessions() {
         INSTANCE.doIncrementSessions();
      }
      
      public static void decrementSessions() {
         INSTANCE.doDecrementSessions();
      }
      
      private final AtomicLong sessions = new AtomicLong();
      
      public void doIncrementSessions() {
         sessions.incrementAndGet();
      }
      
      public void doDecrementSessions() {
         sessions.decrementAndGet();
      }
      
      public long getSessionCount() {
         return sessions.get();
      }
   }
}

