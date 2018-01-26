package com.github.alexjlockwood.kyrie;

import android.animation.TimeInterpolator;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

// TODO: make sure to cover case where < 2 values/keyframes are provided
// TODO: pass a spread of values (instead of just 2) and auto-generate evenly spaced keyframes?
public final class PropertyAnimation<T, V> {

  @NonNull
  public static PropertyAnimation<Float, Float> ofFloat(float startValue, float endValue) {
    return ofObject(new FloatValueEvaluator(), startValue, endValue);
  }

  @NonNull
  @SafeVarargs
  public static PropertyAnimation<Float, Float> ofFloat(Keyframe<Float>... keyframes) {
    return ofKeyframes(new FloatValueEvaluator(), keyframes);
  }

  @NonNull
  public static PropertyAnimation<Integer, Integer> ofArgb(
      @ColorInt int startValue, @ColorInt int endValue) {
    return ofObject(new ArgbValueEvaluator(), startValue, endValue);
  }

  @NonNull
  @SafeVarargs
  public static PropertyAnimation<Integer, Integer> ofArgb(Keyframe<Integer>... keyframes) {
    return ofKeyframes(new ArgbValueEvaluator(), keyframes);
  }

  @NonNull
  public static PropertyAnimation<float[], float[]> ofFloatArray(
      @NonNull float[] startValue, @NonNull float[] endValue) {
    return ofObject(new FloatArrayValueEvaluator(), startValue, endValue);
  }

  @NonNull
  @SafeVarargs
  public static PropertyAnimation<float[], float[]> ofFloatArray(
      @NonNull Keyframe<float[]>... keyframes) {
    return ofKeyframes(new FloatArrayValueEvaluator(), keyframes);
  }

  @NonNull
  public static PropertyAnimation<PathData, PathData> ofPathMorph(
      @NonNull PathData startValue, @NonNull PathData endValue) {
    return ofObject(new PathDataValueEvaluator(), startValue, endValue);
  }

  @NonNull
  @SafeVarargs
  public static PropertyAnimation<PathData, PathData> ofPathMorph(
      @NonNull Keyframe<PathData>... keyframes) {
    return ofKeyframes(new PathDataValueEvaluator(), keyframes);
  }

  @NonNull
  @SafeVarargs
  private static <V> PropertyAnimation<V, V> ofObject(
      @NonNull ValueEvaluator<V> evaluator, V... values) {
    return new PropertyAnimation<>(
        KeyframeSet.ofObject(evaluator, values), new NoopValueTransformer<V>());
  }

  @NonNull
  @SafeVarargs
  private static <V> PropertyAnimation<V, V> ofKeyframes(
      @NonNull ValueEvaluator<V> evaluator, Keyframe<V>... keyframes) {
    return new PropertyAnimation<>(
        KeyframeSet.ofKeyframes(evaluator, keyframes), new NoopValueTransformer<V>());
  }

  public static PropertyAnimation<PointF, PointF> ofPathMotion(@NonNull Path path) {
    return new PropertyAnimation<>(KeyframeSet.ofPath(path), new NoopValueTransformer<PointF>());
  }

  /**
   * This value used used with the {@link #repeatCount(int)} property to repeat the animation
   * indefinitely. Also used to indicate infinite duration.
   */
  public static final int INFINITE = -1;

  @NonNull private final KeyframeSet<T> keyframeSet;

  @IntRange(from = 0L)
  private long startDelay;

  @IntRange(from = 0L)
  private long duration = 300;

  @NonNull private final ValueTransformer<T, V> transformer;
  @Nullable private TimeInterpolator interpolator;

  private int repeatCount;
  @RepeatMode private int repeatMode = RepeatMode.RESTART;

  private PropertyAnimation(
      @NonNull KeyframeSet<T> keyframeSet, @NonNull ValueTransformer<T, V> transformer) {
    this.keyframeSet = keyframeSet;
    this.transformer = transformer;
  }

  public long getStartDelay() {
    return startDelay;
  }

  @NonNull
  public PropertyAnimation<T, V> startDelay(@IntRange(from = 0L) long startDelay) {
    this.startDelay = startDelay;
    return this;
  }

  public long getDuration() {
    return duration;
  }

  @NonNull
  public PropertyAnimation<T, V> duration(@IntRange(from = 0L) long duration) {
    this.duration = duration;
    return this;
  }

  /**
   * Defines how many times the animation should repeat. The default value is 0.
   *
   * @return the number of times the animation should repeat, or {@link #INFINITE}
   */
  public int getRepeatCount() {
    return repeatCount;
  }

  /**
   * Sets how many times the animation should be repeated. If the repeat count is 0, the animation
   * is never repeated. If the repeat count is greater than 0 or {@link #INFINITE}, the repeat mode
   * will be taken into account. The repeat count is 0 by default.
   *
   * @param repeatCount the number of times the animation should be repeated
   */
  public PropertyAnimation<T, V> repeatCount(int repeatCount) {
    this.repeatCount = repeatCount;
    return this;
  }

