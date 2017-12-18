grammar VeLa;

// VeLa: VStar expression Language
//       -     -          -- 

// TODO:
// - let binding of any expression, including functions (HOFs)
//   selection in functions:
//     f <- fun(x:t1,y:t2,z:t3) -> expression-over-x,y,z
//   | fun(x:t1,y:t2,z:t3) -> (boolean-expression : expression-over-x,y,z ...)+
// - Consider maps
// - internal function representation in Models dialog should use VeLa
// - VeLa could replace or be an alternative to JavaScript for scripting
// - Generate class files from VeLa ASTs

// ** Parser rules **

// A VeLa program consists of zero or more bindings, output 
// statements or expressions. Variable bindings are immutable, 
// as is the operation of functions. VeLa identifiers and keywords
// are case-insensitive.

// The expression production will leave a value on the stack,
// therefore the program rule could be the entry point for 
// VStar filters as well as models. If one wishes to create 
// a filter that is a complete VeLa program that happens to
// end in a boolean expression, then that is permitted.

sequence
:
	(
		binding
		| namedFundef
		| out
		| expression
	)*
;

// The intention of the semantics are that within a given scope,
// a binding cannot be repeated without error.

binding
:
	symbol BACK_ARROW expression
;

// A named function definition, when invoked, introduces an additional 
// environment and allows all VeLa program elements operating over that 
// environment and its predecessors.

namedFundef
:
	FUN symbol LPAREN formalParameter?
	(
		comma formalParameter
	)* RPAREN (COLON type)? LBRACE sequence RBRACE
;

// TODO: use {...} for function bodies, omitting -> 

// TODO: really need return type? not used for function signature comparison

// TODO: add IN/INPUT/READ and change below to WRITE if necessary

out
:
	OUT expression
	(
		COMMA expression
	)*
;

expression
:
	selectionExpression
	| booleanExpression
;

// Homage to Haskell/Scala/Erlang functional-style cases

selectionExpression
:
	SELECT
	(
		booleanExpression ARROW booleanExpression
	)+
;

booleanExpression
:
	conjunctiveExpression
	(
		OR conjunctiveExpression
	)*
;

conjunctiveExpression
:
	logicalNegationExpression
	(
		AND logicalNegationExpression
	)*
;

logicalNegationExpression
:
	NOT? relationalExpression
;

relationalExpression
:
	additiveExpression
	(
		(
			EQUAL
			| NOT_EQUAL
			| GREATER_THAN
			| LESS_THAN
			| GREATER_THAN_OR_EQUAL
			| LESS_THAN_OR_EQUAL
			| APPROXIMATELY_EQUAL
			| IN
		) additiveExpression
	)?
;

additiveExpression
:
	multiplicativeExpression
	(
		(
			PLUS
			| MINUS
		) multiplicativeExpression
	)*
;

multiplicativeExpression
:
	unaryExpression
	(
		(
			MULT
			| DIV
		) unaryExpression
	)*
;

unaryExpression
:
	sign? exponentiationExpression
;

sign
:
	MINUS
	| PLUS
;

exponentiationExpression
:
// This rule option is right associative.
	< assoc = right > factor
	(
		(
			POW
		) factor
	)*
;

factor
:
// Note: funcall must precede symbol to avoid errors
	LPAREN expression RPAREN
	| integer
	| real
	| bool
	| string
	| list
	| funcall
	| symbol
	| anonFundef
;

integer
:
	INTEGER
;

real
:
	REAL
;

bool
:
	BOOLEAN
;

string
:
	STRING
;

list
:
	LBRACKET expression?
	(
		comma expression
	)* RBRACKET
;

symbol
:
	IDENT
;

// An anonymous function definition, when invoked, introduces an additional 
// environment and allows all VeLa program elements operating over that environment 
// and its predecessors.

anonFundef
:
	FUN LPAREN formalParameter?
	(
		comma formalParameter
	)* RPAREN (COLON type)? LBRACE sequence RBRACE
;

// A formal parameter consists of a name-type pair

formalParameter
:
	symbol COLON type
;

// TODO: make it int, bool, real ...

