package rtx.nv.api.drags;

final class DragLerpAnim {
    private long lastTickNs;
    float anim;
    float to;
    float speed;

    DragLerpAnim(float f, float f2, float f3) {
        this.anim = f;
        this.to = f2;
        this.speed = f3;
        this.lastTickNs = System.nanoTime();
    }

    float getAnim() {
        long l = System.nanoTime();
        float f = Math.min((float)(l - this.lastTickNs) / 1.0E9f, 0.05f);
        this.lastTickNs = l;
        float f2 = Math.clamp(this.speed * 2.0f, 0.0f, 0.999f);
        float f3 = 1.0f - (float)Math.pow(1.0f - f2, f * 100.0f);
        this.anim += (this.to - this.anim) * f3;
        return this.anim;
    }

    void setAnim(float f) {
        this.anim = f;
        this.lastTickNs = System.nanoTime();
    }

    void setTo(float f) {
        this.to = f;
    }

    void setSpeed(float f) {
        this.speed = f;
    }

    void setToAsBoolean(boolean bl) {
        this.to = bl ? 1.0f : 0.0f;
    }

    float getTo() {
        return this.to;
    }
}

