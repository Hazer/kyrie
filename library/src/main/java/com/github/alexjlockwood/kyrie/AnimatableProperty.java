package com.github.alexjlockwood.kyrie;

import android.animation.TimeInterpolator;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class AnimatableProperty<V> {
  private static final TimeInterpolator DEFAULT_INTERPOLATOR = new LinearInterpolator();
  private static final Comparator<PropertyAnimation<?, ?>> ANIMATION_COMPARATOR =
      new Comparator<PropertyAnimation<?, ?>>() {
        @Override
        public int compare(
            @NonNull PropertyAnimation<?, ?> a1, @NonNull PropertyAnimation<?, ?> a2) {
          // Animations with smaller start times are sorted first.
          final long s1 = a1.getStartDelay();
          final long s2 = a2.getStartDelay();
          if (s1 != s2) {
            return s1 < s2 ? -1 : 1;
          }
          final long d1 = a1.getTotalDuration();
          final long d2 = a2.getTotalDuration();
          if (d1 == PropertyAnimation.INFINITE || d2 == PropertyAnimation.INFINITE) {
            // Infinite animations are sorted last.
            return d1 == d2 ? 0 : d1 == PropertyAnimation.INFINITE ? 1 : -1;
          }
          // Animations with smaller end times are sorted first.
          final long e1 = s1 + d1;
          final long e2 = s2 + d2;
          return e1 < e2 ? -1 : e1 > e2 ? 1 : 0;
        }
      };

  @NonNull private final List<PropertyAnimation<?, V>> animations;
  private final List<Listener> listeners = new ArrayList<>();
  private final long totalDuration;
  private long currentPlayTime;

  public AnimatableProperty(@NonNull List<PropertyAnimation<?, V>> animations) {
    this.animations = new ArrayList<>(animations);
    Collections.sort(this.animations, ANIMATION_COMPARATOR);
    long totalDuration = 0;
    for (int i = 0, size = this.animations.size(); i < size; i++) {
      final long currTotalDuration = this.animations.get(i).getTotalDuration();
      if (currTotalDuration == PropertyAnimation.INFINITE) {
        totalDuration = PropertyAnimation.INFINITE;
        break;
      }
      totalDuration = Math.max(currTotalDuration, totalDuration);
    }
    this.totalDuration = totalDuration;
  }

  public long getTotalDuration() {
    return totalDuration;
  }

  public void setCurrentPlayTime(@IntRange(from = 0L) long currentPlayTime) {
    if (currentPlayTime < 0) {
      currentPlayTime = 0;
    } else if (totalDuration != PropertyAnimation.INFINITE && totalDuration < currentPlayTime) {
      currentPlayTime = totalDuration;
    }
    if (this.currentPlayTime != currentPlayTime) {
      this.currentPlayTime = currentPlayTime;
      // TODO: optimize this by notifying only when we know the computed value has changed
      // TODO: add a computeValue() method or something on PropertyAnimation?
      notifyListeners();
    }
  }

  public void addListener(@NonNull Listener listener) {
    listeners.add(listener);
  }

  private void notifyListeners() {
    for (int i = 0, size = listeners.size(); i < size; i++) {
      listeners.get(i).onCurrentPlayTimeChanged(this);
    }
  }

  @NonNull
  private PropertyAnimation<?, V> getCurrentAnimation() {
    // TODO: can this search be faster?
    final int size = animations.size();
    final PropertyAnimation<?, V> lastAnimation = animations.get(size - 1);
    if (lastAnimation.getStartDelay() <= currentPlayTime) {
      return lastAnimation;
    }
    PropertyAnimation<?, V> animation = lastAnimation;
    for (int i = size - 1; i >= 0; i--) {
      animation = animations.get(i);
      final long startTime = animation.getStartDelay();
      final long totalDuration = animation.getTotalDuration();
      if (startTime <= currentPlayTime
          && (totalDuration == PropertyAnimation.INFINITE
              || currentPlayTime <= startTime + totalDuration)) {
        break;
      }
    }
    return animation;
  }

  private static int getCurrentIteration(float fraction) {
    // If the overall fraction is a positive integer, we consider the current iteration to be
    // complete. In other words, the fraction for the current iteration would be 1, and the
    // current iteration would be overall fraction - 1.
    float iteration = (float) Math.floor(fraction);
    if (fraction == iteration && fraction > 0) {
      iteration--;
    }
    return (int) iteration;
  }

  /**
   * Returns the progress into the current animation between 0 and 1. This does not take into
   * account any interpolation that the animation may have.
   */
  private float getLinearCurrentAnimationFraction() {
    final PropertyAnimation<?, V> animation = getCurrentAnimation();
    final float startTime = animation.getStartDelay();
    final float duration = animation.getDuration();
    if (duration == 0) {
      return 1f;
    }
    final float fraction = (currentPlayTime - startTime) / duration;
    final int currentIteration = getCurrentIteration(fraction);
    final int repeatCount = animation.getRepeatCount();
    final int repeatMode = animation.getRepeatMode();
    float currentFraction = fraction - currentIteration;
    if (0 < currentIteration
        && repeatMode == RepeatMode.REVERSE
        && (currentIteration < repeatCount + 1 || repeatCount == PropertyAnimation.INFINITE)) {
      // TODO: when reversing, check if currentIteration % 2 == 0 instead
      if (currentIteration % 2 != 0) {
        currentFraction = 1 - currentFraction;
      }
    }
    return currentFraction;
  }

  /**
   * Takes the value of {@link #getLinearCurrentAnimationFraction()} and interpolates it with the
   * current animation's getInterpolator.
   */
  private float getInterpolatedCurrentAnimationFraction() {
    final PropertyAnimation<?, V> animation = getCurrentAnimation();
    TimeInterpolator interpolator = animation.getInterpolator();
    if (interpolator == null) {
      interpolator = DEFAULT_INTERPOLATOR;
    }
    return interpolator.getInterpolation(getLinearCurrentAnimationFraction());
  }

  @NonNull
  public V getAnimatedValue() {
    return getCurrentAnimation().getAnimatedValue(getInterpolatedCurrentAnimationFraction());
  }

  public interface Listener {
    void onCurrentPlayTimeChanged(@NonNull AnimatableProperty<?> property);
  }
}
