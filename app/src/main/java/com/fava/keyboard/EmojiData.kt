package com.fava.keyboard

/**
 * A fixed, hand-picked emoji set for the Gboard-style emoji picker panel
 * (app 0.3.3) — not generated from fava-standard/layout.yaml, since emoji
 * aren't part of the Persian script this project is otherwise about.
 * Deliberately restricted to single-codepoint, default-emoji-presentation
 * characters (all from the astral U+1Fxxx planes, entered here as literal
 * Kotlin string characters) — no variation selectors (U+FE0F), no ZWJ
 * sequences, no flags, so every entry renders consistently without
 * needing a presentation-selector fallback.
 */
data class EmojiCategory(val icon: String, val emoji: List<String>)

val EMOJI_CATEGORIES: List<EmojiCategory> = listOf(
    EmojiCategory(
        "😀",
        listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
            "🙂", "😉", "😍", "😘", "😋", "😛", "🤪", "🧐", "😎", "🥳",
            "😢", "😭", "😡", "😱", "🥺", "😴",
        ),
    ),
    EmojiCategory(
        "👍",
        listOf(
            "👍", "👎", "👏", "🙌", "🙏", "👋", "🤝", "💪", "🤞", "👊",
            "🤙", "👆", "👇", "👈", "👉", "🤘", "👌", "🤛", "🤜", "👐",
        ),
    ),
    EmojiCategory(
        "💜",
        listOf(
            "💙", "💚", "💜", "🧡", "💛", "🖤", "🤍", "🤎", "💔", "💕",
            "💞", "💓", "💗", "💖", "🌟", "🔥", "💯", "💢", "💥", "💫",
        ),
    ),
    EmojiCategory(
        "🐶",
        listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯",
            "🦁", "🐮", "🐷", "🐸", "🐵", "🙈", "🐔", "🐧", "🦆", "🦉",
            "🐺", "🐴", "🦋", "🐢", "🐍", "🐙",
        ),
    ),
    EmojiCategory(
        "🍕",
        listOf(
            "🍏", "🍎", "🍌", "🍉", "🍇", "🍓", "🍒", "🍍", "🥝", "🍅",
            "🥑", "🍕", "🍔", "🍟", "🌭", "🍿", "🍩", "🍪", "🎂", "🍰",
            "🍫", "🍬", "🍭", "🍺", "🍷",
        ),
    ),
    EmojiCategory(
        "🎮",
        listOf(
            "🏀", "🏈", "🎾", "🎳", "🎮", "🎲", "🎸", "🎧", "🎤", "🎨",
            "🚗", "🚕", "🚌", "🚲", "🚀", "🚁", "🚤", "🏖", "🗺", "🎡",
        ),
    ),
    EmojiCategory(
        "💡",
        listOf(
            "💡", "🔑", "🔒", "🔓", "📱", "💻", "📷", "🎁", "📚", "📝",
            "📌", "💰", "💎", "🎵", "🎉", "🏆", "🎯", "📅", "🔔", "💧",
        ),
    ),
)
