package org.godot.ironsourceplugin;

import android.app.Activity;
import androidx.collection.ArraySet;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.integration.IntegrationHelper;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.LevelPlayAdSize;
import com.unity3d.mediation.banner.LevelPlayBannerAdView;
import com.unity3d.mediation.banner.LevelPlayBannerAdViewListener;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener;
import com.unity3d.mediation.rewarded.LevelPlayReward;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener;
//import com.ironsource.mediationsdk.sdk.BannerListener;
//import com.ironsource.mediationsdk.sdk.InterstitialListener;
//import com.ironsource.mediationsdk.sdk.RewardedVideoListener;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import java.util.Objects;
import java.util.Set;

public class GodotIronSource extends GodotPlugin {
    public static final String TAG = "GodotIronSource";

    private LevelPlayBannerAdView levelPlayBanner;
    private LevelPlayInterstitialAd mInterstitialAd;
    private String appKey = null;
    private boolean isInitialized = false;
    private FrameLayout layout = null;
    private Activity activity;

    private int bannerHeight;
    private int bannerWidth;


    @Nullable
    @Override
    public View onMainCreate(Activity activity) {
        this.activity = getActivity();
        layout = new FrameLayout(activity);
        return layout;
    }

    public GodotIronSource(Godot godot) {
        super(godot);
    }



    @NonNull
    @Override
    public String getPluginName() {
        return "GodotIronSource";
    }


    @Override
    public void onMainPause() {
        super.onMainPause();
        IronSource.onPause(getActivity());
        levelPlayBanner.pauseAutoRefresh();
    }

    
    @Override
    public void onMainResume() {
        super.onMainResume();
        IronSource.onResume(getActivity());
        // Resume refresh
        levelPlayBanner.resumeAutoRefresh();
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new ArraySet<>();

        // General
        signals.add(new SignalInfo("on_plugin_error", String.class));

        // Rewarded
        signals.add(new SignalInfo("on_rewarded_availability_changed",Boolean.class));
        signals.add(new SignalInfo("on_rewarded_opened"));
        signals.add(new SignalInfo("on_rewarded_closed"));
        signals.add(new SignalInfo("on_rewarded"));

        // Interstitial
        signals.add(new SignalInfo("on_interstitial_loaded"));
        signals.add(new SignalInfo("on_interstitial_opened"));
        signals.add(new SignalInfo("on_interstitial_closed"));


        // Banner
        signals.add(new SignalInfo("on_banner_loaded"));


        return signals;
    }

    @UsedByGodot
    public void init(String appKey , boolean consent){
        isInitialized = true;
        IronSource.setConsent(consent);
        this.appKey = appKey;
    }


    @UsedByGodot
    public void initRewarded(){
        if (!isInitialized){
            Log.d(TAG, "ERROR : You should call init first");
            emitSignal("on_plugin_error","You should call init first");
            return;
        }
        activity.runOnUiThread(() -> {
            LevelPlayRewardedVideoListener rewardedVideoListener = new LevelPlayRewardedVideoListener() {
                @Override
                public void onAdOpened(AdInfo adInfo) {
                    emitSignal("on_rewarded_opened");
                }

                @Override
                public void onAdShowFailed(IronSourceError ironSourceError, AdInfo adInfo) {
                    emitSignal("on_plugin_error",ironSourceError.getErrorMessage());
                    Log.e(TAG, "onRewardedVideoAdShowFailed: " + ironSourceError.getErrorMessage());
                }

                @Override
                public void onAdClicked(Placement placement, AdInfo adInfo) {

                }

                @Override
                public void onAdRewarded(Placement placement, AdInfo adInfo) {
                    emitSignal("on_rewarded");
                }

                @Override
                public void onAdClosed(AdInfo adInfo) {
                    emitSignal("on_rewarded_closed");
                }

                @Override
                public void onAdAvailable(AdInfo adInfo) {
                    emitSignal("on_rewarded_availability_changed",true);
                }

                @Override
                public void onAdUnavailable() {
                    emitSignal("on_rewarded_availability_changed",false);
                }


            };
            IronSource.setLevelPlayRewardedVideoListener((LevelPlayRewardedVideoListener) rewardedVideoListener);
            IronSource.init(getActivity(),appKey, IronSource.AD_UNIT.REWARDED_VIDEO);
        });
    }


