package net.kieker.sourceinstrumentation;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import net.kieker.sourceinstrumentation.instrument.SignatureReader;

public class TestSignatureReader {

   @Test
   public void testSimpleReading() {
      CompilationUnit unit = new JavaParser().parse("public class MyClass { public final void myMethod(){} }").getResult().get();

      String signature = parseMyMethod(unit);

      Assert.assertEquals("public final void MyClass.myMethod()", signature);
   }

   @Test
   public void testModifierOrdering() {
      CompilationUnit unit = new JavaParser().parse("public class MyClass { final public void myMethod(){} }").getResult().get();

      String signature = parseMyMethod(unit);

      Assert.assertEquals("public final void MyClass.myMethod()", signature);
   }

   @Test
   public void testModifierOrderingPrivate() {
      CompilationUnit unit = new JavaParser().parse("public class MyClass { final private void myMethod(){} }").getResult().get();

      String signature = parseMyMethod(unit);

      Assert.assertEquals("private final void MyClass.myMethod()", signature);
   }
   
   @Test
   public void testModifierOrderingProtected() {
      CompilationUnit unit = new JavaParser().parse("public class MyClass { final protected void myMethod(){} }").getResult().get();

      String signature = parseMyMethod(unit);

      Assert.assertEquals("protected final void MyClass.myMethod()", signature);
   }

   private String parseMyMethod(CompilationUnit unit) {
      ClassOrInterfaceDeclaration clazzNode = unit.findAll(ClassOrInterfaceDeclaration.class).get(0);
      MethodDeclaration methodNode = clazzNode.findAll(MethodDeclaration.class).get(0);

      SignatureReader reader = new SignatureReader(unit, "MyClass.myMethod");

      String signature = reader.getSignature((MethodDeclaration) methodNode);
      return signature;
   }
}
