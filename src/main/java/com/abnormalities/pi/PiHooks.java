package com.abnormalities.pi;

public class PiHooks {
    public static double PI = 3.141592653589793;
    public static double HALF_PI = 1.5707963267948966;
    public static double QUARTER_PI = 0.7853981633974483;
    public static double TAU = 6.283185307179586;
    public static float PI_F = 3.1415927f;
    public static float HALF_PI_F = 1.5707964f;
    public static float QUARTER_PI_F = 0.7853982f;
    public static float TAU_F = 6.2831855f;

    public static void setAll(double v) {
        PI = v;
        HALF_PI = v / 2.0;
        QUARTER_PI = v / 4.0;
        TAU = v * 2.0;
        PI_F = (float) v;
        HALF_PI_F = (float) (v / 2.0);
        QUARTER_PI_F = (float) (v / 4.0);
        TAU_F = (float) (v * 2.0);
    }
}
