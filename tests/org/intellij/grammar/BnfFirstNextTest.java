package org.intellij.grammar;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.util.ArrayUtil;
import org.intellij.grammar.analysis.BnfFirstNextAnalyzer;
import org.intellij.grammar.psi.BnfFile;
import org.intellij.grammar.psi.BnfRule;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.intellij.grammar.analysis.BnfFirstNextAnalyzer.*;

/**
 * @author gregsh
 */
public class BnfFirstNextTest extends LightPlatformCodeInsightFixtureTestCase {

  public void testSeq() { doFirstTest("r ::= A B", "A"); }
  public void testChoice() { doFirstTest("r ::= A | B", "A", "B"); }
  public void testSeqOpt() { doFirstTest("r ::= [A] B", "A", "B"); }
  public void testChoiceOpt() { doFirstTest("r ::= [A] | B", MATCHES_EOF, "A", "B"); }
  public void testExternal() { doFirstTest("r ::= s external s ::= some A", "#some"); }
  public void testMeta() { doFirstTest("r ::= <<s B>> meta s ::= A <<p>>", "A"); }
  public void testMeta2() { doFirstTest("r ::= <<s B>> meta s ::= <<p1>>", "B"); }
  public void testMeta3() { doFirstTest("r ::= <<s A B>> meta s ::= <<p1>>|<<p2>>", "A", "B"); }
  public void testAnd1() { doFirstTest("r ::= &X s s ::= X | Y", "X"); }
  public void testNot1() { doFirstTest("r ::= !X s s ::= X | Y", "Y"); }
  public void testNeverMatches() { doFirstTest("r ::= !X s s ::= X Y", MATCHES_NOTHING); }
  public void testAndOrNot1() { doFirstTest("r ::= (!X | &X) X", "X"); }
  public void testAndOrNot2() { doFirstTest("r ::= Y? (!X | &X) X", "X", "Y"); }
  public void testNotOrNot3() { doFirstTest("r ::= Y? (!X | !X) X", MATCHES_NOTHING, "Y"); }
  public void testNotText() { doFirstTest("r ::= !'a' s external s ::= parseA", "#parseA"); }
  public void testAndText() { doFirstTest("r ::= &'a' s external s ::= parseA", "#parseA"); }
  public void testAndExternal() { doFirstTest("r ::= &A s external s ::= parseA", "#parseA"); }
  public void testNotSeqTrivial() { doFirstTest("r ::= !(X) (X Z)", MATCHES_NOTHING); }
  public void testNotSeq() { doFirstTest("r ::= !(X Y) (X Z)", "X"); }
  public void testAndSeq() { doFirstTest("r ::= (&(X | Y) | !X) X", "X"); }
  public void testAndEmptySeq1() { doFirstTest("r ::= &() X", "X"); }
  public void testAndEmptySeq2() { doFirstTest("r ::= &()", MATCHES_EOF); }
  public void testNotEmptySeq() { doFirstTest("r ::= !() X", MATCHES_NOTHING); }

  public void testNext1() { doNextTest("r ::= X s ::= (r [A | B])", MATCHES_EOF, "A", "B"); }
  public void testNextMore() { doNextTest("r ::= X s ::= (r * [A | B])", MATCHES_EOF, "A", "B", "X"); }
  public void testNextPredicate() { doNextTest("r ::= X s ::= r &(r * [A | B]) X", "X"); }

  public void testFirstStrings() { doFirstTest("r ::= &(','|')') s ::= r ','", "','"); }
  public void testFirstAndExternal() { doFirstTest("r ::= &(','|')') external s ::= ss r", "')'", "','"); }
  public void testFirstRecover() { doNextTest("r ::= !(','|')') s ::= X {recoverUntil=\"r\"}", MATCHES_ANY); }

  public void testExternalPredicate1() { doFirstTest("r ::= A | B | isHql X external isHql ::= func", "#func", "A", "B"); }
  public void testExternalPredicate2() { doFirstTest("r ::= A | B | &isHql X external isHql ::= func", "A", "B", "X"); }

  private void doFirstTest(String text, String... expected) { doTest(text, true, expected); }
  private void doNextTest(String text, String... expected) { doTest(text, false, expected); }

  private void doTest(String text, boolean first, String... expected) {
    BnfFile f = (BnfFile)myFixture.configureByText("a.bnf", text);
    List<BnfRule> rules = f.getRules();
    assertFalse(rules.isEmpty());
    BnfFirstNextAnalyzer analyzer = new BnfFirstNextAnalyzer();
    Set<String> strings = analyzer.asStrings(first? analyzer.calcFirst(rules.get(0)) : analyzer.calcNext(rules.get(0)).keySet());
    String[] result = ArrayUtil.toStringArray(strings);
    Arrays.sort(result);
    assertOrderedEquals(result, expected);
  }
}
