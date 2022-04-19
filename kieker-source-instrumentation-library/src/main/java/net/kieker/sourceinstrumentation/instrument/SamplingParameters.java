package net.kieker.sourceinstrumentation.instrument;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import net.kieker.sourceinstrumentation.InstrumentationConstants;
import net.kieker.sourceinstrumentation.instrument.codeblocks.CodeBlockTransformer;

public class SamplingParameters {
   private final String counterName, sumName;
   private final String signature;

   public SamplingParameters(final String signature, final int counterIndex) {
      final String nameBeforeParanthesis = signature.substring(0, signature.indexOf('('));
      final String methodNameSubstring = nameBeforeParanthesis.substring(nameBeforeParanthesis.lastIndexOf('.') + 1);
      if (methodNameSubstring.equals("<init>")) {
         counterName = InstrumentationConstants.PREFIX + "initCounter" + counterIndex;
         sumName = InstrumentationConstants.PREFIX + "initSum" + counterIndex;
      } else {
         counterName = InstrumentationConstants.PREFIX + methodNameSubstring + "Counter" + counterIndex;
         sumName = InstrumentationConstants.PREFIX + methodNameSubstring + "Sum" + counterIndex;
      }

      this.signature = signature;
   }

   public String getCounterName() {
      return counterName;
   }

   public String getSumName() {
      return sumName;
   }

   public String getFinalBlock(TypeDeclaration<?> type, final String signature, final int count) {
      if (type instanceof ClassOrInterfaceDeclaration) {
         ClassOrInterfaceDeclaration declaration = (ClassOrInterfaceDeclaration) type;
         if (declaration.isInterface()) {
            return getPrefixedBlock(signature, count);
         } else {
            return getBlock(signature, count, sumName, counterName);
         }
      } else {
         return getPrefixedBlock(signature, count);
      }
   }

   private String getPrefixedBlock(final String signature, final int count) {
      String localCounterName = TypeInstrumenter.KIEKER_VALUES + "." + counterName;
      String localSumName = TypeInstrumenter.KIEKER_VALUES + "." + sumName;
      String basicBlock = getBlock(signature, count, localSumName, localCounterName);
      return CodeBlockTransformer.replaceStaticVariablesByClassStaticVariables(basicBlock);
   }

   private String getBlock(final String signature, final int count, String localSumName, String localCounterName) {
      return "// measure after\n" +
            "         final long " + InstrumentationConstants.PREFIX + "tout = " + InstrumentationConstants.PREFIX + "TIME_SOURCE.getTime();\n" +
            "        " + localSumName + "+=" + InstrumentationConstants.PREFIX + "tout-" + InstrumentationConstants.PREFIX + "tin;\n" +
            "if (" + localCounterName + "++%" + count + "==0){\n" +
            "final String " + InstrumentationConstants.PREFIX + "signature = \"" + signature + "\";\n" +
            "final long " + InstrumentationConstants.PREFIX + "calculatedTout=" + InstrumentationConstants.PREFIX + "tin+" + localSumName + ";\n"
            + InstrumentationConstants.PREFIX + "controller.newMonitoringRecord(new DurationRecord(" + InstrumentationConstants.PREFIX + "signature, "
            + InstrumentationConstants.PREFIX + "tin, " + InstrumentationConstants.PREFIX + "calculatedTout));\n"
            + localSumName + "=0;}\n";
   }

   public String getSignature() {
      return signature;
   }
}
