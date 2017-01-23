package nidefawl.qubes.util;

public class Ease {
    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            float f = i / 100.0f;
            System.out.println(bounceOut(f));
        }
    }

    public static float bounceInOut(float t) {
      return t < 0.5f
        ? 0.5f * (1.0f - bounceOut(1.0f - t * 2.0f))
        : 0.5f * bounceOut(t * 2.0f - 1.0f) + 0.5f;
    }
    public static float circularInOut(float t) {
        return t < 0.5f
          ? 0.5f * (1.0f - GameMath.sqrtf(1.0f - 4.0f * t * t))
          : 0.5f * (GameMath.sqrtf((3.0f - 2.0f * t) * (2.0f * t - 1.0f)) + 1.0f);
      }

    public static float circularIn(float t) {
        return 1.0f - GameMath.sqrtf(1.0f - t * t);
    }

    public static float circularOut(float t) {
        return GameMath.sqrtf((2.0f - t) * t);
    }

    public static float cubicInOut(float t) {
        return t < 0.5f ? 4.0f * t * t * t : 0.5f * GameMath.pow(2.0f * t - 2.0f, 3) + 1.0f;
    }

    public static float cubicIn(float t) {
        return t * t * t;
    }

    public static float cubicOut(float t) {
        float f = t - 1.0f;
        return f * f * f + 1.0f;
    }
    public static float bounceOut(float t) {
        float a = 4.0f / 11.0f;
        float b = 8.0f / 11.0f;
        float c = 9.0f / 10.0f;

        float ca = 4356.0f / 361.0f;
        float cb = 35442.0f / 1805.0f;
        float cc = 16061.0f / 1805.0f;

        float t2 = t * t;

        return t < a ? 7.5625f * t2 
                : t < b ? 9.075f * t2 - 9.9f * t + 3.4f 
                        : t < c ? ca * t2 - cb * t + cc 
                                : 10.8f * t * t - 20.52f * t + 10.72f;
    }

    public static float bounceIn(float t) {
        return 1.0f - bounceOut(1.0f - t);
    }

}
