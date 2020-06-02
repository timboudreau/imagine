parser grammar Nl;

options{tokenVocab=NlTest; }

// Example rules
document
    : ( paragraphs 
        whitespace )+ EOF?;

horizontalRule
    : HorizontalRule
    | HorizontalRuleTail;
//
//heading
//    : head=OpenHeading body=headingContent HeadingClose;
//
//headingContent
//    : HeadingContent;

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

//unorderedList
//    : head=unorderedListItemHead unorderedListItem+;
//
//unorderedListItemHead
//    : head=NestedListItemHead? ListPrologue;
//
//unorderedListItem
//    : head=unorderedListItemHead? paragraph;
//
paragraphs
    : paragraph+;

paragraph
    : ParaInlineWhitespace?? innerContent+? paraBreak?;

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
    : phrase (( ParaInlineWhitespace | NEWLINE )? ( phrase ))*
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
