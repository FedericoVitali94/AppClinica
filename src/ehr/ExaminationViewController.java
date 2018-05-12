/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ehr;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentManager;
import com.marklogic.client.io.FileHandle;
import db.ServerConnectionManager;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.log4j.Logger;

/**
 * FXML Controller class
 *
 * @author den
 */
public class ExaminationViewController implements Initializable {
   private final static Logger LOGGER = Logger.getLogger(ExaminationViewController.class);

   @FXML
   private ImageView imgView;

   /**
    * Initializes the controller class.
    */
   @Override
   public void initialize(URL url, ResourceBundle rb) { }   
   
   public void setImg(final String imgUri) {
      DatabaseClient client = ServerConnectionManager.getInstance().getDatabaseClient();
      DocumentManager binMan = client.newDocumentManager();
      FileHandle handle = new FileHandle();
      LOGGER.debug(imgUri);
      binMan.read(imgUri, handle);
      
      this.imgView.setImage(new Image(handle.get().toURI().toString()));
   }
   
}
