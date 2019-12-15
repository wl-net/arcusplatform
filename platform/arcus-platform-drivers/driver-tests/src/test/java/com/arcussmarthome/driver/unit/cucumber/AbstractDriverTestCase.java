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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Set;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.arcussmarthome.capability.registry.CapabilityRegistry;
import com.arcussmarthome.capability.registry.CapabilityRegistryModule;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.dao.PersonDAO;
import com.arcussmarthome.core.dao.PersonPlaceAssocDAO;
import com.arcussmarthome.core.driver.DeviceDriverStateHolder;
import com.arcussmarthome.core.messaging.memory.InMemoryMessageModule;
import com.arcussmarthome.core.messaging.memory.InMemoryPlatformMessageBus;
import com.arcussmarthome.core.messaging.memory.InMemoryProtocolMessageBus;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.core.protocol.ProtocolMessageBus;
import com.arcussmarthome.device.attributes.AttributeKey;
import com.arcussmarthome.device.attributes.AttributeMap;
import com.arcussmarthome.driver.DeviceDriver;
import com.arcussmarthome.driver.DeviceDriverContext;
import com.arcussmarthome.driver.PlatformDeviceDriverContext;
import com.arcussmarthome.driver.groovy.GroovyDriverFactory;
import com.arcussmarthome.driver.groovy.GroovyDriverModule;
import com.arcussmarthome.driver.groovy.pin.PinManagementContext;
import com.arcussmarthome.driver.service.executor.DefaultDriverExecutor;
import com.arcussmarthome.driver.service.executor.DriverExecutor;
import com.arcussmarthome.driver.unit.cucumber.MockGroovyDriverModule.CapturedScheduledEvent;
import com.arcussmarthome.driver.unit.cucumber.MockGroovyDriverModule.CapturingSchedulerContext;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.address.ClientAddress;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.DeviceConnectionCapability;
import com.arcussmarthome.messages.capability.PresenceCapability;
import com.arcussmarthome.messages.model.Device;
import com.arcussmarthome.messages.model.Fixtures;
import com.arcussmarthome.population.PlacePopulationCacheManager;
import com.arcussmarthome.test.IrisMockTestCase;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.test.Modules;

import groovy.util.GroovyScriptEngine;

@Mocks({DeviceDAO.class, PersonDAO.class, PlacePopulationCacheManager.class})
@Modules({ InMemoryMessageModule.class, CapabilityRegistryModule.class, MockGroovyDriverModule.class })
public class AbstractDriverTestCase extends IrisMockTestCase {

	private final static Logger logger = LoggerFactory.getLogger(AbstractDriverTestCase.class) ;

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
    
   @Inject
   protected DeviceDAO deviceDAO;

   @Inject
   protected CapturingSchedulerContext scheduler;

   @Inject
   private PinManagementContext pinManager;

    private final ClientAddress clientAddress = Fixtures.createClientAddress();
    private final Address driverAddress = Fixtures.createDeviceAddress();
    private final Address protocolAddress = Fixtures.createProtocolAddress();

    protected DeviceDriver deviceDriver;
    protected PlatformDeviceDriverContext deviceDriverContext;
    protected DriverExecutor executor;

    @Inject
    public void cacheScripts(GroovyScriptEngine engine) throws MalformedURLException {
        engine.getConfig().setRecompileGroovySource(false);
        engine.getConfig().setTargetDirectory("build/drivers");
    }
    
    @Provides @Named(GroovyDriverModule.NAME_GROOVY_DRIVER_DIRECTORIES)
    public Set<URL> groovyDriverUrls() throws MalformedURLException {
       return ImmutableSet.of(
             new File("src/test/resources").toURI().toURL(),
             new File("../../arcus-containers/driver-services/src/main/resources").toURI().toURL(),
             new File("build/drivers").toURI().toURL()
       );

    }
    
