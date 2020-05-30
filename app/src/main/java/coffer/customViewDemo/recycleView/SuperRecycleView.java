package coffer.customViewDemo.recycleView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

import coffer.Const;
import coffer.customViewDemo.CofferLayoutManager;
import coffer.util.Util;

/**
 * @author：张宝全
 * @date：2020/5/26
 * @Description：
 * @Reviser：
 * @RevisionTime：
 * @RevisionDescription：
 */
public class SuperRecycleView extends RecyclerView {

    /**
     * 加载更多监听
     */
    public interface LoadMoreListener {
        void onLoadMore();
    }

    /**
     * 滚动监听
     */
    public interface CustomScrollListener {

        void onScrolled(RecyclerView recyclerView, int dx, int dy);

        void onScrollStateChanged(RecyclerView recyclerView, int newState);
    }

    /**
     * item点击监听
     */
    public interface OnItemTouchListener {
        void onClickListener(int position);

        void onLongClickListener(int position);
    }


    /**
     * 布局类型
     */
    enum ELayoutManagerType {
        //list
        LayoutManager_List,
        //grid
        LayoutManager_Grid,
        //瀑布流
        LayoutManager_Staggered
    }

    /**
     * 默认
     */
    private ELayoutManagerType mLayoutManagerType = ELayoutManagerType.LayoutManager_List;

    /**
     * 是否显示头部
     */
    private boolean mIsHeaderEnable = true;
    /**
     * 是否允许加载更多
     */
    private boolean mIsFooterEnable = true;

    /**
     * 头部view数量
     */
    private int mHeaderCount = 0;
    /**
     * 尾部view数量
     */
    private int mFooterCount = 0;

    /**
     * 头部views
     */
    private List<View> mHeaderList = new ArrayList<>();
    /**
     * 尾部views
     */
    private List<View> mFooterList = new ArrayList<>();
    /**
     * 特殊view的positions
     */
    private List<Integer> mSpecialPositions;

    /**
     * 尾部view
     */
    private LinearLayout headerLayout;

    /**
     * 头部view
     */
    private LinearLayout footerLayout;

    /**
     * 自定义实现了头部和底部的adapter
     */
    private AutoLoadAdapter mAutoLoadAdapter;
    private Adapter mItemAdapter;

    /**
     * 标记是否正在加载更多，防止再次调用加载更多接口
     */
    private boolean mIsLoadingMore;
    /**
     * 数据已经全部加载完
     */
    private boolean mIsNoMoreData;
    private int[] mLastPositionArray;
    /**
     * 标记加载更多的position
     */
    private int mLoadMorePosition;
    /**
     * 记录当前滑动方向
     */
    private int mLocationY;

    /**
     * 加载更多的监听-业务需要实现加载数据
     */
    private LoadMoreListener mListener;

    private CustomScrollListener mCustomScrollListener;

    /**
     * 默认预加载提前量
     */
    public static final int DEFAULT_PRELOAD_COUNT = 5;

    private Paint mPaint;

    public SuperRecycleView(Context context) {
        super(context);
        init();
    }

