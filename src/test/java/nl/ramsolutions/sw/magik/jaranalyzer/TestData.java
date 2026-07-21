package nl.ramsolutions.sw.magik.jaranalyzer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Test data definitions for sw-jar-analyzer fixtures. */
public final class TestData {

  public static final Path PRODUCT_PATH = Path.of("src/test/resources/fixture_product");
  public static final List<Path> PRODUCT_PATHS = List.of(PRODUCT_PATH);

  public static final String CLASS_DOES_NOT_EXIST =
      "magik/fixture_product/fixture_module/does_not_exist_99";

  public static final String PRIMARY_CLASS_CHAR16_VECTOR =
      "magik/fixture_product/fixture_module/char16_vector_36";
  public static final String PRIMARY_CLASS_MIXED = "magik/fixture_product/fixture_module/mixed_60";
  public static final String PRIMARY_CLASS_PRIMARY =
      "magik/fixture_product/fixture_module/primary_64";

  public static final String SUBSIDIARY_CLASS_CHAR16_VECTOR =
      "magik/fixture_product/fixture_module/char16_vector_37";
  public static final String SUBSIDIARY_CLASS_MIXED =
      "magik/fixture_product/fixture_module/mixed_61";

  public static final Path PATH_CHAR16_VECTOR =
      Path.of("modules/fixture_module/source/char16_vector.magik");
  public static final Path PATH_MIXED = Path.of("modules/fixture_module/source/mixed.magik");
  public static final Path PATH_PRIMARY = Path.of("modules/fixture_module/source/primary.magik");

  private TestData() {}

  /** Get a reader over the fixture product. */
  public static SwJarAnalyzerReader getLibReader() throws IOException {
    return new SwJarAnalyzerReader(TestData.PRODUCT_PATHS);
  }

  /** Get an analyzer over the fixture product. */
  public static SwJarAnalyzerAnalyzer getLibAnalyzer() throws IOException {
    return new SwJarAnalyzerAnalyzer(TestData.getLibReader());
  }
}
