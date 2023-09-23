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

import static edu.ufl.cise.cop4020fa23.Kind.EOF;

import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.Kind; // idk if this is right
import java.util.*;

import javax.xml.transform.Source;

import java.io.*;
// fix import statements

public class Lexer implements ILexer {

	private enum State{START, IN_IDENT, IN_NUM, IN_STRLIT, HAVE_EQ, HAVE_MINUS, HAVE_LESS, HAVE_ZERO,
		HAVE_GREAT, HAVE_AND, HAVE_OR, HAVE_LEFTBOX, HAVE_ASTK, HAVE_COLON};
	// There might be more states I'm missing
	// have zero, have dot, in float --> seems usless

	String input;

	// can remane, just vars for now
	// I also don't know where to put these, so they're here for now :,)

	State state;
	Map<String, Kind> rWords = new HashMap<String, Kind>();
	int currentToken;
	int pos = 0;
	int startPos;
	int line;
	int currLine;
	int column;
	char[] chars;
	// if not in map --> create IDENT token

	public Lexer(String input) {
		this.input = input;
		startPos = 0;
		line = 1;
		currLine = line;
		column = 1;
		currentToken = 0;
		chars = input.toCharArray();
		makeMap();
		state = State.START;
		// does source location use the start values?
		// if so, need to keep another line for curr pos in line
	}

	@Override
	public IToken next() throws LexicalException {
		if (pos == input.length()) {
			return new Token(EOF, input.length(), 0, null, null);
		}

		return storeAllTokens();
	}

