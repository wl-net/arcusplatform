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
package com.arcussmarthome.agent.boot;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.arcussmarthome.agent.hal.IrisHal;
import com.arcussmarthome.agent.hal.IrisHalInternal;
import com.arcussmarthome.agent.lifecycle.LifeCycle;
import com.arcussmarthome.agent.lifecycle.LifeCycleService;
import com.arcussmarthome.bootstrap.ServiceLocator;
import com.arcussmarthome.bootstrap.guice.GuiceServiceLocator;
import com.arcussmarthome.capability.definition.DefinitionRegistry;
import com.arcussmarthome.io.json.gson.GsonModule;
import com.arcussmarthome.messages.MessagesModule;
import com.arcussmarthome.messages.capability.ClasspathDefinitionRegistry;
import com.arcussmarthome.protocol.ProtocolMessagesModule;
import com.netflix.governator.guice.BootstrapModule;

public final class BootUtils {
   private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);
   private static final Collection<Class<? extends Module>> EXTRA = new ArrayList<>();

   private BootUtils() {
   }

   public static Injector initialize(File basePath, Collection<String> configFiles) {
      return initialize(null, basePath, configFiles);
   }

   private static Set<File> resolveConfigs(Collection<String> configFiles) {
      Set<File> results = new HashSet<>();
      for (String path : configFiles) {
         results.add(new File(path));
      }

      return results;
   }

   public static Injector initialize(@Nullable IrisHalInternal hal, File basePath, Collection<String> configFiles) {
      Set<File> configs = resolveConfigs(configFiles);

      if (hal != null) {
         IrisHal.start(basePath, configs, hal);
      } else {
         IrisHal.start(basePath, configs);
      }

      Collection<BootstrapModule> bmodules = new LinkedList<>();
      bmodules.addAll(IrisHal.getBootstrapModules());

      Collection<Class<? extends BootstrapModule>> bmoduleClasses = new LinkedList<>();
      bmoduleClasses.addAll(IrisHal.getBootstrapModuleClasses());

      Collection<Module> modules = new LinkedList<>();
      modules.addAll(IrisHal.getApplicationModules());

      Collection<Class<? extends Module>> moduleClasses = new LinkedList<>();
      moduleClasses.addAll(EXTRA);
      moduleClasses.addAll(IrisHal.getApplicationModuleClasses());
      moduleClasses.add(MessagesModule.class);
      moduleClasses.add(ProtocolMessagesModule.class);
      moduleClasses.add(GsonModule.class);
      moduleClasses.add(AgentModule.class);

      Bootstrap.Builder builder = Bootstrap.builder();
      builder.withBootstrapModules(bmodules);
      builder.withBootstrapModuleClasses(bmoduleClasses);
      builder.withModules(modules);
      builder.withModuleClasses(moduleClasses);

      Injector injector = builder.build().bootstrap();
      ServiceLocator.init(GuiceServiceLocator.create(injector));

      Runtime.getRuntime().addShutdownHook(new Thread(IrisShutdownHook.INSTANCE));
      return injector;
   }

   // NOTE: Special hook for test cases
   public static void addExtraModules(Collection<? extends Class<? extends Module>> extra) {
      BootUtils.EXTRA.addAll(extra);
   }

   // NOTE: Special hook for test cases
   public static void clearExtraModules() {
      BootUtils.EXTRA.clear();
   }

   private static class AgentModule extends AbstractModule {
      @Override
      protected void configure() {
         bind(DefinitionRegistry.class).toInstance(ClasspathDefinitionRegistry.instance());
      }
   }

   private static enum IrisShutdownHook implements Runnable {
      INSTANCE;

      @Override
      public void run() {
         try {
            LifeCycleService.setState(LifeCycle.SHUTTING_DOWN);
            ServiceLocator.destroy();
            IrisHal.shutdown();
         } catch (Exception ex) {
            log.warn("unclean shutdown: {}", ex.getMessage(), ex);
         }
      }
   }
}

