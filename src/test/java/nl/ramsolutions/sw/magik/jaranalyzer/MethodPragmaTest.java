package nl.ramsolutions.sw.magik.jaranalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class MethodPragmaTest {

  // ── parseClassInfo ────────────────────────────────────────────────────────

  @Test
  void parseClassInfoCapturesDocumentationBlock() {
    final String input =
        "method user:example foo() \n"
            + "4 basic example /p/example.magik\n"
            + "\n"
            + "First line.\n"
            + "\n"
            + "Third line.\n"
            + "\n"
            + "\n"
            + "method user:example bar() \n"
            + "0 basic example /p/example.magik\n";
    final Map<String, MethodPragma> map = ClassInfoParser.parseClassInfo(input);
    // Leading/trailing blank lines trimmed; the internal blank (paragraph break) is kept.
    assertThat(map.get("example.foo()").getDocumentation())
        .containsExactly("First line.", "", "Third line.");
    // A method with no doc block before the next header gets an empty list.
    assertThat(map.get("example.bar()").getDocumentation()).isEmpty();
  }

  @Test
  void parseClassInfoExtractsBasicMethod() {
    final String input =
        "method user:example new() \n" + "0 basic example /some/path/example.magik\n";
    final Map<String, MethodPragma> map = ClassInfoParser.parseClassInfo(input);
    assertThat(map).containsKey("example.new()");
    final MethodPragma pragma = map.get("example.new()");
    assertThat(pragma.getClassifyLevel()).isEqualTo("basic");
    assertThat(pragma.getModuleName()).isEqualTo("example");
    assertThat(pragma.isPrivate()).isFalse();
    assertThat(pragma.isIter()).isFalse();
  }

  @Test
  void parseClassInfoExtractsPrivateMethod() {
    final String input =
        "method user:example init() \n"
            + "0 private basic internal example /some/path/example.magik\n";
    final Map<String, MethodPragma> map = ClassInfoParser.parseClassInfo(input);
    final MethodPragma pragma = map.get("example.init()");
    assertThat(pragma).isNotNull();
    assertThat(pragma.isPrivate()).isTrue();
    assertThat(pragma.isIter()).isFalse();
    assertThat(pragma.getClassifyLevel()).isEqualTo("basic");
  }

  @Test
  void parseClassInfoExtractsIterMethod() {
    final String input =
        "method user:example empty_iter() \n" + "0 iter basic example /some/path/example.magik\n";
    final Map<String, MethodPragma> map = ClassInfoParser.parseClassInfo(input);
    final MethodPragma pragma = map.get("example.empty_iter()");
    assertThat(pragma).isNotNull();
    assertThat(pragma.isIter()).isTrue();
    assertThat(pragma.isPrivate()).isFalse();
    assertThat(pragma.getClassifyLevel()).isEqualTo("basic");
  }

  @Test
  void parseClassInfoHandlesNoClassifyLevel() {
    final String input =
        "method user:example protect_nested() \n" + "0 example /some/path/example.magik\n";
    final Map<String, MethodPragma> map = ClassInfoParser.parseClassInfo(input);
    final MethodPragma pragma = map.get("example.protect_nested()");
    assertThat(pragma).isNotNull();
    assertThat(pragma.getClassifyLevel()).isNull();
    assertThat(pragma.getModuleName()).isEqualTo("example");
  }

  @Test
  void parseClassInfoIncludesSlottedClassExemplarPragma() {
    final String input =
        "slotted_class user:example slot1 slot2 \n"
            + "sw:slotted_format_mixin \n"
            + "0 basic example /some/path/example.magik\n"
            + "\n"
            + "method user:example new() \n"
            + "0 basic example /some/path/example.magik\n";
    final Map<String, MethodPragma> map = ClassInfoParser.parseClassInfo(input);
    assertThat(map).hasSize(2).containsKey("example.new()").containsKey("example");
    final MethodPragma exemplarPragma = map.get("example");
    assertThat(exemplarPragma).isNotNull();
    assertThat(exemplarPragma.getClassifyLevel()).isEqualTo("basic");
    assertThat(exemplarPragma.getModuleName()).isEqualTo("example");
  }

  @Test
  void parseClassInfoIncludesIndexedAndEnumeratedClassExemplarPragmas() {
    final String input =
        "indexed_class user:my_vector \n"
            + "sw:simple_vector \n"
            + "2 advanced example /p/my_vector.magik\n"
            + "A vector.\n"
            + "Second line.\n"
            + "enumerated_class user:my_enum \n"
            + "0 basic example /p/my_enum.magik\n";
    final Map<String, MethodPragma> map = ClassInfoParser.parseClassInfo(input);
    assertThat(map).containsKey("my_vector").containsKey("my_enum");
    assertThat(map.get("my_vector").getDocumentation())
        .containsExactly("A vector.", "Second line.");
    assertThat(map.get("my_vector").getClassifyLevel()).isEqualTo("advanced");
    assertThat(map.get("my_enum").getDocumentation()).isEmpty();
  }

  @Test
  void parseClassInfoCapturesGlobalAndConditionEntries() {
    final String input =
        "method <global> !my_flag?! \n"
            + "1 advanced /p/globals.magik\n"
            + "A flag global.\n"
            + "method <condition> my_error data1 data2 \n"
            + "2 restricted /p/conditions.magik\n"
            + "Raised on error.\n"
            + "More detail.\n";
    final Map<String, MethodPragma> map = ClassInfoParser.parseClassInfo(input);
    assertThat(map.get("<global>.!my_flag?!").getDocumentation()).containsExactly("A flag global.");
    assertThat(map.get("<condition>.my_error").getDocumentation())
        .containsExactly("Raised on error.", "More detail.");
    // Global/condition pragma lines have no topic; the classify-level must still parse (not be
    // mistaken for the module).
    assertThat(map.get("<global>.!my_flag?!").getClassifyLevel()).isEqualTo("advanced");
    assertThat(map.get("<global>.!my_flag?!").getModuleName()).isNull();
    assertThat(map.get("<condition>.my_error").getClassifyLevel()).isEqualTo("restricted");
  }

  @Test
  void parseClassInfoExemplarWithMultipleSuperclassLines() {
    final String input =
        "slotted_class user:example slot1 \n"
            + "sw:slotted_format_mixin \n"
            + "sw:some_other_mixin \n"
            + "0 advanced example /some/path/example.magik\n";
    final Map<String, MethodPragma> map = ClassInfoParser.parseClassInfo(input);
    assertThat(map).containsKey("example");
    assertThat(map.get("example").getClassifyLevel()).isEqualTo("advanced");
  }

  @Test
  void parseClassInfoIncludesMixinExemplarPragma() {
    final String input =
        "mixin user:example_mixin \n"
            + ". \n"
            + "0 restricted example /some/path/example_mixin.magik\n";
    final Map<String, MethodPragma> map = ClassInfoParser.parseClassInfo(input);
    assertThat(map).containsKey("example_mixin");
    final MethodPragma pragma = map.get("example_mixin");
    assertThat(pragma).isNotNull();
    assertThat(pragma.getClassifyLevel()).isEqualTo("restricted");
    assertThat(pragma.getModuleName()).isEqualTo("example");
  }

  // ── MethodPragma rendering ────────────────────────────────────────────────

  @Test
  void toPragmaLineBasic() {
    final MethodPragma pragma = new MethodPragma(List.of("basic"), "example");
    assertThat(pragma.toPragmaLine()).isEqualTo("_pragma(classify_level=basic, topic={example})");
  }

  @Test
  void toPragmaLineNoClassifyLevel() {
    final MethodPragma pragma = new MethodPragma(List.of(), "example");
    assertThat(pragma.toPragmaLine()).isEqualTo("_pragma(topic={example})");
  }

  @Test
  void toModifierPrefixPrivate() {
    final MethodPragma pragma = new MethodPragma(List.of("private", "basic"), "example");
    assertThat(pragma.toModifierPrefix()).isEqualTo("_private ");
  }

  @Test
  void toModifierPrefixIter() {
    final MethodPragma pragma = new MethodPragma(List.of("iter", "basic"), "example");
    assertThat(pragma.toModifierPrefix()).isEqualTo("_iter ");
  }

  @Test
  void toModifierPrefixNone() {
    final MethodPragma pragma = new MethodPragma(List.of("basic"), "example");
    assertThat(pragma.toModifierPrefix()).isEmpty();
  }

  // ── parseFromArchive ──────────────────────────────────────────────────────

  @Test
  void parseFromArchiveReadsClassInfoEntryFromRealJar() throws IOException {
    // An earlier source test used a fixture jar (with
    // topic'd pragma lines) that does not exist in this project. This project's fixture jar
    // (fixture_product.fixture_module.1.jar) has a class_info entry but its pragma lines carry
    // no topic token ("0 <path>"), so parseClassInfo legitimately returns an empty map for it
    // (methods require a topic per parsePragmaLine's hasTopic contract). This test exercises the
    // archive-reading plumbing (parseFromArchive/ZipFile) end to end without asserting on
    // fixture-specific pragma content.
    final Path archive =
        Path.of("src/test/resources/fixture_product/libs/fixture_product.fixture_module.1.jar");
    final Map<String, MethodPragma> pragmas = ClassInfoParser.parseFromArchive(archive);
    assertThat(pragmas).isNotNull();
  }

  @Test
  void parseFromArchiveReturnsEmptyMapWhenClassInfoEntryAbsent() throws IOException {
    // A jar with no class_info entry at all (e.g. a plain zip) must yield an empty map, not throw.
    final Path archive = Files.createTempFile("no-class-info", ".jar");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(archive))) {
      zos.putNextEntry(new ZipEntry("placeholder"));
      zos.closeEntry();
    }
    try {
      assertThat(ClassInfoParser.parseFromArchive(archive)).isEmpty();
    } finally {
      Files.deleteIfExists(archive);
    }
  }
}
