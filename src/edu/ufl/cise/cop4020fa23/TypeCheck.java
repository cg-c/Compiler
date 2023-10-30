package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
import java.util.EnumSet;
import java.util.List;

import static edu.ufl.cise.cop4020fa23.Kind.*;

// test pool: https://docs.google.com/document/d/1eGeTAau1Dyy3GNaDFZcv3rEgesH9nf7E1XfNsHYZeZ8/edit

public class TypeCheck implements ASTVisitor {

    // Symbol Table Stuff
    SymbolTable symblTbl = new SymbolTable();
    Type programType = null;
    /* Scans, parses, and type checks input. Returns normally if no
    errors. */
    AST typeCheck (String input) throws PLCCompilerException {
        AST ast = ComponentFactory.makeParser(input).parse();
        ASTVisitor typeChecker = ComponentFactory.makeTypeChecker();
        ast.visit(typeChecker, null);
        return ast;
    }

    public Object visitExpr(Expr expr, Object arg) throws PLCCompilerException {

        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        symblTbl.print();
        symblTbl.enterScope();

        Boolean compatible = false;
        assignmentStatement.getE().visit(this, arg);
        Type exprType = assignmentStatement.getE().getType(); // maybe visit?
        assignmentStatement.getlValue().visit(this, arg);
        Type lValType = assignmentStatement.getlValue().getType();

        if (exprType == lValType) {
            compatible = true;
        }
        else if (lValType == Type.PIXEL && exprType == Type.INT) {
            compatible = true;
        }
        else if (lValType == Type.IMAGE && (exprType == Type.PIXEL || exprType == Type.INT || exprType == Type.STRING)) {
            compatible = true;
        }

        symblTbl.leaveScope();

        return compatible;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        symblTbl.print();
        binaryExpr.getLeftExpr().visit(this, arg);
        binaryExpr.getRightExpr().visit(this, arg);
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
        symblTbl.print();
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
        symblTbl.print();
        symblTbl.enterScope(); //was there a reason to add two
        statementBlock.equals(statementBlock.getBlock().visit(this, arg)); //nl
        symblTbl.leaveScope();
        return statementBlock;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        symblTbl.print();
        return channelSelector.color();
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        symblTbl.print();
        conditionalExpr.getGuardExpr().visit(this, arg);
        conditionalExpr.getTrueExpr().visit(this, arg);
        conditionalExpr.getFalseExpr().visit(this, arg);
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
        symblTbl.print();
        if (declaration.getInitializer() == null) {
            declaration.getNameDef().visit(this, arg);
            Type tNameDef = declaration.getNameDef().getType();
            declaration.getNameDef().setType(tNameDef);
            return declaration;
        }

        declaration.getInitializer().visit(this, arg);
        Type exprType = declaration.getInitializer().getType();
        declaration.getNameDef().visit(this, arg);
        Type tNameDef = declaration.getNameDef().getType();

        if ((exprType == tNameDef) || (exprType == Type.STRING && tNameDef == Type.IMAGE)) {
            declaration.getNameDef().setType(tNameDef);
        }
        // err

        return declaration;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        symblTbl.print();
        System.out.println("dimension reached");
        dimension.getWidth().visit(this, arg);
        dimension.getHeight().visit(this, arg);
        Type tWidth = dimension.getWidth().getType();
        if (tWidth != Type.INT) {
            throw new TypeCheckException("Width not a int");
        }
        Type tHeight = dimension.getHeight().getType();
        if (tHeight != Type.INT) {
            throw new TypeCheckException("Height not a int");
        }

        return dimension;
    }

    @Override // S
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        symblTbl.print();
        for (int i = 0; i < doStatement.getGuardedBlocks().size(); i++) {
            doStatement.getGuardedBlocks().get(i).getBlock().visit(this, arg);
//            Type goTo = (Type) visitBlock(doStatement.getGuardedBlocks().get(i).getBlock(), arg);
        }
        return doStatement;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        symblTbl.print();

        expandedPixelExpr.getRed().visit(this, arg);
        expandedPixelExpr.getGreen().visit(this, arg);
        expandedPixelExpr.getBlue().visit(this, arg);

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

    @Override //S
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        symblTbl.print();
        guardedBlock.getGuard().visit(this, arg);
        Type guardType = guardedBlock.getGuard().getType();
        if (guardType != Type.BOOLEAN) {
            throw new TypeCheckException("no boolean present in guarded block");
        }
        //Type exprType = (Type) visitExpr(guardedBlock.getGuard(), arg);
        Type blockType = (Type) visitBlock(guardedBlock.getBlock(), arg);
        return guardedBlock;
    }

    @Override //S
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        symblTbl.print();
        if (symblTbl.lookup(identExpr.getName()) == null) {
            throw new TypeCheckException("name not present in symbol table");
        }
        identExpr.setNameDef(symblTbl.lookup(identExpr.getName()));
        identExpr.getNameDef().visit(this, arg);
        identExpr.setType(identExpr.getNameDef().getType());
        return identExpr;
    }

    @Override //S
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        symblTbl.print();
        for (int i = 0; i < ifStatement.getGuardedBlocks().size(); i++) {
            ifStatement.getGuardedBlocks().get(i).getBlock().visit(this, arg);
//            Type goTo = (Type) visitBlock(ifStatement.getGuardedBlocks().get(i).getBlock(), arg);
        }
        return ifStatement;
    }

    @Override //S + C
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        symblTbl.print();
        NameDef lValND = symblTbl.lookup(lValue.getName());
        Type lValType = lValND.getType();
        lValue.setNameDef(lValND);
        lValue.setVarType(lValType);
