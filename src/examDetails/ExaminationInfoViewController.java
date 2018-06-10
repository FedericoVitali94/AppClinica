/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package examDetails;

import com.marklogic.client.MarkLogicServerException;
import com.marklogic.client.semantics.SPARQLRuleset;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;
import db.ServerConnectionManager;
import diseaseDetail.DisDetViewController;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.log4j.Logger;
import util.PopUps;
import util.QueryUtils;

/**
 * FXML Controller class
 *
 * @author Mattia
 */
public class ExaminationInfoViewController implements Initializable {

    private final static Logger LOGGER = Logger.getLogger(ExaminationInfoViewController.class);
    
    @FXML
    private Label labName;
    @FXML
    private Label labDescription;
    
     private MarkLogicDatasetGraph mldg;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
         this.mldg = ServerConnectionManager.getInstance().getDatasetClient();
    }   
    
    public void setExamAndInit(String examCod, String examName){
       this.labName.setText(examName);
       setDescription(examCod);
   }
    
    private void setDescription(String examCod){
        String queryStr = QueryUtils.loadQueryFromFile("getExamDescription.txt");
      ParameterizedSparqlString query = new ParameterizedSparqlString();
      query.setCommandText(queryStr);
      query.setIri("examUri", examCod);
      
      try (QueryExecution execution = QueryExecutionFactory.create(query.asQuery(), this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         if (res.hasNext()) {
            QuerySolution sol = res.next();
            this.labDescription.setText(sol.getLiteral("examDesc").getString());
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento dei dati base della malattia");
         LOGGER.error(exc.getMessage());
      }
    }
    
}
