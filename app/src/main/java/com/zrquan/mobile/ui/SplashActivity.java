package com.zrquan.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.google.gson.GsonBuilder;

import com.zrquan.mobile.R;
import com.zrquan.mobile.ZrquanApplication;
import com.zrquan.mobile.event.AccountEvent;
import com.zrquan.mobile.event.StartUpEvent;
import com.zrquan.mobile.model.Account;
import com.zrquan.mobile.support.enums.EventCode;
import com.zrquan.mobile.support.enums.EventType;
import com.zrquan.mobile.support.util.GsonUtils;
import com.zrquan.mobile.support.util.LogUtils;
import com.zrquan.mobile.task.StartUpTask;
import com.zrquan.mobile.ui.common.CommonActivity;

import de.greenrobot.event.EventBus;

public class SplashActivity extends CommonActivity {

    /** Minimal Duration of wait **/
    private final int SPLASH_DISPLAY_LENGTH = 2000;
    //标记是否等待　SPLASH_DISPLAY_LENGTH　完成
    private boolean bReadyWaiting = false;
    //标记是否StartUpTask已经完成
    private boolean bReadyBgTask = false;
    //标记是否完成用户校验
    private boolean bReadyVerifyAccount = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                bReadyWaiting = true;
                checkAndstartMainActivity();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        StartUpTask startUpTask = new StartUpTask();
        startUpTask.executeOnMultiThreads(new Object[]{ZrquanApplication.getInstance()});
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void onEvent(StartUpEvent startUpEvent) {
        bReadyBgTask = true;
        checkAndstartMainActivity();
    }

    //从服务器校验用户完成后的处理
    public void onEvent(AccountEvent accountEvent) {
        if(accountEvent.getEventType() == EventType.AE_NET_VERIFY_JWT) {
            Account currentAccount = ZrquanApplication.getInstance().getAccount();
            if(accountEvent.getEventCode() == EventCode.S_OK) {
                GsonUtils.populate(new GsonBuilder(), accountEvent.getUserInfo(), Account.class, currentAccount);
                currentAccount.setVerified(true);
            } else if(accountEvent.getEventCode() == EventCode.FA_SERVER_TIMEOUT) {
                LogUtils.d(LOG_TAG, accountEvent.toString());
                currentAccount.setVerified(true);
            } else {
                LogUtils.d(LOG_TAG, accountEvent.toString());
                currentAccount.setVerified(false);
            }
            bReadyVerifyAccount = true;
            checkAndstartMainActivity();
        }
    }

    private void checkAndstartMainActivity() {
        synchronized (this) {
            //只有几个初始化任务均完成才跳转到主页面
            if(bReadyBgTask && bReadyWaiting && bReadyVerifyAccount) {
                Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                SplashActivity.this.startActivity(mainIntent);
                SplashActivity.this.finish();
                overridePendingTransition(R.anim.main_enter, R.anim.main_exit );
            }
        }
    }
}
