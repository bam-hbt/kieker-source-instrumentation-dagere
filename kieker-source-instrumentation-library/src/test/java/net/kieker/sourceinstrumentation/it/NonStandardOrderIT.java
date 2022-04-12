package net.kieker.sourceinstrumentation.it;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationConfiguration;
import net.kieker.sourceinstrumentation.SourceInstrumentationTestUtil;
import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;
import net.kieker.sourceinstrumentation.util.TestConstants;

public class NonStandardOrderIT {

   @BeforeEach
   public void before() throws IOException {
      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
      
      SimpleProjectUtil.cleanTempDir();
   }

   public File writeAdaptiveInstrumentationInfo() throws IOException {
      final File configFolder = new File(TestConstants.CURRENT_FOLDER, "config");
      configFolder.mkdir();

      final File adaptiveFile = new File(TestConstants.CURRENT_FOLDER, TestConstants.KIEKER_ADAPTIVE_FILENAME);
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(adaptiveFile))) {
         writer.write("- *\n");
         writer.write("+ public void de.peass.MainTest.testMe()\n");
         writer.flush();
      }
      return adaptiveFile;
   }

   @Test
   public void testExecution() throws IOException {
      SourceInstrumentationTestUtil.initSimpleProject("/example_nonStandardOrder/");
      
      File adaptiveFile = writeAdaptiveInstrumentationInfo();

      File tempFolder = new File(TestConstants.CURRENT_FOLDER, "results");
      tempFolder.mkdir();

      InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, 
            false, null, false, true, 0, true);
      
      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(configuration);
      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      final ProcessBuilder pb = new ProcessBuilder("mvn", "test",
            "-Djava.io.tmpdir=" + tempFolder.getAbsolutePath(),
            "-Dkieker.monitoring.adaptiveMonitoring.enabled=true",
            "-Dkieker.monitoring.adaptiveMonitoring.configFile=" + adaptiveFile.getAbsolutePath(),
            "-Dkieker.monitoring.adaptiveMonitoring.readInterval=1");
      pb.directory(TestConstants.CURRENT_FOLDER);

      String monitorLogs =  SimpleProjectUtil.getLogsFromProcessBuilder(pb);
      
      MatcherAssert.assertThat(monitorLogs, Matchers.containsString("public void de.peass.MainTest.testMe()"));
      MatcherAssert.assertThat(monitorLogs, Matchers.containsString("public final void de.peass.C0_0.method0(java.lang.String)"));
      
   }
}
