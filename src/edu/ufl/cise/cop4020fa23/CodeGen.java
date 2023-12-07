package edu.ufl.cise.cop4020fa23;


import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.ast.Dimension;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.runtime.FileURLIO;
import edu.ufl.cise.cop4020fa23.runtime.ImageOps;
import edu.ufl.cise.cop4020fa23.runtime.PixelOps;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class CodeGen implements ASTVisitor {
    StringBuilder result = new StringBuilder();
    String code = "";
    public Set<String> imports;
    SymbolTable symblTable;

    public CodeGen(String x) {
        code = x;
        symblTable = new SymbolTable();
        imports = new HashSet<>();
        result = new StringBuilder();
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
        }
        if (type == Type.IMAGE) {
            return "BufferedImage";
        }
        if (type == Type.PIXEL) {
            return "int";
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
            case ASSIGN -> {
                return "=";
            }
            case GE -> {
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
            case EQ -> {
                return "==";
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
        temp.append("public class ");
        temp.append(ident);
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
        //symblTable.enterScope();
        StringBuilder temp = new StringBuilder();
        temp.append(statementBlock.getBlock().visit(this, arg).toString());
        //symblTable.leaveScope();
        return temp.toString();
    }


    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();

        if (assignmentStatement.getlValue().getType() == Type.IMAGE) {
            if (assignmentStatement.getlValue().getPixelSelector() == null && assignmentStatement.getlValue().getChannelSelector() == null) {
                imports.add("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n");
                if (assignmentStatement.getE().getType() == Type.IMAGE) {
                    temp.append("ImageOps.copyInto(");
                    temp.append(assignmentStatement.getE().visit(this, arg).toString());
                    temp.append(",");
                    temp.append(assignmentStatement.getlValue().visit(this, arg).toString());
                    temp.append(")");
                } else if (assignmentStatement.getE().getType() == Type.PIXEL) {
                    temp.append("ImageOps.setAllPixels(");
                    temp.append(assignmentStatement.getlValue().visit(this, arg).toString());
                    temp.append(",");
                    temp.append(assignmentStatement.getE().visit(this, arg).toString());
                    temp.append(")");
                } else if (assignmentStatement.getE().getType() == Type.STRING) {
                    imports.add("import edu.ufl.cise.cop4020fa23.runtime.FileURLIO;\n");
                    temp.append("ImageOps.copyInto(FileURLIO.readImage(");
                    temp.append(assignmentStatement.getE().visit(this, arg).toString());
                    temp.append("),");
                    temp.append(assignmentStatement.getlValue().visit(this, arg).toString());
                    temp.append(")");
                }
            } else if (assignmentStatement.getlValue().getChannelSelector() != null) {
                throw new UnsupportedOperationException("AssignmentStatement, cs not null");
            } else if (assignmentStatement.getlValue().getPixelSelector() != null && assignmentStatement.getlValue().getChannelSelector() == null) {
//                temp.append("SyntheticNameDef ");
//                temp.append(assignmentStatement.getlValue().getNameDef().getName());
//                temp.append(";");

                imports.add("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n");

//                String w = assignmentStatement.getlValue().getNameDef().getDimension().getWidth().visit(this, arg).toString();
//                String h = assignmentStatement.getlValue().getNameDef().getDimension().getHeight().visit(this, arg).toString();
                String[] strArr = assignmentStatement.getlValue().getNameDef().visit(this, arg).toString().split(" ");
                String name = strArr[1];
                String x = assignmentStatement.getlValue().getPixelSelector().xExpr().visit(this, arg).toString();
                String y = assignmentStatement.getlValue().getPixelSelector().yExpr().visit(this, arg).toString();

                if (!assignmentStatement.getlValue().getPixelSelector().xExpr().toString().contains("IdentExpr")) {
                    SyntheticNameDef xND = new SyntheticNameDef("x" + x);
                    String[] arr = xND.visit(this, arg).toString().split(" ");
                    x = arr[1];
                }
                if (!assignmentStatement.getlValue().getPixelSelector().yExpr().toString().contains("IdentExpr")) {
                    SyntheticNameDef yND = new SyntheticNameDef("y" + y);
                    String[] arr = yND.visit(this, arg).toString().split(" ");
                    y = arr[1];
                }
                boolean redeclaration = false;
                //make this based on $ location
                x = x.substring(0, 1);

                try {
                    symblTable.getScope(x);
                    redeclaration = true;
                }
                catch (PLCCompilerException e) {
                     redeclaration = false;
                }

                if (redeclaration) {
                    temp.append("for (");
                } else {
                    temp.append("for (int ");
                }

                temp.append(x);
                temp.append("=0; ");
                temp.append(x);
                temp.append("<");
                temp.append(name);
                temp.append(".getWidth(); ");
                temp.append(x);
                temp.append("++) {\n");

                temp.append("for (int ");
                temp.append(y);
                temp.append("=0;");
                temp.append(y);
                temp.append("<");
                temp.append(name);
                temp.append(".getHeight(); ");
                temp.append(y);
                temp.append("++) {\n");

                temp.append("ImageOps.setRGB(");
                temp.append(name);
                temp.append(",");
                temp.append(x);
                temp.append(",");
                temp.append(y);
                temp.append(",");
                temp.append(assignmentStatement.getE().visit(this, arg).toString());
                temp.append(");\n}\n}");

            }
        } else if (assignmentStatement.getlValue().getType() == Type.PIXEL && assignmentStatement.getlValue().getChannelSelector() != null) {
            imports.add("import edu.ufl.cise.cop4020fa23.runtime.PixelOps;\n");
            switch (assignmentStatement.getlValue().getChannelSelector().color()) {
                case RES_blue -> {
                    temp.append("PixelOps.setBlue(");
                }
                case RES_green -> {
                    temp.append("PixelOps.setGreen(");
                }
                case RES_red -> {
                    temp.append("PixelOps.setRed(");
                }
            }
            temp.append(assignmentStatement.getlValue().visit(this, arg).toString());
            temp.append(",");
            temp.append(assignmentStatement.getE().visit(this, arg).toString());
            temp.append(")");
        }
//        else if (assignmentStatement.getlValue().getType() == Type.PIXEL && assignmentStatement.getlValue().getChannelSelector() == null && assignmentStatement.getlValue().getPixelSelector() != null)  {
//            imports.add("import edu.ufl.cise.cop4020fa23.runtime.PixelOps;\n");
//
//            temp.append(assignmentStatement.getlValue().visit(this, arg).toString());
//            temp.append("=");
////            temp.append("PixelOps.pack(");
//            temp.append("(");
//            String exp = assignmentStatement.getE().visit(this, arg).toString();
//            temp.append(exp);
////            temp.append(",");
////            temp.append(exp);
////            temp.append(",");
////            temp.append(exp);
//            temp.append(")");
//        }
        else if (assignmentStatement.getlValue().getPixelSelector() != null) {
//            temp.append("SyntheticNameDef "); // IDK REALLY
//            temp.append(assignmentStatement.getlValue().getNameDef().getName());
//            temp.append(";\n");

            imports.add("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n");

            if (assignmentStatement.getlValue().getNameDef().getDimension() != null) {
                String w = assignmentStatement.getlValue().getNameDef().getDimension().getWidth().visit(this, arg).toString();
                String h = assignmentStatement.getlValue().getNameDef().getDimension().getHeight().visit(this, arg).toString();
            }
            String[] strArr = assignmentStatement.getlValue().getNameDef().visit(this, arg).toString().split(" ");
            String name = strArr[1];
            String x = assignmentStatement.getlValue().getPixelSelector().xExpr().visit(this, arg).toString();
            String y = assignmentStatement.getlValue().getPixelSelector().yExpr().visit(this, arg).toString();

            //check if x and/or y are a number, set variables to track these. Will be used further down on append. No for
            //loops in such a circumstance and wherever x or y are used, it will need to be a number
//            System.out.println("test" + x);
            int placementOfDollar = x.indexOf("$");
            boolean numPresentX = true;
            boolean numPresentY = true;
            String xStr = "";
            String yStr = "";
            if (placementOfDollar > 0) {
//                System.out.println("reachd");
                numPresentX = false;
            } else {
                xStr = x;
            }
            placementOfDollar = y.indexOf("$");
            if (placementOfDollar > 0) {
                numPresentY = false;
            } else {
                yStr = y;
            }

            if (!assignmentStatement.getlValue().getPixelSelector().xExpr().toString().contains("IdentExpr")) {
                SyntheticNameDef xND = new SyntheticNameDef("x" + x);
                String[] arr = xND.visit(this, arg).toString().split(" ");
                x = arr[1];


            }


            if (!assignmentStatement.getlValue().getPixelSelector().yExpr().toString().contains("IdentExpr")) {
                SyntheticNameDef yND = new SyntheticNameDef("y" + y);
                String[] arr = yND.visit(this, arg).toString().split(" ");
                y = arr[1];
            }


            if (!numPresentX) {
                Boolean appendInt = true;
                try {
                    int scope = symblTable.getScope(x.substring(0,x.indexOf('$')));
                    if (scope < symblTable.scopes.size() - 1) {
                        appendInt = false;
                    }
//                    x = x.substring(0, x.length() - 1);
                }
                catch (PLCCompilerException e) {
//                    temp.append("for (int ");
                }
//                temp.append("for (int "); //the "int" should not be included when x has already been declared
                if (appendInt) {
                    temp.append("for (int ");
                }
                else {
                    temp.append("for (");
                }

                temp.append(x);
                temp.append("=0; ");
                temp.append(x);
                temp.append("<");
                temp.append(name);
                temp.append(".getWidth(); ");
                temp.append(x);
                temp.append("++) {\n");
            } else {
                x = xStr;
            }

            if (!numPresentY) {
                Boolean appendInt = true;
                try {
                    int scope = symblTable.getScope(x.substring(0,y.indexOf('$')));
                    if (scope < symblTable.scopes.size() - 1) {
                        appendInt = false;
                    }
                }
                catch (PLCCompilerException e) {
//                    temp.append("for (int ");
                }
//                temp.append("for (int "); //the "int" should not be included when y has already been declared
                if (appendInt) {
                    temp.append("for (int ");
                }
                else {
                    temp.append("for (");
                }
                temp.append(y);
                temp.append("=0;");
                temp.append(y);
                temp.append("<");
                temp.append(name);
                temp.append(".getHeight(); ");
                temp.append(y);
                temp.append("++) {\n");
            } else {
                y = yStr;
            }

            temp.append("ImageOps.setRGB(");
            temp.append(name);
            temp.append(",");
            temp.append(x);
            temp.append(",");
            temp.append(y);
            temp.append(",");
            temp.append(assignmentStatement.getE().visit(this, arg).toString());
            temp.append(");");
            if (!numPresentX) {
                temp.append("\n}");
            }
            if (!numPresentY) {
                temp.append("\n}");
            }


        } else if (assignmentStatement.getlValue().getChannelSelector() != null) {
            imports.add("import edu.ufl.cise.cop4020fa23.runtime.PixelOps;\n");
            String lhs = assignmentStatement.getlValue().visit(this, arg).toString();
            temp.append(lhs);
            temp.append("=");
            temp.append("PixelOps.");
            switch (assignmentStatement.getlValue().getChannelSelector().color()) {
                case RES_red -> {
                    temp.append("setRed(");
                }
                case RES_green -> {
                    temp.append("setGreen(");
                }
                case RES_blue -> {
                    temp.append("setBlue(");
                }
            }
            temp.append(lhs);
            temp.append(",");
            temp.append(assignmentStatement.getE().visit(this, arg).toString());
            temp.append(")");
        }
        else {
            temp.append(assignmentStatement.getlValue().visit(this, arg).toString());

            // NEED TO VISIT IF THERE???
//            if (assignmentStatement.getlValue().getChannelSelector() != null) {
//                assignmentStatement.getlValue().getChannelSelector().visit(this, arg);
//            }
//            if (assignmentStatement.getlValue().getPixelSelector() != null) {
//                assignmentStatement.getlValue().getPixelSelector().visit(this, arg);
//            }

            temp.append("=");
            temp.append(assignmentStatement.getE().visit(this, arg).toString());
        }

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
        } else if (lType == Type.IMAGE) {
            imports.add("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n");
            Type rType = binaryExpr.getRightExpr().getType();
            switch (rType) {
                case IMAGE -> {
                    temp.append("(ImageOps.binaryImageImageOp(ImageOps.OP.");
                }
                case PIXEL -> {
                    temp.append("(ImageOps.binaryImagePixelOp(ImageOps.OP.");
                }
                case INT -> {
                    temp.append("(ImageOps.binaryImageScalarOp(ImageOps.OP.");
                }
            }
            temp.append(op);
            temp.append(",");
            temp.append(binaryExpr.getLeftExpr().visit(this, arg).toString());
            temp.append(",");
            temp.append(binaryExpr.getRightExpr().visit(this, arg).toString());
            temp.append("))");
        } else if (lType == Type.PIXEL) {

            if (op == Kind.DIV || op == Kind.TIMES || op == Kind.MINUS || op == Kind.PLUS) {

                imports.add("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n");
                Type rType = binaryExpr.getRightExpr().getType();
                switch (rType) {
                    case BOOLEAN -> {
                        temp.append("(ImageOps.binaryPackedPixelBooleanOp(ImageOps.BoolOP.");
                        if (op == Kind.EQ) {
                            temp.append("EQUALS");
                        }
                        else {
                            temp.append("NOT_EQUALS");
                        }
                    }
                    case PIXEL -> {
                        temp.append("(ImageOps.binaryPackedPixelPixelOp(ImageOps.OP.");
                        temp.append(op);
                    }
                    case INT -> {
                        if (op == Kind.TIMES || op == Kind.DIV) {
                            temp.append("(ImageOps.binaryPackedPixelIntOp(ImageOps.OP.");
                        } else {
                            temp.append("(ImageOps.binaryPackedPixelScalarOp(ImageOps.OP.");
                        }
                        temp.append(op);
                    }
                }
                temp.append(",");
                temp.append(binaryExpr.getLeftExpr().visit(this, arg).toString());
                temp.append(",");
                temp.append(binaryExpr.getRightExpr().visit(this, arg).toString());
                temp.append("))");
            }
            else {
                imports.add("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n");
                temp.append("(ImageOps.binaryPackedPixelBooleanOp(ImageOps.BoolOP.");

                if (op == Kind.EQ) {
                    temp.append("EQUALS");
                }
                else {
                    temp.append("NOT_EQUALS");
                }

                temp.append(",");
                temp.append(binaryExpr.getLeftExpr().visit(this, arg).toString());
                temp.append(",");
                temp.append(binaryExpr.getRightExpr().visit(this, arg).toString());
                temp.append("))");
            }
        } else if (op == Kind.EXP) {
            temp.append("((int)Math.round(Math.pow(");
            temp.append(binaryExpr.getLeftExpr().visit(this, arg).toString());
            temp.append(",");
            temp.append(binaryExpr.getRightExpr().visit(this, arg).toString());
            temp.append(")))");
        } else {
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
//        String nameDef$ = declaration.getNameDef().visit(this, arg).toString();

        if (expr != null) {
            if (declaration.getNameDef().getType() != Type.IMAGE) {
                String rhs = expr.visit(this, arg).toString();
                String nameDef$ = declaration.getNameDef().visit(this, arg).toString();
                temp.append(nameDef$);
                temp.append("=");
                temp.append(rhs);
            } else {
                if (expr.getType() == Type.STRING) {
                    String nameDef$ = declaration.getNameDef().visit(this, arg).toString();
                    imports.add("import java.awt.image.BufferedImage;\n");
                    imports.add("import edu.ufl.cise.cop4020fa23.runtime.FileURLIO;\n");
                    temp.append(nameDef$);
                    temp.append("=");
                    temp.append("FileURLIO.readImage(");
                    temp.append(expr.visit(this, arg).toString());
                    if (declaration.getNameDef().getDimension() != null) {
                        temp.append(",");
                        temp.append(declaration.getNameDef().getDimension().getWidth().visit(this, arg).toString());
                        temp.append(",");
                        temp.append(declaration.getNameDef().getDimension().getHeight().visit(this, arg).toString());
                    } 

                    temp.append(")");
                }
                else if (expr.getType() == Type.IMAGE) {
                    String nameDef$ = declaration.getNameDef().visit(this, arg).toString();
                    imports.add("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n");
                    if (declaration.getNameDef().getDimension() == null) {
                        temp.append(nameDef$);
                        temp.append("=");
                        temp.append("ImageOps.cloneImage(");
                        temp.append(expr.visit(this, arg).toString());
                        temp.append(")");
                    } else {
                        temp.append(nameDef$);
                        temp.append("=");
                        temp.append("ImageOps.copyAndResize(");
                        temp.append(expr.visit(this, arg).toString());
                        temp.append(",");
                        temp.append(declaration.getNameDef().getDimension().getWidth().visit(this, arg).toString());
                        temp.append(",");
                        temp.append(declaration.getNameDef().getDimension().getHeight().visit(this, arg).toString());
                        temp.append(")");
                    }
                }
            }
        } else {
            String nameDef$ = declaration.getNameDef().visit(this, arg).toString();

            if (declaration.getNameDef().getType() != Type.IMAGE) {
                temp.append(nameDef$);
            } else {
                if (declaration.getNameDef().getDimension() == null) {
                    throw new CodeGenException("no dimension in dec");
                }
//                int width = Integer.parseInt(declaration.getNameDef().getDimension().getWidth().visit(this, arg).toString());
//                int height = Integer.parseInt(declaration.getNameDef().getDimension().getHeight().visit(this, arg).toString());
//                BufferedImage img = ImageOps.makeImage(width, height);
                imports.add("import java.awt.image.BufferedImage;\n");
                imports.add("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n");

                temp.append("final ");
                temp.append(nameDef$);
                temp.append("=");
                temp.append("ImageOps.makeImage(");
                temp.append(declaration.getNameDef().getDimension().visit(this, arg));
                temp.append(")");
            }
        }

        return temp.toString();
    }


    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        String name = identExpr.getNameDef().getName();
        temp.append(name);
        temp.append("$");

        try {
            symblTable.getScope(name);
        }
        catch (PLCCompilerException e) {
            identExpr.getNameDef().visit(this, arg);
//            symblTable.insert(name, identExpr.getNameDef());
        }

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
        //System.out.println(temp);
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
        if (unaryExpr.getOp() == Kind.RES_height) {
            temp.append(unaryExpr.getExpr().visit(this, arg).toString());
            temp.append(".getHeight()");
        } else if (unaryExpr.getOp() == Kind.RES_width) {
            temp.append(unaryExpr.getExpr().visit(this, arg).toString());
            temp.append(".getWidth()");
        } else {
            temp.append(convertOp(unaryExpr.getOp()));
            temp.append(unaryExpr.getExpr().visit(this, arg).toString());
        }
        temp.append(")");

        return temp.toString();
    }


    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        imports.add("import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;\n");
        //System.out.println("import added");
        //result.append("import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;\n");
        if (writeStatement.getExpr().getType() == Type.PIXEL) {
            temp.append("ConsoleIO.writePixel(");
        } else {
            temp.append("ConsoleIO.write(");
        }
        temp.append(writeStatement.getExpr().visit(this, arg).toString());
        temp.append(")");

        return temp.toString();
    }


    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        temp.append(getBool(booleanLitExpr.toString()));
        return temp.toString();
    }

    private String getBool(String s) {
        if (s.contains("TRUE")) {
            return "true";
        }
        return "false";
    }


    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        List<GuardedBlock> guardedBlocks = ifStatement.getGuardedBlocks();
        for (int i = 0; i < guardedBlocks.size(); i++) {

            //guardedBlocks.get(i).getGuard().visit(this,arg).equals(true);
            //guardedBlocks.get(i).getGuard().visit(this,arg).toString();
            //System.out.println("here");
            if (i > 0) {
                temp.append("else ");
            }

            temp.append("if (");
            temp.append(guardedBlocks.get(i).getGuard().visit(this, arg).toString());
            temp.append(") \n");
            symblTable.enterScope();
            temp.append(guardedBlocks.get(i).getBlock().visit(this, arg).toString());
            temp.append("\n");
//            temp.append("}");
        }
        for (int i = 0; i < guardedBlocks.size(); i++) {

            symblTable.leaveScope();
        }
        return temp.toString();
    }


    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {

        StringBuilder temp = new StringBuilder();

        if (postfixExpr.primary().getType() == Type.PIXEL) {
            temp.append(postfixExpr.channel().visit(this, arg).toString());
            temp.append("(");
            temp.append(postfixExpr.primary().visit(this, arg).toString());
            temp.append(")");
        } else {
            imports.add("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n");
            if (postfixExpr.channel() != null && postfixExpr.pixel() == null) {
//                ImageOps.getRGB()
                temp.append("ImageOps.");
                switch (postfixExpr.channel().color()) {
                    case RES_blue -> {
                        temp.append("extractBlue(");
                    }
                    case RES_green -> {
                        temp.append("extractGreen(");
                    }
                    case RES_red -> {
                        temp.append("extractRed(");
                    }
                }
                temp.append(postfixExpr.primary().visit(this, arg));

                temp.append(")");
            } else if (postfixExpr.channel() != null && postfixExpr.pixel() != null) {
                temp.append(postfixExpr.channel().visit(this, arg));
                temp.append("(ImageOps.getRGB(");
                temp.append(postfixExpr.primary().visit(this, arg));
                temp.append(",");
                temp.append(postfixExpr.pixel().visit(this, arg));
                temp.append("))");
            } else if (postfixExpr.pixel() != null && postfixExpr.channel() == null) {
                temp.append("ImageOps.getRGB(");
                temp.append(postfixExpr.primary().visit(this, arg));
                temp.append(",");
                temp.append(postfixExpr.pixel().visit(this, arg));
                temp.append(")");
            }
        }

        return temp.toString();
    }


    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        temp.append(pixelSelector.xExpr().visit(this, arg).toString());
        temp.append(",");
        temp.append(pixelSelector.yExpr().visit(this, arg).toString());

        return temp.toString();
    }


    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        imports.add("import edu.ufl.cise.cop4020fa23.runtime.PixelOps;\n");
        temp.append("PixelOps.pack(");
        temp.append(expandedPixelExpr.getRed().visit(this, arg).toString());
        temp.append(",");
        temp.append(expandedPixelExpr.getGreen().visit(this, arg).toString());
        temp.append(",");
        temp.append(expandedPixelExpr.getBlue().visit(this, arg).toString());
        temp.append(")");

        return temp.toString();
    }


    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        //if (guardedBlock.getGuard())
        List<Block.BlockElem> ele = guardedBlock.getBlock().getElems();
