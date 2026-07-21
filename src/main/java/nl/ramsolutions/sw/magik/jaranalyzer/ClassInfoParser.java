package nl.ramsolutions.sw.magik.jaranalyzer;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Parses the {@code class_info} JAR entry into a map of {@link MethodPragma}, keyed by {@code
 * "<exemplar>.<method_sig>"} for methods and by the exemplar name for class/mixin entries.
 */
public final class ClassInfoParser {

  private ClassInfoParser() {}

  /** Reads and parses the {@code class_info} entry of {@code archive}; empty map if absent. */
  public static Map<String, MethodPragma> parseFromArchive(final Path archive) throws IOException {
    final File file = archive.toFile();
    try (final ZipFile zipFile = new ZipFile(file)) {
      final ZipEntry entry = zipFile.getEntry("class_info");
      if (entry == null) {
        return Map.of();
      }
      try (final InputStream is = zipFile.getInputStream(entry)) {
        final String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        return parseClassInfo(content);
      }
    }
  }

  /**
   * Parses the {@code class_info} text into a pragma map containing both method and exemplar
   * entries.
   *
   * <p>Method entries have the form:
   *
   * <pre>
   * method &lt;package&gt;:&lt;exemplar&gt; &lt;method_sig&gt; [&lt;param&gt; ...]
   * &lt;line_num&gt; [&lt;keyword&gt; ...] &lt;module_name&gt; &lt;file_path&gt;
   * </pre>
   *
   * and are keyed by {@code "<exemplar>.<method_sig>"} (e.g. {@code "example.new()"}).
   *
   * <p>Slotted-class and mixin exemplar entries have the form:
   *
   * <pre>
   * slotted_class &lt;package&gt;:&lt;exemplar&gt; [&lt;slot&gt; ...]
   * &lt;superclass&gt; [...]
   * &lt;line_num&gt; [&lt;keyword&gt; ...] &lt;module_name&gt; &lt;file_path&gt;
   *
   * mixin &lt;package&gt;:&lt;exemplar&gt;
   * .
   * &lt;line_num&gt; [&lt;keyword&gt; ...] &lt;module_name&gt; &lt;file_path&gt;
   * </pre>
   *
   * and are keyed by just the exemplar name (e.g. {@code "example"}).
   */
  public static Map<String, MethodPragma> parseClassInfo(final String content) {
    final Map<String, MethodPragma> result = new HashMap<>();
    final String[] lines = content.split("\n");

    for (int i = 0; i < lines.length; i++) {
      final String line = lines[i].trim();

      if (line.startsWith("method ")) {
        // "method <package>:<exemplar> <method_sig> [<param>...]"
        final String[] lineParts = line.split("\\s+", 3);
        if (lineParts.length < 3) {
          continue;
        }
        final String identifier = lineParts[1]; // "user:example"
        final int colon = identifier.indexOf(':');
        final String exemplar = colon >= 0 ? identifier.substring(colon + 1) : identifier;
        final String methodSig = lineParts[2].trim().split("\\s+")[0];

        // Real methods carry a topic token on the pragma line; the <global>/<condition>
        // pseudo-method
        // entries do not (their pragma line is "<count> <classify_level> <source_file>").
        final boolean hasTopic =
            !"<global>".equals(identifier) && !"<condition>".equals(identifier);
        final MethodPragma pragma = parsePragmaLine(lines, i + 1, false, hasTopic);
        if (pragma != null) {
          result.put(exemplar + "." + methodSig, pragma);
        }

      } else if (line.startsWith("slotted_class ")
          || line.startsWith("mixin ")
          || line.startsWith("indexed_class ")
          || line.startsWith("enumerated_class ")) {
        // "slotted_class <package>:<exemplar> [<slot>...]"
        // "mixin <package>:<exemplar>"
        // "indexed_class <package>:<exemplar> ..." / "enumerated_class <package>:<exemplar> ..."
        final String[] lineParts = line.split("\\s+", 3);
        if (lineParts.length < 2) {
          continue;
        }
        final String identifier = lineParts[1]; // "user:example"
        final int colon = identifier.indexOf(':');
        final String exemplar = colon >= 0 ? identifier.substring(colon + 1) : identifier;

        // Skip superclass lines until we reach the pragma line (starts with a digit).
        final MethodPragma pragma = parsePragmaLine(lines, i + 1, true, true);
        if (pragma != null) {
          result.put(exemplar, pragma);
        }
      }
    }
    return result;
  }

