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

import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.Arrays;

import edu.ufl.cise.cop4020fa23.ast.AST;
import edu.ufl.cise.cop4020fa23.ast.BinaryExpr;
import edu.ufl.cise.cop4020fa23.ast.BooleanLitExpr;
import edu.ufl.cise.cop4020fa23.ast.ChannelSelector;
import edu.ufl.cise.cop4020fa23.ast.ConditionalExpr;
import edu.ufl.cise.cop4020fa23.ast.ConstExpr;
import edu.ufl.cise.cop4020fa23.ast.ExpandedPixelExpr;
import edu.ufl.cise.cop4020fa23.ast.Expr;
import edu.ufl.cise.cop4020fa23.ast.IdentExpr;
import edu.ufl.cise.cop4020fa23.ast.NumLitExpr;
import edu.ufl.cise.cop4020fa23.ast.PixelSelector;
import edu.ufl.cise.cop4020fa23.ast.PostfixExpr;
import edu.ufl.cise.cop4020fa23.ast.StringLitExpr;
import edu.ufl.cise.cop4020fa23.ast.UnaryExpr;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;

import static edu.ufl.cise.cop4020fa23.Kind.*;

/**
Expr::=  ConditionalExpr | LogicalOrExpr    
ConditionalExpr ::=  ?  Expr  :  Expr  :  Expr 
LogicalOrExpr ::= LogicalAndExpr (    (   |   |   ||   ) LogicalAndExpr)*
LogicalAndExpr ::=  ComparisonExpr ( (   &   |  &&   )  ComparisonExpr)*
ComparisonExpr ::= PowExpr ( (< | > | == | <= | >=) PowExpr)*
PowExpr ::= AdditiveExpr ** PowExpr |   AdditiveExpr
AdditiveExpr ::= MultiplicativeExpr ( ( + | -  ) MultiplicativeExpr )*
MultiplicativeExpr ::= UnaryExpr (( * |  /  |  % ) UnaryExpr)*
UnaryExpr ::=  ( ! | - | length | width) UnaryExpr  |  UnaryExprPostfix
UnaryExprPostfix::= PrimaryExpr (PixelSelector | ε ) (ChannelSelector | ε )
PrimaryExpr ::=STRING_LIT | NUM_LIT |  IDENT | ( Expr ) | Z 
    ExpandedPixel  
ChannelSelector ::= : red | : green | : blue
PixelSelector  ::= [ Expr , Expr ]
ExpandedPixel ::= [ Expr , Expr , Expr ]
Dimension  ::=  [ Expr , Expr ]                         

 */

public class ExpressionParser implements IParser {
	
	final ILexer lexer;
	private IToken t; // holds curr token
	private int listPos = 0;
	/**
	 * @param lexer
	 * @throws LexicalException 
	 */
	public ExpressionParser(ILexer lexer) throws LexicalException {
		super();
		this.lexer = lexer;
		IToken cur = lexer.next();
		t = cur;
	}

	@Override
	public AST parse() throws PLCCompilerException {
		// throw error for empty string or smt
		// while theres tokens/not eof --> add tokens to array

		//Expr e = expr(); // bc of this we have to change this, it only calls expr, nothing else
		Expr e = priExpr();
		return e;
	}

	private Expr expr() throws PLCCompilerException {
//		IToken firstT = t;

		if (isKind(Kind.RES_if)) {
			return condExpr();
		}
		else {
			return logOrExpr();
		}
	}

	ConditionalExpr condExpr() throws PLCCompilerException {

		IToken first = t; // or token b4 t??? I think its the one before t

		if (!isKind(QUESTION)) {
			throw new PLCCompilerException("Token not ?");
		}
		// next token
		Expr a = expr();

		if (!isKind(RARROW)) {
			throw new PLCCompilerException("Token not ->");
		}

		Expr b = expr();

		if (!isKind(COMMA)) {
			throw new PLCCompilerException("Token not ,");
		}

		Expr c = expr();

		return new ConditionalExpr(first, a, b, c);
	}

	Expr logOrExpr() throws PLCCompilerException {
		IToken first = t;
		Expr left = logAndExpr();

		while (isKind(OR, BITOR)) {
			// something --> op
			IToken op = t;
			Expr right = logAndExpr();
			left = new BinaryExpr(first, left, op, right);
		}

		return left;
	}

	Expr logAndExpr() throws PLCCompilerException {
		IToken first = t;
		Expr left = cmpExpr();

		while (isKind(AND, BITAND)) {
			// something --> op
			IToken op = t;
			Expr right = cmpExpr();
			left = new BinaryExpr(first, left, op, right);
		}

		return left;
	}

	Expr cmpExpr() throws PLCCompilerException {
		Expr left = powExpr();

		while (isKind(GT, LT, GE, LE, EQ)) {
			// something --> op
			IToken op = t;
			Expr right = powExpr();
			left = new BinaryExpr(left.firstToken, left, op, right);
		}

		return left;
	}

	Expr powExpr() throws PLCCompilerException {
		Expr add = addExpr();

		if (isKind(EXP)) {
			// save exp as op
			IToken op = t;
			Expr right = powExpr();
			add = new BinaryExpr(op, add, op, right); // might be wrong
		}

		return add;
	}

	Expr addExpr() throws PLCCompilerException {
		Expr left = multExpr();

		while (isKind(PLUS, MINUS)) {
			// something --> op
			IToken op = t;
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
			Expr right = uExpr();
			left = new BinaryExpr(left.firstToken, left, op, right);
		}

		return left;
		//return new BinaryExpr(first token, left, op, right);
	}

	Expr uExpr() throws PLCCompilerException {
		IToken left = t;

		if (!isKind(BANG, MINUS, RES_width, RES_height)) {
			IToken op = t;
			Expr b = uExpr();
			return new UnaryExpr(left, op, b);
		}

		return poFixExpr();
	}

	Expr poFixExpr() throws PLCCompilerException {
		Expr a = priExpr();
		IToken firstT = t;
		PixelSelector b;
		ChannelSelector c;
		try {
			b = pxSel();
		}
		catch (PLCCompilerException e) {
			b = null;
		}
		try {
			c = chSel();
		}
		catch (PLCCompilerException e) {
			c = null;
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
		IToken a = null, b = null;

		if (isKind(COLON)) {
			// next token -->
//			t = tokenz.get(listPos);
			if (isKind(RES_red, RES_green, RES_blue)) {
				return new ChannelSelector(a,b);
			}
		}

		throw new PLCCompilerException("Not valid channel selector");
	}

	PixelSelector pxSel() throws PLCCompilerException {
		IToken first = t;
		if (isKind(LSQUARE)) {
			Expr a = expr();
			if (isKind(COMMA)) {
				Expr b = expr();
				if (isKind(RSQUARE)) {
					return new PixelSelector(first, a, b);
				}
			}
		}

		throw new PLCCompilerException("Not valid pixel selector");
	}

	ExpandedPixelExpr exPxExpr() throws PLCCompilerException {
		IToken first = t;
		if (isKind(LSQUARE)) {
			Expr a = expr();
			if (isKind(COMMA)) {
				Expr b = expr();
				if (isKind(COMMA)) {
					Expr c = expr();
					if (isKind(RSQUARE)) {
						return new ExpandedPixelExpr(first, a, b, c);
					}
				}
			}
		}

		throw new PLCCompilerException("Not valid expanded pixel expr");
	}



// FROM PowerPoint
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