    public SuperRecycleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SuperRecycleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * 初始化-添加滚动监听
     * <p/>
     * 回调加载更多方法，前提是
     * <pre>
     *    1、有监听并且支持加载更多：null != mAnimatorListener && mIsFooterEnable
     *    2、目前没有在加载，正在上拉（dy>0），当前最后一条可见的view是否是当前数据列表的最好一条--及加载更多
     * </pre>
     */
    private void init() {
        mPaint = new Paint();
        //硬编码color，为了隐藏一个问题
        setBgPaintColor(Color.parseColor("#fcfcfc"));
        //性能优化：禁止系统动画
        setItemAnimator(null);
        Util.setPauseOnScrollListener(this, new OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                //修复瀑布流滑动后 往回滑动顶部留白 mLocationY<0 表示往回走
                if (mLayoutManagerType == ELayoutManagerType.LayoutManager_Staggered && mLocationY < 0 && getFirstVisiblePosition() < 15) {
                    ((StaggeredGridLayoutManager) getLayoutManager()).invalidateSpanAssignments();
                }
                if (mCustomScrollListener != null) {
                    mCustomScrollListener.onScrollStateChanged(recyclerView, newState);
                }

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                mLocationY = dy;
                if (null != mListener && mIsFooterEnable && !mIsLoadingMore && !mIsNoMoreData && dy > 0) {
                    int lastVisiblePosition = getLastVisiblePosition();
                    if (lastVisiblePosition >= mAutoLoadAdapter.getItemCount() - 1 - DEFAULT_PRELOAD_COUNT) {
                        setLoadingMore(true);
                        mLoadMorePosition = lastVisiblePosition;
                        mListener.onLoadMore();
                    }
                }

                if (mCustomScrollListener != null) {
                    mCustomScrollListener.onScrolled(recyclerView, dx, dy);
                }
            }
        });
    }

    /**
     * 解决缺少数据透过去的情况
     *
     * @param canvas
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (getChildCount() > 1) {
            View lastChildView = getChildAt(getChildCount() - 1);
            if (lastChildView != null && lastChildView.getBottom() < getBottom()) {
                canvas.drawRect(0, lastChildView.getBottom(), getWidth(), getHeight(), mPaint);
            }
        }
        super.dispatchDraw(canvas);
    }

    public void setBgPaintColor(int color) {
        mPaint.setColor(color);
    }

    public void resetBgColor() {
        setBgPaintColor(Color.parseColor("#fcfcfc"));
    }

    /**
     * 设置布局类型
     */
    private void setItemType(ELayoutManagerType type) {
        this.mLayoutManagerType = type;
    }

    /**
     * 设置特殊item positions
     */
    public void setSpecialItem(List<Integer> specialItems) {
        this.mSpecialPositions = specialItems;
    }

    /**
     * 添加头部view
     */
    public void addHeaderView(View view) {
        mHeaderCount++;
        mHeaderList.add(view);
    }

    public int getHeaderCount() {
        return mHeaderCount;
    }

    /**
     * 添加尾部view
     */
    public void addFooterView(View view) {
        mFooterCount++;
        mFooterList.add(view);
    }

    public void removeFooterView(View view) {
        if (mFooterList.remove(view)) {
            mFooterCount--;
            getAdapter().notifyDataSetChanged();
        }
    }

    public int getFooterAndHeadCount() {
        return mFooterCount + mHeaderCount;
    }

    /**
     * 判断是否头部view
     */
    public boolean isHeaderPostion(int position) {
        return position < mHeaderCount;
    }

    /**
     * 判断是否尾部view
     */
    public boolean isFooterPosition(int position) {
        return position >= mHeaderCount + mItemAdapter.getItemCount();
    }

    /**
     * 判断是否特殊item
     */
    public boolean isSpecialItem(int position) {
        if (mSpecialPositions != null) {
            for (int p : mSpecialPositions) {
                if (p == position) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取header容器
     */
    private LinearLayout getHeaderLayout(ELayoutManagerType type) {
        if (headerLayout == null) {
            headerLayout = new LinearLayout(getContext());
            headerLayout.setLayoutParams(new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            headerLayout.setOrientation(LinearLayout.VERTICAL);
        } else {
            //避免设置了网格布局后layoutparams变化
            if (type == ELayoutManagerType.LayoutManager_Staggered && !(headerLayout.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams)) {
                headerLayout.setLayoutParams(new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
        return headerLayout;
    }

    /**
     * 获取footer容器
     */
    private LinearLayout getFooterLayout(ELayoutManagerType type) {
        if (footerLayout == null) {
            footerLayout = new LinearLayout(getContext());
            footerLayout.setLayoutParams(new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            footerLayout.setOrientation(LinearLayout.VERTICAL);
        } else {
            if (type == ELayoutManagerType.LayoutManager_Staggered && !(footerLayout.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams)) {
                footerLayout.setLayoutParams(new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
        return footerLayout;
    }

    /**
     * 设置加载更多的监听
     */
    public void setLoadMoreListener(LoadMoreListener listener) {
        mListener = listener;
    }

    public void setCustomScrollListener(CustomScrollListener listener) {
        mCustomScrollListener = listener;
    }

    @Override
    public void setLayoutManager(LayoutManager manager) {
        if (manager instanceof GridLayoutManager) {
            //头部，尾部，特殊item设置全宽度
            ((GridLayoutManager) manager).setSpanSizeLookup(new BaseSpanSizeLookup(((GridLayoutManager) manager).getSpanCount()));
            setItemType(ELayoutManagerType.LayoutManager_Grid);
        } else if (manager instanceof LinearLayoutManager) {
            setItemType(ELayoutManagerType.LayoutManager_List);
        } else if (manager instanceof StaggeredGridLayoutManager) {
            setItemType(ELayoutManagerType.LayoutManager_Staggered);
        }
        super.setLayoutManager(manager);
    }

    /**
     * 设置数据是否已经全部加载完
     */
    public void setIsNoMoreData(boolean isNoMoreData) {
        this.mIsNoMoreData = isNoMoreData;
    }

    /**
     * 设置正在加载更多
     */
    public void setLoadingMore(boolean loadingMore) {
        this.mIsLoadingMore = loadingMore;
    }


    @Override
    public void setAdapter(Adapter adapter) {
        if (adapter != null) {
            mAutoLoadAdapter = new AutoLoadAdapter(adapter);
        }
        mItemAdapter = adapter;
        super.swapAdapter(mAutoLoadAdapter, true);
    }

    /**
     * 切换layoutManager
     */
    public void switchLayoutManager(LayoutManager layoutManager) {
        int firstVisiblePosition = getFirstVisiblePosition();
        setLayoutManager(layoutManager);
        //瀑布流不滚动到上一可见位置，避免滑动到头部后item重新排列
        if (!(layoutManager instanceof StaggeredGridLayoutManager)) {
            getLayoutManager().scrollToPosition(firstVisiblePosition);
        }
    }

    /**
     * 获取第一条展示的位置
     */
    public int getFirstVisiblePosition() {
        int position = 0;
        if (getLayoutManager() != null) {
            if (mLayoutManagerType == ELayoutManagerType.LayoutManager_List) {
                position = ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
            } else if (mLayoutManagerType == ELayoutManagerType.LayoutManager_Grid) {
                position = ((GridLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
            } else if (mLayoutManagerType == ELayoutManagerType.LayoutManager_Staggered) {
                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) getLayoutManager();
                if (mLastPositionArray == null) {
                    mLastPositionArray = new int[layoutManager.getSpanCount()];
                }
                position = getMinPositions(layoutManager.findFirstVisibleItemPositions(mLastPositionArray));
            } else {
                position = 0;
            }
        }
        return position;
    }

    /**
     * 获得当前展示最小的position
     */
    private int getMinPositions(int[] positions) {
        int size = positions == null ? 0 : positions.length;
        int minPosition = Integer.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            minPosition = Math.min(minPosition, positions[i]);
        }
        return minPosition;
    }

    /**
     * 获取最后一条展示的位置
     */
    public int getLastVisiblePosition() {
        int position;
        if (mLayoutManagerType == ELayoutManagerType.LayoutManager_List) {
            position = ((LinearLayoutManager) getLayoutManager()).findLastVisibleItemPosition();
        } else if (mLayoutManagerType == ELayoutManagerType.LayoutManager_Grid) {
            position = ((GridLayoutManager) getLayoutManager()).findLastVisibleItemPosition();
        } else if (mLayoutManagerType == ELayoutManagerType.LayoutManager_Staggered) {
            StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) getLayoutManager();
            if (mLastPositionArray == null) {
                mLastPositionArray = new int[layoutManager.getSpanCount()];
            }
            position = getMaxPosition(layoutManager.findLastVisibleItemPositions(mLastPositionArray));
        } else {
            position = getLayoutManager().getItemCount() - 1;
        }
        return position;
    }

    /**
     * 获得最大的位置
     */
    private int getMaxPosition(int[] positions) {
        int size = positions == null ? 0 : positions.length;
        int maxPosition = Integer.MIN_VALUE;
        for (int i = 0; i < size; i++) {
            maxPosition = Math.max(maxPosition, positions[i]);
        }
        return maxPosition;
    }

    /**
     * 设置头部view是否展示
     */
    public void setHeaderEnable(boolean enable) {
        mAutoLoadAdapter.setHeaderEnable(enable);
    }

    /**
     * 设置是否支持自动加载更多
     */
    public void setAutoLoadMoreEnable(boolean autoLoadMore) {
        mIsFooterEnable = autoLoadMore;
    }

    /**
     * 通知更多的数据已经加载
     */
    public void notifyMoreFinish(boolean hasMore) {
        notifyMoreFinish(hasMore, 0);
    }

    /**
     * 通知更多的数据已经加载
     */
    public void notifyMoreFinish(boolean hasMore, int loadPosition) {
        setAutoLoadMoreEnable(hasMore);
        setLoadingMore(false);
        if (mLoadMorePosition == 0) {
            getAdapter().notifyDataSetChanged();
        } else {
            getAdapter().notifyItemInserted(mLoadMorePosition + DEFAULT_PRELOAD_COUNT + 1);
            //getAdapter().getItemCount());
        }
    }

    /**
     * 网格布局头部，尾部，特殊item设置全宽度
     */
    private class BaseSpanSizeLookup extends GridLayoutManager.SpanSizeLookup {

        private int mSpanSize;

        BaseSpanSizeLookup(int size) {
            mSpanSize = size;
        }

        @Override
        public int getSpanSize(int position) {
            return (isHeaderPostion(position) || isFooterPosition(position) || isSpecialItem(position - mHeaderCount)) ? mSpanSize : 1;
        }
    }

    /**
     * 基础adapter
     */
    public class AutoLoadAdapter extends Adapter<ViewHolder> {

        /**
         * 数据adapter
         */
        private Adapter mInternalAdapter;

        /**
         * 自定义头部，尾部holder
         */
        private HeaderViewHolder headerHolder;
        private FooterViewHolder footerHolder;

        AutoLoadAdapter(Adapter adapter) {
            mInternalAdapter = adapter;
        }

        void setHeaderEnable(boolean enable) {
            mIsHeaderEnable = enable;
        }

        /**
         * 需要计算上加载更多和添加的头部俩个
         */
        @Override
        public int getItemCount() {
            int count = mInternalAdapter.getItemCount();
            if (mIsHeaderEnable) {
                count += mHeaderCount;
            }
            if (mIsFooterEnable) {
                count += mFooterCount;
            }
            return count;
        }

        @Override
        public int getItemViewType(int position) {
            if (mHeaderCount > 0 && position < mHeaderCount && mIsHeaderEnable) {
                return Const.ITEM_TYPE_HEADER;
            }
            if (position >= mHeaderCount && position < mHeaderCount + mInternalAdapter.getItemCount()) {
                if (isSpecialItem(position - mHeaderCount)) {
                    return Const.ITEM_TYPE_SPECIAL;
                } else {
                    return mInternalAdapter.getItemViewType(position - mHeaderCount);
                }
            } else {
                return Const.ITEM_TYPE_FOOTER;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == Const.ITEM_TYPE_HEADER) {
                return new HeaderViewHolder(getHeaderLayout(mLayoutManagerType));
            } else if (viewType == Const.ITEM_TYPE_FOOTER) {
                return new FooterViewHolder(getFooterLayout(mLayoutManagerType));
            } else {
                return mInternalAdapter.onCreateViewHolder(parent, viewType);
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                headerHolder = (HeaderViewHolder) holder;
                if (mLayoutManagerType == ELayoutManagerType.LayoutManager_Staggered) {
                    if (!(headerHolder.contentLayout.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams)) {
                        headerHolder.contentLayout.setLayoutParams(new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    }
                    ((StaggeredGridLayoutManager.LayoutParams) headerHolder.itemView.getLayoutParams()).setFullSpan(true);
                }
                if (headerHolder.contentLayout.findViewWithTag(position) == null) {
                    if (mHeaderList.size() > 0 && position >= 0 && position < mHeaderList.size()) {
                        mHeaderList.get(position).setTag(position);
                        headerHolder.contentLayout.addView(mHeaderList.get(position));
                    }
                }
            } else if (holder instanceof FooterViewHolder) {
                footerHolder = (FooterViewHolder) holder;
                if (mLayoutManagerType == ELayoutManagerType.LayoutManager_Staggered) {
                    if (!(footerHolder.contentLayout.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams)) {
                        footerHolder.contentLayout.setLayoutParams(new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    }
                    ((StaggeredGridLayoutManager.LayoutParams) footerHolder.itemView.getLayoutParams()).setFullSpan(true);
                }
                int footPosition = position - mInternalAdapter.getItemCount() - mHeaderCount;
                if (footerHolder.contentLayout.findViewWithTag(footPosition) == null) {
                    if (mFooterList.size() > 0 && footPosition >= 0 && footPosition < mFooterList.size()) {
                        mFooterList.get(footPosition).setTag(footPosition);
                        footerHolder.contentLayout.addView(mFooterList.get(footPosition));
                    }
                }
            } else {
                if (mLayoutManagerType == ELayoutManagerType.LayoutManager_Staggered) {
                    if (isSpecialItem(position - mHeaderCount)) {
                        if (!(holder.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams)) {
                            holder.itemView.setLayoutParams(new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, holder.itemView.getHeight()));
                        }
                        ((StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams()).setFullSpan(true);
                    }
                }
                mInternalAdapter.onBindViewHolder(holder, position - mHeaderCount);
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
            if ((holder instanceof HeaderViewHolder) || (holder instanceof FooterViewHolder)) {
                super.onBindViewHolder(holder, position, payloads);
            } else {
                if (payloads != null && !payloads.isEmpty()) {
                    mInternalAdapter.onBindViewHolder(holder, position - mHeaderCount, payloads);
                } else {
                    super.onBindViewHolder(holder, position, payloads);
                }
            }
        }

        class FooterViewHolder extends ViewHolder {

            private LinearLayout contentLayout;

            FooterViewHolder(View itemView) {
                super(itemView);
                contentLayout = (LinearLayout) itemView;
            }
        }

        @Override
        public void onViewRecycled(ViewHolder holder) {
            super.onViewRecycled(holder);
            if (!(holder instanceof HeaderViewHolder) && !(holder instanceof FooterViewHolder) && null != mInternalAdapter) {
                mInternalAdapter.onViewRecycled(holder);
            }
        }

        class HeaderViewHolder extends ViewHolder {

            private LinearLayout contentLayout;

            public HeaderViewHolder(View itemView) {
                super(itemView);
                contentLayout = (LinearLayout) itemView;
            }
        }
    }

    /**
     * 基础viewholder，监听点击事件
     */
    public static class ClickViewHolder extends ViewHolder implements OnClickListener, OnLongClickListener {

        public int position;
        private OnItemTouchListener listener;

        public ClickViewHolder(View itemView) {
            this(itemView, null);
        }

        public ClickViewHolder(View itemView, OnItemTouchListener listener) {
            super(itemView);
            this.listener = listener;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (listener != null) {
                listener.onClickListener(position);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (listener != null) {
                listener.onLongClickListener(position);
            }
            return true;
        }
    }

    public View getHeadView() {
        return mHeaderList != null && mHeaderList.size() > 0 ? mHeaderList.get(0) : null;
    }


    /**
     * 获取ScroolY: 不够准确，针对带或者不带headView 并且item高度一致
     *
     * @return
     */
    public int getRealScrollY() {
        if (getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) this.getLayoutManager();
            int position = layoutManager.findFirstVisibleItemPosition();
            View firstVisiableChildView = layoutManager.findViewByPosition(position);
            if (firstVisiableChildView == null) {
                return 0;
            }
            int itemHeight = (position == 1 && getHeadView() != null) ? getHeadView().getMeasuredHeight() : firstVisiableChildView.getHeight();
            if (position > 1) {
                return (getHeadView() == null ? 0 : getHeadView().getMeasuredHeight()) + position * itemHeight - firstVisiableChildView.getTop();
            }
            return (position) * itemHeight - firstVisiableChildView.getTop();
        }
        return getScrollY();
    }

    private float mScrollPercent;
    public void startScroll(final int top, int duration){
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(duration);


        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //滑动recycler动画
                float v = (animation.getAnimatedFraction() - mScrollPercent) * top;
                mScrollPercent = animation.getAnimatedFraction();
                scrollBy(0, (int) v);

            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                mScrollPercent = 0;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mScrollPercent = 0;
            }
        });
        //fixbug 当此动画和notifychanged动画同时产生的时候，tag错乱以及崩溃的bug，冲突位置 BookStorePresenter.getSimilarBook();
        if (getItemAnimator() != null){
            getItemAnimator().endAnimations();
        }
        valueAnimator.start();
    }

    public void endTranslateY(final float locationY, int duration){
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(duration);


        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //滑动recycler动画
                setTranslationY(locationY * (1-animation.getAnimatedFraction()));

            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });
        //fixbug 当此动画和notifychanged动画同时产生的时候，tag错乱以及崩溃的bug，冲突位置 BookStorePresenter.getSimilarBook();
        if (getItemAnimator() != null){
            getItemAnimator().endAnimations();
        }
        valueAnimator.start();
    }

    public void setCanScrollVertically(boolean canScrollVertically){
        if (getLayoutManager() != null && getLayoutManager() instanceof CofferLayoutManager){
            ((CofferLayoutManager)getLayoutManager()).setCanScroll(canScrollVertically);
        }
    }

}