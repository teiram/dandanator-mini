package com.grelobites.romgenerator.util.gameloader.loaders.tap;

public enum Key {

    /*
     IN:    Reads keys (bit 0 to bit 4 inclusive)

      0xfefe  SHIFT, Z, X, C, V            0xeffe  0, 9, 8, 7, 6
      0xfdfe  A, S, D, F, G                0xdffe  P, O, I, U, Y
      0xfbfe  Q, W, E, R, T                0xbffe  ENTER, L, K, J, H
      0xf7fe  1, 2, 3, 4, 5                0x7ffe  SPACE, SYM SHFT, M, N, B
     */

    KEY_SHIFT(0x01, 0xfe), KEY_Z(0x02, 0xfe), KEY_X(0x04, 0xfe), KEY_C(0x08, 0xfe), KEY_V(0x10, 0xfe),

    KEY_0(0x01, 0xef),     KEY_9(0x02, 0xef), KEY_8(0x04, 0xef), KEY_7(0x08, 0xef), KEY_6(0x10, 0xef),

    KEY_A(0x01, 0xfd),     KEY_S(0x02, 0xfd), KEY_D(0x04, 0xfd), KEY_F(0x08, 0xfd), KEY_G(0x10, 0xfd),

    KEY_P(0x01, 0xdf),     KEY_O(0x02, 0xdf), KEY_I(0x04, 0xdf), KEY_U(0x08, 0xdf), KEY_Y(0x10, 0xdf),

    KEY_Q(0x01, 0xfb),     KEY_W(0x02, 0xfb), KEY_E(0x04, 0xfb), KEY_R(0x08, 0xfb), KEY_T(0x10, 0xfb),

    KEY_ENTER(0x01, 0xbf), KEY_L(0x02, 0xbf), KEY_K(0x04, 0xbf), KEY_J(0x08, 0xbf), KEY_H(0x10, 0xbf),

    KEY_1(0x01, 0xf7),     KEY_2(0x02, 0xf7), KEY_3(0x04, 0xf7), KEY_4(0x08, 0xf7), KEY_5(0x10, 0xf7),

    KEY_SPACE(0x01, 0x7f), KEY_SYM_SHIFT(0x02, 0x7f), KEY_M(0x04, 0x7f), KEY_N(0x08, 0x7f), KEY_B(0x10, 0x7f);

    public int mask;
    public int addressMask;

    Key(int mask, int address) {
        this.mask = mask;
        this.addressMask = ~address;
    }

    public boolean respondsTo(int address) {
        return ((address >> 8) & addressMask) == 0;
    }
}
