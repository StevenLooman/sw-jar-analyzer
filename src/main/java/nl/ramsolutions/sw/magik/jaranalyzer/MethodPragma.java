package nl.ramsolutions.sw.magik.jaranalyzer;

import java.util.List;
import java.util.Set;

/** Pragma metadata for a Magik method, parsed from the {@code class_info} JAR entry. */
public final class MethodPragma {

  private static final Set<String> CLASSIFY_LEVELS =
      Set.of("basic", "advanced", "restricted", "debug");

  private final List<String> keywords;
  private final String moduleName;
  private final List<String> documentation;

  /**
   * Constructor for a pragma with no documentation.
   *
   * @param keywords Pragma keywords from the {@code class_info} pragma line (e.g. {@code ["basic",
   *     "private", "internal"]}).
   * @param moduleName Module name from the {@code class_info} pragma line, used as the topic.
   */
  public MethodPragma(final List<String> keywords, final String moduleName) {
    this(keywords, moduleName, List.of());
  }

  /**
   * Constructor.
   *
   * @param keywords Pragma keywords from the {@code class_info} pragma line (e.g. {@code ["basic",
   *     "private", "internal"]}).
   * @param moduleName Module name from the {@code class_info} pragma line, used as the topic.
   * @param documentation The method's documentation lines (the {@code ##} comment block), parsed
   *     from {@code class_info}, with leading/trailing blank lines trimmed.
   */
  public MethodPragma(
      final List<String> keywords, final String moduleName, final List<String> documentation) {
    this.keywords = List.copyOf(keywords);
    this.moduleName = moduleName;
    this.documentation = List.copyOf(documentation);
  }

  /**
   * Returns the method documentation lines (the {@code ##} comment block from {@code class_info}),
   * or an empty list when none.
   *
   * @return The documentation lines.
   */
  public List<String> getDocumentation() {
    return this.documentation;
  }

  /**
   * Renders the documentation as a {@code ##} comment block at the given indent, with a trailing
   * newline; empty string when there is no documentation. A blank documentation line becomes a bare
   * {@code ##} so the block stays a single contiguous comment.
   *
   * @param indent Indentation level for the comment lines.
   * @return The rendered comment block, or {@code ""}.
   */
  public String renderDocumentation(final int indent) {
    if (this.documentation.isEmpty()) {
      return "";
    }
    final String indentStr = "\t".repeat(indent);
    return this.documentation.stream()
        .map(line -> line.isBlank() ? indentStr + "##" : indentStr + "## " + line)
        .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
  }

  /** Returns {@code true} if the method has the {@code _private} modifier. */
  public boolean isPrivate() {
    return this.keywords.contains("private");
  }

  /** Returns {@code true} if the method has the {@code _iter} modifier. */
  public boolean isIter() {
    return this.keywords.contains("iter");
  }

  /**
   * Returns the {@code classify_level} value (e.g. {@code "basic"}), or {@code null} if none of the
   * recognised levels appears in the keywords.
   */
  public String getClassifyLevel() {
    return this.keywords.stream().filter(CLASSIFY_LEVELS::contains).findFirst().orElse(null);
  }

  /** Returns the module name used as the pragma topic. */
  public String getModuleName() {
    return this.moduleName;
  }

  /**
   * Renders the {@code _pragma(...)} line, e.g. {@code _pragma(classify_level=basic,
   * topic={example})}.
   */
  public String toPragmaLine() {
    final StringBuilder sb = new StringBuilder("_pragma(");
    boolean first = true;
    final String level = getClassifyLevel();
    if (level != null) {
      sb.append("classify_level=").append(level);
      first = false;
    }
    if (this.moduleName != null && !this.moduleName.isEmpty()) {
      if (!first) {
        sb.append(", ");
      }
      sb.append("topic={").append(this.moduleName).append("}");
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * Returns the method-modifier prefix string (e.g. {@code "_private "}, {@code "_iter "}, or
   * {@code "_private _iter "}), or an empty string when no modifiers apply.
   */
  public String toModifierPrefix() {
    final StringBuilder sb = new StringBuilder();
    if (isPrivate()) {
      sb.append("_private ");
    }
    if (isIter()) {
      sb.append("_iter ");
    }
    return sb.toString();
  }
}
