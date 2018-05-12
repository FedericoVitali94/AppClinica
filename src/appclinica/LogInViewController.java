package appclinica;

import com.marklogic.client.MarkLogicServerException;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;

import db.ServerConnectionManager;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.log4j.Logger;
import util.PopUps;
import util.Redirecter;

/**
 *
 * @author den
 */
public class LogInViewController implements Initializable {

   private final static Logger LOGGER = Logger.getLogger(LogInViewController.class);

   @FXML
   private Button butLog;
   @FXML
   private TextField tfPass;
   @FXML
   private TextField tfUser;

   private ServerConnectionManager sman;

   @FXML
   private void handleButtonAction(ActionEvent event) {
      String username = this.tfUser.getText();
      String password = this.tfPass.getText();
      if (username.isEmpty() || password.isEmpty()) {
         PopUps.showError("Dati Mancanti", "Username e password richiesti");
      } else {
         boolean err = false;

         //connection test
         this.sman.setUsername(username);
         this.sman.setPassword(password);
         MarkLogicDatasetGraph mldg = ServerConnectionManager.getInstance().getDatasetClient();
         String queryStr = "DESCRIBE <http://testconnection.test/>";
         try (QueryExecution exec = QueryExecutionFactory.create(queryStr, mldg.toDataset())) {
            exec.execDescribe();
         } catch (MarkLogicServerException exc) {
            err = true;
            ServerConnectionManager.getInstance().close();
            PopUps.showError("Errore", exc.getFailedRequest()
                             .getMessage().equalsIgnoreCase("Unauthorized")
                             ? "Errore autenticazione" : "Impossibile collegarsi al server");
         } catch (Exception exc) {
            err = true;
            ServerConnectionManager.getInstance().close();
            PopUps.showError("Errore", "Impossibile collegarsi al server");
            LOGGER.error(exc.getMessage());
         }

         if (!err) {
            LOGGER.info("auth ok, redirecting...");
            Redirecter.getInstance().redirect(this.tfPass.getScene(), Redirecter.MAIN_MENU_WIN, true);
         }
      }
   }

   @Override
   public void initialize(URL url, ResourceBundle rb) {
      this.sman = ServerConnectionManager.getInstance();
      sman.setHost("localhost");
      sman.setPort(8000);
      sman.setDbName("ClinicalDB");
   }

}
