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
package com.arcussmarthome.messages;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.arcussmarthome.gson.AddressTypeAdapterFactory;
import com.arcussmarthome.gson.ClientMessageTypeAdapter;
import com.arcussmarthome.gson.ProtocolDeviceIdTypeAdapter;
import com.arcussmarthome.io.json.gson.GsonModule;
import com.arcussmarthome.io.json.gson.GsonReferenceTypeAdapterFactory;
import com.arcussmarthome.io.json.gson.HubMessageTypeAdapter;
import com.arcussmarthome.io.json.gson.MessageBodyTypeAdapterFactory;
import com.arcussmarthome.io.json.gson.MessageTypeAdapterFactory;
import com.arcussmarthome.io.json.gson.ResultTypeAdapter;

public class MessagesModule extends AbstractModule {
	@Inject
	public MessagesModule(GsonModule gson) {
   }

   @Override
   protected void configure() {
      Multibinder<TypeAdapterFactory> typeAdapterFactoryBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<TypeAdapterFactory>() {});
      typeAdapterFactoryBinder.addBinding().to(AddressTypeAdapterFactory.class);
      typeAdapterFactoryBinder.addBinding().to(GsonReferenceTypeAdapterFactory.class);
      typeAdapterFactoryBinder.addBinding().to(MessageTypeAdapterFactory.class);
      typeAdapterFactoryBinder.addBinding().to(MessageBodyTypeAdapterFactory.class);

      Multibinder<TypeAdapter<?>> typeAdapterBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<TypeAdapter<?>>() {});
      typeAdapterBinder.addBinding().to(ProtocolDeviceIdTypeAdapter.class);
      typeAdapterBinder.addBinding().to(HubMessageTypeAdapter.class);

      Multibinder<JsonSerializer<?>> serializerBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<JsonSerializer<?>>() {});
      serializerBinder.addBinding().to(ClientMessageTypeAdapter.class);
      serializerBinder.addBinding().to(ResultTypeAdapter.class);

      Multibinder<JsonDeserializer<?>> deserializerBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<JsonDeserializer<?>>() {});
      deserializerBinder.addBinding().to(ClientMessageTypeAdapter.class);
      deserializerBinder.addBinding().to(ResultTypeAdapter.class);
   }
}

