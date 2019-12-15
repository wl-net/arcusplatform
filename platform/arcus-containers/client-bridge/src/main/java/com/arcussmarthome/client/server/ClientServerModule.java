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
package com.arcussmarthome.client.server;

import java.net.URISyntaxException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.bridge.bus.PlatformBusListener;
import com.arcussmarthome.bridge.bus.PlatformBusService;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.BridgeConfigModule;
import com.arcussmarthome.bridge.server.config.BridgeServerConfig;
import com.arcussmarthome.bridge.server.config.RESTHandlerConfig;
import com.arcussmarthome.bridge.server.http.RequestAuthorizer;
import com.arcussmarthome.bridge.server.http.RequestHandler;
import com.arcussmarthome.bridge.server.http.RequestMatcher;
import com.arcussmarthome.bridge.server.http.Responder;
import com.arcussmarthome.bridge.server.http.handlers.CheckPage;
import com.arcussmarthome.bridge.server.http.handlers.IndexPage;
import com.arcussmarthome.bridge.server.http.handlers.LoginPage;
import com.arcussmarthome.bridge.server.http.handlers.RootRedirect;
import com.arcussmarthome.bridge.server.http.handlers.SessionLogin;
import com.arcussmarthome.bridge.server.http.handlers.SessionLogout;
import com.arcussmarthome.bridge.server.http.handlers.WebAppStaticResources;
import com.arcussmarthome.bridge.server.http.impl.auth.SessionAuth;
import com.arcussmarthome.bridge.server.http.impl.matcher.WebSocketUpgradeMatcher;
import com.arcussmarthome.bridge.server.http.impl.responder.SessionLoginResponder;
import com.arcussmarthome.bridge.server.http.impl.responder.SessionLogoutResponder;
import com.arcussmarthome.bridge.server.message.DeviceMessageHandler;
import com.arcussmarthome.bridge.server.netty.Authenticator;
import com.arcussmarthome.bridge.server.netty.WebSocketServerHandlerProvider;
import com.arcussmarthome.bridge.server.session.DefaultSessionFactoryImpl;
import com.arcussmarthome.bridge.server.session.DefaultSessionRegistryImpl;
import com.arcussmarthome.bridge.server.session.SessionFactory;
import com.arcussmarthome.bridge.server.session.SessionListener;
import com.arcussmarthome.bridge.server.session.SessionRegistry;
import com.arcussmarthome.bridge.server.shiro.ShiroAuthenticator;
import com.arcussmarthome.bridge.server.shiro.ShiroModule;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTlsContext;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTlsContextImpl;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTrustManagerFactory;
import com.arcussmarthome.bridge.server.ssl.NullTrustManagerFactoryImpl;
import com.arcussmarthome.client.bounce.AppFallbackHandler;
import com.arcussmarthome.client.bounce.AppLaunchHandler;
import com.arcussmarthome.client.bounce.AppleAppSiteAssociationHandler;
import com.arcussmarthome.client.bounce.WebLaunchHandler;
import com.arcussmarthome.client.bounce.WebRunHandler;
import com.arcussmarthome.client.eas.EasCodeManager;
import com.arcussmarthome.client.nws.SameCodeManager;
import com.arcussmarthome.client.security.SubscriberAuthorizationContextLoader;
import com.arcussmarthome.client.server.rest.AcceptInvitationCreateLoginRESTHandler;
import com.arcussmarthome.client.server.rest.ChangePasswordRESTHandler;
import com.arcussmarthome.client.server.rest.ChangePinRESTHandler;
import com.arcussmarthome.client.server.rest.ChangePinV2RESTHandler;
import com.arcussmarthome.client.server.rest.CreateAccountRESTHandler;
import com.arcussmarthome.client.server.rest.FindProductsRESTHandler;
import com.arcussmarthome.client.server.rest.GetBrandsRESTHandler;
import com.arcussmarthome.client.server.rest.GetCategoriesRESTHandler;
import com.arcussmarthome.client.server.rest.GetInvitationRESTHandler;
import com.arcussmarthome.client.server.rest.GetInvoiceRESTHandler;
import com.arcussmarthome.client.server.rest.GetProductCatalogRESTHandler;
import com.arcussmarthome.client.server.rest.GetProductRESTHandler;
import com.arcussmarthome.client.server.rest.GetProductsByBrandRESTHandler;
import com.arcussmarthome.client.server.rest.GetProductsByCategoryRESTHandler;
import com.arcussmarthome.client.server.rest.GetProductsRESTHandler;
import com.arcussmarthome.client.server.rest.GetSameCodeRESTHandler;
import com.arcussmarthome.client.server.rest.ListEasCodesRESTHandler;
import com.arcussmarthome.client.server.rest.ListSameCountiesRESTHandler;
import com.arcussmarthome.client.server.rest.ListSameStatesRESTHandler;
import com.arcussmarthome.client.server.rest.ListTimezonesRESTHandler;
import com.arcussmarthome.client.server.rest.LoadLocalizedStringsRESTHandler;
import com.arcussmarthome.client.server.rest.LockDeviceRESTHandler;
import com.arcussmarthome.client.server.rest.RequestEmailVerificatonRESTHandler;
import com.arcussmarthome.client.server.rest.ResetPasswordRESTHandler;
import com.arcussmarthome.client.server.rest.SendPasswordResetRESTHandler;
import com.arcussmarthome.client.server.rest.SessionLogRESTHandler;
import com.arcussmarthome.client.server.rest.VerifyEmailRESTHandler;
import com.arcussmarthome.client.server.rest.VerifyPinRESTHandler;
import com.arcussmarthome.client.server.session.HandshakeSessionListener;
import com.arcussmarthome.client.server.session.StopCameraPreviewsUploadSessionListener;
import com.arcussmarthome.core.template.TemplateModule;
import com.arcussmarthome.messages.MessageConstants;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.services.PlatformConstants;
import com.arcussmarthome.netty.bus.IrisNettyPlatformBusListener;
import com.arcussmarthome.netty.bus.IrisNettyPlatformBusServiceImpl;
import com.arcussmarthome.netty.security.IrisNettyAuthorizationContextLoader;
import com.arcussmarthome.netty.security.IrisNettyNoopAuthorizationContextLoader;
import com.arcussmarthome.netty.server.message.GetPreferencesHandler;
import com.arcussmarthome.netty.server.message.IrisNettyMessageHandler;
import com.arcussmarthome.netty.server.message.ResetPreferenceHandler;
import com.arcussmarthome.netty.server.message.SetActivePlaceHandler;
import com.arcussmarthome.netty.server.message.SetPreferencesHandler;
import com.arcussmarthome.netty.server.netty.IrisNettyCORSChannelInitializer;
import com.arcussmarthome.platform.location.TimezonesModule;
import com.arcussmarthome.platform.notification.audit.CassandraAuditor;
import com.arcussmarthome.platform.notification.audit.NotificationAuditor;
import com.arcussmarthome.platform.subscription.IrisSubscriptionModule;
import com.arcussmarthome.population.PlacePopulationCacheModule;
import com.arcussmarthome.prodcat.ProductCatalogModule;
import com.arcussmarthome.prodcat.ProductCatalogReloadListener;
import com.arcussmarthome.resource.Resource;
import com.arcussmarthome.resource.Resources;
import com.arcussmarthome.util.ThreadPoolBuilder;
import com.netflix.governator.annotations.Modules;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