	private Token storeAllTokens() throws LexicalException {
		// check if its in start --> gives it a type of state
		// else --> continues through state

		// need to draw out the lexical structure but this is a outline

		Token t;
		state = State.START;
		int startCol = 1;

		while (pos < chars.length+1) { // either this or eof char

			char ch;
			if (pos >= chars.length) {
				ch = ' ';
				//pos++;
			} else {
				ch = chars[pos];
			}
			// any single char token --> create them , pos++
			// else --> change state
			switch (state) {
				case START -> {
					switch (ch) {
						case ' ', '\r', '\t' -> {
							pos++;
							column++;
						}
						case '\n' -> {
							line++;
							column = 1;
							pos++;
							// every newline:
							// reset line, col + 1
						}
						case '#' -> {
							if (pos+1 >= chars.length) {
								pos++;
								throw new LexicalException("invalid input");
							}
							// make sure its ## --> ignore till \n
							// else throw lexical exception
							// while loop till /n, ++po
							if (chars[++pos] != '#') {
								throw new LexicalException("invalid input");
							}
							while (ch != '\n') {
								ch = chars[++pos];
								if (ch == '␡') {
									throw new LexicalException("invalid input");
								}
							}
						}
						case '_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
								'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
								'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
								'Y', 'Z' -> {
							// ident can either start w/ letter or _
							startPos = pos++;
							state = State.IN_IDENT;
							startCol = column++;
						}
						case '0' -> {
							t = new Token(Kind.NUM_LIT, pos++, 1, chars, new SourceLocation(line, column++));
							state = State.START;
							return t;
						}
						case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
							startPos = pos++;
							state = State.IN_NUM;
							startCol = column++;
						} // idk if ops should be here, but they all have their own kind
						case '"' -> {
							startPos = pos++;
							state = State.IN_STRLIT;
							startCol = column++;
						}
						case ',' -> {
							t = new Token(Kind.COMMA, pos, 1, chars, new SourceLocation(line, column++));
							pos++;
							return t;
						}
						case ';' -> {
							t = new Token(Kind.SEMI, pos, 1, chars, new SourceLocation(line, column++));
							pos++;
							return t;
						}
						case '?' -> {
							t = new Token(Kind.QUESTION, pos, 1, chars, new SourceLocation(line, column++));
							pos++;
							return t;
						}
						case ':' -> {
							startPos = pos++;
							state = State.HAVE_COLON;
							startCol = column++;
						}
						case '(' -> {
							t = new Token(Kind.LPAREN, pos, 1, chars, new SourceLocation(line, column++));
							pos++;
							return t;
						}
						case ')' -> {
							t = new Token(Kind.RPAREN, pos, 1, chars, new SourceLocation(line, column++));
							pos++;
							return t;
						}
						case '<' -> {
							startPos = pos++;
							state = State.HAVE_LESS;
							startCol = column++;
						}
						case '>' -> {
							startPos = pos++;
							state = State.HAVE_GREAT;
							startCol = column++;
						}
						case '[' -> {
							startPos = pos++;
							state = State.HAVE_LEFTBOX;
							startCol = column++;
						}
						case ']' -> {
							t = new Token(Kind.RSQUARE, pos, 1, chars, new SourceLocation(line, column++));
							pos++;
							return t;
						}
						case '=' -> {
							startPos = pos++;
							state = State.HAVE_EQ;
							startCol = column++;
						}
						case '!' -> {
							t = new Token(Kind.BANG, pos, 1, chars, new SourceLocation(line, column++));
							pos++;
							return t;
						}
						case '&' -> {
							startPos = pos++;
							state = State.HAVE_AND;
							startCol = column++;
						}
						case '|' -> {
							startPos = pos++;
							state = State.HAVE_OR;
							startCol = column++;
						}
						case '+' -> {
							t = new Token(Kind.PLUS, pos, 1, chars, new SourceLocation(line, column++));
							pos++;
							return t;
						}
						case '-' -> {
							startPos = pos++;
							state = State.HAVE_MINUS;
							startCol = column++;
						}
						case '*' -> {
							startPos = pos++;
							state = State.HAVE_ASTK;
							startCol = column++;
						}
						case '/' -> {
							t = new Token(Kind.DIV, pos, 1, chars, new SourceLocation(line, column++));
							pos++;
							return t;
						}
						case '%' -> {
							t = new Token(Kind.MOD, pos, 1, chars, new SourceLocation(line, column++));
							pos++;
							return t;
						}
						case '^' -> {
							t = new Token(Kind.RETURN, pos, 1, chars, new SourceLocation(line, column++));
							pos++;
							return t;
						}
						default -> {
							throw new LexicalException("invalid input"); //test seven is hitting this - nvm
						}
					}
				}
				case HAVE_AND -> {
					switch (ch) {
						case '&' -> {
							//column--; //no idea why
							t = new Token(Kind.AND, startPos, 2, chars, new SourceLocation(line, startCol));
							column++;
							pos++;
							state = State.START;
							return t;
						}
						default -> {
							//column--;
							t = new Token(Kind.BITAND, startPos, 1, chars, new SourceLocation(line, startCol));
							//column++;
							//pos++;
							state = State.START;
							return t;
						}
					}
				}
				case HAVE_LEFTBOX -> {
					switch (ch) {
						case ']' -> {
							t = new Token(Kind.BOX, startPos, 2, chars, new SourceLocation(line, startCol));
							column++;
							pos++;
							state = State.START;
							return t;
						}
						default -> {
							t = new Token(Kind.LSQUARE, startPos, 1, chars, new SourceLocation(line, startCol));
							//pos++;
							state = State.START;
							return t;
						}
					}
				}
				case IN_IDENT -> {
					switch (ch) {
						case '_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
								'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G',
								'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
								'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
							pos++;
							column++;
						}
						default -> {
							String str = new String(chars, startPos, pos - startPos);
							if (rWords.containsKey(str)) {
								t = new Token(rWords.get(str), startPos, pos - startPos, chars, new SourceLocation(line, startCol));
								//pos++; //less sure about this change
								state = State.START;
								startCol = column;
								return t;
							}
							t = new Token(Kind.IDENT, startPos, pos - startPos, chars, new SourceLocation(line, startCol));
							state = State.START;
							return t;
						}
					}
				}
				case IN_NUM -> {
					switch (ch) {
						case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
							pos++;
							column++;
						}
						default -> {
							try {
								Integer.parseInt(new String(chars, startPos, pos - startPos));
							} catch (NumberFormatException e) {
								throw new LexicalException("invalid input");
							}
							t = new Token(Kind.NUM_LIT, startPos, pos - startPos, chars, new SourceLocation(line, startCol));
							state = State.START;
							return t;
						}
					}
				}
				case HAVE_COLON -> {
					switch (ch) {
						case '>' -> {
							t = new Token(Kind.BLOCK_CLOSE, startPos, 2, chars, new SourceLocation(line, startCol));
							column++;
							pos++;
							state = State.START;
							return t;
						}
						default -> {
							t = new Token(Kind.COLON, startPos, 1, chars, new SourceLocation(line, startCol));
							state = State.START;
							return t;
						}
					}
				}
				case HAVE_EQ -> {
					switch (ch) {
						case '=' -> {
							t = new Token(Kind.EQ, startPos, 2, chars, new SourceLocation(line, startCol));
							column++;
							pos++;
							state = State.START;
							return t;
						}
						default -> {
							t = new Token(Kind.ASSIGN, startPos, 1, chars, new SourceLocation(line, startCol));
							state = State.START;
							return t;
						}
					}
				}
				case IN_STRLIT -> {
					switch (ch) {
						case '"' -> {
							t = new Token(Kind.STRING_LIT, startPos, pos - startPos + 1, chars, new SourceLocation(line, startCol));
							column++;
							pos++;
							state = State.START;
							return t;
						}
						default -> {
							int ascii = (int) ch;
							if (ascii >= 32 && ascii <= 255) {
								pos++;
								column++;
							}
							else {
								throw new LexicalException("lexical error");
							}
						}
					}
				}
				case HAVE_MINUS -> {
					switch (ch) {
						case '>' -> {
							t = new Token(Kind.RARROW, startPos, 2, chars, new SourceLocation(line, startCol));
							column++;
							pos++;
							state = State.START;
							return t;
						}
						default -> {
							t = new Token(Kind.MINUS, startPos, 1, chars, new SourceLocation(line, startCol));
							state = State.START;
							return t;
						}
					}
				}
				case HAVE_LESS -> {
					switch (ch) {
						case '=' -> {
							t = new Token(Kind.LE, startPos, 2, chars, new SourceLocation(line, startCol));
							column++;
							pos++;
							state = State.START;
							return t;
						}
						case ':' -> {
							t = new Token(Kind.BLOCK_OPEN, startPos, 2, chars, new SourceLocation(line, startCol));
							column++;
							pos++;
							state = State.START;
							return t;
						}
						default -> {
							t = new Token(Kind.LT, startPos, 1, chars, new SourceLocation(line, startCol));
							state = State.START;
							return t;
						}
					}
				}
				case HAVE_GREAT -> {
					switch (ch) {
						case '=' -> {
							t = new Token(Kind.GE, startPos, 2, chars, new SourceLocation(line, startCol));
							column++;
							pos++;
							state = State.START;
							return t;
						}
						default -> {
							t = new Token(Kind.GT, startPos, 1, chars, new SourceLocation(line, startCol));
							state = State.START;
							return t;
						}
					}
				}
				case HAVE_OR -> {
					switch (ch) {
						case '|' -> {
							t = new Token(Kind.OR, startPos, 2, chars, new SourceLocation(line, startCol));
							column++;
							pos++;
							state = State.START;
							return t;
						}
						default -> {
							t = new Token(Kind.BITOR, startPos, 1, chars, new SourceLocation(line, startCol));
							state = State.START;
							return t;
						}
					}
				}
				case HAVE_ASTK -> {
					switch (ch) {
						case '*' -> {
							t = new Token(Kind.EXP, startPos, 2, chars, new SourceLocation(line, startCol));
							column++;
							pos++;
							state = State.START;
							return t;
						}
						default -> {
							t = new Token(Kind.TIMES, startPos, 1, chars, new SourceLocation(line, startCol));
							state = State.START;
							return t;
						}
					}
				}

