package dev.plumfire.angelscript.lang

/** Keyword lists from the AngelScript manual, page "Reserved keywords". */
internal object AngelScriptKeywords {
    val CONTROL = words("if else for foreach while do switch case default break continue return try catch")

    val RESERVED =
        words(
            """
            and auto bool break case cast catch class const continue default do double else enum false float for
            foreach funcdef if import in inout int interface int8 int16 int32 int64 is mixin namespace not null or
            out private protected return switch true try typedef uint uint8 uint16 uint32 uint64 using void while xor
            """,
        )

    val CONTEXTUAL =
        words("abstract delete explicit external final from function get override property set shared super this")

    /** Contextual keywords that are common identifier names; the server colors them where they act as keywords. */
    val LEXED_AS_IDENTIFIERS = words("get set from")

    val KEYWORDS = RESERVED + CONTEXTUAL - CONTROL - LEXED_AS_IDENTIFIERS

    /** Longest first, so that the first match is the longest one. */
    val OPERATORS =
        words(
            """
            >>>= **= <<= >>= >>> ... ** <= >= == != += -= *= /= %= ++ -- << >> &= |= ^= && || ^^ ::
            * / % + - < > ? = & | ^ ~ ! @
            """,
        ).sortedByDescending { it.length }

    private fun words(text: String): Set<String> = text.trim().split(Regex("\\s+")).toSet()
}