@Modules(include={
		ProductCatalogModule.class,
		TimezonesModule.class,
		PlacePopulationCacheModule.class
})
public class ClientServerModule extends AbstractIrisModule {
   public static final String AUTHZ_LOADER_PROP = "authz.contextLoader";
   public static final String AUTHZ_LOADER_NONE = "none";
   public static final String DEBUG_PROP = "deploy.debug";
   public static final String TIMEZONE_PATH_PROP = "timezones.path";
   public static final String SAMECODE_PATH_PROP = "samecodes.path";
   public static final String EASCODE_PATH_PROP = "eascodes.path";
   
   /* 
    * I Want to be specific here because I have found cases where the NWS State Codes don't always
    * match globally recognized abbreviations for territories outside the United States
    */
   public static final String SAMECODE_STATEMAPPINGS_PATH_PROP = "samecodes.statename.mappings.path";
   
   @Inject(optional = true)
   @Named(AUTHZ_LOADER_PROP)
   private String algorithm = AUTHZ_LOADER_NONE;

   @Inject(optional = true)
   @Named(DEBUG_PROP)
   private boolean debug = false;

   @Inject(optional = true)
   @Named(SAMECODE_PATH_PROP)
   private String sameCodesPath = "conf/same-codes.csv";
   
   @Inject(optional = true)
   @Named(SAMECODE_STATEMAPPINGS_PATH_PROP)
   private String sameCodeStateMappingsPath = "conf/samecode-statename-mappings.csv";

   @Inject(optional = true)
   @Named(EASCODE_PATH_PROP)
   private String easCodesPath = "conf/eas-event-codes.csv";
   
