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
/**
 * 
 */
package com.arcussmarthome.bridge.server.noauth;

import java.util.Collections;
import java.util.Date;

import org.apache.shiro.authc.AuthenticationException;

import com.arcussmarthome.bridge.server.client.Client;
import com.arcussmarthome.security.authz.AuthorizationContext;
import com.arcussmarthome.security.principal.DefaultPrincipal;
import com.arcussmarthome.security.principal.Principal;

/**
 * 
 */
public class NoAuthClient implements Client {
   private static final NoAuthClient INSTANCE = new NoAuthClient();
   
   private final Principal principal = new DefaultPrincipal("[no-auth]", null);
   private final AuthorizationContext authContext =
         new AuthorizationContext(null, null, Collections.emptyList());
   
   public static Client getInstance() {
      return INSTANCE;
   }
   
   private NoAuthClient() {
      
   }
   
   @Override
   public boolean isAuthenticated() {
      return true;
   }

   @Override
   public Principal getPrincipal() {
      return principal;
   }
   
   @Override
   public String getSessionId() {
      return "";
   }

   @Override
   public AuthorizationContext getAuthorizationContext() {
      return authContext;
   }

   @Override
   public Date getLoginTime() {
      return null;
   }

   @Override
   public Date getExpirationTime() {
      return null;
   }

   @Override
   public void resetExpirationTime() {
   }

   @Override
   public void login(Object credentials) throws AuthenticationException {
      // no-op
   }

   @Override
   public void logout() {
      // no-op
   }

}

