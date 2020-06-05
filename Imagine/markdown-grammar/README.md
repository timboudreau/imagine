# Markdown Grammar

This is a simple Antlr grammar and supporting classes for parsing 
[Markdown](https://daringfireball.net/projects/markdown/syntax), which is used
to generate help content from Java annotations.  

This project does not pretend or even attempt to support the
full Markdown syntax, just those features that are needed for creating help files for this
project, as they become needed.

Having done a review of available Markdown Antlr grammars available, everything
I found was either, slow, awful or both.  So, when in doubt, make the wheel _rounder_.

While it's not the end-all and be-all of Markdown parsers, it is sufficient for its
current uses, and perhaps the bones of what could become a really _good_ Markdown parser.

## Currently supported:

 * Paragraphs
 * Unordered Lists, with nesting
 * Ordered Lists, with nesting
 * Boldface / strong (asterisk)
 * Strikethrough
 * Italic / emphasis (underscore)
 * Code blocks
 * Preformatted text
 * Hyperlinks
 * Embedded Images

## Currently unsupported:

 * H1 and H2 headings using the underline style
 * Nested blockquotes
 * Unusual mixes of text and uncommon punctuation, particularly punctuation that has semantic meaning in Markdown used as the leading character in a word
 * Multi-paragraph list items
 * Tables
 * Code blocks within list items
 * Reference style links
 * Link definitions
 * Escaping (asterisks, backticks, etc.)

Some of these would likely be trivial to implement.

## Never to be supported:

 * Inline HTML - we're not writing a browser here



# UI Support

The project include UI components for parsing and rendering markdown content
_directly to the screen_, with no intervening heavyweight browser or other technologies - 
just parse and give painting instructions to a `Graphics2D`.  There's a bit of flexibility
around colors and fonts via `MarkdownUIProperties`.  `MarkdownComponent` encapsulates all
of this as a simple Swing component.  Under the hood, computing preferred sizes means
rendering a `Markdown` instance once passing a `PrerenderContext` which doesn't really
write to a `Graphics2D`, just computes the bounds it wants to paint, constrained by a
rectangle you provide, or the `GraphicsEnvironment`'s screen bounds.

# Lexer and Grammar Design

Markdown, as with many formats for human, not computer languages, is hard for 
machines to parse, and subject to ambiguity.

The grammar here takes advantage of Antlr's *lexical modes* - these are typically used for
what the Terence Parr's book calls _island grammars_ - nested languages within a language.

Here, we are using them a little bit differently.  The `mode`, `pushMode` and `popMode` lexer
rule directives allow one to switch out what set of rules the lexer is using to recognize
text. In general, the things that Markdown does involve one of two patterns: `$DELIMITER some lines of text`
(such as a heading or list item) or `$DELIMITER some text $DELIMITER` (such as boldface).

Lexers don't do nesting, they just tokenize.  Parsers do do nesting.

For the second category, bi-delimited stuff, we can handle that easily in the parser - just
have a token for the delimiter and a rule that nests anything between those delimiters.

For the first category, that's where lexical modes come in handy.

So, in our grammar, almost _nothing_ is handled in the default mode.  Any lexer rule in another
mode that hands control back to the default mode must do so at the end of a line of text, so the
default mode _only_ deals with the first characters on a line, and only to decide what mode
to enter - it just hands off control to the appropriate mode - a leading `POUND WHITESPACE`
means go into heading mode, a leading `WHITESPACE ASTRISK WHITESPACE` means go into _LIST_
list mode, a leading `WHITESPACE+ DIGIT DOT WHITESPACE` means go into _ORDERED_LIST_ mode,
and other text means go into _PARAGRAPH_ mode (where almost all text is processed).

The `more` directive for lexer rules is the key here:  You can parse some text in lexer rule
_A_, realize you need to go into a different mode, and _hand the text you already parsed into
the first rule in that mode_. Now, the only caveat to this is that you can get some surprises - 
like characters in your whitespace tokens, because you passed those characters into a mode,
and it just blindly prepended them to the whitespace that was the first thing recognized.  So
that's something to be aware of when making modifications.

But modes mean we can have unique named lexer rules that say "this is the beginning of a list"
which the parser grammar can recognize as the start of a rule.

So a lot of this involves flipping between modes, often using the `more` keyword to pass in everything
that got recognized.  This allows us to have token rules that are essentially identical, but have
different names which the parser can use to identify what parser rule should be activated.

For a simple example:

```
OpenBulletList
    : INLINE_WHITESPACE+ ASTERISK -> more, pushMode ( LIST );
```

in the default mode, tells the lexer it is seeing the opening of a bullet list.  In fact, this
rule will _only ever be used if the document starts with a bullet list_.

In the `PARAGRAPH` mode (which does the textual heavy lifting for everything except headings and
preformatted text), is a very simiilar rule - and since most things land in paragraph mode after
processing some sort of prefix, this will almost always be the rule that initiates a list:

```
ParaTransitionToBulletListItem
    : ( ParaDoubleNewline | NEWLINE ) INLINE_WHITESPACE+ ASTERISK -> more,
        mode ( LIST );
```

Using `more` allows the leading spaces-asterisk sequence get re-processed in list mode as two
tokens:

```
ListPrologue
    : ( DOUBLE_NEWLINE? INLINE_WHITESPACE )
    | ( ParaDoubleNewline | NEWLINE )? INLINE_WHITESPACE+ ASTERISK
        INLINE_WHITESPACE;

ListItem
    : ( LETTER | DIGIT ) -> more, pushMode ( PARAGRAPH );
```

and the parser recognizes the `ListPrologue` token as introducing another list item.  So the 
entirety of the parser rule for lists is:

```
unorderedList
    : head=unorderedListItemHead unorderedListItem+;

unorderedListItemHead
    : head=NestedListItemHead? ListPrologue;

unorderedListItem
    : head=unorderedListItemHead? paragraph;
```

In general the pattern is:

 1. Top level lexer rule matches a character pattern that begins a line and indicates a particular kind of markup
 2. That rule dumps the lexer into a mode specific for that type of markup (horizontal rule, ordered list, unordered list, paragraph), usually using `more` to hand the matched text into the new lexer mode
 3. The mode has a specific named prologue token that the parser can use for matching
 4. In most cases, the mode matches the prologue, then dumps the lexer into `PARAGRAPH` mode which handles text, and is the main thing involved in rendering
 5. For things that repeat, such as list items, `PARAGRAPH` mode has similar rules that dump the lexer back into the mode that spawned it, for repeating items
 6. Terminating a paragraph where no other mode should be entered dumps the parser back out into default mode

There's a little bit of lexical predicate black magic to facilitate ordered list items and detect changes 
in indent levels as signals to open or close a sublist.  But other than that, once you get the hang of thinking
about Antlr grammars this way (using lexical modes not for island grammars, but as a way to have a bunch of tokens that
match the same, or nearly the same thing, in ways the parser can differentiate without doing anything terribly exciting),
it works pretty well - and far less torturously than some other grammars I've seen.
