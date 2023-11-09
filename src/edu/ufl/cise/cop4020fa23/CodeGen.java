package edu.ufl.cise.cop4020fa23;


import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.runtime.FileURLIO;
import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class CodeGen implements ASTVisitor {
    StringBuilder result = new StringBuilder();
    String code = "";
    public Set<String> imports = new HashSet<>();
    SymbolTable symblTable = new SymbolTable();
    public CodeGen(String x) {
        code = x;
    }

    public String convertType(Type type) {
        if (type == Type.INT) {
            return "int";
        }
        if (type == Type.STRING) {
            return "String";
        }
        if (type == Type.VOID) {
            return "void";
        }
        if (type == Type.BOOLEAN) {
            return "boolean";
        } else {
            return null; //or just throw exception?
        }
    }


    public String convertOp(Kind kind) {
        switch (kind) {
            case PLUS -> {
                return "+";
            }
            case MINUS -> {
                return "-";
            }
            case BANG -> {
                return "!";
            }
            case DIV -> {
                return "/";
            }
            case MOD -> {
                return "%";
            }
            case LT -> {
                return "<";
            }
            case GT -> {
                return ">";
            }
            case LE -> {
                return "<=";
            }
            case ASSIGN ->  {
                return "=";
            }
            case GE-> {
                return ">=";
            }
            case OR -> {
                return "||";
            }
            case AND -> {
                return "&&";
            }
            case BITOR -> {
                return "|";
            }
            case BITAND -> {
                return "&";
            }
            case TIMES -> {
                return "*";
            }
        }
        return "";
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        String ident = program.getName();
        Type t = program.getType();
        symblTable.enterScope();
        result.append("package edu.ufl.cise.cop4020fa23;\n");
        temp.append("public class ");temp.append(ident);
        temp.append(" {\npublic static ");
        temp.append(convertType(t));
        temp.append(" apply(");
        for (int i = 0; i < program.getParams().size(); i++) {
            temp.append(program.getParams().get(i).visit(this, arg).toString());

            if (i != program.getParams().size() - 1) {
                temp.append(",");
            }
        }
        temp.append(") ");
        temp.append(program.getBlock().visit(this, arg).toString());
        temp.append("}\n");
        //result.append("import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;\n");



        for (String i : imports) {
            result.append(i);
        }
        result.append(temp);



        symblTable.leaveScope();
        return result.toString();
    }


    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        symblTable.enterScope();
        StringBuilder temp = new StringBuilder();
        temp.append("{");
        for (int i = 0; i < block.getElems().size(); i++) {
            temp.append(block.getElems().get(i).visit(this, arg).toString());
            temp.append(";");
            temp.append("\n");
        }
        temp.append("}");
        symblTable.leaveScope();
        return temp.toString();
    }


    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        temp.append(convertType(nameDef.getType()));
        temp.append(" ");

        String name = nameDef.getName();
        //make sure that if redefining in same space, error is thrown
        //if

        symblTable.insert(name, nameDef);
        temp.append(name);
        temp.append("$");
        temp.append(symblTable.getScope(name));

        return temp;
    }


    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        symblTable.enterScope();
        StringBuilder temp = new StringBuilder();
        temp.append(statementBlock.getBlock().visit(this, arg).toString());
        symblTable.leaveScope();
        return temp.toString();
    }


    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        symblTable.enterScope();
        StringBuilder temp = new StringBuilder();
        temp.append(assignmentStatement.getlValue().visit(this, arg).toString());
        temp.append("=");
        temp.append(assignmentStatement.getE().visit(this, arg).toString());
        symblTable.leaveScope();
        return temp.toString();
    }


    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        Type lType = binaryExpr.getLeftExpr().getType();
        Kind op = binaryExpr.getOpKind();


        if (lType == Type.STRING && op == Kind.EQ) {
            temp.append(binaryExpr.getLeftExpr().visit(this, arg).toString());
            temp.append(".equals(");
            temp.append(binaryExpr.getRightExpr().visit(this, arg).toString());
            temp.append(")");
//            result.append(";\n");
        }
        else if (op == Kind.EXP) {
            temp.append("((int)Math.round(Math.pow(");
            temp.append(binaryExpr.getLeftExpr().visit(this, arg).toString());
            temp.append(",");
            temp.append(binaryExpr.getRightExpr().visit(this, arg).toString());
            temp.append(")))");
        }
        else {
            temp.append("(");
            temp.append(binaryExpr.getLeftExpr().visit(this, arg).toString());
            temp.append(convertOp(op));
            temp.append(binaryExpr.getRightExpr().visit(this, arg).toString());
            temp.append(")");
        }


        return temp.toString();
    }


    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();


        temp.append("(");
        temp.append((conditionalExpr.getGuardExpr().visit(this, arg)).toString()); //do we need to visit first
        temp.append("?");
        temp.append((conditionalExpr.getTrueExpr().visit(this, arg)).toString());
        temp.append(":");
        temp.append((conditionalExpr.getFalseExpr().visit(this, arg)).toString());
        temp.append(")");


        return temp.toString();
    }


    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {

        Expr expr = declaration.getInitializer();
        StringBuilder temp = new StringBuilder();
        String nameDef$ = declaration.getNameDef().visit(this, arg).toString();

        if (expr != null) {
            temp.append(nameDef$);
            temp.append("=");
            temp.append(expr.visit(this, arg).toString());
        }
        else {
            temp.append(nameDef$);
        }


        return temp.toString();
    }


    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        String name = identExpr.getNameDef().getName();
        temp.append(name);
        temp.append("$");
        temp.append(symblTable.getScope(name));

        return temp.toString();
    }


    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        String name = lValue.getNameDef().getName();
        temp.append(name);
        temp.append("$");
        temp.append(symblTable.getScope(name));

        return temp.toString();
    }


    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        temp.append(numLitExpr.getText().toString());
        return temp.toString();
    }


    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        temp.append("return ");
        temp.append(returnStatement.getE().visit(this, arg).toString());
        return temp.toString();
    }


    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        temp.append(stringLitExpr.getText().toString());
        return temp.toString();
    }


    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        temp.append("(");
        temp.append(convertOp(unaryExpr.getOp()));
        temp.append(unaryExpr.getExpr().visit(this, arg).toString());
        temp.append(")");
        return temp.toString();
    }


    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        imports.add("import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;\n");
        //System.out.println("import added");
        //result.append("import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;\n");
        temp.append("ConsoleIO.write(");
        temp.append(writeStatement.getExpr().visit(this, arg).toString());
        temp.append(")");

        return temp.toString();
    }


    @Override //major uncertainty
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        temp.append(booleanLitExpr.toString()); //v unsure about this
        return temp.toString();
    }
























    //FOR LATER
    //-------------


    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        throw new UnsupportedOperationException();
    }


    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {


        throw new UnsupportedOperationException();
    }


    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        throw new UnsupportedOperationException();
    }


    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        throw new UnsupportedOperationException();
    }


    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        throw new UnsupportedOperationException();
    }


    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        throw new UnsupportedOperationException();
    }


    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        throw new UnsupportedOperationException();
    }


    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        throw new UnsupportedOperationException();
    }


    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        throw new UnsupportedOperationException();
    }
}