	@Inject(optional=true)
	@Named("client.background.threads")
	private int threads = 100;

	@Inject(optional=true)
	@Named("client.background.threadKeepAliveMs")
	private long threadKeepAliveMs = 10000;
	
   private ExecutorService executor;
   
   @Inject
   public ClientServerModule(BridgeConfigModule bridge, ShiroModule shiro, TemplateModule template, IrisSubscriptionModule subscription) {
   	this.executor = 
   			new ThreadPoolBuilder()
   				.withMaxPoolSize(threads)
   				.withKeepAliveMs(threadKeepAliveMs)
   				.withNameFormat("async-request-%d")
   				.withMetrics("client.background")
   				.build();
   }

   @Override
   protected void configure() {
      bind(BridgeServerConfig.class);
      bind(BridgeServerTlsContext.class).to(BridgeServerTlsContextImpl.class);
      bind(BridgeServerTrustManagerFactory.class).to(NullTrustManagerFactoryImpl.class);
      bind(PlatformBusService.class).to(IrisNettyPlatformBusServiceImpl.class).asEagerSingleton();
      bindSetOf(PlatformBusListener.class)
         .addBinding()
         .to(IrisNettyPlatformBusListener.class);      
      bind(new TypeLiteral<DeviceMessageHandler<String>>(){}).to(IrisNettyMessageHandler.class);
      bind(Authenticator.class).to(ShiroAuthenticator.class);
      bind(SessionFactory.class).to(DefaultSessionFactoryImpl.class);
      bind(SessionRegistry.class).to(DefaultSessionRegistryImpl.class);      
      bind(NotificationAuditor.class).to(CassandraAuditor.class);

      if(algorithm.equalsIgnoreCase(AUTHZ_LOADER_NONE)) {
         bind(IrisNettyAuthorizationContextLoader.class).to(IrisNettyNoopAuthorizationContextLoader.class);
      } else {
         bind(IrisNettyAuthorizationContextLoader.class).to(SubscriberAuthorizationContextLoader.class);
      }

      
      Multibinder<SessionListener> slBindings = Multibinder.newSetBinder(binder(), SessionListener.class);
      slBindings.addBinding().to(HandshakeSessionListener.class);
      slBindings.addBinding().to(StopCameraPreviewsUploadSessionListener.class);

      bind(ChannelInboundHandler.class).toProvider(WebSocketServerHandlerProvider.class);
      bind(new TypeLiteral<ChannelInitializer<SocketChannel>>(){})
         .to(IrisNettyCORSChannelInitializer.class);

      bind(RequestMatcher.class).annotatedWith(Names.named("WebSocketUpgradeMatcher")).to(WebSocketUpgradeMatcher.class);
      bind(RequestAuthorizer.class).annotatedWith(Names.named("SessionAuthorizer")).to(SessionAuth.class);

      //Bind Login and Logout
      bind(Responder.class).annotatedWith(Names.named("SessionLogin")).to(SessionLoginResponder.class);
      bind(Responder.class).annotatedWith(Names.named("SessionLogout")).to(SessionLogoutResponder.class);
      
      // Bind Http Handlers
      Multibinder<RequestHandler> rhBindings = Multibinder.newSetBinder(binder(), RequestHandler.class);
      rhBindings.addBinding().to(SessionLogin.class);
      rhBindings.addBinding().to(RootRedirect.class);
      rhBindings.addBinding().to(CheckPage.class);
      rhBindings.addBinding().to(IndexPage.class);
      rhBindings.addBinding().to(LoginPage.class);
      
      /* Bounce handlers */
      rhBindings.addBinding().to(AppleAppSiteAssociationHandler.class);
      rhBindings.addBinding().to(AppFallbackHandler.class);
      rhBindings.addBinding().to(AppLaunchHandler.class);
      rhBindings.addBinding().to(WebLaunchHandler.class);
      rhBindings.addBinding().to(WebRunHandler.class);
      
      /* ProductCatalog RestHandlers */
      rhBindings.addBinding().to(GetProductCatalogRESTHandler.class);
      rhBindings.addBinding().to(GetProductsRESTHandler.class);
      rhBindings.addBinding().to(FindProductsRESTHandler.class);
      rhBindings.addBinding().to(GetBrandsRESTHandler.class);
      rhBindings.addBinding().to(GetCategoriesRESTHandler.class);
      rhBindings.addBinding().to(GetProductRESTHandler.class);
      rhBindings.addBinding().to(GetProductsByBrandRESTHandler.class);
      rhBindings.addBinding().to(GetProductsByCategoryRESTHandler.class);

      // NWS SAME and EAS Code REST Handlers
      rhBindings.addBinding().to(GetSameCodeRESTHandler.class);
      rhBindings.addBinding().to(ListSameCountiesRESTHandler.class);
      rhBindings.addBinding().to(ListSameStatesRESTHandler.class);    
      rhBindings.addBinding().to(ListEasCodesRESTHandler.class);

      /* Other RestHandlers */
      rhBindings.addBinding().to(ChangePinRESTHandler.class);
      rhBindings.addBinding().to(ChangePinV2RESTHandler.class);
      rhBindings.addBinding().to(VerifyPinRESTHandler.class);
      rhBindings.addBinding().to(CreateAccountRESTHandler.class);
      rhBindings.addBinding().to(ChangePasswordRESTHandler.class);
      rhBindings.addBinding().to(LoadLocalizedStringsRESTHandler.class);
      rhBindings.addBinding().to(ResetPasswordRESTHandler.class);
      rhBindings.addBinding().to(SendPasswordResetRESTHandler.class);
      rhBindings.addBinding().to(SessionLogout.class);
      rhBindings.addBinding().to(SessionLogRESTHandler.class);
      rhBindings.addBinding().to(ListTimezonesRESTHandler.class);
      rhBindings.addBinding().to(AcceptInvitationCreateLoginRESTHandler.class);
      rhBindings.addBinding().to(GetInvitationRESTHandler.class);
      rhBindings.addBinding().to(GetInvoiceRESTHandler.class);
      rhBindings.addBinding().to(LockDeviceRESTHandler.class);
      rhBindings.addBinding().to(RequestEmailVerificatonRESTHandler.class);
      rhBindings.addBinding().to(VerifyEmailRESTHandler.class);

      /* Static resource handler */
      // should be bound last because its matcher is super-greedy 
      rhBindings.addBinding().to(WebAppStaticResources.class);
   }
   
