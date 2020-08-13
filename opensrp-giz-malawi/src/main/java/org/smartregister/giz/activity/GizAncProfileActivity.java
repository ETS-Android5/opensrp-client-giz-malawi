package org.smartregister.giz.activity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONObject;
import org.smartregister.anc.library.activity.ProfileActivity;
import org.smartregister.anc.library.event.ClientDetailsFetchedEvent;
import org.smartregister.anc.library.event.PatientRemovedEvent;
import org.smartregister.anc.library.util.ANCJsonFormUtils;
import org.smartregister.anc.library.util.ConstantsUtils;
import org.smartregister.anc.library.util.Utils;
import org.smartregister.giz.R;
import org.smartregister.giz.util.GizUtils;

import java.util.HashMap;

import timber.log.Timber;

public class GizAncProfileActivity extends ProfileActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean b = super.onCreateOptionsMenu(menu);

        if (isFromMaternity()) {
            if (getDueButton() != null) {
                try {
                    ((ViewGroup) getDueButton().getParent()).setVisibility(View.GONE);
                } catch (NullPointerException e) {
                    Timber.e(e);
                }
            }
            View view = findViewById(R.id.btn_profile_registration_info);
            if (view != null)
                view.setEnabled(false);

            MenuItem menuItem = menu.findItem(R.id.overflow_menu_item);
            if (menuItem != null) {
                menuItem.setEnabled(false);
            }
            return true;
        }
        return b;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && isFromMaternity()) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void startFormForEdit(ClientDetailsFetchedEvent event) {
        if (event != null && event.isEditMode()) {
            String formMetadata = ANCJsonFormUtils.getAutoPopulatedJsonEditRegisterFormString(this, event.getWomanClient());
            try {
                JSONObject form = new JSONObject(formMetadata);
                JSONObject jsonObject = ANCJsonFormUtils.getFieldJSONObject(FormUtils.getMultiStepFormFields(form), ConstantsUtils.JsonFormKeyUtils.PREVIOUS_VISITS);
                if (jsonObject != null) {
                    jsonObject.put(JsonFormConstants.TYPE, JsonFormConstants.HIDDEN);
                }
                ANCJsonFormUtils.startFormForEdit(this, ANCJsonFormUtils.REQUEST_CODE_GET_JSON, form.toString());
            } catch (Exception e) {
                Timber.e(e, " --> startFormForEdit");
            }
        }
    }


    @Override
    public void removePatient(PatientRemovedEvent event) {
        if (event != null) {
            try {
                Utils.removeStickyEvent(event);
                hideProgressDialog();
                GizUtils.showAncMaternityNavigationDialog(event, GizAncProfileActivity.this);
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (isFromMaternity()) {
            finish();
        } else {
            super.onBackPressed();
        }
    }

    private boolean isFromMaternity() {
        try {
            HashMap<String, String> detailMap = (HashMap<String, String>) getIntent().getSerializableExtra(ConstantsUtils.IntentKeyUtils.CLIENT_MAP);
            return (detailMap.get("maternity_history") != null);
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }
}
