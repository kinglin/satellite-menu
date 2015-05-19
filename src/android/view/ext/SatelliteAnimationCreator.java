package android.view.ext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.view.ext.R;

/**
 * Factory class for creating satellite in/out animations
 * 
 * @author Siyamed SINIR
 *
 */
public class SatelliteAnimationCreator {
    
	//Item收入时的动画
    @SuppressLint("NewApi")
	public static Animation createItemInAnimation(Context context, int index, long expandDuration, int x, int y){        
        //旋转
    	RotateAnimation rotate = new RotateAnimation(270, 0, 
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        
        rotate.setInterpolator(context, R.anim.sat_item_in_rotate_interpolator);
        rotate.setDuration(expandDuration);
        
        //位移
        TranslateAnimation translate = new TranslateAnimation(x, 0, y, 0);
        
        long delay = 250;
        if(expandDuration <= 250){
            delay = expandDuration / 3;
        }         
        
        long duration = 400;
        if((expandDuration-delay) > duration){
        	duration = expandDuration-delay; 
        }
        
        translate.setDuration(duration);
        translate.setStartOffset(delay);        
        translate.setInterpolator(context, R.anim.sat_item_anticipate_interpolator);
        
        //透明度
        AlphaAnimation alphaAnimation = new AlphaAnimation(1f, 0f);
        long alphaDuration = 10;
        
        alphaAnimation.setDuration(alphaDuration);
        alphaAnimation.setStartOffset((delay + duration) - alphaDuration);
        
        //整体添加
        AnimationSet animationSet = new AnimationSet(false);
        animationSet.setFillAfter(false);
        animationSet.setFillBefore(true);
        animationSet.setFillEnabled(true);
        
        animationSet.addAnimation(alphaAnimation);
        animationSet.addAnimation(rotate);
        animationSet.addAnimation(translate);
        
        animationSet.setStartOffset(30*index);
        animationSet.start();
        animationSet.startNow();
        return animationSet;
    }
    
    //Item展开时的动画
    @SuppressLint("NewApi")
	public static Animation createItemOutAnimation(Context context, int index, long expandDuration, int x, int y){
    	//位移
        TranslateAnimation translate = new TranslateAnimation(0, x, 0, y);
         
        translate.setStartOffset(0);
        translate.setDuration(expandDuration);        
        translate.setInterpolator(context, R.anim.sat_item_overshoot_interpolator);
          
        //整体添加
        AnimationSet animationSet = new AnimationSet(false);
        animationSet.setFillAfter(false);
        animationSet.setFillBefore(true);
        animationSet.setFillEnabled(true);
        
        animationSet.addAnimation(translate);
        
        animationSet.setStartOffset(30*index);
        
        return animationSet;
    }
    
    public static Animation createMainButtonAnimation(Context context){
    	return AnimationUtils.loadAnimation(context, R.anim.sat_main_rotate_left);
    }
    
    public static Animation createMainButtonInverseAnimation(Context context){
    	return AnimationUtils.loadAnimation(context, R.anim.sat_main_rotate_right);
    }
    //某个item点击后的效果
    public static Animation createItemClickAnimation(Context context){
    	return AnimationUtils.loadAnimation(context, R.anim.sat_item_anim_click);
    }

    //得到每个item最后的位置的x坐标
    public static int getTranslateX(float degree, int distance){
        return Double.valueOf(distance * Math.cos(Math.toRadians(degree))).intValue();
    }
    //得到每个item最后的位置的y坐标
    public static int getTranslateY(float degree, int distance){
        return Double.valueOf(-1 * distance * Math.sin(Math.toRadians(degree))).intValue();
    }

}
