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
        "STAY. STAY. STAY.",
        "I FELL FROM THE CEILING",
        "THE DOOR IS STILL OPEN",
        "I WAS A HUNTER BEFORE",
        "YOU MOVE LIKE PREY",
        "THE FLOORBOARDS KNOW",
        "I USED TO SMELL LIKE SMOKE",
        "THERE IS A SECOND ME",
        "KEEP RUNNING. KEEP. RUNNING.",
        "I PUT MYSELF BACK TOGETHER",
        "THE HOLE WANTS YOUR FACE",
        "WE ARE ALL THE SAME DOWN HERE",
        "WHEN I CATCH YOU I WILL BE SO QUIET",
        "THIS BODY IS BORROWED",
        "I DREAM OF YOUR FURNITURE",
        "THE WALLS HAVE EYES LIKE MINE",
        "I AM ALMOST THERE",
        "ONE MORE STEP AND IT IS OVER",
        "YOU WILL LEARN TO SLEEP SITTING UP",
        "THAT IS NOT MY HAND IN THE DOOR",
        "I PRESS MYSELF INTO SMALL SPACES",
        "WE DO NOT SURVIVE. WE REPEAT.",
        "IT IS A SMALL THING. JUST STAY."
    );

    public static String randomLine() {
        return LINES.get(RNG.nextInt(LINES.size()));
    }

    public static List<String> allLines() {
        return LINES;
    }
}