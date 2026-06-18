package com.siffmember.info.ui.view.indicator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import androidx.viewpager2.widget.ViewPager2;

public class ARIndicatorView extends IndicatorView {

    private RecyclerView recyclerView;
    private ViewPager2 viewPager2;

    private int selectedPosition = 0;
    private boolean isScrubbing = false;

    public ARIndicatorView(Context context) {
        super(context);
    }

    public ARIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ARIndicatorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void attachTo(RecyclerView recyclerView, boolean shouldPage) {
        this.recyclerView = recyclerView;

        addIndicators(recyclerView);

        if (shouldPage) {
            new PagerSnapHelper().attachToRecyclerView(recyclerView);
        }

        this.recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_SETTLING) {
                    if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                        int position;
                        if (dx > 0) {
                            position = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                        } else {
                            position = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                        }
                        if (position <= indicators.size() - 1) {
                            if (selectedPosition != position) {
                                selectIndicatorAt(position);
                                if (indicatorAnimation != 0) {
                                    animateIndicator(position);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    public void attachTo(ViewPager2 viewPager2) {
        this.viewPager2 = viewPager2;

        addIndicators(viewPager2);

        this.viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                selectIndicatorAt(position);
                if (indicatorAnimation != 0) {
                    animateIndicator(position);
                }
            }
        });
    }

    private void addIndicators(RecyclerView recyclerView) {
        if (recyclerView != null) {
            RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
            if (adapter != null) {
                for (int i = 0; i < adapter.getItemCount(); i++) {
                    drawCircle(i);
                }
            } else {
                throw new NullPointerException("RecyclerView Adapter not found or null --> ARIndicatorView");
            }
        } else {
            throw new NullPointerException("RecyclerView is null --> ARIndicatorView");
        }
    }

    private void addIndicators(ViewPager2 viewPager2) {
        if (viewPager2 != null) {
            RecyclerView.Adapter<?> adapter = viewPager2.getAdapter();
            if (adapter != null) {
                for (int i = 0; i < adapter.getItemCount(); i++) {
                    drawCircle(i);
                }
            } else {
                throw new NullPointerException("ViewPager2 Adapter is null --> ARIndicatorView");
            }
        } else {
            throw new NullPointerException("ViewPager2 is null --> ARIndicatorView");
        }
    }

    private void unSelectIndicators() {
        for (int i = 0; i < indicators.size(); i++) {
            this.setUnActiveColorTo(indicators.get(i));
        }
    }

    private void invalidateIndicators() {
        this.removeIndicators();
        if (recyclerView != null) {
            addIndicators(recyclerView);
        } else if (viewPager2 != null) {
            addIndicators(viewPager2);
        }
        this.selectIndicatorAt(this.selectedPosition);
    }

    private void selectIndicatorAt(int position) {
        this.selectedPosition = position;
        this.unSelectIndicators();
        this.setActiveColorTo(this.indicators.get(this.selectedPosition));
    }

    public void setSelectionColor(int selectionColor) {
        this.selectionColor = selectionColor;
        this.invalidateIndicators();
    }

    public int getSelectionColor() {
        return selectionColor;
    }

    public void setIndicatorSize(int indicatorSize) {
        this.indicatorSize = indicatorSize;
        this.invalidateIndicators();
    }

    public int getIndicatorSize() {
        return indicatorSize;
    }

    public void setIndicatorAnimation(int indicatorAnimation) {
        this.indicatorAnimation = indicatorAnimation;
    }

    public int getIndicatorAnimation() {
        return indicatorAnimation;
    }

    public void setIndicatorShape(int indicatorShape) {
        this.indicatorShape = indicatorShape;
        this.invalidateIndicators();
    }

    public int getIndicatorShape() {
        return indicatorShape;
    }

    public void setIndicatorColor(int indicatorColor) {
        this.indicatorColor = indicatorColor;
        this.invalidateIndicators();
    }

    public int getIndicatorColor() {
        return indicatorColor;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        this.unSelectIndicators();
        this.setActiveColorTo(this.indicators.get(this.selectedPosition));
        this.scrollToPosition(selectedPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public int getNumberOfIndicators() {
        return numberOfIndicators;
    }

    public void setNumberOfIndicators(int numberOfIndicators) {
        this.numberOfIndicators = numberOfIndicators;
        if (!this.indicators.isEmpty()) {
            this.removeIndicators();
        }
        for (int i = 0; i < this.numberOfIndicators; i++) {
            drawCircle(i);
        }
    }

    public void removeIndicators() {
        for (ImageView imageView : indicators) {
            removeView(imageView);
        }
        indicators.clear();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            return this.isScrubbingEnabled;
        }
        this.selectIndicatorWhenScrubbing(ev);
        return this.isScrubbingEnabled;
    }

    private void selectIndicatorWhenScrubbing(MotionEvent ev) {
        int x = Math.round(ev.getX());
        int y = Math.round(ev.getY());

        for (int i = 0; i < getChildCount(); i++) {
            ImageView child = (ImageView) getChildAt(i);
            if (x > child.getLeft() && x < child.getRight() && y > child.getTop() && y < child.getBottom()) {
                this.isScrubbing = true;
                this.selectIndicatorAt(i);
                this.scrollToPosition(i);
            }
        }
    }

    private void scrollToPosition(int position) {
        if (this.recyclerView != null) {
            this.selectedPosition = position;
            this.recyclerView.smoothScrollToPosition(position);
        } else if (this.viewPager2 != null) {
            this.selectedPosition = position;
            this.viewPager2.setCurrentItem(position, true);
        }
    }

    private void animateIndicator(int position) {
        if (this.isScrubbingEnabled && this.isScrubbing) {
            if (this.shouldAnimateOnScrubbing) {
                indicators.get(position).startAnimation(AnimationUtils.loadAnimation(getContext(), indicatorAnimation));
            }
        } else {
            indicators.get(position).startAnimation(AnimationUtils.loadAnimation(getContext(), indicatorAnimation));
        }
    }
}