//        guardedBlock.getGuard();
        for (int i = 0; i < ele.size(); i++) {
            if (ele.get(i).visit(this, arg).equals(true)) {
                temp.append(ele.get(i).visit(this, arg).toString());
            }
        }

        return temp.toString();
    }


    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        imports.add("import edu.ufl.cise.cop4020fa23.runtime.PixelOps;\n");

        switch (channelSelector.color()) {
            case RES_red -> {
                temp.append("PixelOps.red");
            }
            case RES_blue -> {
                temp.append("PixelOps.blue");
            }
            case RES_green -> {
                temp.append("PixelOps.green");
            }
        }

        return temp.toString();
    }


    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        temp.append(dimension.getWidth().visit(this, arg).toString());
        temp.append(",");
        temp.append(dimension.getHeight().visit(this, arg).toString());

        return temp.toString();
    }


    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        List<GuardedBlock> guardedBlocks = doStatement.getGuardedBlocks();
        int its = guardedBlocks.size();
        String cont = "continue";
        SyntheticNameDef contND = new SyntheticNameDef(cont);
        cont = contND.visit(this, arg).toString();
        String[] arr = cont.split(" ");
        cont = arr[1];

        if (its > 0) {
            temp.append("boolean ");
            temp.append(cont);
            temp.append("=false;\nwhile (!");
            temp.append(cont);
            temp.append(") {\n ");
            temp.append(cont);
            temp.append("=true;\n");
//            temp.append(its);
//            temp.append("; i++) {");

            for (int i = 0; i < guardedBlocks.size(); i++) {
                symblTable.enterScope();
                temp.append("if (");
                temp.append(guardedBlocks.get(i).getGuard().visit(this, arg).toString());
                temp.append(") {\n");
                temp.append(cont);
                temp.append("=false;\n");
                temp.append(guardedBlocks.get(i).getBlock().visit(this, arg).toString());
                temp.append("\n}");
                //temp.delete(temp.length()-2, temp.length()-1);
                //maybe need to get rid of the {s around the block?
                //temp.append("\nnumExecuted++;\n}\n");
                symblTable.leaveScope();
            }
            temp.append("}");
            //temp.append("if (numExecuted == 0) {\nbreak;\n}\n}");
        }

        return temp.toString();
    }


    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        StringBuilder temp = new StringBuilder();
        if (constExpr.getName().equals("Z")) {
            temp.append("255");
        } else {
            //not sure if constExpr is a "BLUE", "RED", or "GREEN"
            java.awt.Color constColor;
            try {
                java.lang.reflect.Field field = Class.forName("java.awt.Color").getField(constExpr.getName());
                constColor = (java.awt.Color) field.get(null);
            } catch (Exception e) {
                throw new PLCCompilerException("Not a color");
            }
            temp.append("0x");
            temp.append(Integer.toHexString(constColor.getRGB()));
        }
        return temp.toString();
    }
}


