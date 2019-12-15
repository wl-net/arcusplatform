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
package com.arcussmarthome.core.dao.cassandra;

import org.apache.shiro.session.mgt.SessionManager;

import com.google.inject.Inject;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.arcussmarthome.security.SecurityModule;
import com.arcussmarthome.security.SessionConfig;
import com.arcussmarthome.security.dao.AuthenticationDAO;

/**
 * 
 */
public class PersonDAOSecurityModule extends SecurityModule {

   @Inject
	public PersonDAOSecurityModule(SessionConfig config) {
      super(config);
   }


   @Override
   public void configure() {
      super.configure();
      expose(SessionManager.class);
   }
	
	
   @Override
   protected void bindAuthenticationDAO(AnnotatedBindingBuilder<AuthenticationDAO> bind) {
      bind.to(PersonDAOImpl.class);
   }

}

