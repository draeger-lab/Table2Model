/**
 *
 */
package org.sbml.io;

import static java.text.MessageFormat.format;

import java.util.logging.Logger;

import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.util.ModelBuilder;

/**
 * @author Andreas Dr&auml;ger
 *
 */
public class SpeciesRowReader extends RowReader {


  public static final String DEFAULT_COMPARTMENT_ID = "d";
  private static transient Logger logger = Logger.getLogger(SpeciesRowReader.class.getName());

  @Override
  public SBase readRow(String columns[], ModelBuilder builder) {

    String id = toID(columns[0], 'M');

    Species s = builder.buildSpecies(id, columns[1].trim(), builder.getModel().getCompartment(DEFAULT_COMPARTMENT_ID), false, false, false, Double.NaN, (String) null);
    if (!(columns[2].isEmpty() || columns[4].isEmpty())) {
      FBCSpeciesPlugin fbcSpecies = (FBCSpeciesPlugin) s.getPlugin(FBCConstants.shortLabel);
      if (!columns[2].isEmpty()) {
        try {
          fbcSpecies.setChemicalFormula(columns[2].trim());
        } catch (IllegalArgumentException exc) {
          logger.warning(exc.getMessage());
        }
      }
      if (!columns[4].isEmpty()) {
        fbcSpecies.setCharge(Integer.parseInt(columns[4].trim()));
      }
    }
    logger.info(format("Parsed {0}", s.toString()));

    return s;
  }

}