  /**
   * Finds and parses the pragma line following a {@code method} or {@code slotted_class} header.
   *
   * @param lines All lines of the {@code class_info} content.
   * @param start Index of the first line to inspect (immediately after the header line).
   * @param skipNonDigit When {@code true}, skip lines that do not start with a digit (superclass
   *     lines in a {@code slotted_class} block); when {@code false}, skip only blank lines.
   * @param hasTopic When {@code true} the pragma line ends with a topic/module token before the
   *     source-file path ({@code <count> [keyword...] <topic> <file>}); when {@code false} it has
   *     no topic ({@code <count> [keyword...] <file>}, as for {@code <global>}/{@code
   *     <condition>}).
   * @return The parsed {@link MethodPragma}, or {@code null} if none found.
   */
  @CheckForNull
  private static MethodPragma parsePragmaLine(
      final String[] lines, final int start, final boolean skipNonDigit, final boolean hasTopic) {
    int j = start;
    while (j < lines.length) {
      final String candidate = lines[j].trim();
      if (candidate.isEmpty()) {
        j++;
        continue;
      }
      if (skipNonDigit && !Character.isDigit(candidate.charAt(0))) {
        j++;
        continue;
      }
      // Pragma line: "<count> [keyword...] [<topic>] <file_path>". The topic (module) is present
      // only for methods/exemplars; globals/conditions omit it. The file path is always last.
      final String[] pragmaParts = candidate.split("\\s+");
      final int minParts = hasTopic ? 3 : 2;
      if (pragmaParts.length < minParts) {
        return null;
      }
      final int keywordEnd = hasTopic ? pragmaParts.length - 2 : pragmaParts.length - 1;
      final String moduleName = hasTopic ? pragmaParts[pragmaParts.length - 2] : null;
      final List<String> keywords = new ArrayList<>();
      for (int k = 1; k < keywordEnd; k++) {
        keywords.add(pragmaParts[k]);
      }
      return new MethodPragma(keywords, moduleName, collectDocumentation(lines, j + 1));
    }
    return null;
  }

  /**
   * Collect the documentation block that follows a pragma line: every line from {@code start} up to
   * (but not including) the next entry header ({@code method}/{@code slotted_class}/{@code mixin}),
   * with leading and trailing blank lines trimmed.
   *
   * @param lines All lines of the {@code class_info} content.
   * @param start Index of the first line after the pragma line.
   * @return The documentation lines (possibly empty), trailing whitespace stripped.
   */
  private static List<String> collectDocumentation(final String[] lines, final int start) {
    final List<String> doc = new ArrayList<>();
    for (int k = start; k < lines.length && !isEntryHeader(lines[k]); k++) {
      doc.add(lines[k].stripTrailing());
    }
    int from = 0;
    int to = doc.size();
    while (from < to && doc.get(from).isBlank()) {
      from++;
    }
    while (to > from && doc.get(to - 1).isBlank()) {
      to--;
    }
    return new ArrayList<>(doc.subList(from, to));
  }

  /** Whether a {@code class_info} line begins a new method/exemplar/mixin entry. */
  private static boolean isEntryHeader(final String line) {
    final String trimmed = line.trim();
    return trimmed.startsWith("method ")
        || trimmed.startsWith("slotted_class ")
        || trimmed.startsWith("mixin ")
        || trimmed.startsWith("indexed_class ")
        || trimmed.startsWith("enumerated_class ")
        || trimmed.startsWith("delete_class ");
  }
}
