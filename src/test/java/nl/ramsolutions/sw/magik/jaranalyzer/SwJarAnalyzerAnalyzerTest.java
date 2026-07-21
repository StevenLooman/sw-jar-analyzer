package nl.ramsolutions.sw.magik.jaranalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

/** Tests for {@link SwJarAnalyzerAnalyzer}. */
class SwJarAnalyzerAnalyzerTest {

  @Test
  void test() throws IOException {
    final SwJarAnalyzerAnalyzer libAnalyzer = TestData.getLibAnalyzer();

    final SwJarAnalyzerCodeDefinition magikMethod1 =
        libAnalyzer.getElement(TestData.SUBSIDIARY_CLASS_CHAR16_VECTOR, "char16_vector__method1");
    assertThat(magikMethod1)
        .isEqualTo(
            new MethodDefinition(
                TestData.PATH_CHAR16_VECTOR,
                TestData.SUBSIDIARY_CLASS_CHAR16_VECTOR,
                "char16_vector__method1",
                "char16_vector",
                "method1()",
                EnumSet.noneOf(MethodDefinition.Flag.class)));

    final SwJarAnalyzerCodeDefinition magikMethod2 =
        libAnalyzer.getElement(TestData.SUBSIDIARY_CLASS_CHAR16_VECTOR, "char16_vector__method2");
    assertThat(magikMethod2)
        .isEqualTo(
            new MethodDefinition(
                TestData.PATH_CHAR16_VECTOR,
                TestData.SUBSIDIARY_CLASS_CHAR16_VECTOR,
                "char16_vector__method2",
                "char16_vector",
                "method2()",
                EnumSet.noneOf(MethodDefinition.Flag.class)));

    final SwJarAnalyzerCodeDefinition magikMethod3 =
        libAnalyzer.getElement(TestData.SUBSIDIARY_CLASS_CHAR16_VECTOR, "char16_vector__method3?");
    assertThat(magikMethod3)
        .isEqualTo(
            new MethodDefinition(
                TestData.PATH_CHAR16_VECTOR,
                TestData.SUBSIDIARY_CLASS_CHAR16_VECTOR,
                "char16_vector__method3?",
                "char16_vector",
                "method3?()",
                EnumSet.noneOf(MethodDefinition.Flag.class)));

    final SwJarAnalyzerCodeDefinition magikMethod4 =
        libAnalyzer.getElement(TestData.SUBSIDIARY_CLASS_CHAR16_VECTOR, "char16_vector__method4");
    assertThat(magikMethod4)
        .isEqualTo(
            new MethodDefinition(
                TestData.PATH_CHAR16_VECTOR,
                TestData.SUBSIDIARY_CLASS_CHAR16_VECTOR,
                "char16_vector__method4",
                "char16_vector",
                "method4()",
                EnumSet.of(MethodDefinition.Flag.ABSTRACT)));

    final SwJarAnalyzerCodeDefinition magikMethod5 =
        libAnalyzer.getElement(TestData.SUBSIDIARY_CLASS_CHAR16_VECTOR, "char16_vector__method12");
    assertThat(magikMethod5)
        .isEqualTo(
            new MethodDefinition(
                TestData.PATH_CHAR16_VECTOR,
                TestData.SUBSIDIARY_CLASS_CHAR16_VECTOR,
                "char16_vector__method12",
                "char16_vector",
                "method1()",
                EnumSet.noneOf(MethodDefinition.Flag.class)));

    final SwJarAnalyzerCodeDefinition magikMethod6 =
        libAnalyzer.getElement(TestData.SUBSIDIARY_CLASS_MIXED, "float__plus_100");
    assertThat(magikMethod6)
        .isEqualTo(
            new MethodDefinition(
                TestData.PATH_MIXED,
                TestData.SUBSIDIARY_CLASS_MIXED,
                "float__plus_100",
                "float",
                "plus_100()",
                EnumSet.noneOf(MethodDefinition.Flag.class)));

    final SwJarAnalyzerCodeDefinition magikMethod7 =
        libAnalyzer.getElement(TestData.SUBSIDIARY_CLASS_MIXED, "integer__plus_100");
    assertThat(magikMethod7)
        .isEqualTo(
            new MethodDefinition(
                TestData.PATH_MIXED,
                TestData.SUBSIDIARY_CLASS_MIXED,
                "integer__plus_100",
                "integer",
                "plus_100()",
                EnumSet.noneOf(MethodDefinition.Flag.class)));
  }
}
