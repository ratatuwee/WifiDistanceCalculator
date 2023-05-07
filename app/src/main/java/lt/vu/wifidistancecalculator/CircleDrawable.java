package lt.vu.wifidistancecalculator;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class CircleDrawable extends Drawable {

    private final Paint redPaint;
    private final float x;
    private final float y;
    private final float originalWidth;
    private final float originalHeight;

    public CircleDrawable(float x, float y, Drawable originalDrawable) {
        // Set up color and text size
        redPaint = new Paint();
        redPaint.setARGB(255, 255, 0, 0);
        this.x = x;
        this.y = y;
        Bitmap bitmap = ((BitmapDrawable)originalDrawable).getBitmap();
        // coordinates from web service and image dimensions are distorted for some weird reason
        float mobileFishRatio = 1.17F;
        originalWidth = bitmap.getWidth() / mobileFishRatio;
        originalHeight = bitmap.getHeight() / mobileFishRatio;
    }

    @Override
    public void draw(Canvas canvas) {
        // Get the drawable's bounds
        int width = getBounds().width();
        int height = getBounds().height();
        int radius = Math.min(width, height) / 100;

        float heightRatio = height / originalHeight;
        float widthRatio = width / originalWidth;

        // Draw a red circle in the center
        canvas.drawCircle(widthRatio * x, heightRatio * y, radius, redPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        // This method is required
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // This method is required
    }

    @Override
    public int getOpacity() {
        // Must be PixelFormat.UNKNOWN, TRANSLUCENT, TRANSPARENT, or OPAQUE
        return PixelFormat.OPAQUE;
    }

}
