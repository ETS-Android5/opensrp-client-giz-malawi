package org.smartregister.giz.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.giz.R;
import org.smartregister.giz.fragment.EligibleChildrenReportFragment;
import org.smartregister.giz.fragment.FilterReportFragment;
import org.smartregister.view.activity.SecuredActivity;

import timber.log.Timber;

public class FragmentBaseActivity extends SecuredActivity {
    protected static final String DISPLAY_FRAGMENT = "DISPLAY_FRAGMENT";
    protected static final String TITLE = "TITLE";
    protected String INDICATOR_CODE = "INDICATOR_CODE";

    private TextView titleTextView;

    public static void startMe(Activity activity, String fragmentName, String title) {
        Intent intent = new Intent(activity, FragmentBaseActivity.class);
        intent.putExtra(DISPLAY_FRAGMENT, fragmentName);
        intent.putExtra(TITLE, title);
        activity.startActivity(intent);
    }

    public static void startMe(Activity activity, String fragmentName, String title, Bundle bundle) {
        Intent intent = new Intent(activity, FragmentBaseActivity.class);
        intent.putExtras(bundle);
        intent.putExtra(DISPLAY_FRAGMENT, fragmentName);
        intent.putExtra(TITLE, title);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_base);
        Toolbar toolbar = findViewById(R.id.toolbar_top);
        setSupportActionBar(toolbar);

        titleTextView = findViewById(R.id.toolbar_title);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);

            final Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
            upArrow.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
            actionBar.setElevation(0);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String title = bundle.getString(TITLE);
            if (StringUtils.isNotBlank(title)) {
                titleTextView.setText(title);
            }
            INDICATOR_CODE = bundle.getString(INDICATOR_CODE);
            String fragmentName = bundle.getString(DISPLAY_FRAGMENT);
            Fragment fragment = getRequestedFragment(fragmentName);
            if (fragment != null)
                switchToFragment(fragment);

        }

        onCreation();
    }

    public void setTitle(String title) {
        titleTextView.setText(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private void switchToFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .add(R.id.content, fragment)
                .commit();
    }

    private @Nullable Fragment getRequestedFragment(@Nullable String name) {
        if (name == null || StringUtils.isBlank(name))
            return null;

        Fragment fragment;
        switch (name) {
            case FilterReportFragment
                    .TAG:
                fragment = new FilterReportFragment();
                break;
            case EligibleChildrenReportFragment
                    .TAG:
                fragment = new EligibleChildrenReportFragment();
                break;
            default:
                fragment = null;
                break;
        }

        if (fragment != null)
            fragment.setArguments(getIntent().getExtras());

        assert fragment != null;
        return fragment;
    }

    @Override
    protected void onCreation() {
        Timber.v("Empty onCreation");
    }

    @Override
    protected void onResumption() {
        Timber.v("Empty onResumption");
    }
}

