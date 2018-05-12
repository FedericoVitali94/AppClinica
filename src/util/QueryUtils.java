/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.log4j.Logger;

/**
 *
 * @author den
 */
public class QueryUtils {
   private final static Logger LOGGER = Logger.getLogger(QueryUtils.class);
   private final static String BASE_PATH = "resources/queries/";
   
   private QueryUtils() { };
   
   /**
    * *
    * Loads a query from a file.
    *
    * @param path the query file path.
    *
    * @return the query as String. null if error
    */
   public static String loadQueryFromFile(final String path) {
      String queryStr = null;
      try {
         byte[] encodedStr = Files.readAllBytes(Paths.get(BASE_PATH + path));
         queryStr = new String(encodedStr, StandardCharsets.UTF_8);
         LOGGER.debug("query caricata");
      } catch (IOException exc) {
         PopUps.showError("Error", "Errore nel recupero della richiesta per le malattie");
         LOGGER.error(exc.getMessage());
      }

      return queryStr;
   }
}