    @UsedByGodot
    public void showRewarded(){
        if (!IronSource.isRewardedVideoAvailable()){
            emitSignal("on_plugin_error","Rewarded is NOT READY");
            return;
        }
        IronSource.showRewardedVideo();
    }

    
    @UsedByGodot
    public void initInterstitial(String interstitialId){
        if (!isInitialized){
            Log.d(TAG, "ERROR : You should call init first");
            emitSignal("on_plugin_error","You should call init first");
            return;
        }

        activity.runOnUiThread(() -> {
            LevelPlayInterstitialAdListener interstitialListener = new LevelPlayInterstitialAdListener() {
                @Override
                public void onAdLoaded(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
                    emitSignal("on_interstitial_loaded");
                    Log.d(TAG, "onInterstitialAdReady: ");
                    IntegrationHelper.validateIntegration(activity);
                }

                @Override
                public void onAdLoadFailed(@NonNull LevelPlayAdError levelPlayAdError) {
                    emitSignal("on_plugin_error",levelPlayAdError.getErrorMessage());
                }

                @Override
                public void onAdInfoChanged(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
                    LevelPlayInterstitialAdListener.super.onAdInfoChanged(levelPlayAdInfo);
                }

                @Override
                public void onAdDisplayed(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
                    emitSignal("on_interstitial_opened");
                    Log.d(TAG, "onInterstitialAdOpened: ");
                }

                @Override
                public void onAdDisplayFailed(@NonNull LevelPlayAdError levelPlayAdError, @NonNull LevelPlayAdInfo levelPlayAdInfo) {
                    LevelPlayInterstitialAdListener.super.onAdDisplayFailed(levelPlayAdError, levelPlayAdInfo);
                    Log.e(TAG, "onInterstitialAdShowFailed: " + levelPlayAdError.getErrorMessage());
                    emitSignal("on_plugin_error",levelPlayAdError.getErrorMessage());
                }

                @Override
                public void onAdClosed(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
                    LevelPlayInterstitialAdListener.super.onAdClosed(levelPlayAdInfo);

                    emitSignal("on_interstitial_closed");
                    mInterstitialAd.loadAd();
                    Log.d(TAG, "onInterstitialAdClosed: ");
                }

                @Override
                public void onAdClicked(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
                    LevelPlayInterstitialAdListener.super.onAdClicked(levelPlayAdInfo);
                }



            };
            mInterstitialAd = new LevelPlayInterstitialAd(interstitialId);
            mInterstitialAd.setListener(interstitialListener);
//            IronSource.init(activity,appKey, IronSource.AD_UNIT.INTERSTITIAL);
            loadInterstitial();
        });
    }

    
    @UsedByGodot
    public void loadInterstitial(){
        if (!isInitialized){
            Log.d(TAG, "ERROR : You should call init first");
            emitSignal("on_plugin_error","You should call init first");
            return;
        }
        mInterstitialAd.loadAd();
    }


    @UsedByGodot
    public void showInterstitial(){
        // Check that ad is ready and that the placement is not capped
        if (mInterstitialAd.isAdReady()) {
            mInterstitialAd.showAd(Objects.requireNonNull(getActivity()));
        }
        else{
            emitSignal("on_plugin_error","Interstitial is NOT READY");
        }

    }


    @UsedByGodot
    public void initBanner(boolean isTop, String bannerId){
        if (!isInitialized){
            Log.d(TAG, "ERROR : You should call init first");
            emitSignal("on_plugin_error","You should call init first");
            return;
        }
        activity.runOnUiThread(() -> {
            LevelPlayAdSize adSize = LevelPlayAdSize.createAdaptiveAdSize(Objects.requireNonNull(getActivity()));
            LevelPlayBannerAdView levelPlayBanner = new LevelPlayBannerAdView(getActivity(), bannerId);
            if (adSize != null) {
                levelPlayBanner.setAdSize(adSize);
                // To get actual banner layout size (either custom/standard size or adaptive)
                bannerHeight = adSize.getHeight();
                bannerWidth = adSize.getWidth();
            }

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    isTop? Gravity.TOP : Gravity.BOTTOM);

            layout.addView(levelPlayBanner,0,layoutParams);


            LevelPlayBannerAdViewListener bannerListener = new LevelPlayBannerAdViewListener() {
                @Override
                public void onAdClicked(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
                    LevelPlayBannerAdViewListener.super.onAdClicked(levelPlayAdInfo);
                }

                @Override
                public void onAdCollapsed(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
                    LevelPlayBannerAdViewListener.super.onAdCollapsed(levelPlayAdInfo);
                }

                @Override
                public void onAdDisplayFailed(@NonNull LevelPlayAdInfo levelPlayAdInfo, @NonNull LevelPlayAdError levelPlayAdError) {
                    LevelPlayBannerAdViewListener.super.onAdDisplayFailed(levelPlayAdInfo, levelPlayAdError);
                }

                @Override
                public void onAdDisplayed(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
                    LevelPlayBannerAdViewListener.super.onAdDisplayed(levelPlayAdInfo);
                }

                @Override
                public void onAdExpanded(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
                    LevelPlayBannerAdViewListener.super.onAdExpanded(levelPlayAdInfo);
                }

                @Override
                public void onAdLeftApplication(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
                    LevelPlayBannerAdViewListener.super.onAdLeftApplication(levelPlayAdInfo);
                }

                @Override
                public void onAdLoadFailed(@NonNull LevelPlayAdError levelPlayAdError) {
                    Log.e(TAG, "onBannerAdLoadFailed: " + levelPlayAdError.getErrorMessage());
                    emitSignal("on_plugin_error",levelPlayAdError.getErrorMessage());
                }

                @Override
                public void onAdLoaded(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
                    emitSignal("on_banner_loaded");
                    Log.d(TAG, "onBannerAdLoaded: ");
                }


            };
            levelPlayBanner.setBannerListener(bannerListener);
            levelPlayBanner.loadAd();

        });
    }


    @UsedByGodot
    public void showBanner(){
        if(levelPlayBanner == null){
            return;
        }
        levelPlayBanner.setVisibility(View.VISIBLE);
    }

    
    @UsedByGodot
    public void hideBanner() {
        if(levelPlayBanner == null){
            return;
        }
        levelPlayBanner.setVisibility(View.INVISIBLE);
    }


    @Override
    public void onMainDestroy() {
        levelPlayBanner.destroy();
        super.onMainDestroy();
    }
}
