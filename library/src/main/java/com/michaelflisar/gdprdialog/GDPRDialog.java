package com.michaelflisar.gdprdialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.michaelflisar.gdprdialog.helper.GDPRViewManager;

public class GDPRDialog extends AppCompatDialogFragment
{
    public static String ARG_PARENT_MUST_IMPLEMENT_CALLBACK = "ARG_PARENT_MUST_IMPLEMENT_CALLBACK";

    private boolean mForceActivityToImplementCallback;
    private GDPRViewManager mViewManager;

    public static GDPRDialog newInstance(GDPRSetup setup, GDPRLocation location) {
        return newInstance(setup, location, true);
    }

    public static GDPRDialog newInstance(GDPRSetup setup, GDPRLocation location, boolean forceActivityToImplementCallback) {
        GDPRDialog dlg = new GDPRDialog();
        Bundle args = GDPRViewManager.createBundle(setup, location);
        args.putBoolean(ARG_PARENT_MUST_IMPLEMENT_CALLBACK, forceActivityToImplementCallback);
        dlg.setArguments(args);
        return dlg;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setCancelable(false);
        if(GDPR.getInstance().getCallback() != null) {
            mViewManager.setCallback(GDPR.getInstance().getCallback(), mForceActivityToImplementCallback);
        } else {
            mViewManager.setCallback(getActivity(), mForceActivityToImplementCallback);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewManager = new GDPRViewManager(getArguments(), savedInstanceState);
        mForceActivityToImplementCallback = getArguments().getBoolean(ARG_PARENT_MUST_IMPLEMENT_CALLBACK);
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        if (mViewManager.handleBackPress()) {
            return;
        }
        if (mViewManager.getSetup().forceSelection()) {
            return;
        }
        onSaveConsentAndCloseDialog();
        super.onDismiss(dialogInterface);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mViewManager.save(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = initView(inflater, container);
        if (!mViewManager.getSetup().noToolbarTheme()) {
            // Set dialog title
            if(mViewManager.getSetup().getCustomTitle().isEmpty()) {
                getDialog().setTitle(R.string.gdpr_dialog_title);
            } else {
                getDialog().setTitle(mViewManager.getSetup().getCustomTitle());
            }
        }
        return view;
    }

    @Override
    public void onDestroy() {
        mViewManager.reset();
        super.onDestroy();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (mViewManager.shouldUseBottomSheet()) {
            BottomSheetDialog dlg = new BottomSheetDialog(getContext(), mViewManager.getSetup().customDialogTheme()){
                @Override
                public void onBackPressed() {
                    if (mViewManager.handleBackPress()) {
                        return;
                    }
                    if (!mViewManager.getSetup().forceSelection()) {
                        dismiss();
                    }
                }
            };
            dlg.setOnShowListener(dialog -> {
                BottomSheetDialog d = (BottomSheetDialog) dialog;
                FrameLayout bottomSheet = d.findViewById(android.support.design.R.id.design_bottom_sheet);
                BottomSheetBehavior behaviour = BottomSheetBehavior.from(bottomSheet);
                // fully expand sheet and disable collapse state
                behaviour.setState(BottomSheetBehavior.STATE_EXPANDED);
                if (mViewManager.getSetup().forceSelection()) {
                    behaviour.setPeekHeight(bottomSheet.getMeasuredHeight());
                } else {
                    behaviour.setPeekHeight(0);
                    behaviour.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                        @Override
                        public void onStateChanged(@NonNull View bottomSheet, int newState) {
                            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                                while (mViewManager.getCurrentStep() != 0) {
                                    mViewManager.handleBackPress();
                                }
                                dismiss();
                            }
                        }

                        @Override
                        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                        }
                    });
                }
            });
            return dlg;
        } else {
            return new AppCompatDialog(getContext(), mViewManager.getSetup().customDialogTheme()) {
                @Override
                public void onBackPressed() {
                    if (mViewManager.handleBackPress()) {
                        return;
                    }
                    if (!mViewManager.getSetup().forceSelection()) {
                        dismiss();
                    }
                }
            };
        }
    }

    private View initView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.gdpr_dialog, container, false);
        mViewManager.init(getActivity(), view, () -> onSaveConsentAndCloseDialog());
        return view;
    }

    private void onSaveConsentAndCloseDialog() {
        if (mViewManager.shouldCloseApp()) {
            if (getActivity() != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getActivity().finishAndRemoveTask();
                } else {
                    ActivityCompat.finishAffinity(getActivity());
                }
            }
        } else {
            dismiss();
        }
        mViewManager.reset();
    }
}