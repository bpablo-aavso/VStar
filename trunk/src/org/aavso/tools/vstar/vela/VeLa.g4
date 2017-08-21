grammar VeLa;

// VeLa: VStar expression Language
//       -     -          -- 

// TODO:
// - set membership: x in [ ... ]; see filter from plot model descriptions
// - allow "in" to be used for list, set, string
// - check that a symbol doesn't exist in the environment before adding (interpreter)
// - exponentiation (use associativity modifier)
// - selection (e.g. in models): Haskell/Erland functional-style patterns instead of if-then
// - internal function representation in Models dialog should use VeLa:
//     t -> real-expression-over-t
//   | (boolean-expression : t -> real-expression-over-t ...)+
// - print statement for higher-level use of VeLa with LLVM code generation
// - comments (-- or #)

// ** Parser rules **

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
	groupedBooleanExpression
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
		) groupedBooleanExpression
	)*
;

groupedBooleanExpression
:
	LPAREN booleanExpression RPAREN
	| expression
;

expression
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
	sign? factor
;

factor
:
	LPAREN expression RPAREN
	| integer
	| real
	| string
	| list
	| var
	| func
;

integer
:
	INTEGER
;

real
:
	REAL
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

var
:
	IDENT
;

func
:
	IDENT LPAREN expression
	(
		comma expression
	)* RPAREN
;

sign
:
	MINUS
	| PLUS
;

comma
:
	COMMA
;

// ** Lexer rules **

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
	'IN'
	| 'in'
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
	'AND'
	| 'and'
;

OR
:
	'OR'
	| 'or'
;

NOT
:
	'NOT'
	| 'not'
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
:
	(
		LETTER
		| UNDERSCORE
	)
	(
		LETTER
		| DIGIT
		| UNDERSCORE
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