//        lValue.getPixelSelector().visit(this, arg);
//        lValue.getChannelSelector().visit(this, arg);
        PixelSelector ps = lValue.getPixelSelector();
        ChannelSelector cs = lValue.getChannelSelector();
        Type inferLValType = null;

        if (ps == null && cs == null) {
            inferLValType = lValType;
        }
        else if (lValType == Type.IMAGE && ps != null && cs == null) {
            ps.visit(this, arg);
            inferLValType = Type.PIXEL;
        }
        else if (lValType == Type.IMAGE && ps != null && cs != null) {
            ps.visit(this, arg);
            inferLValType = Type.INT;
        }
        else if (lValType == Type.IMAGE && ps == null && cs == null) {
            inferLValType = Type.IMAGE;
        }
        else if (lValType == Type.PIXEL && ps == null && cs != null) {
            inferLValType = Type.INT;
        }

        if (inferLValType == null) {
            throw new TypeCheckException("Null lValue type");
        }

        lValue.setType(inferLValType);
        return lValue;
    }

    @Override //S
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
//        symblTbl.print();

        if (nameDef.getDimension() != null) {
            nameDef.getDimension().visit(this, arg);
            nameDef.setType(Type.IMAGE);
        }
        symblTbl.insert(nameDef.getName(), nameDef);
        symblTbl.print();
        return nameDef;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        symblTbl.print();
        numLitExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        System.out.println("visit pixel selector");
        pixelSelector.xExpr().visit(this, arg);
        pixelSelector.yExpr().visit(this, arg);
        Expr xExpr = pixelSelector.xExpr();
        Expr yExpr = pixelSelector.yExpr();
        if (true /*pixelselector parent is lvalue*/) {
            if (!(xExpr.getClass() == IdentExpr.class || xExpr.getClass() == NumLitExpr.class)) {
                throw new TypeCheckException("xExpr not identExpr or numLitExpr");
            }
            if (!(yExpr.getClass() == IdentExpr.class || yExpr.getClass() == NumLitExpr.class)) {
                throw new TypeCheckException("yExpr not identExpr or numLitExpr");
            }
            if (xExpr.getClass() == IdentExpr.class && symblTbl.lookup(((IdentExpr) xExpr).getName()) == null) {
                //symblTbl.enterScope();
                String xName = ((IdentExpr) xExpr).getName();
                SyntheticNameDef xNameDef = new SyntheticNameDef(xName);
                xNameDef.setType(Type.INT);
                symblTbl.insert(xName, xNameDef);
                symblTbl.print();
                //symblTbl.leaveScope();
            }
            if (yExpr.getClass() == IdentExpr.class && symblTbl.lookup(((IdentExpr) yExpr).getName()) == null) {
                //symblTbl.enterScope();
                String yName = ((IdentExpr) yExpr).getName();
                SyntheticNameDef yNameDef = new SyntheticNameDef(yName);
                yNameDef.setType(Type.INT);
                symblTbl.insert(yName, yNameDef);
                symblTbl.print();
                //symblTbl.leaveScope();
            }
        }
        if (xExpr.getType() != Type.INT) {
            throw new TypeCheckException("X expression type not int");
        }
        if (yExpr.getType() != Type.INT) {
            throw new TypeCheckException("Y expression type not int");
        }
        return pixelSelector;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        Type inferPoFix = null;
//        postfixExpr.pixel().visit(this, arg);
        PixelSelector psType = postfixExpr.pixel();
//        postfixExpr.channel().visit(this, arg);
        ChannelSelector csType = postfixExpr.channel();
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
        Type type = Type.kind2type(program.getTypeToken().kind());
        programType = type;
        program.setType(type);
        symblTbl.enterScope();
        List<NameDef> params = program.getParams();
        for (NameDef param : params) {
            param.visit(this, arg);
        }
        program.getBlock().visit(this, arg);
        symblTbl.leaveScope();
        return type;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        returnStatement.getE().visit(this, arg);
        Type exprType = returnStatement.getE().getType();
        if (programType != exprType) {
            throw new TypeCheckException("expression type not equal to program type");
        }
        return returnStatement;
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

        if (constExpr.getName().equals("Z")) {
            constExpr.setType(Type.INT);
            return constExpr;
        }

        constExpr.setType(Type.PIXEL);
        return constExpr;
    }

    //do we need a "visitExpr"? This seems useful for stuff like guardedBlock and returnStatement that go to broad expressions
}
