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
package com.iris.driver.groovy.zwaveassociation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriver;
import com.iris.driver.capability.Capability;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.GroovyDriverBuilder;
import com.iris.driver.groovy.binding.CapabilityEnvironmentBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.metadata.PlatformEventMatcher;
import com.iris.messages.capability.ZWaveDirectAssociationCapability;

import groovy.lang.Closure;

/**
 * Plugin that registers the {@code zwaveAssociation()} DSL call and wires up
 * all the handlers (GetAttributes, SetAssociation, RemoveAssociation,
 * GetAssociation, GetSupportedGroups, ClearAssociation, and protocol reports).
 */
public class ZWaveAssociationPlugin implements GroovyDriverPlugin {
   private static final Logger log = LoggerFactory.getLogger(ZWaveAssociationPlugin.class);

   @Override
   public void enhanceEnvironment(EnvironmentBinding binding) {
      binding.setProperty("zwaveAssociation", new ZWaveAssociationDSLClosure(binding));
   }

   @Override
   public void postProcessEnvironment(EnvironmentBinding binding) {
      if (!(binding instanceof DriverBinding)) {
         return;
      }
      DriverBinding driverBinding = (DriverBinding) binding;
      GroovyDriverBuilder builder = driverBinding.getBuilder();
      if (!builder.isZWaveAssociationEnabled()) {
         return;
      }

      log.debug("Registering ZWaveDirectAssociation capability");

      // Auto-add the ZWaveDirectAssociation capability definition
      CapabilityDefinition capDef = builder.getCapabilityDefinitionByName(ZWaveDirectAssociationCapability.NAME);
      if (capDef != null) {
         builder.addCapabilityDefinition(capDef);
      } else {
         log.warn("ZWaveDirectAssociation capability definition not found in registry");
      }

      // Register GetAttributes provider
      builder.addGetAttributesProvider(new ZWaveAssociationGetAttributesProvider());

      // Register SetAssociation command handler
      PlatformEventMatcher setMatcher = new PlatformEventMatcher();
      setMatcher.setCapability(ZWaveDirectAssociationCapability.NAMESPACE);
      setMatcher.setEvent("SetAssociation");
      setMatcher.setHandler(new ZWaveAssociationSetHandler());
      builder.addEventMatcher(setMatcher);

      // Register RemoveAssociation command handler
      PlatformEventMatcher removeMatcher = new PlatformEventMatcher();
      removeMatcher.setCapability(ZWaveDirectAssociationCapability.NAMESPACE);
      removeMatcher.setEvent("RemoveAssociation");
      removeMatcher.setHandler(new ZWaveAssociationRemoveHandler());
      builder.addEventMatcher(removeMatcher);

      // Register GetAssociation command handler
      PlatformEventMatcher getMatcher = new PlatformEventMatcher();
      getMatcher.setCapability(ZWaveDirectAssociationCapability.NAMESPACE);
      getMatcher.setEvent("GetAssociation");
      getMatcher.setHandler(new ZWaveAssociationGetHandler());
      builder.addEventMatcher(getMatcher);

      // Register GetSupportedGroups command handler
      PlatformEventMatcher getGroupsMatcher = new PlatformEventMatcher();
      getGroupsMatcher.setCapability(ZWaveDirectAssociationCapability.NAMESPACE);
      getGroupsMatcher.setEvent("GetSupportedGroups");
      getGroupsMatcher.setHandler(new ZWaveAssociationGetGroupsHandler());
      builder.addEventMatcher(getGroupsMatcher);

      // Register ClearAssociation command handler
      PlatformEventMatcher clearMatcher = new PlatformEventMatcher();
      clearMatcher.setCapability(ZWaveDirectAssociationCapability.NAMESPACE);
      clearMatcher.setEvent("ClearAssociation");
      clearMatcher.setHandler(new ZWaveAssociationClearHandler());
      builder.addEventMatcher(clearMatcher);

      // Register Z-Wave Association Report / Groupings Report handler
      ZWaveAssociationReportHandler reportHandler = new ZWaveAssociationReportHandler();
      builder.addProtocolHandler(com.iris.protocol.zwave.ZWaveProtocol.NAMESPACE, reportHandler);
   }

   @Override
   public void enhanceDriver(DriverBinding bindings, DeviceDriver driver) {
      // no-op
   }

   @Override
   public void enhanceCapability(CapabilityEnvironmentBinding bindings, Capability capability) {
      // no-op
   }

   /**
    * No-arg closure for the {@code zwaveAssociation()} DSL call.
    * When called, sets a flag on the builder to enable association support.
    */
   @SuppressWarnings("serial")
   private static class ZWaveAssociationDSLClosure extends Closure<Object> {
      private final EnvironmentBinding binding;

      ZWaveAssociationDSLClosure(EnvironmentBinding binding) {
         super(binding);
         this.binding = binding;
      }

      protected void doCall() {
         if (binding instanceof DriverBinding) {
            GroovyDriverBuilder builder = ((DriverBinding) binding).getBuilder();
            builder.setZWaveAssociationEnabled(true);
         }
      }
   }
}
