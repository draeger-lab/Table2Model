/**
 *
 */
package org.sbml.io;

import org.sbml.jsbml.SBase;
import org.sbml.jsbml.util.ModelBuilder;

import de.zbit.sbml.util.SBMLtools;

/**
 * Abstract super class for all specific readers for table rows.
 *
 * @author Andreas Dr&auml;ger
 */
public abstract class RowReader {

  /**
   * This method converts an abbreviation to a valid SBML identifier and takes a
   * prefix that will be added to the abbreviation up front.
   *
   * @param abbreviation
   *        The abbreviation on whose basis a new identifier should be
   *        constructed
   * @param prefix
   *        the prefix, typically {@code M} for metabolite, {@code R} for
   *        reaction, etc.
   * @return a valid SBML id that begins with the given prefix followed by an
   *         underscore and otherwise tries to be as close to the given
   *         abbreviation as possible.
   */
  protected String toID(String abbreviation, char prefix) {
    String id = SBMLtools.toSId(abbreviation);
    if (id.startsWith("_")) {
      id = prefix + id;
    } else {
      id = prefix + "_" + id;
    }
    return id;
  }

  /**
   * This method is intended to read exactly one row from a table and to parse
   * all content into one instance of {@link SBase}.
   *
   * @param columns
   * @param builder
   * @return an SBML object containing all the information from the row.
   */
  public abstract SBase readRow(String columns[], ModelBuilder builder);

}
