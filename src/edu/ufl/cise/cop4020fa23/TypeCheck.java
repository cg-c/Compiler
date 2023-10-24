package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
import java.util.EnumSet;
import java.util.List;

import static edu.ufl.cise.cop4020fa23.Kind.*;

public class TypeCheck implements ASTVisitor {

    // Symbol Table Stuff
    SymbolTable symblTbl = new SymbolTable();

    /* Scans, parses, and type checks input. Returns normally if no
    errors. */
    AST typeCheck (String input) throws PLCCompilerException {
        AST ast = ComponentFactory.makeParser(input).parse();
        ASTVisitor typeChecker = ComponentFactory.makeTypeChecker();
        ast.visit(typeChecker, null);
        return ast;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        Type left = binaryExpr.getLeftExpr().getType();
        Type right = binaryExpr.getRightExpr().getType();
        Kind opKind = binaryExpr.getOpKind();
        Type inferBiType = null;

        switch(opKind) {
            case AND, OR -> {
                if (left == Type.BOOLEAN && right == Type.BOOLEAN) {
                    inferBiType = Type.BOOLEAN;
                }
            }
            case LT, GT, LE, GE -> {
                if (left == Type.INT && right == Type.INT) {
                    inferBiType = Type.BOOLEAN;
                }
            }
            case EQ -> {
                if (right == left) {
                    inferBiType = Type.BOOLEAN;
                }
            }
            case  BITAND, BITOR -> {
                if (left == Type.PIXEL && right == Type.PIXEL) {
                    inferBiType = Type.PIXEL;
                }
            }
            case EXP -> {
                if (left == Type.INT && right == Type.INT) {
                    inferBiType = Type.INT;
                }
                else if (left == Type.PIXEL && right == Type.INT) {
                    inferBiType = Type.PIXEL;
                }
            }
            case PLUS -> {
                if (left == right) {
                    inferBiType = left;
                }
            }
            case MINUS -> {
                if ((left == Type.INT || left == Type.PIXEL || left == Type.IMAGE) && right == left) {
                    inferBiType = left;
                }
            }
            case TIMES, DIV, MOD -> {
                if ((left == Type.INT || left == Type.PIXEL || left == Type.IMAGE) && right == left) {
                    inferBiType = left;
                }
                else if ((left == Type.PIXEL || left == Type.IMAGE) && right == Type.INT) {
                    inferBiType = Type.INT;
                }
            }
        }

        if (inferBiType == null) {
            throw new TypeCheckException("Binary Null");
        }
        binaryExpr.setType(inferBiType);
        return inferBiType;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        symblTbl.enterScope();
        List<Block.BlockElem> bEle = block.getElems();

        for (Block.BlockElem ele : bEle) {
            ele.visit(this, arg);
        }

        symblTbl.leaveScope();
        return block;
    }

    @Override //S
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        symblTbl.enterScope();
        symblTbl.enterScope();
        Type left = (Type) visitBlock(statementBlock.getBlock(), arg);
        symblTbl.leaveScope();
        return null;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        Type tGuard = conditionalExpr.getGuardExpr().getType();
        Type tTrue = conditionalExpr.getTrueExpr().getType();
        Type tFalse = conditionalExpr.getFalseExpr().getType();

        if (tGuard == Type.BOOLEAN && tTrue == tFalse) {
            conditionalExpr.setType(tTrue);
        }

        return conditionalExpr;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        Object e = declaration.visit(this, arg);
        Type exprType = (Type) e;
        Type tNameDef = declaration.getNameDef().getType();

        if ((e == null) || (exprType == tNameDef) || (exprType == Type.STRING && tNameDef == Type.IMAGE)) {
            // type to tNameDef
        }

        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        Type tWidth = (Type) dimension.getWidth().visit(this, arg);
        if (tWidth != Type.INT) {
            throw new TypeCheckException("Width not a int");
        }
        Type tHeight = (Type) dimension.getHeight().visit(this, arg);
        if (tHeight != Type.INT) {
            throw new TypeCheckException("Height not a int");
        }

