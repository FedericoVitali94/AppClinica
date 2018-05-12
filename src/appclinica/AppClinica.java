/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appclinica;

import db.ServerConnectionManager;
import javafx.application.Application;
import javafx.stage.Stage;
import util.Redirecter;

/**
 *
 * @author den
 */
public class AppClinica extends Application {
   
   @Override
   public void start(Stage stage) throws Exception {
      Redirecter.getInstance().redirect(null, Redirecter.LOGIN_WIN, false);
   }

   /**
    * @param args the command line arguments
    */
   public static void main(String[] args) {
      launch(args);
   }
   
   @Override
   public void stop() {
      ServerConnectionManager.getInstance().close();
   }
   
}
