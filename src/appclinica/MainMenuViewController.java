/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appclinica;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import util.Redirecter;

/**
 * FXML Controller class
 *
 * @author den
 */
public class MainMenuViewController implements Initializable {

   @FXML
   private Button butSearchPerson;
   @FXML
   private Button butSearchDrug;
   @FXML
   private Button butDiagnosis;


   @FXML
   private void handleSearchPerson(ActionEvent event) {
      Redirecter.getInstance().redirect(this.butSearchPerson.getScene(), Redirecter.SEARCH_PERSON_WIN, true);
   }
   
   @FXML
   private void handleSearchDrug(ActionEvent event){
       Redirecter.getInstance().redirect(this.butSearchDrug.getScene(), Redirecter.SEARCH_DRUG_WIN, true);
   }
   
   @FXML
   private void handleArtificialDiagnosis(ActionEvent event){
       Redirecter.getInstance().redirect(this.butDiagnosis.getScene(), Redirecter.DIAGNOSIS_WIN, true);
   }
   
   /**
    * Initializes the controller class.
    */
   @Override
   public void initialize(URL url, ResourceBundle rb) { }   
   
}
