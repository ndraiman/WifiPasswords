package com.gmail.ndrdevelop.wifipasswords.extras;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;


public class MyAnimationUtils {

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
