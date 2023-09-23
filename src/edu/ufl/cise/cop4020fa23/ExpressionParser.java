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

import static edu.ufl.cise.cop4020fa23.Kind.AND;
import static edu.ufl.cise.cop4020fa23.Kind.BANG;
import static edu.ufl.cise.cop4020fa23.Kind.BITAND;
import static edu.ufl.cise.cop4020fa23.Kind.BITOR;
import static edu.ufl.cise.cop4020fa23.Kind.COLON;
import static edu.ufl.cise.cop4020fa23.Kind.COMMA;
import static edu.ufl.cise.cop4020fa23.Kind.DIV;
import static edu.ufl.cise.cop4020fa23.Kind.EOF;
import static edu.ufl.cise.cop4020fa23.Kind.EQ;
import static edu.ufl.cise.cop4020fa23.Kind.EXP;
import static edu.ufl.cise.cop4020fa23.Kind.GE;
import static edu.ufl.cise.cop4020fa23.Kind.GT;
import static edu.ufl.cise.cop4020fa23.Kind.IDENT;
import static edu.ufl.cise.cop4020fa23.Kind.LE;
import static edu.ufl.cise.cop4020fa23.Kind.LPAREN;
import static edu.ufl.cise.cop4020fa23.Kind.LSQUARE;
import static edu.ufl.cise.cop4020fa23.Kind.LT;
import static edu.ufl.cise.cop4020fa23.Kind.MINUS;
import static edu.ufl.cise.cop4020fa23.Kind.MOD;
import static edu.ufl.cise.cop4020fa23.Kind.NUM_LIT;
import static edu.ufl.cise.cop4020fa23.Kind.OR;
import static edu.ufl.cise.cop4020fa23.Kind.PLUS;
import static edu.ufl.cise.cop4020fa23.Kind.QUESTION;
import static edu.ufl.cise.cop4020fa23.Kind.RARROW;
import static edu.ufl.cise.cop4020fa23.Kind.RES_blue;
import static edu.ufl.cise.cop4020fa23.Kind.RES_green;
import static edu.ufl.cise.cop4020fa23.Kind.RES_height;
import static edu.ufl.cise.cop4020fa23.Kind.RES_red;
import static edu.ufl.cise.cop4020fa23.Kind.RES_width;
import static edu.ufl.cise.cop4020fa23.Kind.RPAREN;
import static edu.ufl.cise.cop4020fa23.Kind.RSQUARE;
import static edu.ufl.cise.cop4020fa23.Kind.STRING_LIT;
import static edu.ufl.cise.cop4020fa23.Kind.TIMES;
import static edu.ufl.cise.cop4020fa23.Kind.CONST;

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
	private ArrayList<IToken> tokenz = new ArrayList<IToken>();
	private int listPos = 0;
	/**
	 * @param lexer
	 * @throws LexicalException 
	 */
	public ExpressionParser(ILexer lexer) throws LexicalException {
		super();
		this.lexer = lexer;
		t = lexer.next();
	}

	@Override
	public AST parse() throws PLCCompilerException {
		// throw error for empty string or smt
		// while theres tokens/not eof --> add tokens to array

		Expr e = expr(); // bc of this we have to change this, it only calls expr, nothing else
		return e;
	}

	private Expr expr() throws PLCCompilerException {
		// this just checks if its a IF or OR kind --> returns matching expression
		IToken firstToken = t;

		if (isKind(Kind.RES_if)) {
			return condExpr();
		}
		else if (isKind(BITOR)) {
			return logOrExpr();
		}
		else {
			throw new PLCCompilerException("Not IF or OR");
		}
	}

	ConditionalExpr condExpr() throws PLCCompilerException {

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

		return new ConditionalExpr(t, a, b, c);
	}

	Expr logOrExpr() throws PLCCompilerException {
		Expr left = logAndExpr();

		while (isKind(OR, BITOR)) {
			// something --> op
			Expr right = logAndExpr();
		}

		return left;
	}

	Expr logAndExpr() throws PLCCompilerException {
		Expr left = cmpExpr();

		while (isKind(AND, BITAND)) {
			// something --> op
			Expr right = cmpExpr();
		}

		return left;
	}

	Expr cmpExpr() throws PLCCompilerException {
		Expr left = powExpr();

		while (isKind(GT, LT, GE, LE, EQ)) {
			// something --> op
			Expr right = powExpr();
		}

		return left;
	}

	Expr powExpr() throws PLCCompilerException {
		return null;
	}

	Expr addExpr() throws PLCCompilerException {
		Expr left = multExpr();

		while (isKind(PLUS, MINUS)) {
			// something --> op
			Expr right = multExpr();
		}

		return left;
	}

	Expr multExpr() throws PLCCompilerException {
		Expr left = uExpr();

		while (isKind(TIMES, DIV, MOD)) {
			// save curr token as op to pass into Binary w/ right
			IToken op = tokenz.get(listPos); //token at curr position
			Expr right = uExpr();
			left = new BinaryExpr(t, left, op, right);
		}

		return left;
		//return new BinaryExpr(first token, left, op, right);
	}

	Expr uExpr() throws PLCCompilerException {

		if (!isKind(BANG, MINUS, RES_width, RES_height)) {
			IToken a = null;
			Expr b = uExpr();
			return new UnaryExpr(t, a, b);
		}

		return poFixExpr();
	}

	PostfixExpr poFixExpr() throws PLCCompilerException {
		Expr a = null;
		PixelSelector b = null;
		ChannelSelector c = null;
		return new PostfixExpr(t, a, b, c);
	}

	Expr priExpr() throws PLCCompilerException {
		switch (t.kind()) {
			case STRING_LIT -> {
				return new StringLitExpr(t);
			}
			case NUM_LIT -> {
				return new NumLitExpr(t);
			}
			case IDENT -> {
				return new IdentExpr(t);
			}
			case LPAREN -> {
				// get expr()
				// close with RPAREN
			}
			case CONST -> {
				return new ConstExpr(t);
			}
			case LSQUARE -> {
				return exPxExpr();
			}
			default -> {
				throw new PLCCompilerException("Primary Expression Error");
			}
		}
		throw new PLCCompilerException("something");
	}

	ChannelSelector chSel() throws PLCCompilerException {
		IToken a = null, b = null;

		if (isKind(COLON)) {
			// next token -->
			if (isKind(RES_red, RES_green, RES_blue)) {
				return new ChannelSelector(a,b);
			}
		}

		throw new PLCCompilerException("Not valid channel selector");
	}

	PixelSelector pxSel() throws PLCCompilerException {

		if (isKind(LSQUARE)) {
			Expr a = expr();
			if (isKind(COMMA)) {
				Expr b = expr();
				if (isKind(RSQUARE)) {
					return new PixelSelector(t, a, b);
				}
			}
		}

		throw new PLCCompilerException("Not valid pixel selector");
	}

	ExpandedPixelExpr exPxExpr() throws PLCCompilerException {
		if (isKind(LSQUARE)) {
			Expr a = expr();
			if (isKind(COMMA)) {
				Expr b = expr();
				if (isKind(COMMA)) {
					Expr c = expr();
					if (isKind(RSQUARE)) {
						return new ExpandedPixelExpr(t, a, b, c);
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
