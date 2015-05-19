package android.view.ext;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Provides a "Path" like menu for android. ??
 * 
 * TODO: tell about usage
 * 
 * @author Siyamed SINIR
 * 
 */
public class SatelliteMenu extends FrameLayout {

	private static final int DEFAULT_SATELLITE_DISTANCE = 220;
	private static final float DEFAULT_TOTAL_SPACING_DEGREES = 90f;
	private static final boolean DEFAULT_CLOSE_ON_CLICK = true;
	private static final int DEFAULT_EXPAND_DURATION = 300;

	private Animation mainRotateRight;
	private Animation mainRotateLeft;

	private ImageView imgMain;
	private SateliteClickedListener itemClickedListener;

	private List<SatelliteMenuItem> menuItems = new ArrayList<SatelliteMenuItem>();
	private Map<View, SatelliteMenuItem> viewToItemMap = new HashMap<View, SatelliteMenuItem>();

	private AtomicBoolean plusAnimationActive = new AtomicBoolean(false);

	//States of these variables are saved
	private boolean rotated = false;
	private int measureDiff = 0;
	//States of these variables are saved - Also configured from XML 
	private float totalSpacingDegree = DEFAULT_TOTAL_SPACING_DEGREES;
	private int satelliteDistance = DEFAULT_SATELLITE_DISTANCE;	
	private int expandDuration = DEFAULT_EXPAND_DURATION;
	private boolean closeItemsOnClick = DEFAULT_CLOSE_ON_CLICK;

	public SatelliteMenu(Context context) {
		super(context);
		init(context, null, 0);
	}