type
: 		
	INT_T
	| REAL_T
	| BOOL_T
	| STR_T
	| LIST_T
	| FUN
;

// A function call consists of a function object followed 
// by zero or more parameters surrounded by parentheses.

funcall
:
	funobj LPAREN expression?
	(
		comma expression
	)* RPAREN
;

// IDENT corresponds to an explicit function name
// var allows a HOF (let binding or function parameter)
// anonFundef allows an anonymous function

funobj
:
	(
		IDENT
		| anonFundef
	)
;

comma
:
	COMMA
;

// ** Lexer rules **

SELECT
:
	[Ss] [Ee] [Ll] [Ee] [Cc] [Tt]
;

BACK_ARROW
:
	'<-'
;

COLON
:
	':'
;

ARROW
:
	'->'
;

OUT
:
	[Oo] [Uu] [Tt]
;

// Used for function definition and parameter type

FUN
:
	[Ff] [Uu] [Nn]
;

INT_T
:
	[Ii] [Nn] [Tt] [Ee] [Gg] [Ee] [Rr]
;

REAL_T
:
	[Rr] [Ee] [Aa] [Ll]
;

BOOL_T
:
	[Bb] [Oo] [Oo] [Ll] [Ee] [Aa] [Nn]
;

STR_T
:
	[Ss] [Tt] [Rr] [Ii] [Nn] [Gg]
;

LIST_T
:
	[Ll] [Ii] [Ss] [Tt]
;

MINUS
:
	'-'
;

PLUS
:
	'+'
;

MULT
:
	'*'
;

DIV
:
	'/'
;

POW
:
	'^'
;

EQUAL
:
	'='
;

NOT_EQUAL
:
	'<>'
;

GREATER_THAN
:
	'>'
;

LESS_THAN
:
	'<'
;

GREATER_THAN_OR_EQUAL
:
	'>='
;

LESS_THAN_OR_EQUAL
:
	'<='
;

// Homage to Perl

APPROXIMATELY_EQUAL
:
	'=~'
;

// Homage to SQL, Python, ...

IN
:
	[Ii] [Nn]
;

LPAREN
:
	'('
;

RPAREN
:
	')'
;

LBRACKET
:
	'['
;

RBRACKET
:
	']'
;

LBRACE
:
	'{'
;

RBRACE
:
	'}'
;

PERIOD
:
	'.'
;

COMMA
:
	','
;

AND
:
	[Aa] [Nn] [Dd]
;

OR
:
	[Oo] [Rr]
;

NOT
:
	[Nn] [Oo] [Tt]
;

INTEGER
:
	DIGIT+
;

REAL
:
	DIGIT+
	(
		POINT DIGIT+
	)?
	(
		EXPONENT_INDICATOR MINUS? DIGIT+
	)?
	| POINT DIGIT+
	(
		EXPONENT_INDICATOR MINUS? DIGIT+
	)?
;

BOOLEAN
:
	TRUE
	| FALSE
;

fragment
TRUE
:
	'#T'
	| '#t'
;

fragment
FALSE
:
	'#F'
	| '#f'
;

fragment
DIGIT
:
	[0-9]
;

fragment
POINT
// Locale-inclusive

:
	PERIOD
	| COMMA
;

fragment
EXPONENT_INDICATOR
:
	'E'
	| 'e'
;

IDENT
// TODO: exclude what isn't permitted in an identifier rather than including what can
:
	(
		LETTER
		| UNDERSCORE
		| QUESTION
	)
	(
		LETTER
		| DIGIT
		| UNDERSCORE
		| QUESTION
		
	)*
;

fragment
LETTER
:
	[A-Z]
	| [a-z]
;

UNDERSCORE
:
	'_'
;

QUESTION
:
	'?'
;

STRING
:
	'"'
	(
		~'"'
	)* '"'
;

WS
:
	[ \r\t\n]+ -> skip
;

COMMENT
:
// Could use channel(HIDDEN) instead of skip,
// e.g. https://stackoverflow.com/questions/23976617/parsing-single-line-comments
	'--' ~[\r\n]* -> skip
;