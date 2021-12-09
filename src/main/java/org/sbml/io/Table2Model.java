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
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.Creator;
import org.sbml.jsbml.History;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.util.ModelBuilder;

import de.zbit.util.logging.LogUtil;

/**
 * @author Andreas Dr&auml;ger
 *
 */
public class Table2Model {

  /** Logger */
  private static final transient Logger logger = Logger.getLogger(Table2Model.class.getName());

  // TODO: This will become a constant within ModelBuilder with the next JSBML release
  public static final String MMOL_PER_G_DW_PER_HR = "mmol_per_gDW_per_hr";

  private SBMLDocument doc;

  public Table2Model(File metabolites, File reactions) throws IOException {
    char separator = ';';
    ModelBuilder builder = new ModelBuilder(new SBMLDocument(3, 1));
    builder.buildCompartment("d", false, "default", 3d, Double.NaN, (String) null);
    Model m = builder.getModel();
    FBCModelPlugin fbcPlugin = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);
    fbcPlugin.setStrict(true);

    //TODO! Change with the next JSBML release! Units
    //builder.buildCBMunits();
    buildCBMunits(builder);

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
    for (int i = 0; i < (authors.length - 1); i += 2) {
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
    //TODO: The following lines will become available with a newer release of JSBML
    //m.addResources(Qualifier.BQB_IS_DESCRIBED_BY, "https://identifiers.org/pubmed/28680478");
    //m.addResources(Qualifier.BQB_HAS_TAXON, "https://identifiers.org/taxonomy/196627");
    addResources(m, Qualifier.BQB_IS_DESCRIBED_BY, "https://identifiers.org/pubmed/28680478");
    addResources(m, Qualifier.BQB_HAS_TAXON, "https://identifiers.org/taxonomy/196627");
    doc = builder.getSBMLDocument();
  }

  /**
   * TODO: Delete this method the next update of JSBML!
   */
  public static boolean addResources(SBase sbase, CVTerm.Qualifier qualifier, String... resources) {
    Annotation a = sbase.getAnnotation();
    List<CVTerm> listOfTerms = a.filterCVTerms(qualifier);
    if (listOfTerms.isEmpty()) {
      return a.addCVTerm(new CVTerm(qualifier, resources));
    }
    return listOfTerms.get(0).addResources(resources);
  }

  // TODO: Delete this method with the next release of JSBML!
  private void buildCBMunits(ModelBuilder builder) {
    String HOUR = "hour";
    String F_L = "fL";
    String MMOL_PER_G_DW = "mmol_per_gDW";
    Unit h = builder.buildUnit(3600d, 0, Unit.Kind.SECOND, 1d);
    Unit fL = builder.buildUnit(1d, -3, Unit.Kind.LITRE, 1d);
    Unit mmol = builder.buildUnit(1d, -3, Unit.Kind.MOLE, 1d);
    Unit perGDW = builder.buildUnit(1d, 0, Unit.Kind.GRAM, -1d);
    UnitDefinition hour = builder.buildUnitDefinition(HOUR, HOUR, h.clone());
    hour.setMetaId("meta_" + hour.getId());
    hour.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, "https://identifiers.org/UO:0000032"));
    UnitDefinition femtoLitres = builder.buildUnitDefinition(F_L, "femto litres", fL.clone());
    femtoLitres.setMetaId("meta_" + femtoLitres.getId());
    femtoLitres.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, "https://identifiers.org/UO:0000104"));
    Model m = builder.getModel();
    m.setTimeUnits(hour.getId());
    m.setVolumeUnits(femtoLitres.getId());
    m.setExtentUnits(builder.buildUnitDefinition(MMOL_PER_G_DW, "millimoles per gram dry weight", mmol.clone(), perGDW.clone()));
    m.setSubstanceUnits(m.getExtentUnits());
    h.setExponent(-1d);
    builder.buildUnitDefinition(MMOL_PER_G_DW_PER_HR, "millimoles per gram dry weight per hour", mmol.clone(), perGDW.clone(), h);
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
    for (int row = 0; (line = bfMetabolites.readLine()) != null; row++) {
      if (row > 0) {
        reader.readRow(line.split(String.valueOf(separator)), builder);
      }
    }
    bfMetabolites.close();
  }

  /**
   * Getter for the SBML document.
   *
   * @return The newly created SBML document.
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
    Table2Model converter = new Table2Model(new File(args[0]), new File(args[1]));
    SBMLDocument doc = converter.getSBMLDocument();
    Model m = doc.getModel();
    m.setId(args[2].substring(args[2].lastIndexOf('/') + 1, args[2].lastIndexOf('.')));
    m.setMetaId("meta_" + m.getId());
    logger.info(format("time needed: {0,number}", (System.currentTimeMillis() - time)));
    TidySBMLWriter.write(doc, new File(args[2]), ' ', (short) 2);
  }

}
