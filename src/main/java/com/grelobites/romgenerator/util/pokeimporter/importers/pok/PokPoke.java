package com.grelobites.romgenerator.util.pokeimporter.importers.pok;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 'N' means: this is the Next trainer,
 * 'Y' means: this is the last line of the file (the rest of the file is ignored).
 * After the 'N' follows the name/description of this specific trainer. This string
 * may be up to 30 characters. There is no space between the 'N' and the string.
 * Emulator authors can use these strings to set up the selection entries.
 * <p>
 * The following lines, up to the next 'N' or 'Z' hold the POKEs to be applied for
 * this specific trainer. Again, the first character determines the content.
 * 'M' means: this is not the last POKE (More)
 * 'Z' means: this is the last POKE
 * The rest of a POKE line is built from
 * <p>
 * bbb aaaaa vvv ooo
 * <p>
 * All values are decimal, separation is done by one or more spaces.
 * <p>
 * The field 'bbb' (128K memory bank) is built from
 * bit 0-2 : bank value
 * 3 : ignore bank (1=yes, always set for 48K games)
 * <p>
 * There theoretically is no space between the line tag and the bank field.
 * However, since the bank value is never larger than 8, you will always see
 * 2 spaces in front of the bank.
 * <p>
 * The 'aaaaa' (address) is in range [16384, 65535].
 * <p>
 * If the field 'vvv' (value) is in range 0-255, this is the value to be POKEd. If
 * it is 256, a requester should pop up where the user can enter a value.
 * <p>
 * The field 'ooo' (original) holds the original value at the address. It is used
 * to remove a POKE. Normally, when applying a POKE, the original value can be read
 * from the address. Sometimes however, games as found on the internet are already
 * POKEd, so the original value can not be read. If this field is filled in (non-0)
 * you still have the possibility to remove the trainer.
 * The format cannot handle the case where the original value was 0, but I haven't
 * so far seen a single case where this was true.
 */
public class PokPoke {
    private static final Logger LOGGER = LoggerFactory.getLogger(PokPoke.class);

    public static final String NEXT_TRAINER = "N";
    public static final String LAST_LINE_MARKER = "Y";
    public static final String LAST_POKE_MARKER = "Z";
    public static final String POKE_MARKER = "M";

    private static Pattern trainerLinePattern = Pattern.compile(String.format("^%s(.*)", NEXT_TRAINER));
    private static Pattern pokeLinePattern = Pattern.compile(String
            .format("^([%s%s])\\s*([0-9]{1,3})\\s+([0-9]{1,5})\\s+([0-9]{1,3})\\s+([0-9]{1,3})\\s*",
                    POKE_MARKER, LAST_POKE_MARKER));
    private static Pattern lastLinePattern = Pattern.compile("^" + LAST_LINE_MARKER);

    private List<PokTrainer> trainers;

    public void addTrainer(PokTrainer trainer) {
        if (trainers == null) {
            trainers = new ArrayList<>();
        }
        trainers.add(trainer);
    }

    public List<PokTrainer> getTrainers() {
        return trainers;
    }

    public void setTrainers(List<PokTrainer> trainers) {
        this.trainers = trainers;
    }

    public static PokPoke fromInputStream(InputStream is) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        BufferedReader br = new BufferedReader(new InputStreamReader(bis, StandardCharsets.US_ASCII));
        String line;
        PokPoke poke = new PokPoke();
        PokTrainer trainer = null;
        while ((line = br.readLine()) != null) {
            LOGGER.debug("Processing poke file line " + line);
            if (trainer == null) {
                Matcher m = trainerLinePattern.matcher(line);
                if (m.matches()) {
                    LOGGER.debug("Detected trainer line");
                    trainer = new PokTrainer();
                    trainer.setName(m.group(1).trim());
                    continue;
                }
            }
            Matcher pokeMatcher = pokeLinePattern.matcher(line);
            if (pokeMatcher.matches()) {
                LOGGER.debug("Detected poke line");
                if (trainer != null) {
                    PokValue pokeValue = new PokValue();
                    pokeValue.setBank(pokeMatcher.group(2));
                    pokeValue.setAddress(pokeMatcher.group(3));
                    pokeValue.setValue(pokeMatcher.group(4));
                    pokeValue.setOriginalValue(pokeMatcher.group(5));
                    trainer.addPokeValue(pokeValue);
                    if (LAST_POKE_MARKER.equals(pokeMatcher.group(1))) {
                        LOGGER.debug("Detected last poke marker");
                        poke.addTrainer(trainer);
                        trainer = null;
                    }
                } else {
                    LOGGER.warn("Poke line " + line + " found without opened trainer. Skipped");
                }
            } else if (lastLinePattern.matcher(line).matches()) {
                if (trainer != null) {
                    LOGGER.warn("Adding incomplete trainer to poke " + trainer);
                    poke.addTrainer(trainer);
                }
            }
        }
        return poke;
    }

    @Override
    public String toString() {
        return "PokPoke{" +
                "trainers=" + trainers +
                '}';
    }
}
