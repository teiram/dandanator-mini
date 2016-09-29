package com.grelobites.romgenerator.util.player;

public class FFT {
    // compute the FFT of x[], assuming its length is a power of 2
    public static Complex[] fft(Complex[] x) {
        int length = x.length;

        // base case
        if (length == 1) return new Complex[] {x[0]};

        // radix 2 Cooley-Tukey FFT
        if (length % 2 != 0) {
            throw new IllegalArgumentException("Not a power of 2 array length");
        }

        // fft of even terms
        Complex[] even = new Complex[length/2];
        for (int k = 0; k < length/2; k++) {
            even[k] = x[2*k];
        }
        Complex[] q = fft(even);

        // fft of odd terms
        Complex[] odd  = even;  // reuse the array
        for (int k = 0; k < length/2; k++) {
            odd[k] = x[2*k + 1];
        }
        Complex[] r = fft(odd);

        // combine
        Complex[] y = new Complex[length];
        for (int k = 0; k < length/2; k++) {
            double kth = -2 * k * Math.PI / length;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k]       = q[k].plus(wk.times(r[k]));
            y[k + length/2] = q[k].minus(wk.times(r[k]));
        }
        return y;
    }

    public static Complex[] ifft(Complex[] x) {
        int n = x.length;
        Complex[] y = new Complex[n];

        // take conjugate
        for (int i = 0; i < n; i++) {
            y[i] = x[i].conjugate();
        }

        // compute forward FFT
        y = fft(y);

        // take conjugate again
        for (int i = 0; i < n; i++) {
            y[i] = y[i].conjugate();
        }

        // divide by n
        for (int i = 0; i < n; i++) {
            y[i] = y[i].scale(1.0 / n);
        }

        return y;

    }
}
