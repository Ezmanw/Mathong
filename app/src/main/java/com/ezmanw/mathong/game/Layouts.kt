package com.ezmanw.mathong.game

object Layouts {

    val turtle = layout("turtle", "Turtle", Difficulty.HARD) {
        row(1.0, 0.0, 12, 0)
        row(3.0, 1.0, 8, 0)
        row(2.0, 2.0, 10, 0)
        row(1.0, 3.0, 12, 0)
        row(1.0, 4.0, 12, 0)
        row(2.0, 5.0, 10, 0)
        row(3.0, 6.0, 8, 0)
        row(1.0, 7.0, 12, 0)
        tile(0.0, 3.5, 0); tile(13.0, 3.5, 0); tile(14.0, 3.5, 0)
        rect(4.0, 1.0, 6, 6, 1)
        rect(5.0, 2.0, 4, 4, 2)
        rect(6.0, 3.0, 2, 2, 3)
        tile(6.5, 3.5, 4)
    }

    val pyramid = layout("pyramid", "Pyramid", Difficulty.MEDIUM) {
        rect(0.0, 0.0, 8, 8, 0)
        rect(1.0, 1.0, 6, 6, 1)
        rect(2.0, 2.0, 4, 4, 2)
        rect(3.0, 3.0, 2, 2, 3)
    }

    val pocket = layout("pocket", "Pocket", Difficulty.EASY) {
        rect(0.0, 0.0, 6, 4, 0)
        rect(1.0, 1.0, 4, 2, 1)
    }

    val meadow = layout("meadow", "Meadow", Difficulty.EASY) {
        rect(0.0, 0.0, 8, 5, 0)
        rect(1.0, 1.0, 6, 3, 1)
        rect(2.0, 2.0, 4, 1, 2)
    }

    val cross = layout("cross", "Cross", Difficulty.MEDIUM) {
        rect(3.0, 0.0, 4, 10, 0)
        rect(0.0, 3.0, 3, 4, 0); rect(7.0, 3.0, 3, 4, 0)
        rect(4.0, 1.0, 2, 8, 1)
        rect(1.0, 4.0, 3, 2, 1); rect(6.0, 4.0, 3, 2, 1)
        rect(4.0, 4.0, 2, 2, 2)
        tile(4.5, 4.5, 3); tile(4.5, 1.0, 2)
    }

    val fortress = layout("fortress", "Fortress", Difficulty.HARD) {
        frame(0.0, 0.0, 12, 8, 0)
        rect(2.0, 2.0, 8, 4, 0)
        for ((x, y) in listOf(0.0 to 0.0, 10.0 to 0.0, 0.0 to 6.0, 10.0 to 6.0)) {
            rect(x, y, 2, 2, 1)
            rect(x, y, 2, 2, 2)
        }
        rect(3.0, 3.0, 6, 2, 1)
        rect(4.0, 3.0, 4, 2, 2)
    }

    val bridge = layout("bridge", "Bridge", Difficulty.MEDIUM) {
        rect(0.0, 2.0, 14, 3, 0)
        rect(1.0, 2.0, 12, 3, 1)
        rect(3.0, 2.0, 8, 3, 2)
        rect(5.0, 2.0, 4, 3, 3)
        col(0.0, 0.0, 2, 0); col(13.0, 0.0, 2, 0)
        col(0.0, 5.0, 2, 0); col(13.0, 5.0, 2, 0)
    }

    val butterfly = layout("butterfly", "Butterfly", Difficulty.HARD) {
        grid(
            0, 0.0, 0.0,
            "##....#....##",
            "####..#..####",
            "#####.#.#####",
            "######.######",
            ".#####.#####.",
            "..####.####..",
            ".#####.#####.",
            "######.######",
            "#####...#####",
            "####.....####",
        )
        col(6.0, 3.0, 5, 0)
        rect(2.0, 2.0, 3, 2, 1); rect(8.0, 2.0, 3, 2, 1)
        rect(2.0, 6.0, 3, 2, 1); rect(8.0, 6.0, 3, 2, 1)
        col(6.0, 3.5, 4, 1)
    }

    val heart = layout("heart", "Heart", Difficulty.MEDIUM) {
        grid(
            0, 0.0, 0.0,
            ".###...###.",
            "#####.#####",
            "###########",
            "###########",
            ".#########.",
            "..#######..",
            "...#####...",
            "....###....",
            ".....#.....",
        )
        rect(2.0, 2.0, 7, 3, 1)
    }

    val hourglass = layout("hourglass", "Hourglass", Difficulty.MEDIUM) {
        val widths = listOf(10, 8, 6, 4, 2, 4, 6, 8, 10)
        widths.forEachIndexed { y, w -> row((10 - w) / 2.0, y.toDouble(), w, 0) }
        widths.forEachIndexed { y, w -> if (w > 2) row((10 - w) / 2.0 + 1, y.toDouble(), w - 2, 1) }
    }

    val arena = layout("arena", "Arena", Difficulty.MEDIUM) {
        frame(0.0, 0.0, 12, 10, 0)
        frame(1.0, 1.0, 10, 8, 1)
        frame(2.0, 2.0, 8, 6, 0)
        rect(4.0, 4.0, 4, 2, 0)
        rect(4.0, 4.0, 4, 2, 1)
    }

