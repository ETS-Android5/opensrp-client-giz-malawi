package org.smartregister.giz.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Years;
import org.json.JSONObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.FetchStatus;
import org.smartregister.giz.R;
import org.smartregister.giz.task.OpdTransferTask;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.growthmonitoring.job.HeightIntentServiceJob;
import org.smartregister.growthmonitoring.job.WeightIntentServiceJob;
import org.smartregister.growthmonitoring.job.ZScoreRefreshIntentServiceJob;
import org.smartregister.immunization.job.VaccineServiceJob;
import org.smartregister.job.ImageUploadServiceJob;
import org.smartregister.job.SyncServiceJob;
import org.smartregister.job.SyncSettingsServiceJob;
import org.smartregister.opd.activity.BaseOpdProfileActivity;
import org.smartregister.opd.utils.FormProcessor;
import org.smartregister.opd.utils.OpdDbConstants;
import org.smartregister.opd.utils.OpdUtils;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.util.Utils;

import java.util.Map;

public class GizOpdProfileActivity extends BaseOpdProfileActivity implements FormProcessor.Host,
        SyncStatusBroadcastReceiver.SyncStatusListener {

    protected static final int REQUEST_CODE_GET_JSON = 3432;
    private FormProcessor.Requester requester;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(org.smartregister.opd.R.menu.menu_opd_profile_activity, menu);

        if (GizConstants.RegisterType.OPD.equalsIgnoreCase(getRegisterType())) {
            MenuItem closeMenu = menu.findItem(org.smartregister.opd.R.id.opd_menu_item_close_client);
            if (closeMenu != null) {
                closeMenu.setEnabled(true);
            }
            buildAllClientsMenu(menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        int id = item.getItemId();
        if (id == R.id.opd_menu_item_enrol_anc) {
            showOpdTransferDialog(GizConstants.EventType.OPD_ANC_TRANSFER);
        } else if (id == R.id.opd_menu_item_enrol_pnc) {
            showOpdTransferDialog(GizConstants.EventType.OPD_PNC_TRANSFER);
        } else if (id == R.id.opd_menu_item_enrol_maternity) {
            showOpdTransferDialog(GizConstants.EventType.OPD_MATERNITY_TRANSFER);
        } else if (id == R.id.opd_menu_item_sync) {
            startSync();
        }

        return true;
    }

    private void startSync() {
        if (!SyncStatusBroadcastReceiver.getInstance().isSyncing()) {
            initiateSync();
            Toast.makeText(this, getResources().getText(R.string.action_start_sync),
                    Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(this, getResources().getText(R.string.sync_in_progress),
                    Toast.LENGTH_SHORT).show();
    }

    private void initiateSync() {
        ImageUploadServiceJob.scheduleJobImmediately(ImageUploadServiceJob.TAG);
        SyncServiceJob.scheduleJobImmediately(SyncServiceJob.TAG);
        SyncSettingsServiceJob.scheduleJobImmediately(SyncSettingsServiceJob.TAG);
        ZScoreRefreshIntentServiceJob.scheduleJobImmediately(ZScoreRefreshIntentServiceJob.TAG);
        WeightIntentServiceJob.scheduleJobImmediately(WeightIntentServiceJob.TAG);
        HeightIntentServiceJob.scheduleJobImmediately(HeightIntentServiceJob.TAG);
        VaccineServiceJob.scheduleJobImmediately(VaccineServiceJob.TAG);
    }

    public void showOpdTransferDialog(@Nullable String eventType) {
        if (StringUtils.isNotBlank(eventType)) {
            AlertDialog dialog = new AlertDialog.Builder(GizOpdProfileActivity.this, R.style.AppThemeAlertDialog)
                    .setCancelable(true)
                    .setMessage(R.string.opd_enrol_client_message)
                    .setPositiveButton("Yes", (dialog12, which) -> new OpdTransferTask(GizOpdProfileActivity.this, eventType, getClient().getCaseId()).execute())
                    .setNegativeButton("no", (dialog1, which) -> {
                        dialog1.dismiss();
                    }).create();

            dialog.show();
        }
    }

    public void buildAllClientsMenu(@NonNull Menu menu) {
        CommonPersonObjectClient client = getClient();
        Map<String, String> detailsMap = client.getDetails();
        String dob = detailsMap.get(OpdDbConstants.Column.Client.DOB);
        String gender = detailsMap.get(OpdDbConstants.Column.Client.GENDER);
        if (StringUtils.isNotBlank(dob)) {
            DateTime age = Utils.dobStringToDateTime(dob);
            if (age != null) {
                int years = Years.yearsBetween(age.toLocalDate(), new DateTime().toLocalDate()).getYears();
                if (years >= 10 && StringUtils.isNotBlank(gender) && gender.toLowerCase().startsWith("f")) {
                    MenuItem maternityMenuItem = menu.findItem(R.id.opd_menu_item_enrol_maternity);
                    maternityMenuItem.setVisible(true);

                    MenuItem ancMenuItem = menu.findItem(R.id.opd_menu_item_enrol_anc);
                    ancMenuItem.setVisible(true);

                    MenuItem pncMenuItem = menu.findItem(R.id.opd_menu_item_enrol_pnc);
                    pncMenuItem.setVisible(true);
                }
            }
        }

    }

  /*  @Override
    protected ViewPager setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        NewOpdProfileOverviewFragment profileOverviewFragment = NewOpdProfileOverviewFragment.newInstance(this.getIntent().getExtras());
        setSendActionListenerForProfileOverview(profileOverviewFragment);

        NewOpdProfileVisitsFragment profileVisitsFragment = NewOpdProfileVisitsFragment.newInstance(this.getIntent().getExtras());
        setSendActionListenerToVisitsFragment(profileVisitsFragment);

        adapter.addFragment(profileOverviewFragment, this.getString(R.string.today));
        adapter.addFragment(profileVisitsFragment, this.getString(R.string.history));

        viewPager.setAdapter(adapter);
        return viewPager;
    }*/

    @Override
    public void startForm(JSONObject jsonObject, Form form, FormProcessor.Requester requester) {
        this.requester = requester;
        Intent intent = new Intent(getApplicationContext(), OpdUtils.metadata().getOpdFormActivity());
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.JSON, jsonObject.toString());
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_GET_JSON && resultCode == Activity.RESULT_OK) {
            String jsonString = data.getStringExtra(JsonFormConstants.JSON_FORM_KEY.JSON);
            if (jsonString != null && requester != null) {
                requester.onFormProcessingResult(jsonString);
                requester = null;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSyncStart() {
        Toast.makeText(this, getResources().getText(R.string.syncing),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSyncInProgress(FetchStatus fetchStatus) {
        // Do nothing
    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        Toast.makeText(this, getResources().getText(R.string.sync_complete),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SyncStatusBroadcastReceiver.getInstance().addSyncStatusListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SyncStatusBroadcastReceiver.getInstance().removeSyncStatusListener(this);
    }
}
