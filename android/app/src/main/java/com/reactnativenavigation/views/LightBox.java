package com.reactnativenavigation.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.BlurMaskFilter;
import android.os.Build;
import android.renderscript.RenderScript;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.ImageView;
import android.util.Log;

import com.reactnativenavigation.R;
import com.reactnativenavigation.params.LightBoxParams;
import com.reactnativenavigation.screens.Screen;
import com.reactnativenavigation.utils.ViewUtils;

import jp.wasabeef.blurry.Blurry;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class LightBox extends Dialog implements DialogInterface.OnDismissListener {

	private Screen currentActiveScreen;
    private Runnable onDismissListener;
    private ContentView content;
    private RelativeLayout lightBox;
    private boolean cancelable;

    public LightBox(AppCompatActivity activity, Runnable onDismissListener, LightBoxParams params, Screen currentActiveScreen) {
        super(activity, R.style.LightBox);
        this.onDismissListener = onDismissListener;
		this.cancelable = !params.overrideBackPress;
		this.currentActiveScreen = currentActiveScreen;
        setOnDismissListener(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        createContent(activity, params);
        setCancelable(cancelable);
        getWindow().setWindowAnimations(android.R.style.Animation);
        getWindow().setSoftInputMode(params.adjustSoftInput);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
	}

    private void createContent(final Context context, LightBoxParams params) {
        lightBox = new RelativeLayout(context);
        lightBox.setAlpha(0);
        content = new ContentView(context, params.screenId, params.navigationParams);
		content.setAlpha(0);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		lp.addRule(RelativeLayout.CENTER_IN_PARENT, content.getId());
		lightBox.addView(content, lp);

        if (params.tapBackgroundToDismiss) {
            lightBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hide();
                }
            });
        }

        content.setOnDisplayListener(new Screen.OnDisplayListener() {
            @Override
            public void onDisplay() {
                content.getLayoutParams().height = content.getChildAt(0).getHeight();
                content.getLayoutParams().width = content.getChildAt(0).getWidth();
				content.setBackgroundColor(Color.TRANSPARENT);
                ViewUtils.runOnPreDraw(content, new Runnable() {
                    @Override
                    public void run() {
                        animateShow();
                    }
                });
            }
        });
        setContentView(lightBox, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void hide() {
        animateHide();
    }

    @Override public void onBackPressed() {
        if (cancelable) {
            hide();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        onDismissListener.run();
    }

    public void destroy() {
        if (content != null) {
            content.unmountReactView();
            lightBox.removeAllViews();
            content = null;
        }
        dismiss();
    }

    private void animateShow() {
        ObjectAnimator yTranslation = ObjectAnimator.ofFloat(content, View.TRANSLATION_Y, 80, 0).setDuration(400);
		yTranslation.setInterpolator(new FastOutSlowInInterpolator());
        yTranslation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
				ImageView imageView = new ImageView(getContext());

				Blurry.with(getContext())
					.radius(30).sampling(2).color(Color.argb(100, 0, 0, 0))
					.capture(currentActiveScreen.getContentView()).into(imageView);

				lightBox.setBackgroundDrawable(imageView.getDrawable());

				content.setAlpha(1);
            }
        });

        ObjectAnimator lightBoxAlpha = ObjectAnimator.ofFloat(lightBox, View.ALPHA, 0, 1).setDuration(70);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(lightBoxAlpha, yTranslation);
        animatorSet.start();
	}

	private static Bitmap loadBitmapFromView(View v) {
		Log.d("height:", "" + v.getLayoutParams().height);
		Bitmap b = Bitmap.createBitmap( v.getLayoutParams().width, v.getLayoutParams().height, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
		v.draw(c);
		return b;
	}

    private void animateHide() {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(content, View.ALPHA, 0);
        ObjectAnimator yTranslation = ObjectAnimator.ofFloat(content, View.TRANSLATION_Y, 60);
        AnimatorSet contentAnimators = new AnimatorSet();
        contentAnimators.playTogether(alpha, yTranslation);
        contentAnimators.setDuration(150);

        ObjectAnimator lightBoxAlpha = ObjectAnimator.ofFloat(lightBox, View.ALPHA, 0).setDuration(100);

        AnimatorSet allAnimators = new AnimatorSet();
        allAnimators.playSequentially(contentAnimators, lightBoxAlpha);
        allAnimators.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                destroy();
            }
        });
        allAnimators.start();
    }
}
