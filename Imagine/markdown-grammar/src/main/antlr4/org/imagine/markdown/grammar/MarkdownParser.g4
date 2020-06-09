parser grammar MarkdownParser;

options { tokenVocab = MarkdownLexer; }

 document
    : ( heading | blockquote | unorderedList | orderedList | paragraphs |
        horizontalRule | whitespace | embeddedImage | preformatted )+ EOF?;

horizontalRule
    : ( HorizontalRule HorizontalRuleTail )
    | HorizontalRuleTail+;

heading
    : head=OpenHeading HeadingPrologue body=headingContent whitespace?
        HeadingClose;

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

orderedList
    : ( firstOrderedListItem orderedListItem* ( orderedList ( returningOrderedListItem
        orderedListItem* )* ))
    | ( firstOrderedListItem orderedListItem* )
    | orderedListItem ( firstOrderedListItem orderedListItem* ( orderedList ( returningOrderedListItem
        orderedListItem* )* ));

//    : firstOrderedListItem (orderedList | orderedListItem)*? (firstOrderedListItem (orderedList | orderedListItem)*)*;
orderedListItemHead
    : head=NestedListItemHead? OrderedListPrologue;

firstOrderedListItem
    : head=firstOrderedListItemHead paragraph;

firstOrderedListItemHead
    : NestedOrderedListPrologue;

returningOrderedListItem
    : head=returningOrderedListItemHead paragraph;

returningOrderedListItemHead
    : ReturningOrderedListPrologue;

orderedListItem
    : head=orderedListItemHead paragraph;

unorderedList
    : ( firstUnorderedListItem unorderedListItem* ( unorderedList ( returningUnorderedListItem
        unorderedListItem* )* ))
    | ( firstUnorderedListItem unorderedListItem* )
    | unorderedListItem ( firstUnorderedListItem unorderedListItem* ( unorderedList
        ( returningUnorderedListItem unorderedListItem* )* ));

firstUnorderedListItem
    : head=firstUnorderedListItemHead paragraph;

firstUnorderedListItemHead
    : NestedListPrologue | ListPrologue;

returningUnorderedListItem
    : head=returningUnorderedListItemHead paragraph;

returningUnorderedListItemHead
    : ReturningListPrologue;

unorderedListItemHead
    : head=NestedListItemHead? ListPrologue;

unorderedListItem
    : head=unorderedListItemHead? paragraph;

preformatted
    : OpenPreformattedText body=PreformattedContent ClosePreformattedContent;

paragraphs
    : paragraph+;

paragraph
    : whitespace?? innerContent+ ( whitespace innerContent )*? paraBreak?;

paraBreak
    : ParaBreak;

link
    : linkText=bracketed href=linkTarget;

embeddedImage
    : ParaBangBracket linkText=innerContent ParaBracketClose whitespace? href=linkTarget;

bold
    : ParaBold innerContent ParaBold;

code
    : ParaCode innerContent ParaCode;

italic
    : ParaItalic innerContent ParaItalic;

strikethrough
    : ParaStrikethrough innerContent ParaStrikethrough;

innerContent
    : ( content | embeddedImage )+? ( whitespace ( content | embeddedImage ))*? whitespace?;

text
    : ( words+ whitespace* )+?;

//    : ( words whitespace? )+ (whitespace? ( ( words whitespace? )+ ))*? whitespace?;
words
    : ParaWords;

whitespace
    : ParaInlineWhitespace
    | ParaNewline
    | Whitespace;

linkTarget
    : ParaOpenParen href=ParaLink ParaCloseParen;

bracketed
    : ParaBracketOpen innerContent? ParaBracketClose;

parenthesized
    : ParaOpenParen innerContent? ParaCloseParen;