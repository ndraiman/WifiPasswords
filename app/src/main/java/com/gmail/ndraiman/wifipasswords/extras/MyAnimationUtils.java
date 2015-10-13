package com.gmail.ndraiman.wifipasswords.extras;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;


public class MyAnimationUtils {


    public static void animateSunblind(View viewToAnimate, boolean goesDown) {
        int holderHeight = viewToAnimate.getHeight();
        viewToAnimate.setPivotY(goesDown ? 0 : holderHeight);
        viewToAnimate.setPivotX(viewToAnimate.getHeight());
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator animatorTranslateY = ObjectAnimator.ofFloat(viewToAnimate, "translationY", goesDown ? 300 : -300, 0);
        ObjectAnimator animatorRotation = ObjectAnimator.ofFloat(viewToAnimate, "rotationX", goesDown ? -90f : 90, 0f);
        ObjectAnimator animatorScaleX = ObjectAnimator.ofFloat(viewToAnimate, "scaleX", 0.5f, 1f);
        animatorSet.playTogether(animatorTranslateY, animatorRotation, animatorScaleX);
        animatorSet.setInterpolator(new DecelerateInterpolator(1.1f));
        animatorSet.setDuration(1000);
        animatorSet.start();
    }

    public static void translateY(View viewToAnimate, boolean goesDown) {

        //Animation using ObjectAnimator
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator animatorTranslateY = ObjectAnimator.ofFloat(viewToAnimate,
                "translationY",
                goesDown ? 100 : -100,
                0);

        animatorTranslateY.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorTranslateY.setDuration(500);
        animatorTranslateY.start();

    }

}
