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

import com.google.inject.multibindings.Multibinder;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.driver.groovy.control.ControlProtocolPlugin;
import com.arcussmarthome.driver.groovy.ipcd.IpcdProtocolPlugin;
import com.arcussmarthome.driver.groovy.mock.MockProtocolPlugin;
import com.arcussmarthome.driver.groovy.plugin.GroovyDriverPlugin;
import com.arcussmarthome.driver.groovy.reflex.ReflexPlugin;
import com.arcussmarthome.driver.groovy.zigbee.ZigbeeProtocolPlugin;
import com.arcussmarthome.driver.groovy.zwave.ZWaveProtocolPlugin;

public class GroovyProtocolPluginModule extends AbstractIrisModule {
   @Override
   protected void configure() {
      Multibinder<GroovyDriverPlugin> plugins = bindSetOf(GroovyDriverPlugin.class);
      plugins.addBinding().to(ZWaveProtocolPlugin.class);
      plugins.addBinding().to(ZigbeeProtocolPlugin.class);
      plugins.addBinding().to(IpcdProtocolPlugin.class);
      plugins.addBinding().to(ControlProtocolPlugin.class);
      plugins.addBinding().to(MockProtocolPlugin.class);
      plugins.addBinding().to(ReflexPlugin.class);
   }
}