        return dimension;
    }

    @Override // S
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        for (int i = 0; i < doStatement.getGuardedBlocks().size(); i++) {
            Type goTo = (Type) visitBlock(doStatement.getGuardedBlocks().get(i).getBlock(), arg);
        }
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        Type tRed = expandedPixelExpr.getRed().getType();
        if (tRed != Type.INT) {
            throw new TypeCheckException("Red not a int");
        }
        Type tGreen = expandedPixelExpr.getGreen().getType();
        if (tGreen != Type.INT) {
            throw new TypeCheckException("Green not a int");
        }
        Type tBlue = expandedPixelExpr.getBlue().getType();
        if (tBlue != Type.INT) {
            throw new TypeCheckException("Blue not a int");
        }
        expandedPixelExpr.setType(Type.PIXEL);
        return expandedPixelExpr;
    }

    @Override //S - needs visitExpr
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        if (guardedBlock.getGuard().getType() != Type.BOOLEAN) {
            throw new PLCCompilerException("no boolean present in guarded block");
        }
        //Type left = (Type) visitExpr(guardedBlock.getGuard(), arg);
        Type right = (Type) visitBlock(guardedBlock.getBlock(), arg);
        return null;
    }

    @Override //S
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        if (symblTbl.lookup(identExpr.getName()) == null) {
            throw new PLCCompilerException("name not present in symbol table");
        }
        identExpr.setNameDef(symblTbl.lookup(identExpr.getName()));
        identExpr.setType(identExpr.getNameDef().getType());
        return null;
    }

    @Override //S
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        for (int i = 0; i < ifStatement.getGuardedBlocks().size(); i++) {
            Type goTo = (Type) visitBlock(ifStatement.getGuardedBlocks().get(i).getBlock(), arg);
        }
        return null;
    }

    @Override //S
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        symblTbl.enterScope();
        Type lVal = (Type) lValue.getType();
        //
        //if not assignment compatible(lVal, x) throw error
        symblTbl.leaveScope();
        return null;
    }

    @Override //S
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        Type temp;
        if (nameDef.getDimension() != null) {
            temp = Type.IMAGE;
        } else {
            EnumSet<Type> set = EnumSet.allOf(Type.class);
            set.remove(Type.IMAGE);
            //temp = Type.INT;
        }
        //this function is difficult to understand based on slides

        return null;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        numLitExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        Type inferPoFix = null;
        Type psType = (Type) postfixExpr.pixel().visit(this, arg);
        Type csType = (Type) postfixExpr.channel().visit(this, arg);
        Type exprType = postfixExpr.getType();

        if (psType == null && csType == null) {
            inferPoFix = exprType;
        }
        else if (exprType == Type.IMAGE && psType != null && csType == null) {
            inferPoFix = Type.PIXEL;
        }
        else if (exprType == Type.IMAGE && psType != null && csType != null) {
            inferPoFix = Type.INT;
        }
        else if (exprType == Type.IMAGE && psType == null && csType != null) {
            inferPoFix = Type.IMAGE;
        }
        else if (exprType == Type.PIXEL && psType == null && csType != null) {
            inferPoFix = Type.INT;
        }

        if (inferPoFix == null) {
            throw new TypeCheckException("PostFix is NULL");
        }
        postfixExpr.setType(inferPoFix);
        return postfixExpr;
    }

    @Override //S
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        symblTbl.enterScope();
        //check children: NameDef and Block
        Type left = (Type) visitNameDef(program.getParams().get(0), arg); //is this going to work or do I need to traverse every parameter using for loop?
        Type right = (Type) visitBlock(program.getBlock(), arg);
        symblTbl.leaveScope();
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        Type exprType = returnStatement.getE().getType();

        //if (exprType != )
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        stringLitExpr.setType(Type.STRING);
        return Type.STRING;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {

        Type inferUnaryExpr = null;
        Kind opKind = unaryExpr.getOp();
        Type exprType = unaryExpr.getType();

        switch (opKind) {
            case BANG -> {
                if (exprType == Type.BOOLEAN) {
                    inferUnaryExpr = Type.BOOLEAN;
                }
            }
            case MINUS -> {
                if (exprType == Type.INT) {
                    inferUnaryExpr = Type.INT;
                }
            }
            case RES_width, RES_height -> {
                if (exprType == Type.IMAGE) {
                    inferUnaryExpr = Type.INT;
                }
            }
        }

        if (inferUnaryExpr == null) {
            throw new TypeCheckException("UnaryExpr is NULL");
        }

        unaryExpr.setType(inferUnaryExpr);
        return unaryExpr;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        writeStatement.getExpr().visit(this, arg);
        return writeStatement;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        booleanLitExpr.setType(Type.BOOLEAN);
        return Type.BOOLEAN;
    }


    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {

        if (constExpr.getName() == "Z") {
            constExpr.setType(Type.INT);
        }

        constExpr.setType(Type.PIXEL);
        return constExpr;
    }

    //do we need a "visitExpr"? This seems useful for stuff like guardedBlock and returnStatement that go to broad expressions
}
