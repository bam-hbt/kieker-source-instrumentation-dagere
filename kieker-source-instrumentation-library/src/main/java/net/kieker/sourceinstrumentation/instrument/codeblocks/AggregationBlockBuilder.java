package net.kieker.sourceinstrumentation.instrument.codeblocks;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.TryStmt;

import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationCodeBlocks;
import net.kieker.sourceinstrumentation.instrument.SamplingParameters;

public class AggregationBlockBuilder extends BlockBuilder {

   private final int count;

   public AggregationBlockBuilder(final AllowedKiekerRecord recordType, final int count) {
      super(recordType, false, false);
      this.count = count;
   }

   @Override
   public BlockStmt buildStatement(TypeDeclaration<?> type, final BlockStmt originalBlock, final boolean addReturn, final SamplingParameters parameters,
         final CodeBlockTransformer transformer) {
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         throw new RuntimeException(
               "Not implemented yet (Aggregation + OperationExecutionRecord does not make sense, since OperationExecutionRecord contains too complex metadata for sampling)");
      } else if (recordType.equals(AllowedKiekerRecord.DURATION)) {
         if (!useStaticVariables) {
            return super.buildStatement(type, originalBlock, addReturn, parameters, transformer);
         } else {
            return buildSelectiveSamplingStatement(type, originalBlock, addReturn, parameters);
         }
      } else {
         throw new RuntimeException();
      }
   }

   @Override
   public BlockStmt buildEmptyConstructor(final TypeDeclaration<?> type, final SamplingParameters parameters, final CodeBlockTransformer transformer) {
      if (recordType.equals(AllowedKiekerRecord.OPERATIONEXECUTION)) {
         throw new RuntimeException(
               "Not implemented yet (Aggregation + OperationExecutionRecord does not make sense, since OperationExecutionRecord contains too complex metadata for sampling)");
      } else if (recordType.equals(AllowedKiekerRecord.DURATION)) {
         return buildConstructorStatement(type, parameters);
      } else {
         throw new RuntimeException();
      }
   }

   public BlockStmt buildSelectiveSamplingStatement(TypeDeclaration<?> type, final BlockStmt originalBlock, final boolean addReturn, final SamplingParameters parameters) {
      BlockStmt replacedStatement = new BlockStmt();
      final String beforeText = getBeforeText(type);
      replacedStatement.addAndGetStatement(beforeText);

      BlockStmt finallyBlock = new BlockStmt();
      finallyBlock.addAndGetStatement(parameters.getFinalBlock(type, parameters.getSignature(), count));
      TryStmt stmt = new TryStmt(originalBlock, new NodeList<>(), finallyBlock);
      replacedStatement.addAndGetStatement(stmt);

      return replacedStatement;
   }

   private String getBeforeText(TypeDeclaration<?> type) {
      final String beforeText;
      if (type instanceof ClassOrInterfaceDeclaration) {
         ClassOrInterfaceDeclaration declaration = (ClassOrInterfaceDeclaration) type;
         if (declaration.isInterface()) {
            beforeText = CodeBlockTransformer.replaceStaticVariablesByClassStaticVariables(InstrumentationCodeBlocks.AGGREGATION.getBefore());
         } else {
            beforeText = InstrumentationCodeBlocks.AGGREGATION.getBefore();
         }
      } else {
         beforeText = CodeBlockTransformer.replaceStaticVariablesByClassStaticVariables(InstrumentationCodeBlocks.AGGREGATION.getBefore());
      }
      return beforeText;
   }

   public BlockStmt buildConstructorStatement(TypeDeclaration<?> type, SamplingParameters parameters) {
      BlockStmt replacedStatement = new BlockStmt();
      final String beforeText = getBeforeText(type);
      replacedStatement.addAndGetStatement(beforeText);
      replacedStatement.addAndGetStatement(parameters.getFinalBlock(type, parameters.getSignature(), count));
      return replacedStatement;
   }
}
