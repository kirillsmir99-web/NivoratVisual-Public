package rtx.nv.utils.render.render2d.outline.outline360;

public record Outline360Range(float startDegrees, float endDegrees, int color, int colorEnd, float blendStartDegrees, float blendEndDegrees) {
    public static Outline360Range of(float start, float end, int color) {
        return new Outline360Range(start, end, color, color, 14.0f, 14.0f);
    }

    public static Outline360Range gradient(float start, float end, int colorStart, int colorEnd, float blendStart, float blendEnd) {
        return new Outline360Range(start, end, colorStart, colorEnd, blendStart, blendEnd);
    }
}
