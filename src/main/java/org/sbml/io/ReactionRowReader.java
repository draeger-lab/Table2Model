/**
 *
 */
package org.sbml.io;

import static java.text.MessageFormat.format;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.ext.fbc.converters.GPRParser;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.sbml.jsbml.util.ModelBuilder;
import org.sbml.jsbml.util.StringTools;

import de.zbit.sbml.util.SBMLtools;


/**
 * @author Andreas Dr&auml;ger
 *
 */
public class ReactionRowReader extends RowReader {

  private static final String MINUS_INF = "minus_inf";
  private static final String PLUS_INF = "plus_inf";
  private static final String COBRA_0_BOUND = "cobra_0_bound";
  private static final String COBRA_DEFAULT_LB = "cobra_default_lb";
  private static final String COBRA_DEFAULT_UB = "cobra_default_ub";

  private static final String GROUPS_HASH = "GROUPS_HASH";

  private static transient Logger logger = Logger.getLogger(ReactionRowReader.class.getName());

  @Override
  public SBase readRow(String[] columns, ModelBuilder builder) {
    Reaction r = builder.buildReaction(toID(columns[0], 'R'), columns[1].trim(), (Compartment) null, false, StringTools.parseSBMLBoolean(columns[7].trim()));

    // read reaction equation
    readRxnEqn(columns[2], r, builder);

    // GPRs
    if (!columns[3].isEmpty()) {
      GPRParser.parseGPR(r, columns[3], false, true);
    }

    // Protein annotation to genes
    if (!(columns[4].isEmpty() && columns[5].isEmpty())) {
      String genes[] = columns[4].split(" ");
      String proteins[] = columns[5].split(" ");
      FBCModelPlugin fbc = (FBCModelPlugin) builder.getModel().getPlugin(FBCConstants.shortLabel);
      for (int i = 0; i < genes.length; i++) {
        GeneProduct gp = fbc.getGeneProduct("G_" + genes[i]);
        if (gp == null) {
          logger.warning(format("Gene {0} is not included in model.", genes[i]));
        } else {
          if (!gp.isSetMetaId()) {
            gp.setMetaId("meta_" + genes[i]);
          }
          // TODO: reference contains abbreviation for the organism -> change
          gp.addResources(Qualifier.BQB_IS_ENCODED_BY, "https://identifiers.org/kegg.genes/cgb:" + genes[i]);
          gp.setLabel(genes[i]);
          if (proteins.length > i) {
            gp.setName(proteins[i]);
          }
        }
      }
    }

    // Subsystems
    readSubSystem(r, builder, columns[6]);

    Model m = builder.getModel();
    FBCReactionPlugin rplug = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);

    // Flux Bounds
    readFluxBounds(columns[8], columns[9], builder, r, m, rplug);

    // Objective
    if ((columns.length > 10) && !columns[10].isEmpty()) {
      readObjective(StringTools.parseSBMLInt(columns[10]), r, m);
    }

    // Confidence Score
    if ((columns.length > 11) && !columns[11].isEmpty()) {
      readConfidenceScore(r, StringTools.parseSBMLInt(columns[11]));
    }