  /**
   * Defines what this animation should do when it reaches the end.
   *
   * @return either one of {@link RepeatMode#RESTART} or {@link RepeatMode#REVERSE}
   */
  @RepeatMode
  public int getRepeatMode() {
    return repeatMode;
  }

  /**
   * Defines what this animation should do when it reaches the end. This setting is applied only
   * when the repeat count is either greater than 0 or {@link #INFINITE}. Defaults to {@link
   * RepeatMode#RESTART}.
   *
   * @param repeatMode {@link RepeatMode#RESTART} or {@link RepeatMode#REVERSE}
   */
  public PropertyAnimation<T, V> repeatMode(@RepeatMode int repeatMode) {
    this.repeatMode = repeatMode;
    return this;
  }

  @Nullable
  public TimeInterpolator getInterpolator() {
    return interpolator;
  }

  @NonNull
  public PropertyAnimation<T, V> interpolator(@Nullable TimeInterpolator interpolator) {
    this.interpolator = interpolator;
    return this;
  }

  public long getTotalDuration() {
    if (repeatCount == INFINITE) {
      return INFINITE;
    } else {
      return startDelay + (duration * (repeatCount + 1));
    }
  }

  @NonNull
  public V getAnimatedValue(float fraction) {
    return transformer.transform(keyframeSet.getAnimatedValue(fraction));
  }

  public <W> PropertyAnimation<T, W> transform(@NonNull ValueTransformer<T, W> transformer) {
    return new PropertyAnimation<>(keyframeSet, transformer)
        .startDelay(startDelay)
        .duration(duration)
        .repeatCount(repeatCount)
        .repeatMode(repeatMode)
        .interpolator(interpolator);
  }

  interface ValueEvaluator<V> {
    @NonNull
    V evaluate(float fraction, @NonNull V startValue, @NonNull V endValue);
  }

  private static final class FloatValueEvaluator implements ValueEvaluator<Float> {
    @NonNull
    @Override
    public Float evaluate(float fraction, @NonNull Float startValue, @NonNull Float endValue) {
      return startValue + (endValue - startValue) * fraction;
    }
  }

  private static final class ArgbValueEvaluator implements ValueEvaluator<Integer> {

    @NonNull
    @Override
    public Integer evaluate(
        float fraction, @NonNull Integer startValue, @NonNull Integer endValue) {
      int startInt = startValue;
      float startA = ((startInt >> 24) & 0xff) / 255.0f;
      float startR = ((startInt >> 16) & 0xff) / 255.0f;
      float startG = ((startInt >> 8) & 0xff) / 255.0f;
      float startB = (startInt & 0xff) / 255.0f;
      int endInt = endValue;
      float endA = ((endInt >> 24) & 0xff) / 255.0f;
      float endR = ((endInt >> 16) & 0xff) / 255.0f;
      float endG = ((endInt >> 8) & 0xff) / 255.0f;
      float endB = (endInt & 0xff) / 255.0f;
      // transform from sRGB to linear
      startR = (float) Math.pow(startR, 2.2);
      startG = (float) Math.pow(startG, 2.2);
      startB = (float) Math.pow(startB, 2.2);
      endR = (float) Math.pow(endR, 2.2);
      endG = (float) Math.pow(endG, 2.2);
      endB = (float) Math.pow(endB, 2.2);
      // compute the interpolated color in linear space
      float a = startA + fraction * (endA - startA);
      float r = startR + fraction * (endR - startR);
      float g = startG + fraction * (endG - startG);
      float b = startB + fraction * (endB - startB);
      // transform back to sRGB in the [0..255] range
      a = a * 255.0f;
      r = (float) Math.pow(r, 1.0 / 2.2) * 255.0f;
      g = (float) Math.pow(g, 1.0 / 2.2) * 255.0f;
      b = (float) Math.pow(b, 1.0 / 2.2) * 255.0f;
      return Math.round(a) << 24 | Math.round(r) << 16 | Math.round(g) << 8 | Math.round(b);
    }
  }

  private static final class FloatArrayValueEvaluator implements ValueEvaluator<float[]> {
    @Nullable private float[] array;

    @NonNull
    @Override
    public float[] evaluate(
        float fraction, @NonNull float[] startValue, @NonNull float[] endValue) {
      if (array == null || array.length != startValue.length) {
        array = new float[startValue.length];
      }
      for (int i = 0; i < array.length; i++) {
        float start = startValue[i];
        float end = endValue[i];
        array[i] = start + (fraction * (end - start));
      }
      return array;
    }
  }

  private static final class PathDataValueEvaluator implements ValueEvaluator<PathData> {
    @Nullable private PathData pathData;

    @NonNull
    @Override
    public PathData evaluate(
        float fraction, @NonNull PathData startValue, @NonNull PathData endValue) {
      if (pathData == null || !pathData.canMorphWith(startValue)) {
        pathData = new PathData(startValue);
      }
      pathData.interpolate(startValue, endValue, fraction);
      return pathData;
    }
  }

  public interface ValueTransformer<T, V> {
    @NonNull
    V transform(T value);
  }

  private static class NoopValueTransformer<V> implements ValueTransformer<V, V> {
    @NonNull
    @Override
    public V transform(V value) {
      return value;
    }
  }
}
