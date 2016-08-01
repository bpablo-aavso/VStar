/**
 * VStar: a statistical analysis tool for variable star data.
 * Copyright (C) 2010  AAVSO (http://www.aavso.org/)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package org.aavso.tools.vstar.vela;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * VeLa: VStar expression Language interpreter
 */
public class VeLaInterpreter {

	// TODO:
	// - change to stack of Result which can be either Double, Boolean, String?
	// => that will be the challenge when filters expressions are implemented;
	// => see language implementation patterns book
	// - use a Deque implementation for stack
	private Stack<Double> stack;

	// AST and result caches.
	private static Map<String, AST> exprToAST = new HashMap<String, AST>();
	private static Map<String, Double> exprToRealResult = new HashMap<String, Double>();

	private VeLaErrorListener errorListener;

	public VeLaInterpreter() {
		errorListener = new VeLaErrorListener();
		stack = new Stack<Double>();
	}

	public double realExpression(String expr) throws VeLaParseError {
		return realExpression(expr, false);
	}

	public double realExpression(String expr, boolean verbose)
			throws VeLaParseError {

		AST ast = null;

		// We cache abstract syntax trees by expression to improve performance.
		if (exprToAST.containsKey(expr)) {
			ast = exprToAST.get(expr);
			if (verbose) {
				System.out.println(String.format("AST in cache: %s", ast));
			}
		} else {
			CharStream stream = new ANTLRInputStream(expr);

			VeLaLexer lexer = new VeLaLexer(stream);
			lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
			lexer.addErrorListener(errorListener);

			CommonTokenStream tokens = new CommonTokenStream(lexer);

			VeLaParser parser = new VeLaParser(tokens);
			parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
			parser.addErrorListener(errorListener);

			VeLaParser.RealExpressionContext tree = parser.realExpression();
			RealExpressionListener listener = new RealExpressionListener(stack);
			ParseTreeWalker.DEFAULT.walk(listener, tree);

			ast = listener.getAST();
			exprToAST.put(expr, ast);
		}

		if (verbose) {
			System.out.println(ast);
		}

		double result;

		if (ast.isDeterministic() && exprToRealResult.containsKey(expr)) {
			// For deterministic expressions, we can also use cached results.
			result = exprToRealResult.get(expr);
			if (verbose) {
				System.out.println(String.format(
						"Result for AST '%s' in cache: %f", ast, result));
			}
		} else {
			// Evaluate the abstract syntax tree and cache the result.
			evalRealExpression(ast);
			result = stack.pop();
			exprToRealResult.put(expr, result);
		}

		return result;
	}

	/**
	 * <p>
	 * Given an AST representing a real expression, interpret this via a depth
	 * first traversal, leaving the result of evaluation on the stack.
	 * </p>
	 * <p>
	 * The "eval" prefix is used in deference to Lisp and John McCarthy's eval
	 * function, the equivalent of Maxwell's equations in Computer Science.
	 * 
	 * @param ast
	 *            The abstract syntax tree.
	 */
	private void evalRealExpression(AST ast) {
		if (ast.isLeaf()) {
			stack.push(parseDouble(ast.getToken()));
		} else {
			Operation op = ast.getOp();

			if (op.arity() == 2) {
				// Binary
				evalRealExpression(ast.left());
				evalRealExpression(ast.right());

				double n2 = stack.pop();
				double n1 = stack.pop();

				switch (op) {
				case ADD:
					stack.push(n1 + n2);
					break;
				case SUB:
					stack.push(n1 - n2);
					break;
				case MUL:
					stack.push(n1 * n2);
					break;
				case DIV:
					stack.push(n1 / n2);
					break;
				default:
					break;
				}
			} else if (op.arity() == 1) {
				// Unary
				evalRealExpression(ast.leaf());

				switch (op) {
				case NEG:
					stack.push(-stack.pop());
					break;
				default:
					break;
				}
			}
		}
	}
	
	/**
	 * Parse a string, returning a double primitive value, or if no valid double
	 * value is present, throw a NumberFormatException. The string is first
	 * trimmed of leading and trailing whitespace.
	 * 
	 * @param str
	 *            The string that (hopefully) contains a number.
	 * @return The double value corresponding to the initial parseable portion
	 *         of the string.
	 * @throws NumberFormatException
	 *             If no valid double value is present.
	 */
	private double parseDouble(String str) throws NumberFormatException {
		NumberFormat FORMAT = NumberFormat
				.getNumberInstance(Locale.getDefault());

		if (str == null) {
			throw new NumberFormatException("String was null");
		} else {
			try {
				str = str.trim();

				if (str.startsWith("+")) {
					// Leading "+" causes an exception to be thrown.
					str = str.substring(1);
				}

				if (str.contains("e")) {
					// Convert exponential indicator to parsable form.
					str = str.toUpperCase();
				}

				return FORMAT.parse(str).doubleValue();
			} catch (ParseException e) {
				throw new NumberFormatException(e.getLocalizedMessage());
			}
		}
	}
}
