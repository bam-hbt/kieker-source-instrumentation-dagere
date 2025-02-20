package net.kieker.sourceinstrumentation;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.PrimitiveType;

import net.kieker.sourceinstrumentation.instrument.TypeInstrumenter;
import net.kieker.sourceinstrumentation.parseUtils.JavaParserProvider;
import net.kieker.sourceinstrumentation.util.TestConstants;

public class TestTypeInstrumentation {

   private static final Logger LOG = LogManager.getLogger(TestTypeInstrumentation.class);

   @Test
   public void testEnumInstrumentation() throws IOException {
      CompilationUnit unit = JavaParserProvider.parse(new File(TestConstants.RESOURCE_FOLDER, "project_2_interface/src/main/java/de/peass/SomeEnum.java"));
      EnumDeclaration declaration = unit.findAll(EnumDeclaration.class).get(0);

      InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, null, true, true, 0, false);
      TypeInstrumenter instrumenter = new TypeInstrumenter(configuration, unit, declaration);
      instrumenter.handleTypeDeclaration(declaration, "de.dagere.test");

      ConstructorDeclaration defaultConstructor = declaration.findAll(ConstructorDeclaration.class).get(0);

      System.out.println(defaultConstructor);
      MatcherAssert.assertThat(defaultConstructor.toString(), Matchers.not(Matchers.containsString("_kieker_sourceInstrumentation_controller.isProbeActivated")));
   }

   @Test
   public void testBasicInstrumentation() throws IOException {
      ClassOrInterfaceDeclaration clazz = buildClass();

      InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, null, true, true, 0, false);
      TypeInstrumenter instrumenter = new TypeInstrumenter(configuration, Mockito.mock(CompilationUnit.class), clazz);
      boolean hasChange = instrumenter.handleTypeDeclaration(clazz, "de.dagere.test");

      Assert.assertTrue(hasChange);

      MatcherAssert.assertThat(clazz.toString(), Matchers.containsString("long _kieker_sourceInstrumentation_tout = MyClazz._kieker_sourceInstrumentation_TIME_SOURCE.getTime();"));
   }

   @Test
   public void testExtractionInstrumentation() throws IOException {
      ClassOrInterfaceDeclaration clazz = buildClass();

      InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, null, true, true, 0, true);
      TypeInstrumenter instrumenter = new TypeInstrumenter(configuration, Mockito.mock(CompilationUnit.class), clazz);
      boolean hasChange = instrumenter.handleTypeDeclaration(clazz, "de.dagere.test");

      Assert.assertTrue(hasChange);

      if (configuration.isExtractMethod()) {
         List<MethodDeclaration> myMethod = clazz.getMethodsByName(InstrumentationConstants.PREFIX + "myMethod");
         Assert.assertEquals(1, myMethod.size());
         MatcherAssert.assertThat(myMethod.get(0).getModifiers(), Matchers.containsInAnyOrder(Modifier.privateModifier(), Modifier.finalModifier()));
         MatcherAssert.assertThat(clazz.toString(), Matchers.containsString("private final int " + InstrumentationConstants.PREFIX + "myMethod()"));

         List<MethodDeclaration> myRegularMethod = clazz.getMethodsByName(InstrumentationConstants.PREFIX + "myRegularMethod");
         Assert.assertEquals(1, myRegularMethod.size());
         Assert.assertEquals(myRegularMethod.get(0).getDeclarationAsString(), "private final void " + InstrumentationConstants.PREFIX + "myRegularMethod(int someParameter)");

         List<MethodDeclaration> myRegularMethodInstrumented = clazz.getMethodsByName("myRegularMethod");
         MatcherAssert.assertThat(myRegularMethodInstrumented.get(0).toString(), Matchers.containsString(InstrumentationConstants.PREFIX + "myRegularMethod(someParameter)"));

         MatcherAssert.assertThat(clazz.toString(), Matchers.containsString("private final static void " + InstrumentationConstants.PREFIX + "myStaticMethod()"));
         System.out.println(clazz.toString());
      } else {
         LOG.info("No method extraction was done since this may cause problems with Java 8. Use Java 11+ for source instrumentation.");
      }
   }

   @Test
   public void testNoExtractionAndInstrumentationWithEmptyBody() throws IOException {
      final ClassOrInterfaceDeclaration clazz = setupClazzWithAbstractMethodWithoutBody();
      final InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, null, true, true, 0, true);
      final TypeInstrumenter instrumenter = new TypeInstrumenter(configuration, Mockito.mock(CompilationUnit.class), clazz);
      Assert.assertTrue(instrumenter.handleTypeDeclaration(clazz, null));
   }

   @Test
   public void testNoModifierInstrumentation() throws IOException {
      ClassOrInterfaceDeclaration clazz = new ClassOrInterfaceDeclaration();
      clazz.setName("MyClazz");
      addMethodWithoutModifier(clazz);

      HashSet<String> includes = new HashSet<String>();
      includes.add("public void de.dagere.testMyClazz.myReqularMethodWithoutModifier()");

      final InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, includes, true, true, 0, false);

      final TypeInstrumenter instrumenter = new TypeInstrumenter(configuration, Mockito.mock(CompilationUnit.class), clazz);
      Assert.assertTrue(instrumenter.handleTypeDeclaration(clazz, "de.dagere.test"));

      if (configuration.isExtractMethod()) {
         List<MethodDeclaration> myReqularMethodWithoutModifier = clazz.getMethodsByName(InstrumentationConstants.PREFIX + "myReqularMethodWithoutModifier");
         Assert.assertEquals(1, myReqularMethodWithoutModifier.size());
         MatcherAssert.assertThat(myReqularMethodWithoutModifier.get(0).getModifiers(), Matchers.containsInAnyOrder(Modifier.privateModifier(), Modifier.finalModifier()));
         MatcherAssert.assertThat(clazz.toString(), Matchers.containsString("private final void " + InstrumentationConstants.PREFIX + "myReqularMethodWithoutModifier()"));
      }
   }

   private ClassOrInterfaceDeclaration setupClazzWithAbstractMethodWithoutBody() {
      final ClassOrInterfaceDeclaration clazz = new ClassOrInterfaceDeclaration();
      clazz.setName("MyClazz");
      clazz.addMethod("myAbstractMethod", Modifier.Keyword.ABSTRACT);
      clazz.getMethodsByName("myAbstractMethod").get(0).removeBody();
      Assert.assertFalse(clazz.getMethodsByName("myAbstractMethod").get(0).getBody().isPresent());
      return clazz;
   }

   private ClassOrInterfaceDeclaration buildClass() {

      ClassOrInterfaceDeclaration clazz = new ClassOrInterfaceDeclaration();
      clazz.setName("MyClazz");

      addSimpleMethod(clazz);
      addMethodWithParameters(clazz);
      addStaticMethod(clazz);

      return clazz;
   }

   private void addSimpleMethod(final ClassOrInterfaceDeclaration clazz) {
      BlockStmt simpleBlock = TestBlockBuilder.buildSimpleBlock();
      simpleBlock.addStatement(new ReturnStmt("return 5"));
      MethodDeclaration method = clazz.addMethod("myMethod");
      method.setBody(simpleBlock);
      method.setType("int");
   }

   private void addMethodWithParameters(final ClassOrInterfaceDeclaration clazz) {
      BlockStmt simpleBlock = TestBlockBuilder.buildSimpleBlock();
      MethodDeclaration method = clazz.addMethod("myRegularMethod", Modifier.Keyword.PRIVATE);
      method.getParameters().add(new Parameter(PrimitiveType.intType(), "someParameter"));
      method.setBody(simpleBlock);
   }

   private void addStaticMethod(final ClassOrInterfaceDeclaration clazz) {
      BlockStmt simpleBlock = TestBlockBuilder.buildSimpleBlock();
      MethodDeclaration method = clazz.addMethod("myStaticMethod", Modifier.Keyword.STATIC);
      method.setBody(simpleBlock);
   }

   private void addMethodWithoutModifier(final ClassOrInterfaceDeclaration clazz) {
      BlockStmt simpleBlock = TestBlockBuilder.buildSimpleBlock();
      MethodDeclaration method = clazz.addMethod("myReqularMethodWithoutModifier");
      method.setBody(simpleBlock);
   }

}
