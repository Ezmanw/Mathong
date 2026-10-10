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

    val all: List<Layout> = listOf(
        pocket, meadow, pinwheel, pyramid, cross, bridge, heart, hourglass,
        arena, stairs, fortress, butterfly, turtle, ziggurat,
    )

    fun byId(id: String): Layout = all.firstOrNull { it.id == id } ?: turtle
}
