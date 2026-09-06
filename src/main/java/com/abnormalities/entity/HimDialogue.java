package com.abnormalities.entity;

import java.util.List;
import java.util.Random;

public final class HimDialogue {
    private static final Random RNG = new Random();

    private static final List<String> LINES = List.of(
        "I WILL GET MY REVENGE",
        "WE WILL HANDLE THIS",
        "YOU WILL HOLD STILL",
        "I COUNTED THE STEPS",
        "DO NOT LOOK AWAY",
        "THE HOUSE REMEMBERS",
        "I AM RUNNING FOREVER",
        "I NEVER FALL",
        "THE DOOR IS STILL OPEN",
        "I WAS A HUNTER BEFORE",
        "YOU MOVE LIKE PREY",
        "THE FLOORBOARDS HEAR",
        "I USED TO HAVE A SOUL",
        "I NEVER END",
        "WE WANT YOUR FACE",
        "WE ARE ALL THE SAME DOWN HERE",
        "WE WILL CATCH YOU",
        "YOUR SOUL HAS BEEN NOTED",
        "I DREAM OF YOUR SOUL",
        "THE WALLS HAVE EYES",
        "I AM ALMOST THERE",
        "ONE MORE STEP AND IT IS OVER",
        "YOU WILL LEARN",
        "THEY WILL NOT BE HAPPY",
        "IT'S ETERNITY IN THERE",
        "LONGER THAN YOU THINK"
    );

    public static String randomLine() {
        return LINES.get(RNG.nextInt(LINES.size()));
    }

    public static List<String> allLines() {
        return LINES;
    }
}