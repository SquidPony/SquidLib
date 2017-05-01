package squidpony.examples;

import squidpony.FakeLanguageGen;
import squidpony.NaturalLanguageCipher;
import squidpony.squidmath.CrossHash;
import squidpony.squidmath.StatefulRNG;

import java.util.HashMap;

/**
 * Created by Tommy Ettinger on 11/29/2015.
 */
public class LanguageGenTest {
    public static void main(String[] args)
    {
        StatefulRNG rng = new StatefulRNG(0xf00df00L);
        FakeLanguageGen flg = FakeLanguageGen.ENGLISH;

        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 10, new String[]{",", ",", ",", ";"},
                    new String[]{".", ".", ".", "!", "?", "..."}, 0.17));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.LOVECRAFT;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 3, 9, new String[]{",", ",", ";"},
                    new String[]{".", ".", "!", "!", "?", "...", "..."}, 0.15));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.GREEK_ROMANIZED;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 11, new String[]{",", ",", ";"},
                    new String[]{".", ".", ".", "!", "?", "..."}, 0.2));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.GREEK_AUTHENTIC;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 11, new String[]{",", ",", ";"},
                    new String[]{".", ".", ".", "!", "?", "..."}, 0.2));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.FRENCH;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 4, 12, new String[]{",", ",", ",", ";", ";"},
                    new String[]{".", ".", ".", "!", "?", "..."}, 0.17));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.RUSSIAN_ROMANIZED;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 6, 13, new String[]{",", ",", ",", ",", ";", " -"},
                    new String[]{".", ".", ".", "!", "?", "..."}, 0.25));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.RUSSIAN_AUTHENTIC;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 6, 13, new String[]{",", ",", ",", ",", ";", " -"},
                    new String[]{".", ".", ".", "!", "?", "..."}, 0.25));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.JAPANESE_ROMANIZED;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 13, new String[]{",", ",", ",", ",", ";"},
                    new String[]{".", ".", ".", "!", "?", "...", "..."}, 0.12));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.SWAHILI;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 4, 9, new String[]{",", ",", ",", ";", ";"},
                    new String[]{".", ".", ".", "!", "?"}, 0.12));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.SOMALI;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 4, 9, new String[]{",", ",", ",", ";", ";"},
                    new String[]{".", ".", ".", "!", "?"}, 0.12));
        }

        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.HINDI_ROMANIZED;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 4, 9, new String[]{",", ",", ",", ";", ";"},
                    new String[]{".", ".", ".", "!", "?"}, 0.12));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.ARABIC_ROMANIZED;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 6, 9, new String[]{",", ",", ",", ";", ";"},
                    new String[]{".", ".", ".", "!", "?"}, 0.18));
        }

        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.INUKTITUT;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 12, new String[]{",", ",", ";"},
                    new String[]{".", ".", "!", "?", "..."}, 0.15));
        }

        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.NORSE;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 12, new String[]{",", ",", ";"},
                    new String[]{".", ".", "!", "?", "..."}, 0.15));
        }

        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.NORSE.addModifiers(FakeLanguageGen.Modifier.SIMPLIFY_NORSE);
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 12, new String[]{",", ",", ";"},
                    new String[]{".", ".", "!", "?", "..."}, 0.15));
        }

        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.NAHUATL;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 10, new String[]{",", ",", ";"},
                    new String[]{".", ".", "!", "?", "..."}, 0.1));
        }

        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.MONGOLIAN;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 9, new String[]{",", ",", ";", ",", " -"},
                    new String[]{".", ".", ".", ".", "!", "?", "..."}, 0.16));
        }
        System.out.println("\n\nLANGUAGE MIXES:\n\n");

        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.ENGLISH.mix(FakeLanguageGen.FRENCH.removeAccents(), 0.5);
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 11, new String[]{",", ",", ";"},
                    new String[]{".", ".", "!", "?", "..."}, 0.18));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.RUSSIAN_ROMANIZED.mix(FakeLanguageGen.ENGLISH, 0.35);
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 4, 10, new String[]{",", ",", ",", ",", ";", " -"},
                    new String[]{".", ".", ".", "!", "?", "..."}, 0.22));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.FRENCH.mix(FakeLanguageGen.GREEK_ROMANIZED, 0.55);
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 6, 12, new String[]{",", ",", ",", ";"},
                    new String[]{".", ".", "!", "?", "..."}, 0.22));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.ENGLISH.mix(FakeLanguageGen.GREEK_AUTHENTIC, 0.25);
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 11, new String[]{",", ",", ";"},
                    new String[]{".", ".", "!", "?", "..."}, 0.18));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.ENGLISH.addAccents(0.5, 0.15);
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 12, new String[]{",", ",", ";"},
                    new String[]{".", ".", "!", "?", "..."}, 0.18));
        }

        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.FRENCH.mix(FakeLanguageGen.JAPANESE_ROMANIZED, 0.65);
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 6, 12, new String[]{",", ",", ",", ";"},
                    new String[]{".", ".", "!", "?", "...", "..."}, 0.17));
        }

        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.RUSSIAN_ROMANIZED.mix(FakeLanguageGen.JAPANESE_ROMANIZED, 0.75);
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 6, 12, new String[]{",", ",", ",", ";", " -"},
                    new String[]{".", ".", ".", "!", "?", "...", "..."}, 0.2));
        }

        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.ENGLISH.addModifiers(FakeLanguageGen.Modifier.NO_DOUBLES);
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 12, new String[]{",", ",", ";"},
                    new String[]{".", ".", "!", "?", "..."}, 0.18));
        }

        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.JAPANESE_ROMANIZED.addModifiers(FakeLanguageGen.Modifier.DOUBLE_CONSONANTS);
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 12, new String[]{",", ",", ";"},
                    new String[]{".", ".", "!", "?", "..."}, 0.18));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.SOMALI.mix(FakeLanguageGen.JAPANESE_ROMANIZED, 0.3).mix(FakeLanguageGen.SWAHILI, 0.1);
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 12, new String[]{",", ",", ";"},
                    new String[]{".", ".", "!", "?", "..."}, 0.15));
        }

        System.out.println("\n\nFANTASY GROUPS:\n\n");
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.GOBLIN;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 4, 8, new String[]{",", ",", ";"},
                    new String[]{".", ".", ".", ".", "?", "...", "..."}, 0.08));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.ELF;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 10, new String[]{",", ",", ";"},
                    new String[]{".", ".", ".", "?", "?", "...", "..."}, 0.18));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.DEMONIC;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 10, new String[]{",", ",", ";"},
                    new String[]{".", ".", "!", "!", "!", "..."}, 0.07));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.INFERNAL;
        for (int i = 0; i < 40; i++) {
            System.out.println(flg.sentence(rng, 5, 10, new String[]{",", ",", ";"},
                    new String[]{".", ".", "?", "?", "!", "...", "..."}, 0.2));
        }
        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.FANTASY_NAME;
        System.out.print(flg.word(rng, true, rng.between(2, 4)));
        for (int i = 1; i < 10; i++) {
            System.out.print(", " + flg.word(rng, true, rng.between(2, 4)));
        }
        System.out.println("...");

        rng.setState(0xf00df00L);
        flg = FakeLanguageGen.FANCY_FANTASY_NAME;
        System.out.print(flg.word(rng, true, rng.between(2, 4)));
        for (int i = 1; i < 10; i++) {
            System.out.print(", " + flg.word(rng, true, rng.between(2, 4)));
        }
        System.out.println("\n\nDEFAULT SENTENCES:\n\n");
        System.out.println('"' + FakeLanguageGen.ENGLISH.sentence(rng, 4, 7, new String[]{" -", ",", ",", ";"}, new String[]{"!", "!", "...", "...", ".", "?"}, 0.2) + "\",");
        System.out.println('"' + FakeLanguageGen.JAPANESE_ROMANIZED.sentence(rng, 4, 7, new String[]{" -", ",", ",", ";"}, new String[]{"!", "!", "...", "...", ".", "?"}, 0.2) + "\",");
        System.out.println('"' + FakeLanguageGen.FRENCH.sentence(rng, 5, 8, new String[]{" -", ",", ",", ";"}, new String[]{"!", "?", ".", "...", ".", "?"}, 0.1) + "\",");
        System.out.println('"' + FakeLanguageGen.GREEK_ROMANIZED.sentence(rng, 5, 8, new String[]{",", ",", ";"}, new String[]{"!", "?", ".", "...", ".", "?"}, 0.15) + "\",");
        System.out.println('"' + FakeLanguageGen.GREEK_AUTHENTIC.sentence(rng, 5, 8, new String[]{",", ",", ";"}, new String[]{"!", "?", ".", "...", ".", "?"}, 0.15) + "\",");
        System.out.println('"' + FakeLanguageGen.RUSSIAN_ROMANIZED.sentence(rng, 4, 7, new String[]{" -", ",", ",", ",", ";"}, new String[]{"!", "!", ".", "...", ".", "?"}, 0.22) + "\",");
        System.out.println('"' + FakeLanguageGen.RUSSIAN_AUTHENTIC.sentence(rng, 4, 7, new String[]{" -", ",", ",", ",", ";"}, new String[]{"!", "!", ".", "...", ".", "?"}, 0.22) + "\",");
        System.out.println('"' + FakeLanguageGen.LOVECRAFT.sentence(rng, 4, 7, new String[]{" -", ",", ",", ";"}, new String[]{"!", "!", "...", "...", ".", "?"}, 0.2) + "\",");
        System.out.println('"' + FakeLanguageGen.SWAHILI.sentence(rng, 4, 7, new String[]{",", ",", ";"}, new String[]{"!", "?", ".", ".", "."}, 0.12) + "\",");
        flg = FakeLanguageGen.FRENCH.mix(FakeLanguageGen.JAPANESE_ROMANIZED, 0.65);
        System.out.println('"' + flg.sentence(rng, 6, 12, new String[]{",", ",", ",", ";"},
                new String[]{".", ".", "!", "?", "...", "..."}, 0.17) + "\",");
        flg = FakeLanguageGen.ENGLISH.addAccents(0.5, 0.15);
        System.out.println('"' + flg.sentence(rng, 6, 12, new String[]{",", ",", ",", ";"},
                new String[]{".", ".", "!", "?", "...", "..."}, 0.17) + "\",");
        flg = FakeLanguageGen.RUSSIAN_AUTHENTIC.mix(FakeLanguageGen.GREEK_AUTHENTIC, 0.5).mix(FakeLanguageGen.FRENCH, 0.35);
        System.out.println('"' + flg.sentence(rng, 6, 12, new String[]{",", ",", ",", ";", " -"},
                new String[]{".", ".", "!", "?", "...", "..."}, 0.2) + "\",");
        flg = FakeLanguageGen.FANCY_FANTASY_NAME;
        System.out.println('"' + flg.sentence(rng, 6, 12, new String[]{",", ",", ",", ";", " -"},
                new String[]{".", ".", "!", "?", "...", "..."}, 0.2) + "\",");
        flg = FakeLanguageGen.SWAHILI.mix(FakeLanguageGen.JAPANESE_ROMANIZED, 0.35); //.mix(FakeLanguageGen.FRENCH, 0.35)
        System.out.println('"' + flg.sentence(rng, 4, 7, new String[]{",", ",", ";"},
                new String[]{"!", "?", ".", ".", "."}, 0.12) + "\",");
        flg = FakeLanguageGen.SWAHILI.mix(FakeLanguageGen.JAPANESE_ROMANIZED, 0.32).mix(FakeLanguageGen.FANCY_FANTASY_NAME, 0.25);
        System.out.println('"' + flg.sentence(rng, 4, 7, new String[]{",", ",", ";"},
                new String[]{"!", "?", ".", ".", "."}, 0.12) + "\",");
        flg = FakeLanguageGen.SOMALI.mix(FakeLanguageGen.JAPANESE_ROMANIZED, 0.3).mix(FakeLanguageGen.SWAHILI, 0.15);
        System.out.println('"' + flg.sentence(rng, 4, 7, new String[]{",", ",", ";"},
                new String[]{"!", "?", ".", ".", "."}, 0.15) + "\",");
        flg = FakeLanguageGen.INUKTITUT;
        System.out.println('"' + flg.sentence(rng, 4, 7, new String[]{",", ",", ";"},
                new String[]{"!", "?", ".", ".", "."}, 0.15) + "\",");
        flg = FakeLanguageGen.NORSE;
        System.out.println('"' + flg.sentence(rng, 4, 7, new String[]{",", ",", ";"},
                new String[]{"!", "?", ".", ".", "."}, 0.15) + "\",");
        flg = FakeLanguageGen.NORSE.addModifiers(FakeLanguageGen.Modifier.SIMPLIFY_NORSE);
        System.out.println('"' + flg.sentence(rng, 4, 7, new String[]{",", ",", ";"},
                new String[]{"!", "?", ".", ".", "."}, 0.15) + "\",");
        flg = FakeLanguageGen.NAHUATL;
        System.out.println('"' + flg.sentence(rng, 3, 6, new String[]{",", ",", ";"},
                new String[]{"!", "?", ".", ".", "."}, 0.1) + "\",");
        flg = FakeLanguageGen.MONGOLIAN;
        System.out.println('"' + flg.sentence(rng, 3, 7, new String[]{",", ",", ";", ",", " -"},
                new String[]{"!", "?", ".", ".", ".", ".", "..."}, 0.16) + "\",");

        System.out.println("\n-----------------------------------------------------------------------------");
        System.out.println();
        FakeLanguageGen[] languages = new FakeLanguageGen[]{

                FakeLanguageGen.ENGLISH,
                FakeLanguageGen.LOVECRAFT,
                FakeLanguageGen.JAPANESE_ROMANIZED,
                FakeLanguageGen.FRENCH,
                FakeLanguageGen.GREEK_ROMANIZED,
                FakeLanguageGen.GREEK_AUTHENTIC,
                FakeLanguageGen.RUSSIAN_ROMANIZED,
                FakeLanguageGen.RUSSIAN_AUTHENTIC,
                FakeLanguageGen.SWAHILI,
                FakeLanguageGen.SOMALI,
                FakeLanguageGen.FANTASY_NAME,
                FakeLanguageGen.FANCY_FANTASY_NAME,
                FakeLanguageGen.ARABIC_ROMANIZED,
                FakeLanguageGen.HINDI_ROMANIZED.removeAccents(),
                FakeLanguageGen.RUSSIAN_ROMANIZED.mix(FakeLanguageGen.SOMALI, 0.25),
                FakeLanguageGen.GREEK_ROMANIZED.mix(FakeLanguageGen.HINDI_ROMANIZED.removeAccents(), 0.5),
                FakeLanguageGen.SWAHILI.mix(FakeLanguageGen.FRENCH, 0.3),
                FakeLanguageGen.ARABIC_ROMANIZED.addModifiers(FakeLanguageGen.Modifier.SIMPLIFY_ARABIC).mix(FakeLanguageGen.JAPANESE_ROMANIZED, 0.4),
                FakeLanguageGen.SWAHILI.mix(FakeLanguageGen.GREEK_ROMANIZED, 0.4),
                FakeLanguageGen.GREEK_ROMANIZED.mix(FakeLanguageGen.SOMALI, 0.4),
                FakeLanguageGen.ENGLISH.mix(FakeLanguageGen.HINDI_ROMANIZED.removeAccents(), 0.4),
                FakeLanguageGen.ENGLISH.mix(FakeLanguageGen.JAPANESE_ROMANIZED, 0.4),
                FakeLanguageGen.SOMALI.mix(FakeLanguageGen.HINDI_ROMANIZED.removeAccents(), 0.4),
                FakeLanguageGen.FRENCH.addModifiers(FakeLanguageGen.modifier("([^aeiou])\\1", "$1ph", 0.3),
                        FakeLanguageGen.modifier("([^aeiou])\\1", "$1ch", 0.4),
                        FakeLanguageGen.modifier("([^aeiou])\\1", "$1sh", 0.5),
                        FakeLanguageGen.modifier("([^aeiou])\\1", "$1", 0.9)),
                FakeLanguageGen.JAPANESE_ROMANIZED.addModifiers(FakeLanguageGen.Modifier.DOUBLE_VOWELS),
                FakeLanguageGen.SOMALI.addModifiers(FakeLanguageGen.modifier("([kd])h", "$1"),
                        FakeLanguageGen.modifier("([pfsgkcb])([aeiouy])", "$1l$2", 0.35),
                        FakeLanguageGen.modifier("ii", "ai"),
                        FakeLanguageGen.modifier("uu", "ia"),
                        FakeLanguageGen.modifier("([aeo])\\1", "$1"),
                        FakeLanguageGen.modifier("^x", "v"),
                        FakeLanguageGen.modifier("([^aeiou]|^)u([^aeiou]|$)", "$1a$2", 0.6),
                        FakeLanguageGen.modifier("([aeiou])[^aeiou]([aeiou])", "$1v$2", 0.06),
                        FakeLanguageGen.modifier("([aeiou])[^aeiou]([aeiou])", "$1l$2", 0.07),
                        FakeLanguageGen.modifier("([aeiou])[^aeiou]([aeiou])", "$1n$2", 0.07),
                        FakeLanguageGen.modifier("([aeiou])[^aeiou]([aeiou])", "$1z$2", 0.08),
                        FakeLanguageGen.modifier("([^aeiou])[aeiou]+$", "$1ia", 0.35),
                        FakeLanguageGen.modifier("([^aeiou])[bpdtkgj]", "$1"),
                        FakeLanguageGen.modifier("[jg]$", "th"),
                        FakeLanguageGen.modifier("g", "c", 0.92),
                        FakeLanguageGen.modifier("([aeiou])[wy]$", "$1l", 0.6),
                        FakeLanguageGen.modifier("([aeiou])[wy]$", "$1n"),
                        FakeLanguageGen.modifier("[qf]$", "l", 0.4),
                        FakeLanguageGen.modifier("[qf]$", "n", 0.65),
                        FakeLanguageGen.modifier("[qf]$", "s"),
                        FakeLanguageGen.modifier("cy", "sp"),
                        FakeLanguageGen.modifier("kl", "sk"),
                        FakeLanguageGen.modifier("qu+", "qui"),
                        FakeLanguageGen.modifier("q([^u])", "qu$1"),
                        FakeLanguageGen.modifier("cc", "ch"),
                        FakeLanguageGen.modifier("[^aeiou]([^aeiou][^aeiou])", "$1"),
                        FakeLanguageGen.Modifier.NO_DOUBLES
                ),
                FakeLanguageGen.randomLanguage(CrossHash.Lightning.hash64("Kittenish")),
                FakeLanguageGen.randomLanguage(CrossHash.Lightning.hash64("Puppyspeak")),
                FakeLanguageGen.randomLanguage(CrossHash.Lightning.hash64("Rabbitese")),
                FakeLanguageGen.randomLanguage(CrossHash.Lightning.hash64("Rabbit Language")),
                FakeLanguageGen.randomLanguage(CrossHash.Lightning.hash64("The Roar Of That Slumbering Shadow That Mankind Wills Itself To Forget")),
                FakeLanguageGen.INUKTITUT,
                FakeLanguageGen.NORSE,
                FakeLanguageGen.NORSE.addModifiers(FakeLanguageGen.Modifier.SIMPLIFY_NORSE),
                FakeLanguageGen.NAHUATL
                //FakeLanguageGen.RUSSIAN_ROMANIZED.mix(FakeLanguageGen.GREEK_ROMANIZED, 0.4),
                //FakeLanguageGen.LOVECRAFT.mix(FakeLanguageGen.RUSSIAN_ROMANIZED, 0.4),
                //FakeLanguageGen.randomLanguage(new StatefulRNG(2252637788195L)),
        };
        String[] oz = new String[]{
                "Uncle Uncles Carbuncle Carbuncles Live Lives Lived Living Liver Livers Livery Liveries",
                "Dorothy lived in the midst of the great Kansas prairies, with Uncle Henry, who was a ",
                "farmer, and Aunt Em, who was the farmer's wife. Their house was small, for the ",
                "lumber to build it had to be carried by wagon many miles. There were four walls, ",
                "a floor and a roof, which made one room; and this room contained a rusty looking ",
                "cookstove, a cupboard for the dishes, a table, three or four chairs, and the beds. ",
                "Uncle Henry and Aunt Em had a big bed in one corner, and Dorothy a little bed in ",
                "another corner. There was no garret at all, and no cellar-except a small hole dug ",
                "in the ground, called a cyclone cellar, where the family could go in case one of ",
                "those great whirlwinds arose, mighty enough to crush any building in its path. It ",
                "was reached by a trap door in the middle of the floor, from which a ladder led ",
                "down into the small, dark hole.",

        }, oz2 = new String[oz.length];
        System.out.println("ORIGINAL:");
        for(String o : oz)
        {
            System.out.println(o);
        }
        System.out.println("\n\nGENERATED:\n");
        StatefulRNG sr = new StatefulRNG(2252637788195L);
        for(FakeLanguageGen lang : languages) {
            NaturalLanguageCipher cipher = new NaturalLanguageCipher(lang, 41041041L);
            //LanguageCipher cipher = new LanguageCipher(FakeLanguageGen.randomLanguage(sr));
            int ctr = 0;
            for (String s : oz) {
                oz2[ctr] = cipher.cipher(s);
                System.out.println(oz2[ctr++]);
            }

            HashMap<String, String> vocabulary = new HashMap<>(16);
            cipher.learnTranslations(vocabulary, "Dorothy", "farmer", "the", "room", "one", "uncle", "aunt");
            for (String s : oz2) {
                System.out.println(cipher.decipher(s, vocabulary));
            }
            System.out.println();
            for (String s : oz2) {
                System.out.println(cipher.decipher(s, cipher.reverse));
            }
            System.out.println();

            /*
            LanguageCipher cipher = new LanguageCipher(lang, 2252637788195L);
            //LanguageCipher cipher = new LanguageCipher(FakeLanguageGen.randomLanguage(sr));
            int ctr = 0;
            for (String s : oz) {
                oz2[ctr] = cipher.cipher(s);
                System.out.println(oz2[ctr++]);
            }

            HashMap<String, String> vocabulary = new HashMap<>(16);
            cipher.learnTranslations(vocabulary, "Dorothy", "farmer", "the", "room", "one", "uncle", "aunt");
            for (String s : oz2) {
                System.out.println(cipher.decipher(s, vocabulary));
            }
            System.out.println();
            for (String s : oz2) {
                System.out.println(cipher.decipher(s, cipher.reverse));
            }
            System.out.println();
            */
            /*
            cipher = new LanguageCipher(lang, 0x123456789L);
            ctr = 0;
            for (String s : oz) {
                oz2[ctr] = cipher.cipher(s);
                System.out.println(oz2[ctr++]);
            }

            vocabulary.clear();
            cipher.learnTranslations(vocabulary, "Dorothy", "farmer", "the", "room", "one", "uncle", "aunt");
            for (String s : oz2) {
                System.out.println(cipher.decipher(s, vocabulary));
            }
            System.out.println();
            for (String s : oz2) {
                System.out.println(cipher.decipher(s, cipher.reverse));
            }
            System.out.println();
            */
        }
        /*
        StatefulRNG nrng = new StatefulRNG("SquidLib!");

        System.out.println(nrng.getState());
        for(FakeLanguageGen lang : languages) {
            for (int n = 0; n < 20; n++) {
                for (int i = 0; i < 4; i++) {
                    System.out.print(nrng.getState() + " : " + lang.word(nrng, false, 3) + ", ");
                }
                System.out.println();
            }
            System.out.println();
        }
        */
    }
}
