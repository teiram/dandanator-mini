package com.grelobites.romgenerator.exomizer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class MatchContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchContext.class);

    private byte[] inputData;
    private PreCalc[] preCalc;
    private int[] rle;
    private int[] rle_r;
    private int length;
    private int maxOffset;
    private int maxLength;

    public MatchContext(byte[] inputData, int maxLength, int maxOffset, boolean useImpreciseRle) {
        this.inputData = inputData;
        this.maxLength = maxLength;
        this.maxOffset = maxOffset;

        length = inputData.length;

        this.rle = new int[length + 1];
        this.rle_r = new int[length + 1];
        this.preCalc = new PreCalc[length + 1];

        int value = inputData[0];

        for (int i = 1; i < length; i++) {
            if (inputData[i] == value) {
                int len = rle[i - 1] + 1;
                if (len > maxLength) {
                    len = 0;
                }
                rle[i] = len;
            } else {
                rle[i] = 0;
            }
            value = inputData[i];
        }

        for (int i = length - 2; i >= 0; i--) {
            if (rle[i] < rle[i + 1]) {
                rle_r[i] = rle_r[i + 1] + 1;
            } else {
                rle_r[i] = 0;
            }
        }

        byte[] rle_map = new byte[65536];
        MatchNode prev_np = null;
        /* add extra nodes to rle sequences */
        for (int c = 0; c < 256; c++) {
            int rle_len;
            Arrays.fill(rle_map, (byte) 0);
            /* for each possible rle char */
            for (int i = 0; i < length; i++) {
                /* must be the correct char */
                if (inputData[i] != c) {
                    continue;
                }

                rle_len = rle[i];
                if (rle_map[rle_len] == (byte) 0 && rle_r[i] > 16) {
                    /* no previous lengths and not our primary length*/
                    continue;
                }

                if (useImpreciseRle &&
                        rle_r[i] != 0 && rle[i] != 0) {
                    continue;
                }

                MatchNode matchNode = new MatchNode(i);
                rle_map[rle_len] = 1;

                LOGGER.debug(String.format("0) c = %d, added np idx %d -> %d\n", c, i, 0));
                if (prev_np != null) {
                    prev_np.setNext(matchNode);
                }
                preCalc[i] = new PreCalc(matchNode);
                prev_np = matchNode;
            }

            Arrays.fill(rle_map, (byte) 0);
            prev_np = null;
            for (int i = length - 1; i >= 0; --i) {
            /* must be the correct char */
                if (inputData[i] != c) {
                    continue;
                }

                rle_len = rle_r[i];
                MatchNode np = preCalc[i].getSingle();
                if (np == null) {
                    if (rle_map[rle_len] != 0 && prev_np != null && rle_len > 0) {
                        MatchNode matchNode = new MatchNode(i);
                        matchNode.setNext(prev_np);
                        preCalc[i] = new PreCalc(matchNode);
                        LOGGER.debug(String.format("2) c = %d, added np idx %d -> %d\n",
                                c, i, prev_np.getIndex()));
                    }
                } else {
                    prev_np = np;
                }

                if (rle_r[i] > 0) {
                    continue;
                }
                rle_len = rle[i] + 1;
                rle_map[rle_len] = 1;
            }
        }

        for (int i = length - 1; i >= 0; i--) {
            Match matches = calculateMatches(i);
        /* add to cache */
            preCalc[i].setCache(matches);

        }
    }

    private Match calculateMatches(int index) {
        Match mp;
        MatchNode np;

        byte [] buf = this.inputData;
        Match matches = null;

        LOGGER.debug(String.format("index %d, char '%c', rle %d, rle_r %d\n",
                index, buf[index], rle[index],
                rle_r[index]));

    /* proces the literal match and add it to matches */
        mp = new Match(matches, 1, 0);
        matches = mp;

    /* get possible match */
        np = preCalc[index].getSingle();
        if(np != null) {
            np = np.getNext();
        }
        for (; np != null; np = np.getNext()) {
            int pos;
            int offset;

        /* limit according to max offset */
            if(np.getIndex() > index + maxOffset) {
                break;
            }

            LOGGER.debug(String.format("find lengths for index %d to index %d\n",
                    index, np.getIndex()));
        /* get match len */
            int mp_len = mp.getOffset() > 0 ? mp.getLength() : 0;
            LOGGER.debug(String.format("0) comparing with current best [%d] off %d len %d\n",
                    index, mp.getOffset(), mp_len));

            offset = np.getIndex() - index;
            int len = mp_len;
            pos = index + 1 - len;
        /* Compare the first <previous len> bytes backwards. We can
         * skip some comparisons by increasing by the rle count. We
         * don't need to compare the first byte, hence > 1 instead of
         * > 0 */
            while(len > 1 && buf[pos] == buf[pos + offset]) {
                int offset1 = rle_r[pos];
                int offset2 = rle_r[pos + offset];
                int offsetp = offset1 < offset2 ? offset1 : offset2;

                LOGGER.debug(String.format("1) compared sucesssfully [%d] %d %d\n",
                        index, pos, pos + offsetp));

                len -= 1 + offsetp;
                pos += 1 + offsetp;
            }
            if (len > 1) {
            /* sequence length too short, skip this match */
                continue;
            }

            if (offset < 17) {
            /* allocate match struct and add it to matches */
                mp = new Match(matches, 1, offset);
                matches = mp;
            }

        /* Here we know that the current match is atleast as long as
         * the previuos one. let's compare further. */
            len = mp_len;
            pos = index - len;
            while(len <= maxLength &&
                    pos >= 0 && buf[pos] == buf[pos + offset])
            {
                LOGGER.debug(String.format("2) compared sucesssfully [%d] %d %d\n",
                        index, pos, pos + offset));
                ++len;
                --pos;
            }
            if(len > mp_len)
            {
            /* allocate match struct and add it to matches */
                mp = new Match(matches, index - pos, offset);
                matches = mp;
            }
            if (len > maxLength)
            {
                break;
            }
            if(pos < 0)
            {
            /* we have reached the eof, no better matches can be found */
                break;
            }
        }
        LOGGER.debug(String.format("adding matches for index %d to cache\n", index));

        return matches;
    }

}
