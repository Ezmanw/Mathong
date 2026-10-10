package com.ezmanw.mathong.game

enum class Suit { DOTS, BAMBOO, CHARACTERS, WIND, DRAGON, FLOWER, SEASON }

/**
 * A tile face. [id] runs 0..41:
 * 0-8 dots, 9-17 bamboo, 18-26 characters, 27-30 winds (E S W N),
 * 31-33 dragons (red, green, white), 34-37 flowers, 38-41 seasons.
 */
@JvmInline
value class TileKind(val id: Int) {
    val suit: Suit
        get() = when (id) {
            in 0..8 -> Suit.DOTS
            in 9..17 -> Suit.BAMBOO
            in 18..26 -> Suit.CHARACTERS
            in 27..30 -> Suit.WIND
            in 31..33 -> Suit.DRAGON
            in 34..37 -> Suit.FLOWER
            else -> Suit.SEASON
        }

    /** 1-based rank inside the suit. */
    val rank: Int
        get() = when (suit) {
            Suit.DOTS -> id + 1
            Suit.BAMBOO -> id - 8
            Suit.CHARACTERS -> id - 17
            Suit.WIND -> id - 26
            Suit.DRAGON -> id - 30
            Suit.FLOWER -> id - 33
            Suit.SEASON -> id - 37
        }

    /** Tiles with the same match group can be paired (all flowers match, all seasons match). */
    val matchGroup: Int
        get() = when (suit) {
            Suit.FLOWER -> 34
            Suit.SEASON -> 38
            else -> id
        }

    fun matches(other: TileKind) = matchGroup == other.matchGroup

    val displayName: String
        get() = when (suit) {
            Suit.DOTS -> "$rank of Dots"
            Suit.BAMBOO -> "$rank of Bamboo"
            Suit.CHARACTERS -> "$rank of Characters"
            Suit.WIND -> listOf("East", "South", "West", "North")[rank - 1] + " Wind"
            Suit.DRAGON -> listOf("Red", "Green", "White")[rank - 1] + " Dragon"
            Suit.FLOWER -> listOf("Plum", "Orchid", "Chrysanthemum", "Bamboo")[rank - 1] + " Flower"
            Suit.SEASON -> listOf("Spring", "Summer", "Autumn", "Winter")[rank - 1]
        }

    companion object {
        const val COUNT = 42

        /** All 72 matching pairs of a full 144 tile set. */
        fun fullSetPairs(): List<Pair<TileKind, TileKind>> = buildList {
            for (id in 0 until 34) {
                repeat(2) { add(TileKind(id) to TileKind(id)) }
            }
            add(TileKind(34) to TileKind(35)); add(TileKind(36) to TileKind(37))
            add(TileKind(38) to TileKind(39)); add(TileKind(40) to TileKind(41))
        }
    }
}
