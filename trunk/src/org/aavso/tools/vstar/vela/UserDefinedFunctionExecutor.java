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

import java.util.List;
import java.util.Optional;

/**
 * This class executes user defined functions.
 */
public class UserDefinedFunctionExecutor extends FunctionExecutor {

	private VeLaInterpreter vela;
	private List<String> parameterNames;
	private Optional<AST> ast;

	// TODO: capture environment and set/reset it in interpreter in apply()

	/**
	 * Construct a named function definition
	 * 
	 * @param vela
	 *            The interpreter instance from which this object is being
	 *            created.
	 * @param funcName
	 *            The function's name.
	 * @param parameterNames
	 *            The function's formal parameter names.
	 * @param parameterTypes
	 *            The function's formal parameter types.
	 * @param returnType
	 *            The function's return type.
	 * @param ast
	 *            The AST corresponding to the body of the function.
	 */
	public UserDefinedFunctionExecutor(VeLaInterpreter vela,
			Optional<String> funcName, List<String> parameterNames,
			List<Type> parameterTypes, Optional<Type> returnType,
			Optional<AST> ast) {
		super(funcName, parameterTypes, returnType);
		this.vela = vela;
		this.parameterNames = parameterNames;
		this.ast = ast;
	}

	/**
	 * Construct an anonymous function definition
	 * 
	 * @param vela
	 *            The interpreter instance from which this object is being
	 *            created.
	 * @param parameterNames
	 *            The function's formal parameter names.
	 * @param parameterTypes
	 *            The function's formal parameter types.
	 * @param returnType
	 *            The function's return type.
	 * @param ast
	 *            The AST corresponding to the body of the function.
	 */
	public UserDefinedFunctionExecutor(VeLaInterpreter vela,
			List<String> parameterNames, List<Type> parameterTypes,
			Optional<Type> returnType, Optional<AST> ast) {
		this(vela, Optional.empty(), parameterNames, parameterTypes,
				returnType, ast);
	}

	@Override
	public Optional<Operand> apply(List<Operand> operands) throws VeLaEvalError {
		// If the function has a body, push a new scope, bind the actual
		// parameters to the formal parameters, evaluate the body AST and pop
		// the scope.
		if (ast.isPresent()) {
			if (!isTailRecursive()) {
				vela.pushEnvironment(new VeLaScope());
			}

			for (int i = 0; i < operands.size(); i++) {
				vela.bind(parameterNames.get(i), operands.get(i));
			}

			vela.eval(ast.get());

			if (!isTailRecursive()) {
				vela.popEnvironment();
			}
		}

		// The result, if any, will be on the stack.
		return Optional.empty();
	}

	/**
	 * Is the function body tail recursive?
	 */
	private boolean isTailRecursive() {
		boolean isTailRecursive = false;

		AST lastChild = ast.get().lastChild();

		switch (lastChild.getOp()) {
		case FUNCALL:
			if (getFuncName().isPresent()
					&& getFuncName().get().equalsIgnoreCase(
							lastChild.getToken())) {
				isTailRecursive = true;
			}
			break;
		default:
		}

		return isTailRecursive;
	}
}
