lexer grammar NlTest;

OpenHeading
    : POUND+ INLINE_WHITESPACE* -> pushMode ( HEADING );

OpenPara

//    : ( LETTER | DIGIT | OPEN_BRACE | OPEN_PAREN ) -> more, pushMode ( PARAGRAPH );
    : ( LETTER | DIGIT ) -> more, pushMode ( PARAGRAPH );

OpenBulletList
    : INLINE_WHITESPACE+ ASTERISK -> more, pushMode ( LIST );

OpenNumberedList
    : INLINE_WHITESPACE+ DIGIT+ DOT -> more, pushMode ( LIST );

OpenBlockQuote
    : INLINE_WHITESPACE* GT -> pushMode ( BLOCKQUOTE );

Whitespace
    : ALL_WS;

OpenHr
    : (( DASH DASH DASH+ ) | ( ASTERISK ASTERISK ASTERISK+ ) | ( DASH SPACE DASH
        SPACE ( DASH SPACE )+ ) | ( ASTERISK SPACE ASTERISK SPACE ( ASTERISK
        SPACE )+ SPACE* )) -> more, mode ( HR );


mode HR;

HorizontalRule
    : (( DASH DASH DASH+ ) | ( ASTERISK ASTERISK ASTERISK+ ) | ( DASH SPACE DASH
        SPACE ( DASH SPACE )+ ) | ( ASTERISK SPACE ASTERISK SPACE ( ASTERISK
        SPACE )+ SPACE* )) HorizontalRuleTail;

HorizontalRuleTail
    : NEWLINE;

HrExit
    : NON_HR -> more, mode ( DEFAULT_MODE );


mode PARAGRAPH;

ParaLink
    : LINK_TEXT+ COLON SLASH SLASH? LINK_TEXT+;

ParaHorizontalRule
    : NEWLINE? (( DASH DASH DASH+ ) | ( ASTERISK ASTERISK ASTERISK+ ) | ( DASH
        SPACE DASH SPACE ( DASH SPACE )+ ) | ( ASTERISK SPACE ASTERISK SPACE ( ASTERISK
        SPACE )+ SPACE* )) NEWLINE -> more, mode ( HR );

ParaBold
    : ASTERISK PUNC2?;

ParaWords
    : WORD_LIKE PUNC2? ( INLINE_WHITESPACE+ WORD_LIKE PUNC2? )*;

//( WORD_LIKE PUNC2? ( NEWLINE*? INLINE_WHITESPACE+ WORD_LIKE PUNC2? )* )
//    | ( WORD_LIKE PUNC2? ( INLINE_WHITESPACE*? WORD_LIKE PUNC2? )* )
//    | ( WORD_LIKE PUNC2? ( NEWLINE*? WORD_LIKE PUNC2? )* )
//    | ( INLINE_WHITESPACE PUNC2+? INLINE_WHITESPACE?? );
ParaNextBullet
    : ( ParaDoubleNewline | NEWLINE )? INLINE_WHITESPACE+ ASTERISK INLINE_WHITESPACE -> more,
        popMode, mode ( LIST );

ParaBlockquoteHead
    : ( NEWLINE GT ) -> mode ( BLOCKQUOTE );

ParaHeadingHead
    : ( NEWLINE POUND+ ) -> mode ( HEADING );

ParaBreak
    : ParaDoubleNewline -> mode ( DEFAULT_MODE );

ParaDoubleNewline
    : INLINE_WHITESPACE*? NEWLINE INLINE_WHITESPACE*? NEWLINE ( INLINE_WHITESPACE*?
        NEWLINE )*?;

ParaInlineWhitespace
    : INLINE_WHITESPACE+;

ParaItalic
    : UNDERSCORE PUNC2?;

ParaStrikethrough
    : STRIKE PUNC2?;

ParaCode
    : BACKTICK PUNC2?;

ParaBracketOpen
    : PUNC2? OPEN_BRACKET;

ParaBracketClose
    : CLOSE_BRACKET PUNC2?;

ParaOpenParen
    : PUNC2? OPEN_PAREN;

ParaCloseParen
    : CLOSE_PAREN PUNC2?;

ParaEof
    : EOF -> more, popMode;


mode BLOCKQUOTE;

BlockQuotePrologue
    : INLINE_WHITESPACE;

BlockQuote
    : NON_WHITESPACE -> more, mode ( PARAGRAPH );

BlockquoteDoubleNewline
    : INLINE_WHITESPACE* NEWLINE INLINE_WHITESPACE* NEWLINE ( INLINE_WHITESPACE*?
        NEWLINE )* -> popMode;


