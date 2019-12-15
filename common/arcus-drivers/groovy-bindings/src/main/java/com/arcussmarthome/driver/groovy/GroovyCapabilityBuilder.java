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
package com.arcussmarthome.driver.groovy;

import groovy.lang.Closure;

import java.util.Map;

import com.arcussmarthome.Utils;
import com.arcussmarthome.capability.key.NamespacedKey;
import com.arcussmarthome.capability.registry.CapabilityRegistry;
import com.arcussmarthome.device.attributes.AttributeMap;
import com.arcussmarthome.device.model.CapabilityDefinition;
import com.arcussmarthome.driver.capability.Capability;
import com.arcussmarthome.driver.event.DriverEvent;
import com.arcussmarthome.driver.groovy.context.GroovyCapabilityDefinition;
import com.arcussmarthome.driver.groovy.handler.GetAttributesClosureProvider;
import com.arcussmarthome.driver.groovy.handler.SetAttributesClosureConsumer;
import com.arcussmarthome.driver.handler.ContextualEventHandler;
import com.arcussmarthome.driver.metadata.EventMatcher;
import com.arcussmarthome.driver.metadata.PlatformEventMatcher;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.model.CapabilityId;
import com.arcussmarthome.model.Version;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.validators.ValidationException;

public class GroovyCapabilityBuilder extends GroovyBuilder {

   private CapabilityDefinition definition;
   private String name;

   public GroovyCapabilityBuilder(CapabilityRegistry registry) {
      super(registry);
   }

   public String getName() {
      return name;
   }

   public GroovyCapabilityBuilder withName(String name) {
      this.name = name;
      return this;
   }

   public CapabilityDefinition getCapabilityDefinition() {
      return definition;
   }

   public void setCapabilityDefinition(CapabilityDefinition definition) {
      this.definition = definition;
   }

   public GroovyCapabilityBuilder withCapabilityDefinition(Object o) {
      if(o == null) {
         return this;
      }

      if(o instanceof String) {
         CapabilityDefinition definition = getCapabilityDefinition((String) o);
         if(definition == null) {
            GroovyValidator.error("Unrecognized capability [" + o + "]");
         }
         else {
            setCapabilityDefinition(definition);
         }
         return this;
      }

      if(o instanceof GroovyCapabilityDefinition) {
         setCapabilityDefinition(((GroovyCapabilityDefinition) o).getDefinition());
         return this;
      }

      if(o instanceof CapabilityDefinition) {
         setCapabilityDefinition((CapabilityDefinition) o);
         return this;
      }

      GroovyValidator.error("Invalid type [" + o + "] for capability");
      return this;
   }

   protected GroovyCapabilityBuilder withDescription(String description) {
      setDescription(description);
      return this;
   }

   @Override
   public GroovyCapabilityBuilder withVersion(Object o) {
      super.withVersion(o);
      return this;
   }

   @Override
   public GroovyCapabilityBuilder addEventMatcher(EventMatcher matcher) {
      super.addEventMatcher(matcher);
      return this;
   }

   @Override
   public GroovyCapabilityBuilder addAttributeValue(String name, Object value) {
      if(definition == null) {
         GroovyValidator.error("Must specify the capability before configuring attribute [" + name + "]");
         return this;
      }
      if(Utils.isNamespaced(name)) {
         String namespace = Utils.getNamespace(name);
         if(!definition.getNamespace().equals(namespace)) {
            GroovyValidator.error("Can't configure attributes from namepsace [" + namespace + "], only attributes in namespace [" + definition.getNamespace() + "] may be configured.");
            return this;
         }
      }
      else {
         name = Utils.namespace(definition.getNamespace(), name);
      }
      super.addAttributeValue(name, value);
      return this;
   }

   @Override
   public GroovyCapabilityBuilder addAttributeValues(Map<String, Object> values) {
      super.addAttributeValues(values);
      return this;
   }

   public void addDeviceCommandHandler(NamespacedKey key, ContextualEventHandler<Object> handler) {
      if(definition == null) {
         GroovyValidator.error("Must specify a capability before adding handlers");
         return;
      }
      String namespace, command, instance;
      if(key == null) {
         namespace = definition.getNamespace();
         command = null;
         instance = null;
      }
      else if(key.isNamed()) {
         namespace = key.getNamespace();
         command = key.getName();
         instance = key.getInstance();
      }
      // this weird block means we did NamespaceKey.parse("commandName"), which gets misinterpreted as a namespace
      // TODO we should probably just deprecate implied namespace, its confusing all over the place
      else {
         namespace = definition.getNamespace();
         command = key.getNamespace();
         instance = null;
      }
      if(!definition.getNamespace().equals(namespace)) {
         GroovyValidator.error(("Can't configure an attribute from namespace [" + namespace + "] in capability [" + definition.getCapabilityName() + "]"));
         return;
      }

      PlatformEventMatcher matcher = new PlatformEventMatcher();
      matcher.setCapability(definition.getNamespace());
      matcher.setEvent(command);
      matcher.setHandler(handler);
      matcher.setInstance(instance);
      addEventMatcher(matcher);
   }

   public void addGetAttributesProvider(Closure<?> closure) {
      if(definition == null) {
         GroovyValidator.error("Must specify a capability header before implementing getAttributes");
         return;
      }

      addGetAttributesProvider(new GetAttributesClosureProvider(definition.getNamespace(), closure));
   }

   public void addSetAttributesProvider(Closure<?> closure) {
      if(definition == null) {
         GroovyValidator.error("Must specify a capability before implementing setAttributes");
         return;
      }

      addSetAttributesConsumer(new SetAttributesClosureConsumer(definition.getNamespace(), closure));
   }

   public Capability create() throws ValidationException {
      try (AutoCloseable pop = GroovyValidator.pushCopy()) {
         CapabilityId capabilityId = null;
         try {
            String capabilityName = definition != null ? definition.getCapabilityName() : null;
            String implementationName = getName();
            Version version = getVersion();
            GroovyValidator.assertNotEmpty(capabilityName, "No capability name specified, please add the header: capability <capability name>");
            GroovyValidator.assertNotEmpty(implementationName, "No capability name specified");
            GroovyValidator.assertNotNull(version, "Missing version header");
            capabilityId = new CapabilityId(capabilityName, implementationName, version);
         }
         catch(IllegalArgumentException e) {
            GroovyValidator.error(e.getMessage());
         }

         CapabilityDefinition capabilityDefinition = getCapabilityDefinition();
         AttributeMap attributes = getAttributes();

         ContextualEventHandler<DriverEvent> driverEventHandler = createDriverEventHandler();
         ContextualEventHandler<PlatformMessage> platformMessageHandler = createPlatformMessageHandler();
         ContextualEventHandler<ProtocolMessage> protocolMessageHandler = createProtocolMessageHandler();

         GroovyValidator.throwIfErrors();
         return new Capability(
               capabilityId,
               capabilityDefinition,
               getHash(),
               attributes,
               driverEventHandler,
               platformMessageHandler,
               protocolMessageHandler,
               getAttributeProviders(),
               getAttributeConsumers()
         );
      } catch (Exception ex) {
         throw new ValidationException("could not close validator", ex);
      }
   }

}

