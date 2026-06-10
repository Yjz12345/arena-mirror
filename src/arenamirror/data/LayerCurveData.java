package arenamirror.data;

public class LayerCurveData {
    public String curveName = "Default";
    public LayerStatEntry[] layers = new LayerStatEntry[100];

    public LayerCurveData() {
        for (int i = 0; i < 100; i++) {
            float t = (i + 1) / 100f;
            // Softer early, much steeper late. Quadratic edge at high layers.
            float hpCurve  = 0.6f + t * 4f + t * t * 10f;   // 0.6x -> 14.6x
            float atkCurve = 0.5f + t * 2f + t * t * 5f;    // 0.5x -> 7.5x
            float defCurve = 0.1f + t * 1f + t * t * 4f;    // 0.1x -> 5.1x
            layers[i] = new LayerStatEntry(
                i + 1,
                hpCurve,
                atkCurve,
                defCurve,
                0.9f + t * 1.2f     // spd unchanged
            );
        }
    }

    public LayerStatEntry getLayerStats(int layer) {
        int index = Math.max(0, Math.min(99, layer - 1));
        return layers[index];
    }
}