   @Provides
   @Singleton
   public RESTHandlerConfig provideRestHandlerConfig() {
	   RESTHandlerConfig restHandlerConfig = new RESTHandlerConfig();
	   restHandlerConfig.setSendChunked(false);
	   return restHandlerConfig;
   }
   
   @Provides
   @Singleton
   @Named("UseChunkedSender")
   public RESTHandlerConfig provideChunkedRestHandlerConfig() {
	   RESTHandlerConfig restHandlerConfig = new RESTHandlerConfig();
	   restHandlerConfig.setSendChunked(true);
	   return restHandlerConfig;
   }
   
   @Provides
   @Singleton
   @Named(ProductCatalogReloadListener.GENERIC_MESSAGE_BUS_ACTOR_ADDRESS)
   public Address provideMessageBusActorAddress() {
      return Address.fromString(MessageConstants.SERVICE + ":" + PlatformConstants.SERVICE_CLIENTBRIDGE + ":");
   }
   
   @Provides @Singleton
   public BridgeMetrics provideBridgeMetrics() {
      return new BridgeMetrics("client");
   }
   
   @Provides
   @Singleton
   public SameCodeManager provideSameCodeManager() throws IllegalArgumentException, URISyntaxException {
      Resource sameCodesDirectory = Resources.getResource(sameCodesPath);
      Resource sameCodesStateMappingsDirectory = Resources.getResource(sameCodeStateMappingsPath);
      return new SameCodeManager(sameCodesDirectory, sameCodesStateMappingsDirectory);
   }   
   
   @Provides
   @Singleton
   public EasCodeManager provideEasCodeManager() throws IllegalArgumentException, URISyntaxException {
      Resource easCodesDirectory = Resources.getResource(easCodesPath);
      return new EasCodeManager(easCodesDirectory);
   }
   
   @Provides @Singleton @Named(GetInvoiceRESTHandler.NAME_EXECUTOR)
   public Executor getInvoiceExecutor() {
   	return executor;
   }
   
   
   @Provides @Singleton @Named(SetActivePlaceHandler.NAME_EXECUTOR)
   public Executor setActivePlaceExecutor() {
   	return executor;
   }
   
   @Provides @Singleton @Named(GetPreferencesHandler.NAME_EXECUTOR)
   public Executor getPreferencesExecutor() {
   	return executor;
   }
   
   @Provides @Singleton @Named(SetPreferencesHandler.NAME_EXECUTOR)
   public Executor setPreferencesExecutor() {
   	return executor;
   }
   
   @Provides @Singleton @Named(ResetPreferenceHandler.NAME_EXECUTOR)
   public Executor resetPreferenceExecutor() {
   	return executor;
   }
}

