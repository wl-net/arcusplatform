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
package com.arcussmarthome.hubcom.server;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.platform.pairing.PairingDeviceDaoModule;
import com.arcussmarthome.bridge.bus.PlatformBusListener;
import com.arcussmarthome.bridge.bus.PlatformBusService;
import com.arcussmarthome.bridge.bus.ProtocolBusListener;
import com.arcussmarthome.bridge.bus.ProtocolBusService;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.BridgeConfigModule;
import com.arcussmarthome.bridge.server.config.BridgeServerConfig;
import com.arcussmarthome.bridge.server.http.RequestAuthorizer;
import com.arcussmarthome.bridge.server.http.RequestHandler;
import com.arcussmarthome.bridge.server.http.RequestMatcher;
import com.arcussmarthome.bridge.server.http.handlers.CheckPage;
import com.arcussmarthome.bridge.server.http.handlers.IndexPage;
import com.arcussmarthome.bridge.server.http.handlers.RootRedirect;
import com.arcussmarthome.bridge.server.http.handlers.RootRemoteRedirect;
import com.arcussmarthome.bridge.server.http.impl.auth.AlwaysAllow;
import com.arcussmarthome.bridge.server.http.impl.matcher.WebSocketUpgradeMatcher;
import com.arcussmarthome.bridge.server.message.DeviceMessageHandler;
import com.arcussmarthome.bridge.server.netty.Binary10WebSocketServerHandlerProvider;
import com.arcussmarthome.bridge.server.netty.Bridge10ChannelInitializer;
import com.arcussmarthome.bridge.server.session.SessionListener;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTlsContext;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTlsContextImpl;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTrustManagerFactory;
import com.arcussmarthome.hubcom.authz.DefaultHubMessageFilterImpl;
import com.arcussmarthome.hubcom.authz.HubMessageFilter;
import com.arcussmarthome.hubcom.bus.HubPlatformBusListener;
import com.arcussmarthome.hubcom.bus.HubProtocolBusListener;
import com.arcussmarthome.hubcom.bus.PlatformBusServiceImpl;
import com.arcussmarthome.hubcom.bus.ProtocolBusServiceImpl;
import com.arcussmarthome.hubcom.server.message.DirectMessageHandler;
import com.arcussmarthome.hubcom.server.message.HubConnectedHandler;
import com.arcussmarthome.hubcom.server.message.HubFirmwareUpdateResponseHandler;
import com.arcussmarthome.hubcom.server.message.HubFirmwareUpgradeProcessEventHandler;
import com.arcussmarthome.hubcom.server.message.HubMessageHandler;
import com.arcussmarthome.hubcom.server.message.HubRegisteredResponseHandler;
import com.arcussmarthome.hubcom.server.session.HubAuthModule;
import com.arcussmarthome.hubcom.server.session.HubSessionModule;
import com.arcussmarthome.hubcom.server.ssl.HubTrustManagerFactoryImpl;
import com.arcussmarthome.hubcom.server.ssl.TrustConfig;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.address.AddressMatchers;
import com.arcussmarthome.platform.hub.registration.HubRegistrationModule;
import com.arcussmarthome.population.PlacePopulationCacheModule;
import com.netflix.governator.annotations.Modules;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

@Modules(include = {
        HubRegistrationModule.class,
        PlacePopulationCacheModule.class,
        PairingDeviceDaoModule.class
})
public class HubServerModule extends AbstractIrisModule {
    private static final Logger log = LoggerFactory.getLogger(HubServerModule.class);
    private boolean clientAuthRequired;

    @Inject
    public HubServerModule(BridgeServerConfig config, BridgeConfigModule bridge, HubAuthModule noauth, HubSessionModule sessions) {
        this.clientAuthRequired = config.isTlsNeedClientAuth();
    }

