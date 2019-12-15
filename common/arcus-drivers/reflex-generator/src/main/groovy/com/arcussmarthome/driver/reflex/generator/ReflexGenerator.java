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
package com.arcussmarthome.driver.reflex.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import com.beust.jcommander.JCommander;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.arcussmarthome.bootstrap.Bootstrap;
import com.arcussmarthome.bootstrap.ServiceLocator;
import com.arcussmarthome.bootstrap.guice.GuiceServiceLocator;
import com.arcussmarthome.capability.definition.DefinitionRegistry;
import com.arcussmarthome.capability.registry.CapabilityRegistry;
import com.arcussmarthome.driver.DeviceDriver;
import com.arcussmarthome.driver.DeviceDriverDefinition;
import com.arcussmarthome.driver.groovy.GroovyDriverFactory;
import com.arcussmarthome.driver.groovy.GroovyDriverModule;
import com.arcussmarthome.driver.groovy.GroovyProtocolPluginModule;
import com.arcussmarthome.driver.reflex.ReflexDB;
import com.arcussmarthome.driver.reflex.ReflexDriverDefinition;
import com.arcussmarthome.driver.reflex.ReflexJson;
import com.arcussmarthome.messages.capability.ClasspathDefinitionRegistry;
import com.arcussmarthome.messages.capability.DefinitionTransformCapabilityRegistry;

public class ReflexGenerator {
   private final ReflexGeneratorOptions options;

   public ReflexGenerator(ReflexGeneratorOptions options) {
      this.options = options;
   }

   public static void main(String[] args) {
      try {
         ReflexGeneratorOptions options = new ReflexGeneratorOptions();
         JCommander jcmd = new JCommander(options);

         try {
            jcmd.parse(args);
            if (options.isHelp()) {
               throw new Exception("show help");
            }
         } catch (Exception ex) {
            jcmd.usage();
            System.exit(0);
         }

         Bootstrap.Builder builder = Bootstrap.builder();
         builder.withModules(new ReflexGeneratorModule(options));
         builder.withModuleClasses(GroovyDriverModule.class, GroovyProtocolPluginModule.class);

         Injector injector = builder.build().bootstrap();
         ServiceLocator.init(GuiceServiceLocator.create(injector));
         Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
               ServiceLocator.destroy();
            }
         }));

         new ReflexGenerator(options).run(injector.getInstance(GroovyDriverFactory.class));
      } catch (Exception ex) {
         ex.printStackTrace();
         System.exit(1);
      }
   }

   public void run() throws Exception {
      Bootstrap.Builder builder = Bootstrap.builder();
      builder.withModules(new ReflexGeneratorModule(options));
      builder.withModuleClasses(GroovyDriverModule.class, GroovyProtocolPluginModule.class);

      Injector injector = builder.build().bootstrap();
      ServiceLocator.init(GuiceServiceLocator.create(injector));
      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
         @Override
         public void run() {
            ServiceLocator.destroy();
         }
      }));

      run(injector.getInstance(GroovyDriverFactory.class));
   }

   public void run(GroovyDriverFactory factory) throws Exception {
      Set<File> parentDirectories = new LinkedHashSet<>();
      for (String file : options.getInputFiles()) {
         File fl = new File(file);
         parentDirectories.add(fl.getParentFile());
      }

      System.out.println("generating hub local reflex db...");

      boolean validated = true;
      for (String file : options.getInputFiles()) {
         try {
            File fl = new File(file);
            System.out.println("parsing " + fl.getName() + "...");
            
            DeviceDriver driver = factory.load(fl.getAbsolutePath());
            DeviceDriverDefinition def = driver.getDefinition();

            ReflexDriverDefinition reflexes = def.getReflexes();
            if (reflexes == null) {
               continue;
            }

            String json1 = ReflexJson.toJson(reflexes);
            ReflexDriverDefinition temp = ReflexJson.fromJson(json1);
            String json2 = ReflexJson.toJson(reflexes);

            if (!json1.equals(json2)) {
               validated = false;
               System.out.println("validation of reflex serialization/deserialization failed for: " + def.getName() + " " + def.getVersion());
            }
         } catch (Exception ex) {
            throw ex;
         }
      }

      if (!validated) {
         throw new RuntimeException("failed to validate drivers");
      }
   }

   @Deprecated
   private void output(ReflexDB db) throws Exception {
      try {
         String json = ReflexDB.toJson(db);
         ReflexDB db2 = ReflexDB.fromJson(json);
         String json2 = ReflexDB.toJson(db2);
         ReflexDB db3 = ReflexDB.fromJson(json2);
         String json3 = ReflexDB.toJson(db3);

         try {
            if (!json2.equals(json3)) {
               throw new RuntimeException("hub local reflex database could not be deserialized correctly");
            }

            System.out.println("verified that hub local reflex database can be deserialized correctly");
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);

            File output = new File(options.getOutputFile());
            output.getParentFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(output);
               GZIPOutputStream os = new GZIPOutputStream(fos)) {
               os.write(payload);
            }

            System.out.println("hub local reflex db output: " + output);
         } catch (Exception ex) {
            System.out.println("failed to verify reflex db:\nbefore:\n" + json + "\nafter:\n" + json2);
            ex.printStackTrace();
         }
      } catch (Exception ex) {
         System.out.println("could not serialize reflex db");
         ex.printStackTrace();
         System.exit(1);
      }
   }

   private static final Set<URL> createGroovyDriverUrls(ReflexGeneratorOptions options) {
      try {
         Set<URL> urls = new LinkedHashSet<>();
         for (String file : options.getInputFiles()) {
            File fl = new File(file);
            urls.add(fl.getParentFile().getCanonicalFile().toURI().toURL());
         }

         System.out.println("URLS: " + urls);
         return urls;
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   private static final class ReflexGeneratorModule extends AbstractModule {
      private final ReflexGeneratorOptions options;

      public ReflexGeneratorModule(ReflexGeneratorOptions options) {
         this.options = options;
      }

      @Override
      protected void configure() {
         bind(DefinitionRegistry.class)
            .toInstance(ClasspathDefinitionRegistry.instance());

         bind(CapabilityRegistry.class)
            .to(DefinitionTransformCapabilityRegistry.class)
            .asEagerSingleton();

         bind(new TypeLiteral<Set<URL>>() {})
            .annotatedWith(Names.named(GroovyDriverModule.NAME_GROOVY_DRIVER_DIRECTORIES))
            .toInstance(createGroovyDriverUrls(options));
      }
   }
}

