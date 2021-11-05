package net.kieker.sourceinstrumentation.it;

import java.io.IOException;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class SourceInstrumentationConstructorProblemIT {

   @Test
   public void testExecution() throws IOException {
      String monitorLogs = SimpleProjectUtil.obtainLogs("/example_constructorProblem/");
      MatcherAssert.assertThat(monitorLogs, Matchers.containsString("public void de.peass.MainTest.testMe()"));
   }
}