    val stairs = layout("stairs", "Stairs", Difficulty.MEDIUM) {
        rect(0.0, 0.0, 10, 4, 0)
        rect(2.0, 0.0, 8, 4, 1)
        rect(4.0, 0.0, 6, 4, 2)
        rect(6.0, 0.0, 4, 4, 3)
        rect(8.0, 0.0, 2, 4, 4)
    }

    val pinwheel = layout("pinwheel", "Pinwheel", Difficulty.EASY) {
        rect(0.0, 0.0, 5, 2, 0)
        rect(5.0, 0.0, 2, 5, 0)
        rect(2.0, 5.0, 5, 2, 0)
        rect(0.0, 2.0, 2, 5, 0)
        rect(2.0, 2.0, 3, 3, 0)
        tile(3.0, 3.0, 1)
        row(0.5, 0.5, 4, 1)
        col(5.5, 0.5, 4, 1)
        row(2.5, 5.5, 4, 1)
        col(0.5, 2.5, 4, 1)
    }

    val ziggurat = layout("ziggurat", "Ziggurat", Difficulty.EXPERT) {
        rect(0.0, 0.0, 6, 6, 0)
        rect(0.5, 0.5, 5, 5, 1)
        rect(1.0, 1.0, 4, 4, 2)
        rect(1.5, 1.5, 3, 3, 3)
        rect(2.0, 2.0, 2, 2, 4)
    }

    val diamond = layout("diamond", "Diamond", Difficulty.MEDIUM) {
        val w0 = listOf(2, 4, 6, 8, 10, 8, 6, 4, 2)
        w0.forEachIndexed { y, w -> row((10 - w) / 2.0, y.toDouble(), w, 0) }
        val w1 = listOf(2, 4, 6, 4, 2)
        w1.forEachIndexed { y, w -> row((10 - w) / 2.0, y + 2.0, w, 1) }
        row(4.0, 4.0, 2, 2)
    }

    val spider = layout("spider", "Spider", Difficulty.HARD) {
        rect(5.0, 3.0, 4, 4, 0)
        rect(5.5, 3.5, 3, 3, 1)
        tile(6.5, 4.5, 2)
        for ((x, y) in listOf(4.0 to 2.0, 3.0 to 1.0, 2.0 to 0.0, 4.0 to 7.0, 3.0 to 8.0, 2.0 to 9.0)) {
            tile(x, y, 0); tile(13.0 - x, y, 0)
        }
        row(0.0, 4.0, 5, 0); row(9.0, 4.0, 5, 0)
        row(0.0, 5.0, 5, 0); row(9.0, 5.0, 5, 0)
    }

    val castle = layout("castle", "Castle", Difficulty.HARD) {
        grid(
            0, 0.0, 0.0,
            "##.##.##.##",
            "###########",
            ".#########.",
            ".#########.",
            ".#########.",
            ".#########.",
            "###########",
        )
        rect(2.0, 2.0, 7, 4, 1)
        rect(3.0, 3.0, 5, 2, 2)
        tile(0.5, 0.0, 1); tile(9.5, 0.0, 1)
        tile(0.5, 0.0, 2); tile(9.5, 0.0, 2)
    }

    val crab = layout("crab", "Crab", Difficulty.MEDIUM) {
        grid(
            0, 0.0, 0.0,
            "#.......#",
            "##.....##",
            ".##...##.",
            "..#####..",
            ".#######.",
            "#########",
            ".#######.",
            "..#.#.#..",
        )
        rect(2.0, 4.0, 5, 2, 1)
        row(3.0, 4.5, 3, 2)
    }

    val temple = layout("temple", "Temple", Difficulty.EXPERT) {
        rect(0.0, 0.0, 10, 6, 0)
        rect(1.0, 0.5, 8, 5, 1)
        rect(2.0, 1.0, 6, 4, 2)
        rect(3.0, 1.5, 4, 3, 3)
        rect(4.0, 2.0, 2, 2, 4)
    }

    val checkers = layout("checkers", "Checkers", Difficulty.MEDIUM) {
        rect(0.0, 0.0, 10, 7, 0)
        for (r in listOf(1, 3, 5)) {
            val start = if (r % 4 == 1) 1 else 2
            for (x in start..8 step 2) tile(x.toDouble(), r.toDouble(), 1)
        }
    }

    val greatWall = layout("wall", "Great Wall", Difficulty.MEDIUM) {
        rect(0.0, 0.0, 10, 3, 0)
        row(0.5, 0.0, 9, 1); row(0.5, 1.0, 9, 1); row(0.5, 2.0, 9, 1)
        row(1.0, 1.0, 9, 2)
    }

    val cat = layout("cat", "Cat", Difficulty.EASY) {
        grid(
            0, 0.0, 0.0,
            "#.....#",
            "##...##",
            "#######",
            "#.#.#.#",
            "#######",
            ".#####.",
            "..###..",
        )
        row(1.5, 4.0, 4, 1)
        col(7.0, 5.0, 2, 0)
    }

    val all: List<Layout> = listOf(
        pocket, cat, meadow, pinwheel, pyramid, diamond, cross, bridge, heart, hourglass, crab,
        checkers, greatWall, arena, stairs, fortress, spider, castle, butterfly, turtle, temple, ziggurat,
    )

    fun byId(id: String): Layout = all.firstOrNull { it.id == id } ?: turtle
}
