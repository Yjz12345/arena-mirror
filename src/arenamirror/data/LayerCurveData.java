package arenamirror.data;

public class LayerCurveData {
    public String curveName = "Default";
    public LayerStatEntry[] layers = new LayerStatEntry[100];

    public LayerCurveData() {
        for (int i = 0; i < 100; i++) {
            float t = (i + 1) / 100f;
            // Steeper scaling at high layers to match player power growth
            // HP uses quadratic curve for late-game challenge
            float hpCurve = 0.8f + t * 6.2f + t * t * 6f;  // 0.8x -> 13x (was 7x)
            float atkCurve = 0.7f + t * 3.3f + t * t * 2f;  // 0.7x -> 6x (was 4x)
            float defCurve = 0.2f + t * 1.5f + t * t * 3f;  // 0.2x -> 4.7x (was 1.7x)
            layers[i] = new LayerStatEntry(
                i + 1,
                hpCurve,
                atkCurve,
                defCurve,
                0.9f + t * 1.2f     // spd: unchanged
            );
        }
    }

    public LayerStatEntry getLayerStats(int layer) {
        int index = Math.max(0, Math.min(99, layer - 1));
        return layers[index];
    }
}
