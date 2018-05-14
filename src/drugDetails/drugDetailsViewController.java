/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package drugDetails;

import com.marklogic.client.MarkLogicServerException;
import com.marklogic.client.semantics.SPARQLRuleset;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;
import db.ServerConnectionManager;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.log4j.Logger;
import util.PopUps;
import util.QueryUtils;


/**
 *
 * @author Mattia
 */
public class drugDetailsViewController implements Initializable {
    
    private final static Logger LOGGER = Logger.getLogger(drugDetailsViewController.class);

   @FXML
   private Label txtDrugName;
   private Label txtBearerOf;
   @FXML
   private TableView<SimpleStringProperty> tvSubDrugs;

   private ObservableList<SimpleStringProperty> obsLSubDrugs;
   private MarkLogicDatasetGraph mldg;
   private String disUri;

   /**
    * Initializes the controller class.
    * @param url
    * @param rb
    */
   @Override
   public void initialize(URL url, ResourceBundle rb) {
      this.mldg = ServerConnectionManager.getInstance().getDatasetClient();
      this.obsLSubDrugs = FXCollections.observableArrayList();

      this.setCols("Dosaggio", this.obsLSubDrugs, this.tvSubDrugs);
   }
    
    public void setDrugAndInit(final String drugName) {
      this.txtDrugName.setText(drugName);
   }
    
    /**
    * *
    * set the column and column cell factory
    *
    * @param colName
    * @param obList
    * @param table
    */
   private void setCols(final String colName,
                        ObservableList<SimpleStringProperty> obList,
                        TableView<SimpleStringProperty> table) {
      TableColumn<SimpleStringProperty, String> col = new TableColumn(colName);
      col.setCellValueFactory(
              new PropertyValueFactory<>("value"));

      table.getColumns().add(col);
      table.setItems(obList);
   }
    
}
