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
package com.iris.ipcd.session;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.DefaultSessionRegistryImpl;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Singleton
public class IpcdSessionRegistry extends DefaultSessionRegistryImpl {
    private static final Logger logger = LoggerFactory.getLogger(IpcdSessionRegistry.class);

    @Inject
    public IpcdSessionRegistry(ClientFactory clientFactory, Set<SessionListener> listeners) {
        super(clientFactory, listeners);
    }

    /***
     * Register a client other than the default against with this session.
     * This is useful when an IPCD device would like to host act as a hub and provide additional devices.
     * @param ct
     * @param session
     */
    public void putSession(ClientToken ct, Session session) {
        if (ct == session.getClientToken()) {
            logger.debug("putSession(ClientToken, Session) is intended to be used with a sub-device");
            // TODO: throw an exception, this is only used for special cases, and is not intended to be used normally.
        }
        // more than one client per session, so this isn't a new session, and isn't in the metrics.
        sessionMap.put(ct, session);
        logger.debug("Registered alternative session for [" + session.getClientToken() + "]. Now operating in 'hub' mode");
    }

}
