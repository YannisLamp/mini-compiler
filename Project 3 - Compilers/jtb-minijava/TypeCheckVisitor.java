import syntaxtree.*;
import visitor.GJDepthFirst;
/**
 * Performs the rest of the semantic check using the data stored into the Symbol Table
 */

public class TypeCheckVisitor extends GJDepthFirst<String, SymbolTable> {

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    public String visit(MainClass n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);

        String classId = n.f1.accept(this, symTable);
        //n.f2.accept(this, symTable);
        symTable.enter(classId);

        //n.f3.accept(this, symTable);
        //n.f4.accept(this, symTable);

        String mainName = n.f6.toString();

        //n.f7.accept(this, symTable);
        symTable.enter(mainName);

        //n.f8.accept(this, symTable);
        //n.f9.accept(this, symTable);
        //n.f10.accept(this, symTable);
        n.f11.accept(this, symTable);
        //n.f12.accept(this, symTable);
        //n.f13.accept(this, symTable);
        n.f14.accept(this, symTable);
        n.f15.accept(this, symTable);

        symTable.exit();
        //n.f16.accept(this, symTable);

        symTable.exit();
        //n.f17.accept(this, symTable);
        return null;
    }


    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public String visit(ClassDeclaration n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);

        String classId = n.f1.accept(this, symTable);
        //n.f2.accept(this, symTable);
        symTable.enter(classId);

        n.f3.accept(this, symTable);
        n.f4.accept(this, symTable);

        symTable.exit();
        //n.f5.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public String visit(ClassExtendsDeclaration n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);

        String classId = n.f1.accept(this, symTable);

        //n.f2.accept(this, symTable);
        //n.f3.accept(this, symTable);

        //n.f4.accept(this, symTable);
        symTable.enter(classId);

        n.f5.accept(this, symTable);
        n.f6.accept(this, symTable);

        symTable.exit();
        //n.f7.accept(this, symTable);
        return null;
    }


    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public String visit(MethodDeclaration n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);

        //n.f1.accept(this, symTable);
        String methodId = n.f2.accept(this, symTable);

        symTable.enter(methodId);
        //n.f3.accept(this, symTable);
        n.f4.accept(this, symTable);

        //n.f5.accept(this, symTable);
        //n.f6.accept(this, symTable);
        n.f7.accept(this, symTable);
        n.f8.accept(this, symTable);
        //n.f9.accept(this, symTable);

        String retExpr = n.f10.accept(this, symTable);
        String retType;
        if (isType(retExpr))
            retType = retExpr;
        else if (symTable.classExists(retExpr))
            retType = retExpr;
        else if (retExpr.equals("this"))
            retType = symTable.getCurrClassId();
        else
            retType = symTable.lookup(retExpr);

        if (retType == null)
            throw new Exception("Error at line " + n.f11.beginLine + ": " + retExpr + " has not been declared");
        else if (!retType.equals(symTable.getCurrMethRetType()))
            throw new Exception("Error at line " + n.f11.beginLine + ": Method " + methodId +
                    " should return " + symTable.getCurrMethRetType());

        //n.f11.accept(this, symTable);
        symTable.exit();
        //n.f12.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> Block()
     *       | AssignmentStatement()
     *       | ArrayAssignmentStatement()
     *       | IfStatement()
     *       | WhileStatement()
     *       | PrintStatement()
     */
    public String visit(Statement n, SymbolTable symTable) throws Exception {
        return n.f0.accept(this, symTable);
    }

    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    public String visit(Block n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);
        //n.f2.accept(this, symTable);
        return n.f1.accept(this, symTable);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, SymbolTable symTable) throws Exception {
        String leftId = n.f0.accept(this, symTable);
        String leftType = symTable.lookup(leftId);
        if (leftType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + leftId + " has not been declared");

        //n.f1.accept(this, symTable);
        String rightExpr = n.f2.accept(this, symTable);
        String rightType;
        if (isType(rightExpr))
            rightType = rightExpr;
        else if (symTable.classExists(rightExpr))
            rightType = rightExpr;
        else if (rightExpr.equals("this"))
            rightType = symTable.getCurrClassId();
        else
            rightType = symTable.lookup(rightExpr);

        if (rightType == null)
            throw new Exception("Error at line " + n.f3.beginLine + ": " + rightExpr + " has not been declared");
        if (!leftType.equals(rightType) && !symTable.isSubclassOf(rightType, leftType))
            throw new Exception("Error at line " + n.f3.beginLine + ": Assignment to " + leftId +
                    " should be of type " + leftType);

        //n.f3.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public String visit(ArrayAssignmentStatement n, SymbolTable symTable) throws Exception {
        String leftId = n.f0.accept(this, symTable);
        String leftType = symTable.lookup(leftId);
        if (leftType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + leftId + " has not been declared");

        //n.f1.accept(this, symTable);
        String indexExpr = n.f2.accept(this, symTable);
        String indexType;
        if (isType(indexExpr))
            indexType = indexExpr;
        else
            indexType = symTable.lookup(indexExpr);

        if (indexType == null)
            throw new Exception("Error at line " + n.f3.beginLine + ": " + indexExpr + " has not been declared");
        if (!indexType.equals("int"))
            throw new Exception("Error at line " + n.f3.beginLine + ": Index in array lookup " + leftId +
                    "[] should be of type integer");

        //n.f3.accept(this, symTable);
        //n.f4.accept(this, symTable);

        String rightExpr = n.f5.accept(this, symTable);
        String rightType;
        if (isType(rightExpr))
            rightType = rightExpr;
        else
            rightType = symTable.lookup(rightExpr);

        if (rightType == null)
            throw new Exception("Error at line " + n.f6.beginLine + ": " + rightExpr + " has not been declared");
        if (!rightType.equals("int"))
            throw new Exception("Error at line " + n.f6.beginLine + ": Assignment to " + leftId +
                    " should return a result of type " + leftType);

        //n.f6.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public String visit(IfStatement n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);
        //n.f1.accept(this, symTable);

        String condExpr = n.f2.accept(this, symTable);
        String condType;
        if (isType(condExpr))
            condType = condExpr;
        else
            condType = symTable.lookup(condExpr);

        if (condType == null)
            throw new Exception("Error at line " + n.f3.beginLine + ": " + condExpr + " has not been declared");
        if (!condType.equals("boolean"))
            throw new Exception("Error at line " + n.f3.beginLine +
                    ": Expression inside if statement condition should be of type boolean");

        //n.f3.accept(this, symTable);
        n.f4.accept(this, symTable);
        //n.f5.accept(this, symTable);
        n.f6.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public String visit(WhileStatement n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);
        //n.f1.accept(this, symTable);
        String condExpr = n.f2.accept(this, symTable);
        String condType;
        if (isType(condExpr))
            condType = condExpr;
        else
            condType = symTable.lookup(condExpr);

        if (condType == null)
            throw new Exception("Error at line " + n.f3.beginLine + ": " + condExpr + " has not been declared");
        if (!condType.equals("boolean"))
            throw new Exception("Error at line " + n.f3.beginLine +
                    ": Expression inside while statement condition should be of type boolean");

        //n.f3.accept(this, symTable);
        n.f4.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public String visit(PrintStatement n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);
        //n.f1.accept(this, symTable);
        String prntExpr = n.f2.accept(this, symTable);
        String prntType;
        if (isType(prntExpr))
            prntType = prntExpr;
        else
            prntType = symTable.lookup(prntExpr);

        if (prntType == null)
            throw new Exception("Error at line " + n.f3.beginLine + ": " + prntExpr + " has not been declared");
        if (!prntType.equals("int"))
            throw new Exception("Error at line " + n.f3.beginLine +
                    ": Expression inside println statement must be of type integer");

        //n.f3.accept(this, symTable);
        //n.f4.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | Clause()
     */
    public String visit(Expression n, SymbolTable symTable) throws Exception {
        return n.f0.accept(this, symTable);
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, SymbolTable symTable) throws Exception {
        String leftClause = n.f0.accept(this, symTable);
        String leftClauseType;
        if (isType(leftClause))
            leftClauseType = leftClause;
        else
            leftClauseType = symTable.lookup(leftClause);

        if (leftClauseType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + leftClause + " has not been declared");
        if (!leftClauseType.equals("boolean") )
            throw new Exception("Error at line " + n.f1.beginLine + ": Left clause of \"&&\" expression must be of type boolean");

        //n.f1.accept(this, symTable);

        String rightClause = n.f0.accept(this, symTable);
        String rightClauseType;
        if (isType(rightClause))
            rightClauseType = rightClause;
        else
            rightClauseType = symTable.lookup(rightClause);

        if (rightClauseType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + rightClause + " has not been declared");
        if (!rightClauseType.equals("boolean") )
            throw new Exception("Error at line " + n.f1.beginLine + ": Right clause of \"&&\" expression must be of type boolean");

        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, SymbolTable symTable) throws Exception {
        String leftPriExp = n.f0.accept(this, symTable);
        String leftPriExpType;
        if (isType(leftPriExp))
            leftPriExpType = leftPriExp;
        else
            leftPriExpType = symTable.lookup(leftPriExp);

        if (leftPriExpType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + leftPriExp + " has not been declared");
        if (!leftPriExpType.equals("int"))
            throw new Exception("Error at line " + n.f1.beginLine + ": Left expression of \"<\" comparison must be of type int");

        //n.f1.accept(this, symTable);

        String rightPriExp = n.f0.accept(this, symTable);
        String rightPriExpType;
        if (isType(rightPriExp))
            rightPriExpType = rightPriExp;
        else
            rightPriExpType = symTable.lookup(rightPriExp);

        if (rightPriExpType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + rightPriExp + " has not been declared");
        if (!rightPriExpType.equals("int"))
            throw new Exception("Error at line " + n.f1.beginLine + ": Right expression of \"<\" comparison must be of type int");

        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, SymbolTable symTable) throws Exception {
        String leftPriExp = n.f0.accept(this, symTable);
        String leftPriExpType;
        if (isType(leftPriExp))
            leftPriExpType = leftPriExp;
        else
            leftPriExpType = symTable.lookup(leftPriExp);

        if (leftPriExpType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + leftPriExp + " has not been declared");
        if (!leftPriExpType.equals("int"))
            throw new Exception("Error at line " + n.f1.beginLine + ": Left expression of \"+\" expression must be of type int");

        //n.f1.accept(this, symTable);

        String rightPriExp = n.f0.accept(this, symTable);
        String rightPriExpType;
        if (isType(rightPriExp))
            rightPriExpType = rightPriExp;
        else
            rightPriExpType = symTable.lookup(rightPriExp);

        if (rightPriExpType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + rightPriExp + " has not been declared");
        if (!rightPriExpType.equals("int"))
            throw new Exception("Error at line " + n.f1.beginLine + ": Right expression of \"+\" expression must be of type int");

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, SymbolTable symTable) throws Exception {
        String leftPriExp = n.f0.accept(this, symTable);
        String leftPriExpType;
        if (isType(leftPriExp))
            leftPriExpType = leftPriExp;
        else
            leftPriExpType = symTable.lookup(leftPriExp);

        if (leftPriExpType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + leftPriExp + " has not been declared");
        if (!leftPriExpType.equals("int"))
            throw new Exception("Error at line " + n.f1.beginLine + ": Left expression of \"-\" expression must be of type int");

        //n.f1.accept(this, symTable);

        String rightPriExp = n.f0.accept(this, symTable);
        String rightPriExpType;
        if (isType(rightPriExp))
            rightPriExpType = rightPriExp;
        else
            rightPriExpType = symTable.lookup(rightPriExp);

        if (rightPriExpType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + rightPriExp + " has not been declared");
        if (!rightPriExpType.equals("int"))
            throw new Exception("Error at line " + n.f1.beginLine + ": Right expression of \"-\" expression must be of type int");

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, SymbolTable symTable) throws Exception {
        String leftPriExp = n.f0.accept(this, symTable);
        String leftPriExpType;
        if (isType(leftPriExp))
            leftPriExpType = leftPriExp;
        else
            leftPriExpType = symTable.lookup(leftPriExp);

        if (leftPriExpType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + leftPriExp + " has not been declared");
        if (!leftPriExpType.equals("int"))
            throw new Exception("Error at line " + n.f1.beginLine + ": Left expression of \"*\" expression must be of type int");

        //n.f1.accept(this, symTable);

        String rightPriExp = n.f0.accept(this, symTable);
        String rightPriExpType;
        if (isType(rightPriExp))
            rightPriExpType = rightPriExp;
        else
            rightPriExpType = symTable.lookup(rightPriExp);

        if (rightPriExpType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + rightPriExp + " has not been declared");
        if (!rightPriExpType.equals("int"))
            throw new Exception("Error at line " + n.f1.beginLine + ": Right expression of \"*\" expression must be of type int");

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, SymbolTable symTable) throws Exception {
        String arrayId = n.f0.accept(this, symTable);
        String arrayType = symTable.lookup(arrayId);
        if (arrayType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + arrayId + " has not been declared");
        if (!arrayType.equals("int[]"))
            throw new Exception("Error at line " + n.f1.beginLine + ": " + arrayId + " is not of type array");

        //n.f1.accept(this, symTable);

        String arrayIndex = n.f2.accept(this, symTable);
        String arrayIndexType;
        if (isType(arrayIndex))
            arrayIndexType = arrayIndex;
        else
            arrayIndexType = symTable.lookup(arrayIndex);

        if (arrayIndexType == null)
            throw new Exception("Error at line " + n.f3.beginLine + ": " + arrayIndex + " has not been declared");
        if (!arrayIndexType.equals("int"))
            throw new Exception("Error at line " + n.f3.beginLine + ": Index of array" + arrayId + " should be of type int");

        //n.f3.accept(this, symTable);

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, SymbolTable symTable) throws Exception {
        String arrayId = n.f0.accept(this, symTable);
        String arrayType = symTable.lookup(arrayId);
        if (arrayType == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": " + arrayId + " has not been declared");
        if (!arrayType.equals("int[]"))
            throw new Exception("Error at line " + n.f1.beginLine + ": " + arrayId + " is not an array");

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, SymbolTable symTable) throws Exception {
        String objectId = n.f0.accept(this, symTable);
        String classId;
        if (objectId.equals("this"))
            classId = symTable.getCurrClassId();
        else if (symTable.classExists(objectId))
            classId = objectId;
        else
            classId = symTable.lookup(objectId);

        if (classId == null)
            throw new Exception("Error at line " + n.f1.beginLine + ": Object " + objectId + " has not been declared");

        //n.f1.accept(this, symTable);

        String methodId = n.f2.accept(this, symTable);
        symTable.loadChkMethInfo(classId, methodId, n.f3.beginLine);
        if (!symTable.chkMethFound())
            throw new Exception("Error at line " + n.f3.beginLine + ": Object " + objectId +
                    " does not have a method " + methodId);

        //n.f3.accept(this, symTable);
        n.f4.accept(this, symTable);
        if (!symTable.chkMethodCorrectArgNum())
            throw new Exception("Error at line " + n.f5.beginLine + ": Wrong argument number for method " + methodId + " call");

        String retType = symTable.getChkMethRetType();
        symTable.resetChkMethInfo();
        //n.f5.accept(this, symTable);

        return retType;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */

    public String visit(ExpressionList n, SymbolTable symTable) throws Exception {
        String expr = n.f0.accept(this, symTable);
        String exprType;
        if (isType(expr))
            exprType = expr;
        else if (symTable.classExists(expr))
            exprType = expr;
        else if (expr.equals("this"))
            exprType = symTable.getCurrClassId();
        else
            exprType = symTable.lookup(expr);

        if (exprType == null)
            throw new Exception("Error at line " + symTable.getChkCurrLine() + ": " + expr + " has not been declared");

        String argCorrType = symTable.getChkMethNextArgType();

        if (!exprType.equals(argCorrType) && !symTable.isSubclassOf(exprType, argCorrType))
            throw new Exception("Error at line " + symTable.getChkCurrLine() + ": Argument must be of type "
                    + argCorrType + " or a subclass of it");

        n.f1.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public String visit(ExpressionTerm n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);
        String expr = n.f1.accept(this, symTable);
        String exprType;
        if (isType(expr))
            exprType = expr;
        else if (symTable.classExists(expr))
            exprType = expr;
        else if (expr.equals("this"))
            exprType = symTable.getCurrClassId();
        else
            exprType = symTable.lookup(expr);

        if (exprType == null)
            throw new Exception("Error at line " + n.f0.beginLine + ": " + expr + " has not been declared");

        String argCorrType = symTable.getChkMethNextArgType();

        if (!exprType.equals(argCorrType) && !symTable.isSubclassOf(exprType, argCorrType))
            throw new Exception("Error at line " + n.f0.beginLine + ": Argument should be of type " + argCorrType +
                    " or a subclass of it");

        return null;
    }

    /**
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */
    public String visit(Clause n, SymbolTable symTable) throws Exception {
        return n.f0.accept(this, symTable);
    }

    /**
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | BracketExpression()
     */
    public String visit(PrimaryExpression n, SymbolTable symTable) throws Exception {
        return n.f0.accept(this, symTable);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, SymbolTable symTable) throws Exception {
        return "int";
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n, SymbolTable symTable) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, SymbolTable symTable) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, SymbolTable symTable) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, SymbolTable symTable) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(ArrayAllocationExpression n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);
        //n.f1.accept(this, symTable);
        //n.f2.accept(this, symTable);
        String retExp = n.f3.accept(this, symTable);
        String retType;
        if (isType(retExp))
            retType = retExp;
        else
            retType = symTable.lookup(retExp);

        if (retType == null)
            throw new Exception("Error at line " + n.f4.beginLine + ": " + retExp + " has not been declared");
        else if (!retType.equals("int"))
            throw new Exception("Error at line " + n.f4.beginLine +
                    ": Expression inside an array allocation should be an integer");

        //n.f4.accept(this, symTable);
        return "int[]";
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);
        String classId = n.f1.accept(this, symTable);
        if(!symTable.classExists(classId))
            throw new Exception("Error at line " + n.f0.beginLine + ": Class named " + classId + "has not been declared");
        else
            //n.f2.accept(this, symTable);
            //n.f3.accept(this, symTable);
            return classId;
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);
        String retClause = n.f1.accept(this, symTable);
        String retClauseType;
        if (retClause.equals("boolean"))
            retClauseType = retClause;
        else
            retClauseType = symTable.lookup(retClause);

        if (!retClauseType.equals("boolean"))
            throw new Exception("Error at line " + n.f0.beginLine +
                    ": Not (!) should be followed by an expression of type boolean");
        else
            return retClauseType;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);
        //n.f2.accept(this, symTable);
        return n.f1.accept(this, symTable);
    }

    private boolean isType(String chk) {
        if (chk.equals("int") || chk.equals("int[]") || chk.equals("boolean"))
            return true;
        else
            return false;
    }
}