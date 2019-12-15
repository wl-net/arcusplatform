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
/**
 *
 */
package com.arcussmarthome.oculus.modules.session.ux;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.arcussmarthome.Utils;
import com.arcussmarthome.bootstrap.Bootstrap;
import com.arcussmarthome.bootstrap.ServiceLocator;
import com.arcussmarthome.bootstrap.guice.GuiceServiceLocator;
import com.arcussmarthome.client.event.ClientFuture;
import com.arcussmarthome.core.IrisApplicationModule;
import com.arcussmarthome.oculus.Oculus;
import com.arcussmarthome.oculus.OculusModule;
import com.arcussmarthome.oculus.modules.session.LoginCredentials;
import com.arcussmarthome.oculus.modules.session.OculusSession;
import com.arcussmarthome.oculus.modules.session.SessionController;
import com.arcussmarthome.oculus.widget.ComboBoxBuilder;
import com.arcussmarthome.oculus.widget.Dialog;
import com.arcussmarthome.oculus.widget.FormView;
import com.arcussmarthome.oculus.widget.HyperLink;




/**
 * @author tweidlin
 *
 */
public class LoginDialog {
   private static final Logger logger = LoggerFactory.getLogger(LoginDialog.class);

   private static class InstanceRef {
      private static final LoginDialogImpl INSTANCE = new LoginDialogImpl();
   }

   public static ClientFuture<LoginCredentials> prompt() {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt();
   }

   public static ClientFuture<LoginCredentials> prompt(String errorMessage) {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt(errorMessage);
   }

   protected static void hide() {
      InstanceRef.INSTANCE.setVisible(false);
   }

   private static class LoginDialogImpl extends Dialog<LoginCredentials> {
      List<OculusSession> lastSessions = ImmutableList.of();

      JComboBox<OculusSession> serviceUri;
      JTextField username = new JTextField();
      JPasswordField password = new JPasswordField();
      JLabel createAccount = new HyperLink("Create Account").getComponent();
      JLabel createLoginByInvite = new HyperLink("Invite Code").getComponent();
      JLabel forgotPassword = new HyperLink("Forgot My Password").getComponent();
      JButton login = new JButton("Login");
      LoginCredentials.Action action = LoginCredentials.Action.LOGIN;

      LoginDialogImpl() {
         super(true);
         setTitle("Login");
         setModal(true);
         setResizable(false);
         setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
      }

      public ClientFuture<LoginCredentials> prompt() {
         clearErrorMessage();
         return super.prompt();
      }

      public ClientFuture<LoginCredentials> prompt(String errorMessage) {
         setErrorMessage(errorMessage);
         return super.prompt();
      }

      protected LoginCredentials getValue() {
         Object selectedItem = serviceUri.getSelectedItem();
         String serviceUri = selectedItem instanceof OculusSession ?
               ((OculusSession) selectedItem).getHost() :
               (String) selectedItem;

         LoginCredentials credentials = new LoginCredentials();
         credentials.setAction(action);
         credentials.setServiceUri(serviceUri);
         credentials.setUsername(username.getText());
         credentials.setPassword(password.getPassword());
         return credentials;
      }

      protected void submit(LoginCredentials.Action action) {
         this.action = action;
         this.submit();
      }

      protected void onShow() {
         List<OculusSession> recentSessions = ServiceLocator.getInstance(SessionController.class).listRecentSessions();
         if(!lastSessions.equals(recentSessions)) {
            lastSessions = recentSessions;
            DefaultComboBoxModel<OculusSession> model = ((DefaultComboBoxModel<OculusSession>) serviceUri.getModel());
            model.removeAllElements();
            for(OculusSession hau: ServiceLocator.getInstance(SessionController.class).listRecentSessions()) {
               model.addElement(hau);
            }
         }

         if(username.getText().isEmpty()) {
            username.requestFocusInWindow();
         }
         else {
            password.requestFocusInWindow();
         }
      }

      protected Component createContents() {
         login.addActionListener((e) -> this.submit(LoginCredentials.Action.LOGIN));
         Oculus.invokeOnEnter(password, () -> this.submit(LoginCredentials.Action.LOGIN));
         
         FormView view = new FormView();

         serviceUri =
               new ComboBoxBuilder<OculusSession>()
                  .editable()
                  .withValueRenderer(OculusSession::getHost)
                  .withCellRenderer((info) -> StringUtils.isEmpty(info.getUserName()) ? info.getHost() : info.getUserName() + " (" + info.getHost() + ")")
                  .create();
         serviceUri.addItemListener((event) -> {
            if(event.getStateChange() == ItemEvent.SELECTED) {
               Object item = event.getItem();
               if(item instanceof OculusSession) {
                  username.setText(((OculusSession) item).getUserName());
                  password.selectAll();
                  password.requestFocusInWindow();
               }
            }
         });

         createAccount.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               submit(LoginCredentials.Action.CREATE_ACCOUNT);
            }
         });
         createLoginByInvite.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               submit(LoginCredentials.Action.ACCEPT_INVITE);
            }
         });
         forgotPassword.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               submit(LoginCredentials.Action.RESET_PASSWORD);
            }
         });

         view.addField(new JLabel("Address"), serviceUri);
         view.addField(new JLabel("Username"), username);
         view.addField(new JLabel("Password"), password);
         view.addButton(createAccount);
         view.addButton(createLoginByInvite);
         view.addButton(forgotPassword);
         view.addButton(login);

         return view.getComponent();
      }

   }

   public static void main(String [] args) throws Exception {
      Bootstrap bootstrap =
         Bootstrap
            .builder()
            .withBootstrapModules(new IrisApplicationModule())
            .withModuleClasses(OculusModule.class)
            .build();
      ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));
      SwingUtilities.invokeAndWait(() -> {
         LoginDialog
            .prompt()
            .onSuccess((token) -> System.out.printf(
                  "Action:   %s%n" +
                  "Host:     %s%n" +
                  "Username: %s%n",
                  token.getAction(),
                  token.getServiceUri(),
                  token.getUsername()
            ))
            .onCompletion((e) -> System.exit(0))
            ;
      });
   }
}

