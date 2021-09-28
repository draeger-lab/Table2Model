/**
 *
 */
package org.sbml;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Species;

/**
 * @author Andreas Dr&auml;ger
 *
 */
public class SBMLCompare {

  private SBMLDocument doc1;
  private SBMLDocument doc2;
  private File f1;
  private File f2;

  /**
   * @param args
   * @throws IOException
   * @throws XMLStreamException
   */
  public static void main(String[] args) throws XMLStreamException, IOException {
    new SBMLCompare(new File(args[0]), new File(args[1]));
  }

  /**
   * @param f1
   * @param f2
   * @throws XMLStreamException
   * @throws IOException
   */
  public SBMLCompare(File f1, File f2)
      throws XMLStreamException, IOException {
    this.f1 = f1;
    this.f2 = f2;
    doc1 = SBMLReader.read(f1);
    doc2 = SBMLReader.read(f2);

    /*Map<String, Reaction> eco2rxn1 = new HashMap<String, Reaction>();
    Map<String, Reaction> eco2rxn2 = new HashMap<String, Reaction>();*/

    Set<String> sids1 = new HashSet<String>();
    Set<String> sids2 = new HashSet<String>();
    for (Species s : doc1.getModel().getListOfSpecies()) {
      sids1.add(s.getId());
    }
    for (Species s : doc2.getModel().getListOfSpecies()) {
      sids2.add(s.getId());
    }
    System.out.println("Total species count in " + f1.getName() + ": " + sids1.size());
    Set<String> intersect = new HashSet<String>(sids1);
    intersect.retainAll(sids2);
    sids1.removeAll(intersect);
    System.out.println("Unique:\t" + sids1.size());
    System.out.println("Total species count in " + f2.getName() + ": " + sids2.size());
    sids2.removeAll(intersect);
    System.out.println("Unique:\t" + sids2.size());
    System.out.println("Overlap:\t" + intersect.size() + "\n");


    compare(Qualifier.BQB_IS, "ec-code");
    System.out.println();
    compare(Qualifier.BQB_IS_ENCODED_BY, "kegg.genes");
  }

  /**
   * @param qualifier
   * @param filterTerm
   */
  private void compare(Qualifier qualifier, String filterTerm) {
    Set<String> s1 = new HashSet<String>();
    Set<String> s2 = new HashSet<String>();
    s1.addAll(doc1.filterCVTerms(qualifier, ".*" + filterTerm + ".*", true));
    if (qualifier == Qualifier.BQB_IS_ENCODED_BY) {
      // Hack!!!
      qualifier = Qualifier.BQB_IS;
    }
    s2.addAll(doc2.filterCVTerms(qualifier, ".*" + filterTerm + ".*", true));
    Set<String> intersect = new HashSet<String>(s1);
    intersect.retainAll(s2);


    System.out.println(format("Overlapp for {0} terms with qualifier {1}", filterTerm, qualifier));
    System.out.println(format("Total number of {1} terms in {0}: {2}", f1.getName(), filterTerm, s1.size()));
    s1.removeAll(intersect);
    System.out.println(format("Unique {1} terms in {0}: {2}", f1.getName(), filterTerm, s1.size()));
    System.out.println(format("Total number of {1} terms in {0}: {2}", f2.getName(), filterTerm, s2.size()));
    s2.removeAll(intersect);
    System.out.println(format("Unique {1} terms in {0}: {2}", f2.getName(), filterTerm, s2.size()));
    System.out.println(format("Number of overlapping {0} terms in both files: {1}", filterTerm, intersect.size()));
  }
}
