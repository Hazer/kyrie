package com.github.alexjlockwood.kyrie;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class Node {

  @NonNull
  abstract Layer toLayer(PropertyTimeline timeline);

  interface Layer {
    void draw(Canvas canvas, Matrix parentMatrix, PointF viewportScale);

    void onDraw(Canvas canvas, Matrix parentMatrix, PointF viewportScale);
  }

  abstract static class Builder<N extends Node, B extends Builder<N, B>> {
    @NonNull final B self;

    Builder() {
      this.self = self();
    }

    @NonNull
    abstract B self();

    @NonNull
    public abstract N build();

    @NonNull
    final <T> B replaceFirstAnimation(List<Animation<?, T>> animations, Animation<?, T> animation) {
      Node.replaceFirstAnimation(animations, animation);
      return self;
    }

    @NonNull
    @SafeVarargs
    final <T> B replaceAnimations(
        List<Animation<?, T>> animations, Animation<?, T>... newAnimations) {
      Node.replaceAnimations(animations, newAnimations);
      return self;
    }

    @NonNull
    final <T> B replaceAnimations(
        List<Animation<?, T>> animations, List<Animation<?, T>> newAnimations) {
      Node.replaceAnimations(animations, newAnimations);
      return self;
    }
  }

  @NonNull
  static Animation<?, Float> asAnimation(float initialValue) {
    return Animation.ofFloat(initialValue, initialValue).duration(0);
  }

  @NonNull
  static Animation<?, Integer> asAnimation(@ColorInt int initialValue) {
    return Animation.ofArgb(initialValue, initialValue).duration(0);
  }

  @NonNull
  static Animation<?, float[]> asAnimation(float[] initialValue) {
    return Animation.ofFloatArray(initialValue, initialValue).duration(0);
  }

  @NonNull
  static Animation<?, PathData> asAnimation(PathData initialValue) {
    return Animation.ofPathMorph(initialValue, initialValue).duration(0);
  }

  @NonNull
  static List<Animation<?, Float>> asAnimations(float initialValue) {
    return asList(asAnimation(initialValue));
  }

  @NonNull
  static List<Animation<?, Integer>> asAnimations(int initialValue) {
    return asList(asAnimation(initialValue));
  }

  @NonNull
  static List<Animation<?, float[]>> asAnimations(float[] initialValue) {
    return asList(asAnimation(initialValue));
  }

  @NonNull
  static List<Animation<?, PathData>> asAnimations(PathData initialValue) {
    return asList(asAnimation(initialValue));
  }

  @NonNull
  private static <T> List<Animation<?, T>> asList(Animation<?, T> animation) {
    final List<Animation<?, T>> animations = new ArrayList<>(1);
    animations.add(animation);
    return animations;
  }

  static <T> void replaceFirstAnimation(
      List<Animation<?, T>> animations, Animation<?, T> animation) {
    animations.set(0, animation);
  }

  @SafeVarargs
  static <T> void replaceAnimations(
      List<Animation<?, T>> animations, Animation<?, T>... newAnimations) {
    for (int i = animations.size() - 1; i > 0; i--) {
      animations.remove(i);
    }
    Collections.addAll(animations, newAnimations);
  }

  static <T> void replaceAnimations(
      List<Animation<?, T>> animations, List<Animation<?, T>> newAnimations) {
    for (int i = animations.size() - 1; i > 0; i--) {
      animations.remove(i);
    }
    animations.addAll(newAnimations);
  }
}