    @Provides
    public PersonPlaceAssocDAO providesPersonPlaceAssocDao() {
       return EasyMock.createMock(PersonPlaceAssocDAO.class);
    }
    
    public Address getProtocolAddress() {
        return protocolAddress;
    }

    public Address getDriverAddress() {
        return driverAddress;
    }

    public Device getDevice() {
       Preconditions.checkState(deviceDriverContext != null, "Must call initializeDriver first");
       return deviceDriverContext.getDevice();
    }
    
    public DeviceDriverContext getDeviceDriverContext() {
        return deviceDriverContext;
    }

    public DeviceDriver getDeviceDriver() {
        return deviceDriver;
    }

    public ClientAddress getClientAddress() {
        return clientAddress;
    }
    
    public DriverExecutor getDriverExecutor() {
		return executor;
	}

	public InMemoryPlatformMessageBus getPlatformBus() {
        return (InMemoryPlatformMessageBus) platformBus;
    }

    public InMemoryProtocolMessageBus getProtocolBus() {
        return (InMemoryProtocolMessageBus) protocolBus;
    }

    public CapabilityRegistry getCapabilityRegistry() {
        return capabilityRegistry;
    }
    
    public PinManagementContext getPinManager() {
       return pinManager;
    }
    
    @Nullable
    public CapturedScheduledEvent pollScheduledEvent() {
       return scheduler.events().poll();
    }

    public void initializeDriver(String driverScriptResource) throws Exception {
       logger.info("Initializing driver: {}", driverScriptResource);
       assert null != factory: "Factory null";
       deviceDriver = factory.load(driverScriptResource);
       Device device = createDeviceFromDriver(deviceDriver);
       deviceDriverContext = new PlatformDeviceDriverContext(device, deviceDriver, mockPopulationCacheMgr);
       // initialize online state so that it doesn't generate a ValueChange in the test cases
       deviceDriverContext.setAttributeValue(DeviceConnectionCapability.KEY_STATE, DeviceConnectionCapability.STATE_ONLINE);
       deviceDriverContext.setAttributeValue(DeviceConnectionCapability.KEY_LASTCHANGE, new Date());
       if(deviceDriverContext.getAttributeValue(Capability.KEY_CAPS).contains(PresenceCapability.NAMESPACE)) {
          deviceDriverContext.setAttributeValue(PresenceCapability.KEY_PRESENCE, PresenceCapability.PRESENCE_PRESENT);
          deviceDriverContext.setAttributeValue(PresenceCapability.KEY_PRESENCECHANGED, new Date());
       }
       deviceDriverContext.clearDirty();
       
       executor = new DefaultDriverExecutor(deviceDriver, deviceDriverContext, null, 10);

       /* adding mock behavior for mock DeviceDAO
        * This will make the PlatformDeviceDriverContext to think that the device is dirty and trigger deviceDao.save.
        * It is mock to return a not null value to avoid NullPointerException.
        * 
        * The plumbing is needed to set endpoint to the AttributeMap. ( see ~/^the Zigbee device has endpoint (.+)$/)
        * Likewise, it is apply to the deviceDao.updateDriverState
        */
        Capture<Device> deviceRef = EasyMock.newCapture(CaptureType.LAST);
        expect(deviceDAO.save(EasyMock.capture(deviceRef))).andAnswer(() -> deviceRef.getValue().copy()).anyTimes();
        deviceDAO.updateDriverState(anyObject(Device.class), anyObject(DeviceDriverStateHolder.class));
        EasyMock.expectLastCall().anyTimes();
        EasyMock.replay(deviceDAO);

    }

    @SuppressWarnings("unchecked")
    protected Device createDeviceFromDriver(DeviceDriver driver) {

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
    
    protected <V> void addProtocolAttribute(AttributeKey<V> key, V value) {
       AttributeMap copy = getDevice().getProtocolAttributes();
       copy.set(key, value);
       getDevice().setProtocolAttributes(copy);
    }
 
}