mode LIST;

ListPrologue
    : DOUBLE_NEWLINE? INLINE_WHITESPACE;

ListItem
    : ( LETTER | DIGIT ) -> more, pushMode ( PARAGRAPH );

NestedListItemHead
    : (( INLINE_WHITESPACE+ ASTERISK ) | ( INLINE_WHITESPACE+ DIGIT+ DOT ))
        INLINE_WHITESPACE;

ListHorizontalRule
    : NEWLINE? (( DASH DASH DASH+ ) | ( ASTERISK ASTERISK ASTERISK+ ) | ( DASH
        SPACE DASH SPACE ( DASH SPACE )+ NEWLINE ) | ( ASTERISK SPACE ASTERISK
        SPACE ( ASTERISK SPACE )+ SPACE* )) NEWLINE -> more, mode ( HR );

CloseList
    : DOUBLE_NEWLINE -> popMode;

CloseList2
    : NEWLINE NON_WHITESPACE -> more, popMode;


mode HEADING;

HeadingContent
    : WORD_LIKE PUNCTUATION? ( INLINE_WHITESPACE*? WORD_LIKE PUNCTUATION? )*;

HeadingClose
    : NEWLINE -> mode ( DEFAULT_MODE );

HeadingWordLike
    : ( LETTER | DIGIT )( LETTER | DIGIT | PUNC2)+;

fragment WORDS
    : WORD_LIKE PUNC2? INLINE_WHITESPACE??;

fragment WORD_LIKE
    : ( LETTER | DIGIT )( LETTER | DIGIT | PUNC2 )*;

fragment PUNC2
    : GT
    | LT
    | OPEN_BRACE
    | CLOSE_BRACE
    | SLASH
    | BACKSLASH
    | SEMICOLON
    | COLON
    | AT
    | DOT
    | DOLLARS
    | PERCENT
    | DASH
    | EQUALS
    | SQUOTE
    | DQUOTE
    | QUESTION
    | COMMA;

//[><{}/\\:;+!@.,$%^&\-='"?];
fragment PUNCTUATION
    : [\p{Punctuation}];

fragment LETTER
    : [A-Za-z];
//    : [\p{Alphabetic}];

fragment DIGIT
    : [0-9];
//    : [\p{Digit}];

fragment NON_HR
    :~[ -*];

//    : '0'..'9';
fragment ALL_WS
    : INLINE_WHITESPACE
    | NEWLINE;

fragment DOUBLE_NEWLINE
    : NEWLINE NEWLINE NEWLINE*?;

fragment NEWLINE
    : '\n';

fragment SPECIAL_CHARS
    : ASTERISK
    | STRIKE
    | BACKTICK
    | POUND
    | STRIKE;

fragment NON_WHITESPACE
    :~[ \r\n\t];

//    :~[\p{White_Space}];
fragment LINK_TEXT
    : [a-zA-Z0-9]
    | '/'
    | '.';

fragment PRE
    : '```';

fragment INLINE_WHITESPACE
    : SPACE
    | TAB
    | CARRIAGE_RETURN;

fragment SPACE
    : ' ';

fragment TAB
    : '\t';

fragment CARRIAGE_RETURN
    : '\r';

fragment BACKSLASH
    : '\\';

fragment SQUOTE
    : '\'';

fragment DQUOTE
    : '"';

fragment OPEN_PAREN
    : '(';

fragment CLOSE_PAREN
    : ')';

fragment OPEN_BRACE
    : '{';

fragment CLOSE_BRACE
    : '}';

fragment OPEN_BRACKET
    : '[';

fragment CLOSE_BRACKET
    : ']';

fragment COLON
    : ':';

fragment AMPERSAND
    : '&';

fragment COMMA
    : ',';

fragment PLUS
    : '+';

fragment DOLLARS
    : '$';

fragment PERCENT
    : '%';

fragment CAREN
    : '^';

fragment AT
    : '@';

fragment BANG
    : '!';

fragment GT
    : '>';

fragment LT
    : '<';

fragment QUESTION
    : '?';

fragment SEMICOLON
    : ';';

fragment SLASH
    : '/';

fragment UNDERSCORE
    : '_';

fragment STRIKE
    : '~~';

fragment BACKTICK
    : '`';

fragment DASH
    : '-';

fragment EQUALS
    : '=';

fragment ASTERISK
    : '*';

fragment POUND
    : '#';

fragment DOT
    : '.';