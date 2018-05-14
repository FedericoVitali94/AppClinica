/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import db.ServerConnectionManager;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;

/**
 *
 * @author den
 */
public class Redirecter {

   public final static String LOGIN_WIN = "appclinica/LogInView.fxml";
   public final static String SEARCH_PERSON_WIN = "search/SearchView.fxml";
   public final static String SEARCH_DISEASE_WIN = "searchDis/SearchDisView.fxml";
   public final static String SEARCH_DRUG_WIN = "searchDrug/SearchDrugView.fxml";
   public final static String EHR_WIN = "ehr/EHRView.fxml";
   public final static String DISEASE_DET_WIN = "diseaseDetail/DisDetView.fxml";
   public final static String DRUG_DET_WIN = "drugDetails/drugDetailsView.fxml";
   public final static String EXAM_DET_WIN = "ehr/ExaminationView.fxml";
   public final static String MAIN_MENU_WIN = "appclinica/MainMenuView.fxml";

   private static Redirecter redir = null;
   private final static Logger LOGGER = Logger.getLogger(Redirecter.class);

   //costruttore privato
   private Redirecter() {
   }

   /**
    * *
    * Getter for the singleton instance.
    *
    * @return Redirecter the instance.
    */
   public static Redirecter getInstance() {
      if (redir == null) {
         synchronized (Redirecter.class) {
            if (redir == null) {
               redir = new Redirecter();
            }
         }
      }
      return redir;
   }

   /**
    * *
    * Redirects to a new window and closes the current one.
    *
    * @param fromHere current Scene
    * @param toHere new scene fxml file path
    * @param close true if the current window must be closed after the new one is opened
    */
   public void redirect(final Scene fromHere, final String toHere, final boolean close) {
      Stage newStage = new Stage();
      Stage curStage = null;
      if (close) {
         curStage = (Stage) fromHere.getWindow();
      }

      try {
         Parent view = FXMLLoader.load(getClass().getClassLoader().getResource(toHere));
         Scene scene = new Scene(view);
         newStage.setScene(scene);
         if (close && curStage != null) {
            curStage.close();
         }
         newStage.show();
      } catch (IOException exc) {
         PopUps.showError("Errore", "Impossibile caricare la pagina "+ exc);
         LOGGER.error(exc.getMessage());
      }
   }
}
