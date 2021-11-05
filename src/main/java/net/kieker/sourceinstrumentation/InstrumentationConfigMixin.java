package net.kieker.sourceinstrumentation;

import picocli.CommandLine.Option;

public class InstrumentationConfigMixin {
   @Option(names = { "-extractMethod", "--extractMethod" }, description = "Whether to extract the monitored method to a separate method")
   private boolean extractMethod = false;

   public void setExtractMethod(final boolean extractMethod) {
      this.extractMethod = extractMethod;
   }

   public boolean isExtractMethod() {
      return extractMethod;
   }
   
}
