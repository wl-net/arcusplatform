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
package com.arcussmarthome.client.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.arcussmarthome.client.IrisClient;
import com.arcussmarthome.client.IrisClientFactory;
import com.arcussmarthome.client.Types;
import com.arcussmarthome.client.event.Listener;
import com.arcussmarthome.client.impl.netty.ClientConfig;
import com.arcussmarthome.client.model.Model;
import com.arcussmarthome.client.model.ModelAddedEvent;
import com.arcussmarthome.client.model.ModelCache;
import com.arcussmarthome.client.model.ModelChangedEvent;
import com.arcussmarthome.client.model.ModelDeletedEvent;
import com.arcussmarthome.client.model.ModelEvent;
import com.arcussmarthome.client.model.ModelFactory;
import com.arcussmarthome.client.model.Store;
import com.arcussmarthome.client.model.proxy.ProxyModelFactory;
import com.arcussmarthome.client.service.AccountService;
import com.arcussmarthome.client.service.AccountServiceImpl;
import com.arcussmarthome.client.service.EasCodeService;
import com.arcussmarthome.client.service.I18NService;
import com.arcussmarthome.client.service.InvitationService;
import com.arcussmarthome.client.service.NwsSameCodeService;
import com.arcussmarthome.client.service.NwsSameCodeServiceImpl;
import com.arcussmarthome.client.service.PersonService;
import com.arcussmarthome.client.service.PlaceService;
import com.arcussmarthome.client.service.ProductCatalogService;
import com.arcussmarthome.client.service.RuleService;
import com.arcussmarthome.client.service.SceneService;
import com.arcussmarthome.client.service.SchedulerService;
import com.arcussmarthome.client.service.Service;
import com.arcussmarthome.client.service.SessionService;
import com.arcussmarthome.client.service.SubsystemService;
import com.arcussmarthome.client.service.VideoService;
import com.arcussmarthome.client.service.VideoServiceImpl;
import com.arcussmarthome.client.service.ProductCatalogServiceImpl;

/**
 *
 */
public abstract class BaseClientFactory extends IrisClientFactory {
   private final IrisClient client;
   private final ModelCache cache;
   
   // Uses Double Locked Checking to Create!
   private volatile ClientConfig clientConfig;
   
   private final Map<String, Store<? extends Model>> stores =
         Collections.synchronizedMap(new HashMap<String, Store<? extends Model>>());
   private final Map<String, Service> services =
         Collections.synchronizedMap(new HashMap<String, Service>());

   protected BaseClientFactory(IrisClient client) {
      this(client, new ProxyModelFactory(client));
   }

   protected BaseClientFactory(IrisClient client, ModelFactory factory) {
      this(client, new ModelCache(factory));
   }

   protected BaseClientFactory(IrisClient client, ModelCache cache) {
      this.cache = cache;
      this.client = client;
      this.client.addMessageListener(cache);
      this.services.put(AccountService.class.getName(), new AccountServiceImpl(client));
      this.services.put(I18NService.class.getName(), new I18NServiceImpl(client));
      this.services.put(PersonService.class.getName(), new PersonServiceImpl(client));
      this.services.put(PlaceService.class.getName(), new PlaceServiceImpl(client));
      this.services.put(RuleService.class.getName(), new RuleServiceImpl(client));
      this.services.put(SceneService.class.getName(), new SceneServiceImpl(client));
      this.services.put(SchedulerService.class.getName(), new SchedulerServiceImpl(client));
      this.services.put(SessionService.class.getName(), new SessionServiceImpl(client));
      this.services.put(SubsystemService.class.getName(), new SubsystemServiceImpl(client));
      
      this.services.put(InvitationService.class.getName(), new InvitationServiceImpl(client));
      this.services.put(ProductCatalogService.class.getName(), new ProductCatalogServiceImpl(client));
      this.services.put(NwsSameCodeService.class.getName(), new NwsSameCodeServiceImpl(client));
      this.services.put(EasCodeService.class.getName(), new EasCodeServiceImpl(client));
      
      // TODO switch over all to use generated classes
      this.services.put(VideoService.class.getName(), new VideoServiceImpl(client));
   }

   @Override
   protected IrisClient doGetClient() {
      return client;
   }

   @Override
   protected ClientConfig doGetClientConfig() {
		if (null == clientConfig) {
			synchronized (this) {
				if (null == clientConfig) {
					clientConfig = ClientConfig.builder()
							.maxReconnectionAttempts(ClientConfig.DFLT_MAX_RECONNECTION_ATTEMPTS)
							.secondsBetweenReconnectionAttempts(ClientConfig.DFLT_SECS_BETWEEN_RECONNECTION_ATTEMPTS)
							.maxResponseSize(ClientConfig.DFLT_MAX_RESPONSE_SIZE).build();
				}
			}
		}
		return clientConfig;
   }
   @Override
   protected ModelCache doGetModelCache() {
      return cache;
   }

   @SuppressWarnings("unchecked")
	@Override
   protected Store<Model> doGetStore(String name) throws IllegalArgumentException {
      Class<? extends Model> type = Types.getModel(name);
      return (Store<Model>) doGetStore(type);
   }

   @SuppressWarnings("unchecked")
	@Override
   protected <M extends Model> Store<M> doGetStore(final Class<M> modelType) throws IllegalArgumentException {
      	String mapName = modelType.getName();
      	
         synchronized(stores) {
         	Store<? extends Model> s = stores.get(mapName);
         	
            if(s != null) {
            	return (Store<M>) s;
            }
            
            ClientStore<M> store = new ClientStore<M>(modelType);
            cache.addModelListener(store);
            stores.put(mapName, store);
            return store;
         }
   }

   @SuppressWarnings("unchecked")
	@Override
   protected <S extends Service> S doGetService(Class<S> serviceType) throws IllegalArgumentException {
      Service s = services.get(serviceType.getName());
      if(s == null) {
         synchronized(services) {
            s = services.get(serviceType.getName());
            if(s == null) {
               // TODO add service creation support
               if(true) throw new UnsupportedOperationException("Not implemented");
               // services.put(serviceType.getName(), s);
            }
         }
      }
      return (S) s;
   }

   private static final class ClientStore<M extends Model> extends InMemoryStore<M> implements Listener<ModelEvent> {
      private final Class<M> modelType;

      ClientStore(Class<M> modelType) {
         this.modelType = modelType;
      }

      @SuppressWarnings("unchecked")
		@Override
      public void onEvent(ModelEvent event) {
         Model model = event.getModel();
         if(model == null || !modelType.isAssignableFrom(model.getClass())) {
            return;
         }

         if(event instanceof ModelAddedEvent) {
            add((M) model);
         }
         else if(event instanceof ModelDeletedEvent) {
            remove((M) model);
         }
         else if(event instanceof ModelChangedEvent) {
            // forward
            fireEvent(event);
         }
      }

   }
}

