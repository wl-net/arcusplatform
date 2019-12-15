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
package com.arcussmarthome.ipcd.session;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.arcussmarthome.bridge.bus.ProtocolBusService;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.session.DefaultSessionImpl;
import com.arcussmarthome.bridge.server.session.SessionRegistry;
import com.arcussmarthome.bridge.server.session.SessionUtil;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.core.protocol.ipcd.IpcdDeviceDao;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.service.IpcdService;
import com.arcussmarthome.platform.partition.DefaultPartition;
import com.arcussmarthome.platform.partition.Partitioner;
import com.arcussmarthome.platform.partition.PlatformPartition;
import com.arcussmarthome.population.PlacePopulationCacheManager;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.protocol.control.ControlProtocol;
import com.arcussmarthome.protocol.control.DeviceOnlineEvent;
import com.arcussmarthome.protocol.ipcd.IpcdDevice;
import com.arcussmarthome.protocol.ipcd.IpcdDevice.ConnState;
import com.arcussmarthome.protocol.ipcd.IpcdProtocol;
import com.arcussmarthome.protocol.ipcd.message.IpcdMessage;
import com.arcussmarthome.protocol.ipcd.message.model.Device;
import com.arcussmarthome.protocol.ipcd.message.model.DeviceInfo;

import io.netty.channel.Channel;

public abstract class IpcdSession extends DefaultSessionImpl implements PartitionedSession {

   private final static Logger logger = LoggerFactory.getLogger(IpcdSession.class);

   private volatile IpcdDevice.RegistrationState registrationState;
   private volatile PlatformPartition partition;

   protected final IpcdDeviceDao ipcdDeviceDao;
   protected final ProtocolBusService protocolBusService;
   protected final DeviceDAO deviceDao;
   protected final PlaceDAO placeDao;
   protected final PlatformMessageBus platformBus;
   protected final Partitioner partitioner;
   protected final PlacePopulationCacheManager populationCacheMgr;

   public IpcdSession(
         SessionRegistry parent,
         IpcdDeviceDao ipcdDeviceDao,
         DeviceDAO deviceDao,
         PlaceDAO placeDao,
         Channel channel,
         PlatformMessageBus platformBus,
         ProtocolBusService protocolBusService,
         Partitioner partitioner,
         BridgeMetrics bridgeMetrics, 
         PlacePopulationCacheManager populationCacheMgr) {

      super(parent, channel, bridgeMetrics);
      this.ipcdDeviceDao = ipcdDeviceDao;
      this.deviceDao = deviceDao;
      this.placeDao = placeDao;
      this.platformBus = platformBus;
      this.protocolBusService = protocolBusService;
      this.partitioner = partitioner;
      this.populationCacheMgr = populationCacheMgr;
   }

   public abstract void sendMessage(IpcdMessage msg);

   public void initializeSession(Device device) {
      initializeSession(device, null);
   }

   public void initializeSession(Device device, DeviceInfo deviceInfo) {
      logger.debug("Initializing IPCD Session with Device {}", device);
      Address protocolAddress = IpcdProtocol.ipcdAddress(device);
      IpcdDevice ipcdDevice = getOrCreateIpcdDevice(protocolAddress.getRepresentation(), device, deviceInfo);
      setClientToken(IpcdClientToken.fromProtocolAddress(protocolAddress));
      if(ipcdDevice.getPlaceId() == null) {
      	SessionUtil.clearPlace(this);
      }else{
      	SessionUtil.setPlace(ipcdDevice.getPlaceId(), this);
      }
      this.registrationState = ipcdDevice.getRegistrationState();
      reportOnline(ipcdDevice.getDevice());
   }

   public IpcdDevice.RegistrationState getRegistrationState() {
      return registrationState;
   }

   @Override
   public PlatformPartition getPartition() {
      return partition;
   }

   @Override
   public void setActivePlace(String placeId) {
      super.setActivePlace(placeId);
      this.partition = placeId == null ? new DefaultPartition(0) : partitioner.getPartitionForPlaceId(placeId);
   }

   public void claim(String accountId, String placeId, String population) {
      setActivePlace(placeId);
      this.registrationState = IpcdDevice.RegistrationState.PENDING_DRIVER;
   }

   public void register(String accountId, String placeId, String population, String driverAddress) {
      this.setActivePlace(placeId);
      this.registrationState = IpcdDevice.RegistrationState.REGISTERED;
   }

   public void unregister() {
   	SessionUtil.clearPlace(this);
      this.registrationState = IpcdDevice.RegistrationState.UNREGISTERED;
   }

   public void onFactoryReset() {
   	SessionUtil.clearPlace(this);
   }

   private void reportOnline(Device device) {
      if(getActivePlace() != null) {
         ProtocolMessage onlineMsg = ProtocolMessage.builder()
               .broadcast()
               .from(getClientToken().getRepresentation())
               .withPlaceId(getActivePlace())
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(getActivePlace()))
               .withPayload(ControlProtocol.INSTANCE, DeviceOnlineEvent.create())
               .create();
         protocolBusService.placeMessageOnProtocolBus(onlineMsg);

         MessageBody connectedMsg = IpcdService.DeviceConnectedEvent.builder()
               .withProtocolAddress(getClientToken().getRepresentation())
               .build();

         PlatformMessage msg = PlatformMessage.buildMessage(connectedMsg, Address.bridgeAddress(IpcdProtocol.NAMESPACE), Address.platformService(IpcdService.NAMESPACE))
               .withActor(Address.clientAddress("ipcd-bridge", String.valueOf(partitioner.getMemberId())))
               .withPlaceId(getActivePlace())
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(getActivePlace()))
               .create();
         platformBus.send(msg);
      }
   }

   protected void onReportOnline(Device device) {

   }

   private IpcdDevice getOrCreateIpcdDevice(String protocolAddress, Device device, DeviceInfo deviceInfo) {
      if(isMixedCase(device.getSn())) {
         logger.warn("[{}] has a mixed case SN, which will almost certainly cause an issue registering the device", device);
      }
      IpcdDevice ipcdDevice = ipcdDeviceDao.findByProtocolAddress(protocolAddress);
      if (ipcdDevice == null) {
         ipcdDevice = new IpcdDevice();
         ipcdDevice.setProtocolAddress(protocolAddress);
      }

      ipcdDevice.updateWithDevice(device);
      if(deviceInfo != null) {
         ipcdDevice.updateWithDeviceInfo(deviceInfo);
      }

      //Make sure the ipcd-device table is in sync with the device table.
      ipcdDevice = syncIpcdDeviceToIrisDevice(ipcdDevice);
      ipcdDevice.setConnState(ConnState.ONLINE);
      ipcdDevice.setLastConnected(new Date());
      ipcdDeviceDao.save(ipcdDevice);
      return ipcdDevice;
   }

   private IpcdDevice syncIpcdDeviceToIrisDevice(IpcdDevice ipcdDevice) {
      com.arcussmarthome.messages.model.Device irisDevice = deviceDao.findByProtocolAddress(ipcdDevice.getProtocolAddress());
      if (irisDevice != null) {
         ipcdDevice.syncToIrisDevice(irisDevice);
      }
      return ipcdDevice;
   }

   private boolean isMixedCase(String sn) {
      return CharMatcher.JAVA_LOWER_CASE.matchesAnyOf(sn) && CharMatcher.JAVA_UPPER_CASE.matchesAnyOf(sn);
   }
}

