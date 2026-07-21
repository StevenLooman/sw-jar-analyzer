package nl.ramsolutions.sw.magik.jaranalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/** Regression test for the entry-filter: only {@code magik/*.class} entries are loaded. */
class SwJarAnalyzerReaderFilterTest {

  private static byte[] trivialClass(final String internalName) {
    final ClassWriter cw = new ClassWriter(0);
    cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
    cw.visitEnd();
    return cw.toByteArray();
  }

  @Test
  void loadsOnlyMagikClassEntries(@TempDir final Path tmp) throws IOException {
    final Path libs = Files.createDirectories(tmp.resolve("libs"));
    final Path jar = libs.resolve("fixture.jar");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(jar))) {
      // (a) valid magik class — MUST load
      zos.putNextEntry(new ZipEntry("magik/pkg/good.class"));
      zos.write(trivialClass("magik/pkg/good"));
      zos.closeEntry();
      // (b) non-magik .class — MUST NOT load
      zos.putNextEntry(new ZipEntry("other/pkg/bad.class"));
      zos.write(trivialClass("other/pkg/bad"));
      zos.closeEntry();
      // (c) magik/ non-.class — MUST NOT load
      zos.putNextEntry(new ZipEntry("magik/pkg/notes.txt"));
      zos.write("not bytecode".getBytes());
      zos.closeEntry();
    }

    final SwJarAnalyzerReader reader = new SwJarAnalyzerReader(List.of(tmp));

    assertThat(reader.getClassByName("magik/pkg/good.class")).isNotNull();
    assertThat(reader.getClassByName("other/pkg/bad.class")).isNull();
    assertThat(reader.getClassByName("magik/pkg/notes.txt")).isNull();
  }
}
