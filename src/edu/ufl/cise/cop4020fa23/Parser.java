/*Copyright 2023 by Beverly A Sanders
 *
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the
 * University of Florida during the fall semester 2023 as part of the course project.
 *
 * No other use is authorized.
 *
 * This code may not be posted on a public web site either during or after the course.
 */
package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;

import static edu.ufl.cise.cop4020fa23.Kind.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser implements IParser {
	final ILexer lexer;
	private IToken t;
	private int listPos = 0;
	/**
	 * @param lexer
	 * @throws LexicalException
	 */

	public Parser(ILexer lexer) throws LexicalException {
		super();
		this.lexer = lexer;
		t = lexer.next();
	}

	@Override
	public AST parse() throws PLCCompilerException {
		AST e = program();

		t = lexer.next();
		if (!isKind(EOF)) {
			throw new SyntaxException("Not end of");
		}

		return e;
	}

	private AST program() throws PLCCompilerException {
		IToken first = t;
		IToken type;

		type = type();
		t = lexer.next();

		if (!isKind(IDENT)) {
			throw new SyntaxException("Token not ident");
		}
		IToken name = t;
		t = lexer.next();

		if (!isKind(LPAREN)) {
			throw new SyntaxException("Token not lparen");
		}
		t= lexer.next();

		List<NameDef> params = new ArrayList<>();
		if (!isKind(RPAREN)) {
			params = paramList();

			if (!isKind(RPAREN)) {
				throw new SyntaxException("Token not rparen");
			}
			t= lexer.next();
		}
		else {
			t = lexer.next();
		}

		Block block = block();

		return new Program(first, type, name, params, block);
	}


	private Block block() throws PLCCompilerException {
		IToken first = t;
		List<Block.BlockElem> blockList = new ArrayList<>();
		Block.BlockElem b = null;

		if (isKind(BLOCK_OPEN)) {
			t = lexer.next();
			while(!isKind(BLOCK_CLOSE)) {
				// either declaration or statement
				if (isKind(RES_image,RES_pixel,RES_int,RES_string,RES_void,RES_boolean)) {
					b = declaration();

					if (isKind(SEMI)) {
						blockList.add(b);
						t = lexer.next();
					}
					else {
						throw new SyntaxException("Not valid block");
					}
				}
				else {
					b = statement();

					if (isKind(SEMI)) {
						blockList.add(b);
						t = lexer.next();
					}
					else {
						throw new SyntaxException("Not valid block");
					}
				}
			}
		}
		return new Block(t, blockList);
	}

	private List<NameDef> paramList() throws PLCCompilerException {
		IToken first = t;
		List<NameDef> nList = new ArrayList<>();

		if (isKind(RES_image,RES_pixel,RES_int,RES_string,RES_void,RES_boolean)) {
			NameDef n = nameDef();
			nList.add(n);
		}
		else {
			return null;
		}
		t = lexer.next();

		while (isKind(COMMA)) {
			t = lexer.next();
			nList.add(nameDef());
			t = lexer.next();
		}

		return nList;
	}

	private NameDef nameDef() throws PLCCompilerException {
		IToken first = t;

		IToken type = type();
		t = lexer.next(); // MIGHT WANT TO GET RID OF THIS
		Dimension d = null;

		if (isKind(LSQUARE)) {
			d = dimension();
			if (!isKind(RSQUARE)) {
				throw new SyntaxException("Not valid NameDef");
			}
			t = lexer.next();
		}

		if (isKind(IDENT)) {
			return new NameDef(first, type, d, t);
		}

		throw new SyntaxException("Not valid NameDef");
	}

	private IToken type() throws PLCCompilerException {

		if (isKind(RES_image, RES_pixel, RES_int, RES_string, RES_void, RES_boolean)) {
			return t;
		}

		throw new SyntaxException("Not valid type");
	}

	private Declaration declaration() throws PLCCompilerException {
		IToken first = t;
		NameDef n = nameDef();
		Expr e = null;

		t = lexer.next();

		if (isKind(ASSIGN)) {
			t = lexer.next();
			e = expr();
		}
		return new Declaration(t, n, e);
	}

	private Dimension dimension() throws PLCCompilerException {
		IToken first = t;

		if (isKind(LSQUARE)) {
			t = lexer.next();
			Expr left = expr();
			if (isKind(COMMA)) {
				t = lexer.next();
				Expr right = expr();
				if (isKind(RSQUARE)) {
					return new Dimension(first, left, right);
				}
			}
		}
		throw new SyntaxException("Not valid dimension");
	}

	private LValue lVal() throws PLCCompilerException {
		IToken first = t;

		if (!isKind(IDENT)) {
			throw new SyntaxException("Not valid LValue");
		}

		IToken ident = t;
		PixelSelector ps = null;
		ChannelSelector cs = null;
		t = lexer.next();

		if (isKind(LSQUARE)) {
			ps = pxSel();
		}

		if (isKind(COLON)) {
			cs = chSel();
		}

		return new LValue(first, ident, ps, cs);
	}

	private Statement statement() throws PLCCompilerException {
		IToken first = t;

		if (isKind(RES_write)) {
			t = lexer.next();
			Expr e = expr();
			return new WriteStatement(first, e);
		}
		else if (isKind(RES_do)) {
			t = lexer.next();
			List<GuardedBlock> gList = new ArrayList<>();

			gList.add(gBlock());

			t = lexer.next();
			if (!isKind(BOX)) {
				throw new SyntaxException("Not Box");
			}

			t = lexer.next();

			while (!isKind(RES_od)) {
				gList.add(gBlock());
				t = lexer.next();
			}
			if (isKind(RES_od)) {
				t = lexer.next();
			}

			return new DoStatement(t,gList);
		}
		else if (isKind(RES_if)) {
			t = lexer.next();
			List<GuardedBlock> gList = new ArrayList<>();
			gList.add(gBlock());

			t = lexer.next();
			if (!isKind(BOX)) {
				throw new SyntaxException("Not Box");
			}

			t = lexer.next();

			while (!isKind(RES_fi)) {
				gList.add(gBlock());
				t = lexer.next();
			}
			if (isKind(RES_fi)) {
				t = lexer.next();
			}
			return new IfStatement(t, gList);
		}
		else if (isKind(RETURN)) {
			t = lexer.next();
			Expr e = expr();
			return new ReturnStatement(first, e);
		}
		else if (isKind(IDENT)) {
			LValue lv = lVal();
			Expr e = null;

			if (isKind(ASSIGN)) {
				t = lexer.next();
				e = expr();
			}

			return new AssignmentStatement(first, lv, e);
		}
		else if (isKind(BLOCK_OPEN)) {
			Block b = bStatement();
			t = lexer.next();
			return new StatementBlock(first, block());
		}
		throw new SyntaxException("Not valid statement");
	}

	private GuardedBlock gBlock() throws PLCCompilerException {
		IToken first = t;
		Expr guard = expr();

		if (!isKind(RARROW)) {
			throw new SyntaxException("not ->");
		}

		t = lexer.next();

		if (!isKind(BLOCK_OPEN)) {
			throw new SyntaxException("Guarded Block");
		}
		Block block = block();

		return new GuardedBlock(first, guard, block);
	}

	private Block bStatement() throws PLCCompilerException {
		IToken first = t;
		return block();

//     List<Block.BlockElem> elems = null;
//     try {
//        for (int i = 0; i < block().getElems().size(); i++) {
//           elems.add(block().getElems().get(i));
//        }
//     }
//     catch (PLCCompilerException e) {
//        throw new SyntaxException("Guarded Block");
//     }
//     return new Block(first, elems);
	}

	public AST exprParse() throws PLCCompilerException {
		Expr e = expr();
		return e;
	}

	private Expr expr() throws PLCCompilerException {

		if (isKind(RES_if, QUESTION)) {
			return condExpr();
		}
		else {
			return logOrExpr();
		}
	}

	ConditionalExpr condExpr() throws PLCCompilerException {
		IToken first = t;

		if (!isKind(QUESTION)) {
			throw new SyntaxException("Token not ?");
		}

		t = lexer.next();
		Expr a = expr();

		if (!isKind(RARROW)) {
			throw new SyntaxException("Token not ->");
		}

		t = lexer.next();
		Expr b = expr();

		if (!isKind(COMMA)) {
			throw new SyntaxException("Token not ,");
		}

		t = lexer.next();
		Expr c = expr();

		return new ConditionalExpr(first, a, b, c);
	}

	Expr logOrExpr() throws PLCCompilerException {
		IToken first = t;
		Expr left = logAndExpr();

		while (isKind(OR, BITOR)) {
			IToken op = t;
			t = lexer.next();
			Expr right = logAndExpr();
			left = new BinaryExpr(first, left, op, right);
		}

		return left;
	}

	Expr logAndExpr() throws PLCCompilerException {
		IToken first = t;
		Expr left = cmpExpr();

		while (isKind(AND, BITAND)) {
			IToken op = t;
			t = lexer.next();
			Expr right = cmpExpr();
			left = new BinaryExpr(first, left, op, right);
		}

		return left;
	}

	Expr cmpExpr() throws PLCCompilerException {
		Expr left = powExpr();

		while (isKind(GT, LT, GE, LE, EQ)) {
			IToken op = t;
			t = lexer.next();
			Expr right = powExpr();
			left = new BinaryExpr(left.firstToken, left, op, right);
		}

		return left;
	}

	Expr powExpr() throws PLCCompilerException {
		Expr add = addExpr();

		if (isKind(EXP)) {
			IToken op = t;
			t = lexer.next();
			Expr right = powExpr();
			add = new BinaryExpr(op, add, op, right);
		}

		return add;
	}

	Expr addExpr() throws PLCCompilerException {
		Expr left = multExpr();

		while (isKind(PLUS, MINUS)) {
			// something --> op
			IToken op = t;
			t = lexer.next();
			Expr right = multExpr();
			left = new BinaryExpr(left.firstToken, left, op, right);
		}

		return left;
	}

	Expr multExpr() throws PLCCompilerException {
		Expr left = uExpr();

		while (isKind(TIMES, DIV, MOD)) {
			// save curr token as op to pass into Binary w/ right
			IToken op = t; //token at curr position
			t = lexer.next();
			Expr right = uExpr();
			left = new BinaryExpr(left.firstToken, left, op, right);
		}

		return left;
	}

	Expr uExpr() throws PLCCompilerException {
		IToken left = t;


		if (isKind(BANG, MINUS, RES_width, RES_height)) {
			IToken op = t;
			t = lexer.next();
			Expr b = uExpr();
			return new UnaryExpr(left, op, b);
		}

		return poFixExpr();
	}

	Expr poFixExpr() throws PLCCompilerException {
		IToken firstT = t;
		Expr a = priExpr();
		PixelSelector b = null;
		ChannelSelector c = null;
		t = lexer.next();

		if (isKind(LSQUARE)) {
			b = pxSel();
		}

		if (isKind(COLON)) {
			c = chSel();
		}

		if (b == null && c == null) {
			return a;
		}
		return new PostfixExpr(firstT, a, b, c);
	}

	Expr priExpr() throws PLCCompilerException {
		switch (t.kind()) {
			case STRING_LIT -> {
				return new StringLitExpr(t);
			}
			case NUM_LIT -> {
				return new NumLitExpr(t);
			}
			case BOOLEAN_LIT -> {
				return new BooleanLitExpr(t);
			}
			case IDENT -> {
				return new IdentExpr(t);
			}
			case LPAREN -> {
				t = lexer.next();
				Expr check = expr();
				if (isKind(RPAREN)) {
					return check;
				} else {
					throw new SyntaxException("Missing right paren");
				}
			}
			case CONST -> {
				return new ConstExpr(t);
			}
			case LSQUARE -> {
				return exPxExpr();
			}
			default -> {
				throw new SyntaxException("Primary Expression Error");
			}
		}
	}

	ChannelSelector chSel() throws PLCCompilerException {
		IToken first = t, color = null;

		if (isKind(COLON)) {
			t = lexer.next();
			if (isKind(RES_red, RES_green, RES_blue)) {
				color = t;
				t = lexer.next();
				return new ChannelSelector(first,color);
			}
		}

		throw new SyntaxException("Not valid channel selector");
	}


	PixelSelector pxSel() throws PLCCompilerException {
		IToken first = t;

		if (isKind(LSQUARE)) {
			t = lexer.next();
			Expr a = expr();
			if (isKind(COMMA)) {
				t = lexer.next();
				Expr b = expr();
				if (isKind(RSQUARE)) {
					t = lexer.next();
					return new PixelSelector(first, a, b);
				}
			}
		}

		throw new SyntaxException("Not valid pixel selector");
	}

	ExpandedPixelExpr exPxExpr() throws PLCCompilerException {
		IToken first = t;
		if (isKind(LSQUARE)) {
			t = lexer.next();
			Expr a = expr();
			if (isKind(COMMA)) {
				t = lexer.next();
				Expr b = expr();
				if (isKind(COMMA)) {
					t = lexer.next();
					Expr c = expr();
					if (isKind(RSQUARE)) {
						return new ExpandedPixelExpr(first, a, b, c);
					}
				}
			}
		}

		throw new SyntaxException("Not valid expanded pixel expr");
	}

	private boolean isKind(Kind kind) {
		return t.kind() == kind;
	}
	private boolean isKind(Kind... kinds) {
		for (Kind k : kinds) {
			if (k == t.kind())
				return true;
		}
		return false;
	}
}



