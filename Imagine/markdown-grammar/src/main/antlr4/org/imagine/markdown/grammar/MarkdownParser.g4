parser grammar MarkdownParser;

options{tokenVocab=MarkdownLexer; }

// Example rules
document
    : ( heading | blockquote | unorderedList | paragraphs | horizontalRule |
        whitespace )+ EOF?;

horizontalRule
    : (HorizontalRule HorizontalRuleTail)
    | HorizontalRuleTail+;

heading
    : head=OpenHeading HeadingPrologue body=headingContent;

headingContent
    : HeadingContent;

content
    : text
    | bold
    | code
    | italic
    | strikethrough
    | link
    | bracketed
    | parenthesized;

blockquote
    : ( OpenBlockQuote | ParaBlockquoteHead ) BlockQuotePrologue whitespace?
        paragraph;

unorderedList
    : head=unorderedListItemHead unorderedListItem+;

unorderedListItemHead
    : head=NestedListItemHead? ListPrologue;

unorderedListItem
    : head=unorderedListItemHead? paragraph;

paragraphs
    : paragraph+;

paragraph
    : ParaNewLine?? ParaInlineWhitespace?? innerContent+? paraBreak?;

paraBreak
    : ParaBreak;

link
    : linkText=bracketed href=linkTarget;

bold
    : ParaBold innerContent ParaBold;

code
    : ParaCode innerContent ParaCode;

italic
    : ParaItalic innerContent ParaItalic;

strikethrough
    : ParaStrikethrough innerContent ParaStrikethrough;

innerContent
    : content+ ( whitespace content )*;

text
    : phrase (( ParaInlineWhitespace | ParaNewline )? ( phrase ))*
        whitespace?;

phrase
    : ( ParaWords ParaInlineWhitespace? )( ParaWords ParaInlineWhitespace? )*;

whitespace
    : ParaInlineWhitespace;

linkTarget
    : ParaOpenParen href=ParaLink ParaCloseParen;

bracketed
    : ParaBracketOpen innerContent ParaBracketClose;

parenthesized
    : ParaOpenParen innerContent ParaCloseParen;