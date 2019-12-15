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
package com.arcussmarthome.agent.zwave.process;

import com.arcussmarthome.agent.zwave.ZWNetwork;
import com.arcussmarthome.agent.zwave.ZWRouter;
import com.arcussmarthome.agent.zwave.ZWServices;
import com.arcussmarthome.agent.zwave.db.ZWDao;
import com.arcussmarthome.agent.zwave.events.ZWEvent;
import com.arcussmarthome.agent.zwave.events.ZWEventDispatcher;
import com.arcussmarthome.agent.zwave.events.ZWEventListener;

/**
 * Encapsulates the bootstrapping process for the ZWave controller. 
 * 
 * @author Erik Larson
 */
public class Bootstrapper implements ZWEventListener {
   public final static Bootstrapper INSTANCE = new Bootstrapper();
   
   private Bootstrapper() {}
   
   /**
    * Start the bootstrap process. This adds this process
    * to the ZWave message router, starts the database DAO, and
    * initializes the network which will either load nodes from the
    * database or look for nodes.
    */
   public void bootstrap() {
      ZWEventDispatcher.INSTANCE.register(this);
      ZWDao.start();
      ZWServices.INSTANCE.getZWaveEngine().bootstrap();
   }
   
   /**
    * Processes ZWave controller events. The only command the bootstrapping cares about
    * is the bootstrapping finished command. When bootstrapping is finished, the bootstrapping
    * process is removed from the ZipRouter and the pairing and platform processes are added.
    * Finally, a request is made to get version information from the ZWave controller.
    * 
    * @param event the ZWave controller event to handle
    */
   @Override
   public void onZWEvent(ZWEvent event) {
      if (event.getType() == ZWEvent.ZWEventType.BOOTSTRAPPED) {
         ZWEventDispatcher.INSTANCE.unregister(this);
         ZWRouter.INSTANCE.registerCmdHandler(Pairing.INSTANCE);
         ZWRouter.INSTANCE.registerCmdHandler(Platform.INSTANCE);
         ZWNetwork zwNetwork = ZWServices.INSTANCE.getNetwork();
         ZWRouter.INSTANCE.registerCmdHandler(zwNetwork);
         zwNetwork.requestVersion();
      }
   }
}

