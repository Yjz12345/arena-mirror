package arenamirror.rendering;

public class Vec2 {
    public float x, y;

    public Vec2() { this.x = 0; this.y = 0; }
    public Vec2(float x, float y) { this.x = x; this.y = y; }
    public Vec2(Vec2 other) { this.x = other.x; this.y = other.y; }

    public Vec2 add(Vec2 other) { return new Vec2(x + other.x, y + other.y); }
    public Vec2 sub(Vec2 other) { return new Vec2(x - other.x, y - other.y); }
    public Vec2 scale(float s) { return new Vec2(x * s, y * s); }
    public float length() { return (float) Math.sqrt(x * x + y * y); }
    public Vec2 normalized() {
        float len = length();
        if (len == 0) return new Vec2(0, 0);
        return new Vec2(x / len, y / len);
    }
    public float distance(Vec2 other) { return sub(other).length(); }
    public float dot(Vec2 other) { return x * other.x + y * other.y; }
    public Vec2 perpendicular() { return new Vec2(-y, x); }
    public Vec2 lerp(Vec2 target, float t) {
        return new Vec2(x + (target.x - x) * t, y + (target.y - y) * t);
    }
    public Vec2 clampLength(float max) {
        float len = length();
        if (len > max && len > 0) return scale(max / len);
        return new Vec2(this);
    }

    @Override public String toString() { return String.format("(%.1f, %.1f)", x, y); }
}
