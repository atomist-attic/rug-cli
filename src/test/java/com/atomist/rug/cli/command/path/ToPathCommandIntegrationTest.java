package com.atomist.rug.cli.command.path;

import com.atomist.rug.cli.AbstractCommandTest;

import org.junit.Test;

public class ToPathCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testJava() throws Exception {
        assertSuccess(
                "/File()[@path='com/atomist/rug/cli/Main.java']/JavaFile()/typeDeclaration[1]/classDeclaration[1]/normalClassDeclaration[1]/classBody[1]/classBodyDeclaration[2]/classMemberDeclaration[1]/methodDeclaration[1]/methodBody[1]/block[1]/blockStatements[1]/blockStatement[2]/statement[1]/whileStatement[1]/statement[1]/statementWithoutTrailingSubstatement[1]/block[1]/blockStatements[1]/blockStatement[1]/statement[1]/statementWithoutTrailingSubstatement[1]/tryStatement[1]/block[1]/blockStatements[1]/blockStatement[2]/statement[1]/statementWithoutTrailingSubstatement[1]/expressionStatement[1]/statementExpression[1]/methodInvocation[1]/primary[1]/primaryNoNewArray_lfno_primary[1]/classInstanceCreationExpression_lfno_primary[1]/Identifier[1]",
                "to-path", "com/atomist/rug/cli/Main.java", "--line", "34", "--column", "21",
                "--kind", "JavaFile", "--change-dir", "../../../main/java");
    }

    @Test
    public void testKindFailure() throws Exception {
        assertFailure("ScalaFile", "to-path", "com/atomist/rug/cli/Main.java", "--line", "34",
                "--column", "21", "--kind", "TestKind", "--change-dir", "../../../main/java");
    }

    @Test
    public void testValidationFailure() throws Exception {
        assertFailure("Options --kind, --line and --column are required.", "to-path",
                "com/atomist/rug/cli/Main.java", "--change-dir", "../../../main/java");
    }

}