    // EC Number
    if ((columns.length > 12) && !columns[12].isEmpty()) {
      r.setMetaId("meta_" + r.getId());
      r.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, "https://identifiers.org/ec-code/" + columns[12]));
    }

    // Notes
    if ((columns.length > 13) && !columns[13].isEmpty()) {
      try {
        r.appendNotes(columns[13]);
      } catch (XMLStreamException exc) {
        logger.warning(exc.getMessage());
      }
    }
    if ((columns.length > 14) && !columns[14].isEmpty()) {
      try {
        r.appendNotes(columns[14]);
      } catch (XMLStreamException exc) {
        logger.warning(exc.getMessage());
      }
    }

    logger.info(format("Parsed {0}", r.toString()));
    return r;
  }

  /**
   * @param eqn
   * @param r
   */
  private void readRxnEqn(String eqn, Reaction r, ModelBuilder builder) {
    StringTokenizer reqn = new StringTokenizer(eqn);
    ListOf<SpeciesReference> participants = r.getListOfReactants();
    Model m = builder.getModel();
    Set<Compartment> rCompartments = new HashSet<Compartment>();

    double coeff = 1d;
    while (reqn.hasMoreElements()) {
      String elem = reqn.nextElement().toString();

      // Current element is reaction arrow:
      if (elem.equals("<=>") || elem.equals("->")) {
        participants = r.getListOfProducts();
        if ((elem.equals("<=>") && !r.isReversible()) || (elem.equals("->") && r.isReversible())) {
          logger.warning(format("Reversibility flag ''{2}'' of reaction with id ''{0}'' conflicting with reaction equation ''{1}''.", r.getId(), eqn, r.getReversible()));
        }
        // reset stoichiometric coefficient for the next species:
        coeff = 1d;
        continue;
      }

      // Current element is species identifier followed by compartment abbreviation:
      if (elem.endsWith("]")) {
        int split = elem.indexOf('[');
        String sId = toID(elem.substring(0, split), 'M');
        String cId = elem.substring(split + 1, elem.length() - 1);

        Compartment c = m.getCompartment(cId);
        if (c == null) {
          c = builder.buildCompartment(cId, true, null, 3d, Double.NaN, (String) null);
        }
        rCompartments.add(c);

        Species s = m.getSpecies(sId);
        String sIdc = sId + '_' + cId;
        if (s == null) {
          s = builder.buildSpecies(sIdc, null, c, false, false, false, Double.NaN, (String) null);
          logger.warning(format("Created species used in reaction but not declared as metabolite: {0}", s));
        } else {
          // Species exists, but in the default compartment
          if (m.getSpecies(sIdc) == null) {
            // Make sure to only add the compartmentalized species once.
            s = s.clone();
            s.setId(sIdc);
            s.setCompartment(c);
            m.addSpecies(s);
          }
          s = m.getSpecies(sIdc);
        }

        SpeciesReference specRef = new SpeciesReference(s);
        specRef.setConstant(true);
        specRef.setStoichiometry(coeff);
        participants.add(specRef);

        // reset stoichiometric coefficient for the next species:
        coeff = 1d;

      } else if (elem.equals("+")) {
        continue;
      } else {
        // Stoichiometric coefficient
        coeff = StringTools.parseSBMLDouble(elem);
      }

    }

    // Let's see if we can assign a compartment to the reaction:
    if (rCompartments.size() == 1) {
      // There is exactly one compartment in the set. Hence, all species are in the same compartment.
      r.setCompartment(rCompartments.iterator().next());
      logger.info(format("Reaction with id ''{0}'' is located in the compartment with id ''{1}''.", r.getId(), r.getCompartment()));
    }
  }

  /**
   * @param objective
   * @param r
   * @param m
   */
  private void readObjective(int objective, Reaction r, Model m) {
    if (objective != 0) {
      FBCModelPlugin fbc = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);
      Objective o = fbc.createObjective("obj", null, Objective.Type.MAXIMIZE);
      o.createFluxObjective(null, null, objective, r);
      fbc.setActiveObjective(o);
    }
  }

  /**
   * @param r
   * @param confidence
   */
  private void readConfidenceScore(Reaction r, int confidence) {
    Integer ecoTerm;
    // See BIOINF3371 Lecture 2.
    switch (confidence) {
    case 0: // lowest confidence
      ecoTerm = 1; // Reaction is included to improve simulation results.
      break;
    case 1:
      ecoTerm = 5551; // There is physiological data to support inclusion in the model.
      break;
    case 2:
      ecoTerm = 44; // There is significant sequence similarity to another gene with known function.
      break;
    case 3:
      ecoTerm = 73; // Gene over-expression and purification, gene deletions.
      break;
    case 4: // highest confidence
      ecoTerm = 2; // Enzyme has been tested biochemically.
      break;
    default:
      ecoTerm = null;
      break;
    }
    // http://evidenceontology.org/browse/#ECO_<digits>
    if (ecoTerm != null) {
      String term = StringTools.leadingZeros(7, ecoTerm);
      logger.info(format("Evidence Ontology Term: {0}", term));
      r.addResources(Qualifier.BQB_IS, "https://identifiers.org/eco/ECO:" + term);
    }
  }

  /**
   * @param columns
   * @param builder
   * @param r
   * @param m
   * @param rplug
   */
  private void readFluxBounds(String lowerBound, String upperBound, ModelBuilder builder,
    Reaction r, Model m, FBCReactionPlugin rplug) {
    // Lower bound
    if (!lowerBound.isEmpty()) {
      double lb = StringTools.parseSBMLDouble(lowerBound);
      Parameter p;
      String pid = r.getId() + "_lower_bound";
      int sbo = 626; // default flux bound
      if (Double.isInfinite(lb)) {
        pid = MINUS_INF;
      } else if (lb == 0d) {
        pid = COBRA_0_BOUND;
      } else if (lb == -1000d) {
        pid = COBRA_DEFAULT_LB;
      } else {
        sbo = 625; // flux bound
      }
      p = m.getParameter(pid);
      if (p == null) {
        p = builder.buildParameter(pid, null, lb, true, ModelBuilder.MMOL_PER_G_DW_PER_HR);
        p.setSBOTerm(sbo);
      }
      rplug.setLowerFluxBound(p);
    }
    // Upper bound
    if (!upperBound.isEmpty()) {
      double ub = StringTools.parseSBMLDouble(upperBound);
      Parameter p;
      String pid = r.getId() + "_upper_bound";
      int sbo = 626; // default flux bound
      if (Double.isInfinite(ub)) {
        pid = PLUS_INF;
      } else if (ub == 0d) {
        pid = COBRA_0_BOUND;
      } else if (ub == 1000d) {
        pid = COBRA_DEFAULT_UB;
      } else {
        sbo = 625; // flux bound
      }
      p = m.getParameter(pid);
      if (p == null) {
        p = builder.buildParameter(pid, null, ub, true, ModelBuilder.MMOL_PER_G_DW_PER_HR);
        p.setSBOTerm(sbo);
      }
      rplug.setUpperFluxBound(p);
    }
  }

  /**
   * @param r
   * @param builder
   * @param groupName
   */
  private void readSubSystem(Reaction r, ModelBuilder builder,
    String groupName) {
    if (!groupName.isEmpty()) {
      Model m = builder.getModel();
      GroupsModelPlugin gmp = (GroupsModelPlugin) m.getPlugin(GroupsConstants.shortLabel);
      if (gmp.getUserObject(GROUPS_HASH) == null) {
        gmp.putUserObject(GROUPS_HASH, new HashMap<String, Group>());
      }
      @SuppressWarnings("unchecked")
      HashMap<String, Group> groups = (HashMap<String, Group>) gmp.getUserObject(GROUPS_HASH);
      if (groups.get(groupName) == null) {
        Group g = gmp.createGroup(SBMLtools.nextId(m));
        g.setSBOTerm(633);
        g.setKind(Group.Kind.partonomy);
        g.setName(groupName);
        groups.put(groupName, g);
        logger.info(format("Created {0}", g));
      }
      groups.get(groupName).createMember(null, r);
    }
  }

}