    @Override
    protected void configure() {
        bind(TrustConfig.class);

        bind(HubMessageFilter.class).to(DefaultHubMessageFilterImpl.class);

        bind(BridgeServerTlsContext.class).to(BridgeServerTlsContextImpl.class);
        bind(BridgeServerTrustManagerFactory.class).to(HubTrustManagerFactoryImpl.class);
        bind(PlatformBusService.class).to(PlatformBusServiceImpl.class).asEagerSingleton();
        bind(ProtocolBusService.class).to(ProtocolBusServiceImpl.class).asEagerSingleton();
        bindSetOf(PlatformBusListener.class)
                .addBinding()
                .to(HubPlatformBusListener.class);

        bindSetOf(ProtocolBusListener.class)
                .addBinding()
                .to(HubProtocolBusListener.class);

        bind(new TypeLiteral<DeviceMessageHandler<ByteBuf>>() {
        }).to(HubMessageHandler.class);

        Multibinder<DirectMessageHandler> directHandlers = bindSetOf(DirectMessageHandler.class);
        directHandlers.addBinding().to(HubConnectedHandler.class);
        directHandlers.addBinding().to(HubRegisteredResponseHandler.class);
        directHandlers.addBinding().to(HubFirmwareUpgradeProcessEventHandler.class);
        directHandlers.addBinding().to(HubFirmwareUpdateResponseHandler.class);


        bind(new TypeLiteral<ChannelInitializer<SocketChannel>>() {
        }).to(Bridge10ChannelInitializer.class);
        bind(ChannelInboundHandler.class).toProvider(Binary10WebSocketServerHandlerProvider.class);

        bind(RequestMatcher.class).annotatedWith(Names.named("WebSocketUpgradeMatcher")).to(WebSocketUpgradeMatcher.class);
        bind(RequestAuthorizer.class).annotatedWith(Names.named("SessionAuthorizer")).to(AlwaysAllow.class);

        // No Session Listeners
        Multibinder<SessionListener> slBindings = Multibinder.newSetBinder(binder(), SessionListener.class);

        // Bind Http Handlers
        Multibinder<RequestHandler> rhBindings = Multibinder.newSetBinder(binder(), RequestHandler.class);
        if (clientAuthRequired) {
            rhBindings.addBinding().to(RootRedirect.class);
            rhBindings.addBinding().to(IndexPage.class);
        } else {
            rhBindings.addBinding().to(RootRemoteRedirect.class);
        }
        rhBindings.addBinding().to(CheckPage.class);

    }

    @Provides
    @Named(DefaultHubMessageFilterImpl.ADMIN_ADDRESS_PROP)
    @Singleton
    public Predicate<Address> getHubAdminAddresses(@Named("hub.bridge.admin.addresses") String adminAddresses) {
        return AddressMatchers.fromCsvString(adminAddresses);
    }

    @Provides
    @Named(DefaultHubMessageFilterImpl.ADMIN_ONLY_MESSAGES_PROP)
    @Singleton
    public Predicate<String> getHubAdminOnlyMessages(@Named("hub.bridge.admin.only.messages") String adminOnlyMessages) {
        List<Predicate<String>> components = new ArrayList<>();

        for (String msg : Splitter.on(',').omitEmptyStrings().trimResults().split(adminOnlyMessages)) {
            List<String> parts = Splitter.on(':').splitToList(msg);
            switch (parts.size()) {
                case 2:
                    if (parts.get(1).isEmpty() || "*".equals(parts.get(1))) {
                        final String capMatch = parts.get(0) + ":";
                        log.warn("adding all commands to admin only list: {}", parts.get(0));
                        components.add(new Predicate<String>() {
                            @Override
                            public boolean apply(String msgType) {
                                return msgType != null && msgType.startsWith(capMatch);
                            }
                        });
                    } else {
                        log.warn("adding command to admin only list: {}", msg);
                        components.add(Predicates.equalTo(msg));
                    }
                    break;

                default:
                    log.warn("adding non-conforming command to admin only list: {}", msg);
                    components.add(Predicates.equalTo(msg));
                    break;
            }

        }

        switch (components.size()) {
            case 0:
                return Predicates.alwaysFalse();

            case 1:
                return components.get(0);

            default:
                return Predicates.or(components);
        }
    }

    @Provides
    @Singleton
    public BridgeMetrics provideBridgeMetrics() {
        return new BridgeMetrics("hub");
    }

}

