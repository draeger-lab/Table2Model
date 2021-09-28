/**
 *
 */
package org.sbml.io;

import org.sbml.jsbml.SBase;
import org.sbml.jsbml.util.ModelBuilder;

import de.zbit.sbml.util.SBMLtools;

/**
 * @author Andreas Dr&auml;ger
 *
 */
public abstract class RowReader {

  /**
   *
   * @param abbreviation
   * @param prefix
   * @return
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

  public abstract SBase readRow(String columns[], ModelBuilder builder);

}
