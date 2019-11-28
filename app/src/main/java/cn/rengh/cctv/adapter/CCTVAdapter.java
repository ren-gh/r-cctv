
package cn.rengh.cctv.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.r.library.common.player.PlayerItem;
import com.r.library.common.util.UIUtils;

import java.util.List;

import cn.rengh.cctv.R;
import cn.rengh.cctv.utils.ColorsUtils;

public class CCTVAdapter extends RecyclerView.Adapter implements RecyclerView.ChildDrawingOrderCallback {
    private Context context;
    private OnClickListener clickListener;
    private List<PlayerItem> list;
    private ViewGroup viewGroup;

    public CCTVAdapter(Context context) {
        super();
        this.context = context;
    }

    public void setList(List<PlayerItem> list) {
        this.list = list;
    }

    public void clear() {
        this.list.clear();
        this.list = null;
        this.context = null;
        this.clickListener = null;
    }

    public void setOnClickListener(OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (null == viewGroup) {
            viewGroup = parent;
        }
        View view = LayoutInflater.from(context).inflate(R.layout.layout_item_of_cctv, parent, false);
        CCTVViewHolder holder = new CCTVViewHolder(view);
        return holder;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PlayerItem item = this.list.get(position);

        ((CCTVViewHolder) holder).textView.setText(item.getNumber() + ". " + item.getName());

        holder.itemView.setFocusable(true);
        holder.itemView.setBackgroundResource(R.drawable.item_bg);
        holder.itemView.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                /**
                 * 不管是自定义 RecyclerView 还是实现 ChildDrawingOrderCallback 接口， <br>
                 * 放大时必须重绘，否则会被其他 Item 遮盖，需配合自定义的 FocusKeepRecyclerView 使用
                 */
                viewGroup.invalidate();
                UIUtils.scaleView(view, 1.2f);
            } else {
                UIUtils.scaleView(view, 1.0f);
            }
        });
        holder.itemView.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.setBackgroundResource(R.drawable.item_bg_focus);
                    break;
                case MotionEvent.ACTION_UP:
                    view.setBackgroundResource(R.drawable.item_bg_normal);
                case MotionEvent.ACTION_CANCEL:
                    view.setBackgroundColor(0);
                    break;
            }
            return false;
        });
        holder.itemView.setOnClickListener(v -> {
            if (null != clickListener) {
                this.clickListener.onClick(position);
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public int getItemCount() {
        return this.list.size();
    }

    @Override
    public int onGetChildDrawingOrder(int childCount, int i) {
        View focusedChild = viewGroup.getFocusedChild();
        int focusViewIndex = viewGroup.indexOfChild(focusedChild);
        if (focusViewIndex == -1) {
            return i;
        }

        if (focusViewIndex == i) {
            return childCount - 1;
        } else if (i == childCount - 1) {
            return focusViewIndex;
        } else {
            return i;
        }
    }

    private class CCTVViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;

        public CCTVViewHolder(View itemView) {
            super(itemView);
            this.textView = itemView.findViewById(R.id.tv_cctv_item);
            this.textView.setBackgroundColor(ContextCompat.getColor(textView.getContext(), ColorsUtils.getRandColor()));
        }
    }

    public interface OnClickListener {
        void onClick(int pos);
    }
}
