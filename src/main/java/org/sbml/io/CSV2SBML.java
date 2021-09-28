/**
 *
 */
package org.sbml.io;

import static java.text.MessageFormat.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.Creator;
import org.sbml.jsbml.History;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.util.ModelBuilder;

import de.zbit.util.logging.LogUtil;

/**
 * @author Andreas Dr&auml;ger
 *
 */
public class CSV2SBML {

  /** Logger */
  private static final transient Logger logger = Logger.getLogger(CSV2SBML.class.getName());

  private SBMLDocument doc;

  public CSV2SBML(File metabolites, File reactions) throws IOException {
    char separator = ';';
    ModelBuilder builder = new ModelBuilder(new SBMLDocument(3, 1));
    builder.buildCompartment("d", false, "default", 3d, Double.NaN, (String) null);
    Model m = builder.getModel();
    FBCModelPlugin fbcPlugin = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);
    fbcPlugin.setStrict(true);

    // Units
    builder.buildCBMunits();

    readTable(metabolites, builder, separator, new SpeciesRowReader());
    readTable(reactions, builder, separator, new ReactionRowReader());

    // tidy up unnecessary species and delete the default compartment:
    for (int i = m.getSpeciesCount() - 1; i >= 0; i--) {
      Species s = m.getSpecies(i);
      if (s.getCompartment().equals(SpeciesRowReader.DEFAULT_COMPARTMENT_ID)) {
        s = m.removeSpecies(i);
        logger.info(format("Removed species with id ''{0}'' because it has only a default compartment assigned.", s.getId()));
      }
    }
    m.removeCompartment(SpeciesRowReader.DEFAULT_COMPARTMENT_ID);

    // TODO: Hard-coded model history and taxon annotation!
    String authors[] = {
      "Yu Zhang", "CAS Key Laboratory of Pathogenic Microbiology and Immunology, Institute of Microbiology, Chinese Academy of Sciences, Beijing 100101",
      "Jingyi Cai", "Beijing University of Chemical Technology, Beijing, 100029 China",
      "Xiuling Shang", "CAS Key Laboratory of Pathogenic Microbiology and Immunology, Institute of Microbiology, Chinese Academy of Sciences, Beijing 100101",
      "Bo Wang", "CAS Key Laboratory of Pathogenic Microbiology and Immunology, Institute of Microbiology, Chinese Academy of Sciences, Beijing 100101",
      "Shuwen Liu", "CAS Key Laboratory of Pathogenic Microbiology and Immunology, Institute of Microbiology, Chinese Academy of Sciences, Beijing 100101",
      "Xin Chai", "CAS Key Laboratory of Pathogenic Microbiology and Immunology, Institute of Microbiology, Chinese Academy of Sciences, Beijing 100101",
      "Tianwei Tan", "Beijing University of Chemical Technology, Beijing, 100029 China",
      "Yun Zhang", "CAS Key Laboratory of Pathogenic Microbiology and Immunology, Institute of Microbiology, Chinese Academy of Sciences, Beijing, 100101 China",
      "Tingyi Wen", "CAS Key Laboratory of Pathogenic Microbiology and Immunology, Institute of Microbiology, Chinese Academy of Sciences, Beijing 100101",
    };
    History history = m.createHistory();
    for (int i = 0; i < (authors.length - 1); i+= 2) {
      Creator c = new Creator();
      String name[] = authors[i].split(" ");
      c.setGivenName(name[0]);
      c.setFamilyName(name[1]);
      c.setOrganization(authors[i+1]);
      history.addCreator(c);
    }
    Calendar c = Calendar.getInstance();
    history.setModifiedDate(c.getTime());
    c.set(2017, Calendar.JUNE, 30, 12, 00);
    history.setCreatedDate(c.getTime());
    m.addResources(Qualifier.BQB_IS_DESCRIBED_BY, "https://identifiers.org/pubmed/28680478");
    m.addResources(Qualifier.BQB_HAS_TAXON, "https://identifiers.org/taxonomy/196627");

    doc = builder.getSBMLDocument();
  }

  /**
   * @param inFile
   * @param builder
   * @param separator
   * @throws FileNotFoundException
   * @throws IOException
   */
  private void readTable(File inFile, ModelBuilder builder,
    char separator, RowReader reader) throws FileNotFoundException, IOException {
    String line;
    BufferedReader bfMetabolites = new BufferedReader(new FileReader(inFile));
    int row = 0;
    while ((line = bfMetabolites.readLine()) != null) {
      if (row > 0) {
        reader.readRow(line.split(String.valueOf(separator)), builder);
      }
      row++;
    }
    bfMetabolites.close();
  }

  /**
   * Getter for the SBML document.
   *
   * @return
   */
  public SBMLDocument getSBMLDocument() {
    return doc;
  }

  /**
   * @param args metabolites file (CSV format), reactions file (CSV format), SBML output file
   * @throws IOException
   * @throws XMLStreamException
   * @throws SBMLException
   */
  public static void main(String[] args) throws IOException, SBMLException, XMLStreamException {
    LogUtil.initializeLogging("io.sbml");
    long time = System.currentTimeMillis();
    CSV2SBML converter = new CSV2SBML(new File(args[0]), new File(args[1]));
    SBMLDocument doc = converter.getSBMLDocument();
    Model m = doc.getModel();
    m.setId(args[2].substring(args[2].lastIndexOf('/') + 1, args[2].lastIndexOf('.')));
    m.setMetaId("meta_" + m.getId());
    logger.info(format("time needed: {0,number}", (System.currentTimeMillis() - time)));
    TidySBMLWriter.write(doc, new File(args[2]), ' ', (short) 2);
  }

}