	public SatelliteMenu(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, 0);
	}

	public SatelliteMenu(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}

	//初始化
	private void init(Context context, AttributeSet attrs, int defStyle) {
		LayoutInflater.from(context).inflate(R.layout.sat_main, this, true);		
		imgMain = (ImageView) findViewById(R.id.sat_main);

		if(attrs != null){			
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SatelliteMenu, defStyle, 0);					
			satelliteDistance = typedArray.getDimensionPixelSize(R.styleable.SatelliteMenu_satelliteDistance, DEFAULT_SATELLITE_DISTANCE);
			totalSpacingDegree = typedArray.getFloat(R.styleable.SatelliteMenu_totalSpacingDegree, DEFAULT_TOTAL_SPACING_DEGREES);
			closeItemsOnClick = typedArray.getBoolean(R.styleable.SatelliteMenu_closeOnClick, DEFAULT_CLOSE_ON_CLICK);
			expandDuration = typedArray.getInt(R.styleable.SatelliteMenu_expandDuration, DEFAULT_EXPAND_DURATION);
			//float satelliteDistance = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170, getResources().getDisplayMetrics());
			typedArray.recycle();
		}
		
		//获取主图左右旋转的animation
		mainRotateLeft = SatelliteAnimationCreator.createMainButtonAnimation(context);
		mainRotateRight = SatelliteAnimationCreator.createMainButtonInverseAnimation(context);

		//时刻监听
		Animation.AnimationListener plusAnimationListener = new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				plusAnimationActive.set(false);
			}
		};

		mainRotateLeft.setAnimationListener(plusAnimationListener);
		mainRotateRight.setAnimationListener(plusAnimationListener);

		//点击主图
		imgMain.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SatelliteMenu.this.onClick();
			}
		});

	}
	
	//点击主图事件
	private void onClick() {
		if (plusAnimationActive.compareAndSet(false, true)) {
			if (!rotated) {
				imgMain.startAnimation(mainRotateLeft);
				for (SatelliteMenuItem item : menuItems) {
					item.getView().startAnimation(item.getOutAnimation());
				}
				this.closeItemsOnClick = true;
			} else {
				imgMain.startAnimation(mainRotateRight);
				for (SatelliteMenuItem item : menuItems) {
					item.getView().startAnimation(item.getInAnimation());
				}
				this.closeItemsOnClick = false;
			}
			rotated = !rotated;
		}
	}

	//关闭整个控件
	private void closeItems() {
		if (plusAnimationActive.compareAndSet(false, true)) {
			if (rotated) {
				imgMain.startAnimation(mainRotateRight);
				for (SatelliteMenuItem item : menuItems) {
					item.getView().startAnimation(item.getInAnimation());
				}
				
			}
			rotated = !rotated;
		}
	}
	
	//给控件添加一个Item
	public void addItems(List<SatelliteMenuItem> items) {

		menuItems.addAll(items);
		this.removeView(imgMain);
		

		float[] degrees = getDegrees(menuItems.size(),totalSpacingDegree);		//得到每个item的角度
		int index = 0;
		for (SatelliteMenuItem menuItem : menuItems) {
			//得到每个item距离主图的最终位置
			int finalX = SatelliteAnimationCreator.getTranslateX(
					degrees[index], satelliteDistance);
			int finalY = SatelliteAnimationCreator.getTranslateY(
					degrees[index], satelliteDistance);

			ImageView itemView = (ImageView) LayoutInflater.from(getContext())
					.inflate(R.layout.sat_item_cr, this, false);
			ImageView cloneView = (ImageView) LayoutInflater.from(getContext())
					.inflate(R.layout.sat_item_cr, this, false);
			itemView.setTag(menuItem.getId());		//获得的这个item的id
			itemView.setVisibility(View.GONE);		//刚开始view不可见
			
			cloneView.setVisibility(View.GONE);
			cloneView.setTag(menuItem.getId());
			//cloneView的点击事件监听
			cloneView.setOnClickListener(new InternalSatelliteOnClickListener(this));
			
			FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) cloneView.getLayoutParams();
			layoutParams.bottomMargin = Math.abs(finalY);
			layoutParams.rightMargin = Math.abs(finalX);
			cloneView.setLayoutParams(layoutParams);

			if (menuItem.getImgResourceId() > 0) {
				itemView.setImageResource(menuItem.getImgResourceId());
				cloneView.setImageResource(menuItem.getImgResourceId());
			}
			
			//获得每个item的动画效果
			Animation itemOut = SatelliteAnimationCreator.createItemOutAnimation(getContext(), index,expandDuration, -finalX, finalY);
			Animation itemIn = SatelliteAnimationCreator.createItemInAnimation(getContext(), index, expandDuration, -finalX, finalY);
			Animation itemClick = SatelliteAnimationCreator.createItemClickAnimation(getContext());
			
			//设置每个item的属性
			menuItem.setView(itemView);
			menuItem.setCloneView(cloneView);
			menuItem.setInAnimation(itemIn);
			menuItem.setOutAnimation(itemOut);
			menuItem.setClickAnimation(itemClick);
			menuItem.setFinalX(finalX);
			menuItem.setFinalY(finalY);
			
			//给动画添加监听事件
			itemIn.setAnimationListener(new SatelliteAnimationListener(itemView, true, viewToItemMap));
			itemOut.setAnimationListener(new SatelliteAnimationListener(itemView, false, viewToItemMap));
			itemClick.setAnimationListener(new SatelliteItemClickAnimationListener(this, menuItem.getId()));
			
			this.addView(itemView);
			this.addView(cloneView);
			viewToItemMap.put(itemView, menuItem);
			viewToItemMap.put(cloneView, menuItem); 
			index++;
		}

		this.addView(imgMain);
	}
	
	//cloneView的点击事件监听的实现
	private static class InternalSatelliteOnClickListener implements View.OnClickListener {
		private WeakReference<SatelliteMenu> menuRef;
		
		public InternalSatelliteOnClickListener(SatelliteMenu menu) {
			this.menuRef = new WeakReference<SatelliteMenu>(menu);
		}

		@Override
		public void onClick(View v) {
			SatelliteMenu menu = menuRef.get();
			if(menu != null){
				SatelliteMenuItem menuItem = menu.getViewToItemMap().get(v);
				v.startAnimation(menuItem.getClickAnimation());	
			}
		}
	}

	//获得每个item的角度  
	private float[] getDegrees(int count,float totalDegrees) {
		if(count < 1)
        {
            return new float[]{};
        }

        float[] result = new float[count];
        float delta = totalDegrees / (count-1);
        
        for(int index=0; index<count; index++){
            result[index] = index * delta;
        }
        
        return result;
	}

	//屏幕控件位置调整
	private void recalculateMeasureDiff() {
		int itemWidth = 0;
		if (menuItems.size() > 0) {
			itemWidth = menuItems.get(0).getView().getWidth();
		}
		measureDiff = Float.valueOf(satelliteDistance * 0.2f).intValue()
				+ itemWidth;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		recalculateMeasureDiff();

		int totalHeight = imgMain.getHeight() + satelliteDistance + measureDiff;
		int totalWidth = imgMain.getWidth() + satelliteDistance + measureDiff;
		setMeasuredDimension(totalWidth, totalHeight);
	}
	
	//每个item展开收入时的动画监听事件实现
	private static class SatelliteAnimationListener implements Animation.AnimationListener {
		private WeakReference<View> viewRef;
		private boolean isInAnimation;
		private Map<View, SatelliteMenuItem> viewToItemMap;

		public SatelliteAnimationListener(View view, boolean isIn, Map<View, SatelliteMenuItem> viewToItemMap) {
			this.viewRef = new WeakReference<View>(view);
			this.isInAnimation = isIn;
			this.viewToItemMap = viewToItemMap;
		}

		//动画开始时
		@Override
		public void onAnimationStart(Animation animation) {
			if (viewRef != null) {
				View view = viewRef.get();
				if (view != null) {
					SatelliteMenuItem menuItem = viewToItemMap.get(view);
					if (isInAnimation) {
						//item收入动画开始时view可见
						menuItem.getView().setVisibility(View.VISIBLE);
						menuItem.getCloneView().setVisibility(View.GONE);
					} else {
						//item展开动画开始时view可见
						menuItem.getCloneView().setVisibility(View.GONE);
						menuItem.getView().setVisibility(View.VISIBLE);
					}
				}
			}
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

		//动画结束时
		@Override
		public void onAnimationEnd(Animation animation) {
			if (viewRef != null) {
				View view = viewRef.get();
				if (view != null) {
					SatelliteMenuItem menuItem = viewToItemMap.get(view);

					if (isInAnimation) {
						//item收入动画结束时view均不可见
						menuItem.getView().setVisibility(View.GONE);
						menuItem.getCloneView().setVisibility(View.GONE);
					} else {
						//item展开动画结束时cloneview可见
						menuItem.getCloneView().setVisibility(View.VISIBLE);
						menuItem.getView().setVisibility(View.GONE);
					}
				}
			}
		}
	}

	//某个item被点击时的动画监听事件实现
	private static class SatelliteItemClickAnimationListener implements Animation.AnimationListener {
		private WeakReference<SatelliteMenu> menuRef;
		private int tag;	//人为设定的每个item的id
		
		public SatelliteItemClickAnimationListener(SatelliteMenu menu, int tag) {
			this.menuRef = new WeakReference<SatelliteMenu>(menu);
			this.tag = tag;
		}
		
		@Override
		public void onAnimationEnd(Animation animation) {
		}
		
		@Override
		public void onAnimationRepeat(Animation animation) {
		}
		
		//点击item后动画开始时调用接口类中的eventOccured（）函数，把tag实参传入，可知道点击的是哪一个item
		@Override
		public void onAnimationStart(Animation animation) {
			SatelliteMenu menu = menuRef.get();
			if(menu != null && menu.closeItemsOnClick){
				menu.closeItems();
				menu.closeItemsOnClick = false;
				if(menu.itemClickedListener != null){
					menu.itemClickedListener.eventOccured(tag);
				}
			}
		}		
	}

	//接口，某个item点击后可得到这个item的id，这个id是在添加item时给定的
	public interface SateliteClickedListener {
		//使用者在使用这个项目时，需要重写下面的函数，来实现自己想要的操作
		public void eventOccured(int id);
	}
	
	public Map<View, SatelliteMenuItem> getViewToItemMap() {
		return viewToItemMap;
	}
	
	//在新的项目中改变menu的属性时重新将item添加一遍
	private void resetItems() {
		if (menuItems.size() > 0) {
			List<SatelliteMenuItem> items = new ArrayList<SatelliteMenuItem>(
					menuItems);
			menuItems.clear();
			this.removeAllViews();
			addItems(items);
		}
	}
	
	/*下面是一系列set函数*/
	
	//点击这个控件的item时触发的事件响应的set函数
	public void setOnItemClickedListener(SateliteClickedListener itemClickedListener) {
		this.itemClickedListener = itemClickedListener;
	}

	public void setTotalSpacingDegree(float totalSpacingDegree) {
		this.totalSpacingDegree = totalSpacingDegree;
		resetItems();
	}

	public void setSatelliteDistance(int distance) {
		this.satelliteDistance = distance;
		resetItems();
	}

	public void setExpandDuration(int expandDuration) {
		this.expandDuration = expandDuration;
		resetItems();
	}
	
	public void setMainImage(int resource) {
		this.imgMain.setImageResource(resource);
	}

	public void setCloseItemsOnClick(boolean closeItemsOnClick) {
		this.closeItemsOnClick = closeItemsOnClick;
	}


}
