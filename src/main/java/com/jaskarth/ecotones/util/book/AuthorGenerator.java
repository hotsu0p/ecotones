package com.jaskarth.ecotones.util.book;

import net.minecraft.util.Formatting;

import java.util.Random;

public final class AuthorGenerator {
    public static String generate(Random random) {
        if (random.nextInt(2000) == 0) {
            return Formatting.FORMATTING_CODE_PREFIX + Formatting.OBFUSCATED.getCode() + "ecotones";
        }

        if (random.nextInt(8) == 0) {
            return "an unintelligible name";
        }

        return "an unknown author";
    }
}
