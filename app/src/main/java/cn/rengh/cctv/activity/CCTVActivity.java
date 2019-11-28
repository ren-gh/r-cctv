
package cn.rengh.cctv.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.r.library.common.player.PlayerItem;
import com.r.library.common.player2.VideoView;
import com.r.library.common.util.FileUtils;
import com.r.library.common.util.LogUtils;
import com.r.library.common.util.PreferenceUtils;
import com.r.library.common.util.UIUtils;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import cn.rengh.cctv.adapter.CCTVAdapter;
import cn.rengh.cctv.R;
import cn.rengh.cctv.view.FocusKeepRecyclerView;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CCTVActivity extends AppCompatActivity {
    private final String TAG = CCTVActivity.class.getSimpleName();
    private Context context;
    private MyHandler weakHandler;

    /*
     * Views
     */
    @BindView(R.id.cctv_recyclerview)
    FocusKeepRecyclerView recyclerView;
    @BindView(R.id.cctv_videoview_parent)
    RelativeLayout videoParent;
    @BindView(R.id.cctv_videoview)
    VideoView videoView;
    @BindView(R.id.cctv_title)
    TextView videoTile;
    @BindView(R.id.cctv_clock)
    TextView tvClock;

    /*
     * RecyclerView 相关
     */
    private LinearLayoutManager layoutManager;
    private Disposable loadDisposable;
    private CopyOnWriteArrayList<PlayerItem> list;
    private CCTVAdapter cctvAdapter;
    private PlayerItem playerItem;

    /*
     * VideoView 全屏处理相关参数
     */
    private Point point;
    private RelativeLayout.LayoutParams videoParantParam;
    private int videoParantPadding = -1;

    /*
     * 时钟相关
     */
    private Disposable clockDisposable;
    private SimpleDateFormat clockSimpleDateFormat;

    /*
     * 频道按键事件处理
     */
    private int channelId = -1;
    private Runnable onChannelIdChanged = () -> play();

    /*
     * 返回按键事件处理
     */
    private boolean onBackKeyPressed = false; // 记录是否有首次按键
    private Runnable onBackKeyClicked = () -> onBackKeyPressed = false;

    private PreferenceUtils preferenceUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        UIUtils.setFullStateBar(this, true);
        setContentView(R.layout.activity_cctv);
        LogUtils.i(TAG, "onCreate()");
        ButterKnife.bind(this);

        context = this;
        weakHandler = new MyHandler(this);
        point = new Point();
        preferenceUtils = new PreferenceUtils(context, "history");
        channelId = preferenceUtils.getInt("channelId", 0);

        initViews();
    }

    @Override
    protected void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        LogUtils.i(TAG, "onPostCreate()");
        getWindowManager().getDefaultDisplay().getSize(point);
        loadData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtils.i(TAG, "onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtils.i(TAG, "onResume()");
        startClock();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtils.i(TAG, "onPause()");
        stopClock();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtils.i(TAG, "onStop()");
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtils.i(TAG, "onDestroy()");

        if (null != loadDisposable && !loadDisposable.isDisposed()) {
            loadDisposable.dispose();
            loadDisposable = null;
        }

        playerItem = null;

        videoView.release();

        videoParantPadding = -1;
        videoParantParam = null;
        point = null;

        cctvAdapter.clear();
        cctvAdapter = null;

        weakHandler.removeCallbacksAndMessages(null);
        weakHandler = null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            if (videoParantPadding != -1 && videoParantParam != null) {
                videoParent.performClick();
                return true;
            }
            if (doubleClickBackToFinish()) {
                Toast.makeText(context, "连按两次【返回】键退出", Toast.LENGTH_LONG).show();
                return true;
            }
        } else if (KeyEvent.KEYCODE_CHANNEL_UP == keyCode || KeyEvent.KEYCODE_CHANNEL_DOWN == keyCode) {
            if (KeyEvent.KEYCODE_CHANNEL_UP == keyCode) {
                if (channelId == list.size() - 1) {
                    return true;
                }
                channelId++;
            } else {
                if (channelId == 0) {
                    return true;
                }
                channelId--;
            }
            if (channelId < 0) {
                channelId = 0;
            }
            if (channelId >= list.size()) {
                channelId = list.size() - 1;
            }
            LogUtils.i(TAG, "============ channel: " + channelId);
            if (weakHandler.hasCallbacks(onChannelIdChanged)) {
                weakHandler.removeCallbacks(onChannelIdChanged);
            }
            smoothScrollToPosition(true);
            weakHandler.postDelayed(onChannelIdChanged, 1000);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initViews() {
        videoParent.setVisibility(View.GONE);

        videoView.setOnPreparedListener(mediaPlayer -> {
            LogUtils.i(TAG, "setOnPreparedListener()");
        });
        videoView.setOnCompletionListener(mediaPlayer -> {
            LogUtils.i(TAG, "setOnCompletionListener()");
            videoTile.setText("播放结束：" + playerItem.getName());
        });
        videoView.setOnErrorListener((mediaPlayer, i, i1) -> {
            LogUtils.i(TAG, "setOnErrorListener()");
            videoTile.setText("播放出错：" + playerItem.getName());
            return false;
        });

        cctvAdapter = new CCTVAdapter(this);
        cctvAdapter.setOnClickListener(pos -> {
            recyclerView.setCurrentFocusPosition(pos);
            if (channelId == pos && !isVideoFullScreen()) {
                videoParent.performClick();
                return;
            }
            channelId = pos;
            play();
        });

        layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        // recyclerView.addItemDecoration(new LinearItemDecoration(context, LinearLayoutManager.VERTICAL));
        recyclerView.setChildDrawingOrderCallback(cctvAdapter);
        recyclerView.setLayoutManager(layoutManager);
    }

    @OnFocusChange(R.id.cctv_videoview_parent)
    void onVideoViewFocusChanged(boolean b) {
        if (!b) {
            smoothScrollToPosition(false);
        }
    }

    @OnClick(R.id.cctv_videoview_parent)
    void onVideoViewClicked() {
        if (isVideoFullScreen()) {
            videoParent.setPadding(videoParantPadding, videoParantPadding, videoParantPadding, videoParantPadding);
            videoParent.setLayoutParams(videoParantParam);
            videoParantParam = null;
            videoParantPadding = -1;
            recyclerView.setVisibility(View.VISIBLE);
            smoothScrollToPosition(false);
        } else {
            videoParantParam = (RelativeLayout.LayoutParams) videoParent.getLayoutParams();
            videoParantPadding = videoParent.getPaddingTop();
            videoParent.setPadding(0, 0, 0, 0);
            videoParent.setLayoutParams(new RelativeLayout.LayoutParams(point.x, point.y));
            recyclerView.clearFocus();
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void smoothScrollToPosition(boolean smooth) {
        if (channelId < 0 || channelId >= list.size()) {
            return;
        }
        if (!isVideoFullScreen()) {
            recyclerView.setCurrentFocusPosition(channelId);
            if (smooth) {
                recyclerView.smoothScrollToPosition(channelId);
                onScrollToPosition.run();
            } else {
                recyclerView.scrollToPosition(channelId);
                weakHandler.postDelayed(onScrollToPosition, 100);
            }
        }
    }

    private Runnable onScrollToPosition = new Runnable() {
        @Override
        public void run() {
            View itemView = recyclerView.getLayoutManager().findViewByPosition(channelId);
            if (itemView != null) {
                itemView.requestFocus();
            }
        }
    };

    private boolean isVideoFullScreen() {
        if (videoParantPadding != -1 || videoParantParam != null) {
            return true;
        } else {
            return false;
        }
    }

    private void startClock() {
        clockDisposable = Observable
                .interval(0, 200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> {
                    try {
                        if (null == clockSimpleDateFormat) {
                            clockSimpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                        }
                        String time = clockSimpleDateFormat.format(System.currentTimeMillis());
                        tvClock.setText(time);
                    } catch (Exception e) {
                    }
                });
    }

    private void stopClock() {
        if (clockDisposable != null && !clockDisposable.isDisposed()) {
            clockDisposable.dispose();
            clockDisposable = null;
        }
    }

    private void loadData() {
        findViewById(R.id.pb_load_data).setVisibility(View.VISIBLE);
        loadDisposable = Observable
                .create((ObservableOnSubscribe<Integer>) emitter -> {
                    if (null != list) {
                        list.clear();
                    } else {
                        list = new CopyOnWriteArrayList<>();
                    }
                    String text;
                    // targetVersion 为 28 及以上时，禁止直接访问 http 服务，需要使用 https。
                    // try {
                    // OKHTTPHelper.HttpResponse httpResponse = OKHTTPHelper.getDefault().request("http://ivi.bupt.edu.cn/");
                    // if (null != httpResponse && httpResponse.isSuccess()) {
                    // text = httpResponse.getResponse();
                    // getItems(text);
                    // }
                    // } catch (Exception e) {
                    // e.printStackTrace();
                    // }
                    if (list.size() == 0) {
                        InputStream in = FileUtils.getAssetsFileInputStream(context, "live_list.html");
                        text = FileUtils.getContent(in);
                        getItems(text);
                        LogUtils.i(TAG, "Get data from local.");
                    } else {
                        LogUtils.i(TAG, "Get data from network.");
                    }
                    emitter.onNext(0);
                    emitter.onComplete();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(integer -> {
                    if (list.size() == 0) {
                        List<PlayerItem> errList = new ArrayList<>();
                        PlayerItem item = new PlayerItem();
                        item.setName("加载失败！");
                        errList.add(item);
                        cctvAdapter.setList(errList);
                    } else {
                        cctvAdapter.setList(list);
                    }
                    findViewById(R.id.pb_load_data).setVisibility(View.GONE);
                    recyclerView.setAdapter(cctvAdapter);
                    smoothScrollToPosition(false);
                    play();
                });
    }

    private void getItems(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        String[] lines = text.split("\n");
        int number = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (TextUtils.isEmpty(line) || !line.contains("移动端") || i < 2) {
                continue;
            }
            number++;
            PlayerItem item = new PlayerItem();
            String name = lines[i - 2]
                    .replaceAll("<p>", "")
                    .replaceAll("</p>", "")
                    .trim();
            String url = "http://ivi.bupt.edu.cn/" + lines[i]
                    .substring(lines[i].indexOf("href=\"") + 6,
                            lines[i].indexOf("\" target=\""));
            LogUtils.i(TAG, "number: " + number + " name: " + name + " url: " + url);
            item.setNumber(number);
            item.setName(name);
            item.setPath(url);
            list.add(item);
        }
    }

    private void play() {
        if (channelId >= 0 && channelId < list.size()) {
            playerItem = this.list.get(channelId);
            LogUtils.i(TAG, "Play: " + playerItem);
        }
        if (null == playerItem) {
            return;
        }
        preferenceUtils.putInt("channelId", channelId);
        if (videoParent.getVisibility() != View.VISIBLE) {
            videoParent.setVisibility(View.VISIBLE);
        }
        videoTile.setText("正在播放：" + playerItem.getNumber() + ". " + playerItem.getName());
        videoView.setVideoPath(Uri.parse(playerItem.getPath()));
        videoView.start();
    }

    private boolean doubleClickBackToFinish() {
        if (!onBackKeyPressed) {
            onBackKeyPressed = true;
            weakHandler.postDelayed(onBackKeyClicked, 1000);
            return true;
        } else {
            onBackKeyPressed = false;
            weakHandler.removeCallbacks(onBackKeyClicked);
            return false;
        }
    }

    private static class MyHandler extends Handler {
        private WeakReference<CCTVActivity> weakReference;

        public MyHandler(CCTVActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            CCTVActivity activity = weakReference.get();
            if (null == activity) {
                LogUtils.i("MyHandler", "activity is null.");
            }
        }
    }
}
