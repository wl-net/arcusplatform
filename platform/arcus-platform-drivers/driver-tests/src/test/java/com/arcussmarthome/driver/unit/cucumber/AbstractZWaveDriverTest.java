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
package com.arcussmarthome.driver.unit.cucumber;

import groovy.util.GroovyScriptEngine;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.arcussmarthome.capability.registry.CapabilityRegistry;
import com.arcussmarthome.capability.registry.CapabilityRegistryModule;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.messaging.memory.InMemoryMessageModule;
import com.arcussmarthome.core.messaging.memory.InMemoryPlatformMessageBus;
import com.arcussmarthome.core.messaging.memory.InMemoryProtocolMessageBus;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.core.protocol.ProtocolMessageBus;
import com.arcussmarthome.device.attributes.AttributeKey;
import com.arcussmarthome.driver.DeviceDriver;
import com.arcussmarthome.driver.DeviceDriverContext;
import com.arcussmarthome.driver.PlatformDeviceDriverContext;
import com.arcussmarthome.driver.groovy.ClasspathResourceConnector;
import com.arcussmarthome.driver.groovy.GroovyDriverFactory;
import com.arcussmarthome.driver.groovy.customizer.DriverCompilationCustomizer;
import com.arcussmarthome.driver.groovy.GroovyProtocolPluginModule;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.address.ClientAddress;
import com.arcussmarthome.messages.model.Device;
import com.arcussmarthome.messages.model.Fixtures;
import com.arcussmarthome.population.PlacePopulationCacheManager;
import com.arcussmarthome.protocol.Protocol;
import com.arcussmarthome.protocol.zwave.ZWaveProtocol;
import com.arcussmarthome.protocol.zwave.message.ZWaveMessage;
import com.arcussmarthome.protocol.zwave.model.ZWaveNode;
import com.arcussmarthome.test.IrisMockTestCase;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.test.Modules;

@Mocks({DeviceDAO.class, PlacePopulationCacheManager.class})
@Modules({ InMemoryMessageModule.class, CapabilityRegistryModule.class, GroovyProtocolPluginModule.class })
public abstract class AbstractZWaveDriverTest extends IrisMockTestCase implements DriverTestContext<ZWaveMessage> {

    @Inject
    protected PlatformMessageBus platformBus;
    
    @Inject
    protected PlacePopulationCacheManager mockPopulationCacheMgr;

    @Inject
    protected ProtocolMessageBus protocolBus;

    @Inject
    protected CapabilityRegistry capabilityRegistry;

    @Inject
    protected GroovyDriverFactory factory;

    private final ClientAddress clientAddress = Fixtures.createClientAddress();
    private final Address driverAddress = Fixtures.createDeviceAddress();
    private final Address protocolAddress = Fixtures.createProtocolAddress();

    private Device device;
    private DeviceDriver deviceDriver;
    private DeviceDriverContext deviceDriverContext;

    @Provides
    public GroovyScriptEngine scriptEngine(CapabilityRegistry capabilityRegistry) {
        GroovyScriptEngine engine = new GroovyScriptEngine(new ClasspathResourceConnector());
        engine.getConfig().addCompilationCustomizers(new DriverCompilationCustomizer(capabilityRegistry));
        return engine;
    }

    @Override
    public Address getProtocolAddress() {
        return protocolAddress;
    }

    @Override
    public Address getDriverAddress() {
        return driverAddress;
    }

    @Override
    public Protocol<ZWaveMessage> getProtocol() {
        return ZWaveProtocol.INSTANCE;
    }

    @Override
    public DeviceDriverContext getDeviceDriverContext() {
        return deviceDriverContext;
    }

    @Override
    public DeviceDriver getDeviceDriver() {
        return deviceDriver;
    }

    @Override
    public ClientAddress getClientAddress() {
        return clientAddress;
    }

    @Override
    public InMemoryPlatformMessageBus getPlatformBus() {
        return (InMemoryPlatformMessageBus) platformBus;
    }

    @Override
    public InMemoryProtocolMessageBus getProtocolBus() {
        return (InMemoryProtocolMessageBus) protocolBus;
    }

    @Override
    public CapabilityRegistry getCapabilityRegistry() {
        return capabilityRegistry;
    }

    @Override
    public ZWaveNode getDevice() {
        ZWaveNode node = new ZWaveNode((byte)10);
        return node;
    }

    public void initializeDriver(String driverScriptResource) throws Exception {
        deviceDriver = factory.load(driverScriptResource);
        device = createDeviceFromDriver(deviceDriver);
        deviceDriverContext = new PlatformDeviceDriverContext(device, deviceDriver, mockPopulationCacheMgr);
    }

    @SuppressWarnings("unchecked")
    private Device createDeviceFromDriver(DeviceDriver driver) {

        Device device = Fixtures.createDevice();
        for (AttributeKey<?> thisKey : driver.getBaseAttributes().keySet()) {
            Object thisValue = driver.getBaseAttributes().get(thisKey);

            switch (thisKey.getName()) {
            case "devadv:drivername":
                device.setDrivername((String) thisValue);
                break;
            case "devadv:protocol":
                device.setProtocol((String) thisValue);
                break;
            case "dev:devtypehint":
                device.setDevtypehint((String) thisValue);
                break;
            case "base:caps":
                device.setCaps((Set<String>) thisValue);
                break;
            case "dev:vendor":
                device.setVendor((String) thisValue);
                break;
            case "dev:model":
                device.setModel((String) thisValue);
                break;
            }
        }

        device.setDriverversion(driver.getDefinition().getVersion());
        return device;
    }
}