				default -> throw new LexicalException("invalid state"); //not sure what this is supposed to say

			}
		}
		return new Token(EOF, chars.length, 0, null, null);
	}

	private void makeMap() {
		rWords.put("TRUE", Kind.BOOLEAN_LIT);
		rWords.put("FALSE", Kind.BOOLEAN_LIT);
		rWords.put("Z", Kind.CONST);
		rWords.put("BLACK", Kind.CONST);
		rWords.put("BLUE", Kind.CONST);
		rWords.put("CYAN", Kind.CONST);
		rWords.put("DARK_GRAY", Kind.CONST);
		rWords.put("GRAY", Kind.CONST);
		rWords.put("GREEN", Kind.CONST);
		rWords.put("LIGHT_GRAY", Kind.CONST);
		rWords.put("MAGENTA", Kind.CONST);
		rWords.put("ORANGE", Kind.CONST);
		rWords.put("PINK", Kind.CONST);
		rWords.put("RED", Kind.CONST);
		rWords.put("WHITE", Kind.CONST);
		rWords.put("YELLOW", Kind.CONST);
		rWords.put("image", Kind.RES_image);
		rWords.put("pixel", Kind.RES_pixel);
		rWords.put("int", Kind.RES_int);
		rWords.put("string", Kind.RES_string);
		rWords.put("void", Kind.RES_void);
		rWords.put("boolean", Kind.RES_boolean);
		rWords.put("nil", Kind.RES_nil);
		rWords.put("write", Kind.RES_write);
		rWords.put("height", Kind.RES_height);
		rWords.put("width", Kind.RES_width);
		rWords.put("if", Kind.RES_if);
		rWords.put("fi", Kind.RES_fi);
		rWords.put("do", Kind.RES_do);
		rWords.put("od", Kind.RES_od);
		rWords.put("red", Kind.RES_red);
		rWords.put("green", Kind.RES_green);
		rWords.put("blue", Kind.RES_blue);
	}



}



/*

Notes:

source location: line + col
creat(g) new token: kind, position, length, char[], SourceLocation
check for reserved words + constants + bool_lit --> diff from ident
ignore whitespace + comments
throw error when int too big/small

one of the lectures, she gives a lot of hints on how to do this/get started
 */
