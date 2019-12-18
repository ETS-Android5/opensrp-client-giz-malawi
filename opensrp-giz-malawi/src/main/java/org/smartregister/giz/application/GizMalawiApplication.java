package org.smartregister.giz.application;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatDelegate;
import android.util.DisplayMetrics;
import android.util.Pair;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.evernote.android.job.JobManager;

import org.jetbrains.annotations.NotNull;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.anc.library.AncLibrary;
import org.smartregister.anc.library.activity.ActivityConfiguration;
import org.smartregister.anc.library.util.DBConstantsUtils;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.activity.BaseChildFormActivity;
import org.smartregister.child.domain.ChildMetadata;
import org.smartregister.commonregistry.CommonFtsObject;
import org.smartregister.configurableviews.ConfigurableViewsLibrary;
import org.smartregister.configurableviews.helper.JsonSpecHelper;
import org.smartregister.giz.BuildConfig;
import org.smartregister.giz.activity.AncRegisterActivity;
import org.smartregister.giz.activity.ChildImmunizationActivity;
import org.smartregister.giz.activity.ChildProfileActivity;
import org.smartregister.giz.activity.LoginActivity;
import org.smartregister.giz.activity.OpdFormActivity;
import org.smartregister.giz.configuration.GizOpdRegisterRowOptions;
import org.smartregister.giz.configuration.GizOpdRegisterSwitcher;
import org.smartregister.giz.configuration.OpdRegisterQueryProvider;
import org.smartregister.giz.job.GizMalawiJobCreator;
import org.smartregister.giz.processor.GizMalawiProcessorForJava;
import org.smartregister.giz.repository.GizMalawiRepository;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.giz.util.GizUtils;
import org.smartregister.giz.util.VaccineDuplicate;
import org.smartregister.growthmonitoring.GrowthMonitoringConfig;
import org.smartregister.growthmonitoring.GrowthMonitoringLibrary;
import org.smartregister.growthmonitoring.repository.HeightRepository;
import org.smartregister.growthmonitoring.repository.HeightZScoreRepository;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.growthmonitoring.repository.WeightZScoreRepository;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.VaccineSchedule;
import org.smartregister.immunization.domain.jsonmapping.Vaccine;
import org.smartregister.immunization.domain.jsonmapping.VaccineGroup;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.VaccinateActionUtils;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.opd.OpdLibrary;
import org.smartregister.opd.activity.BaseOpdProfileActivity;
import org.smartregister.opd.configuration.OpdConfiguration;
import org.smartregister.opd.pojos.OpdMetadata;
import org.smartregister.opd.utils.OpdConstants;
import org.smartregister.opd.utils.OpdDbConstants;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.Repository;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.DrishtiSyncScheduler;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.view.activity.DrishtiApplication;
import org.smartregister.view.receiver.TimeChangedBroadcastReceiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class GizMalawiApplication extends DrishtiApplication implements TimeChangedBroadcastReceiver.OnTimeChangedListener {

    private static CommonFtsObject commonFtsObject;
    private static JsonSpecHelper jsonSpecHelper;
    private String password;
    private boolean lastModified;
    private ECSyncHelper ecSyncHelper;

    private EventClientRepository eventClientRepository;
    private static List<VaccineGroup> vaccineGroups;

    public static JsonSpecHelper getJsonSpecHelper() {
        return jsonSpecHelper;
    }

    public static CommonFtsObject createCommonFtsObject(android.content.Context context) {
        if (commonFtsObject == null) {
            commonFtsObject = new CommonFtsObject(getFtsTables());
            for (String ftsTable : commonFtsObject.getTables()) {
                commonFtsObject.updateSearchFields(ftsTable, getFtsSearchFields(ftsTable));
                commonFtsObject.updateSortFields(ftsTable, getFtsSortFields(ftsTable, context));
            }
        }
        commonFtsObject.updateAlertScheduleMap(getAlertScheduleMap(context));

        return commonFtsObject;
    }

    private static String[] getFtsTables() {
        return new String[]{GizConstants.TABLE_NAME.CHILD, DBConstantsUtils.WOMAN_TABLE_NAME, OpdDbConstants.KEY.TABLE};
    }

    private static String[] getFtsSearchFields(String tableName) {
        if (tableName.equals(GizConstants.TABLE_NAME.CHILD)) {
            return new String[]{GizConstants.KEY.ZEIR_ID, GizConstants.KEY.FIRST_NAME, GizConstants.KEY.LAST_NAME};
        } else if (tableName.equalsIgnoreCase(DBConstantsUtils.WOMAN_TABLE_NAME)) {
            return new String[]{DBConstantsUtils.KeyUtils.FIRST_NAME, DBConstantsUtils.KeyUtils.LAST_NAME, DBConstantsUtils.KeyUtils.ANC_ID};
        } else if (tableName.equals(OpdDbConstants.KEY.TABLE)) {
            return new String[]{OpdDbConstants.KEY.FIRST_NAME, OpdDbConstants.KEY.LAST_NAME, OpdDbConstants.KEY.OPENSRP_ID};
        }

        return null;
    }

    private static String[] getFtsSortFields(String tableName, android.content.Context context) {
        if (tableName.equals(GizConstants.TABLE_NAME.CHILD)) {

            List<VaccineGroup> vaccines = getVaccineGroups(context);
            List<String> names = new ArrayList<>();
            names.add(GizConstants.KEY.FIRST_NAME);
            names.add(GizConstants.KEY.DOB);
            names.add(GizConstants.KEY.ZEIR_ID);
            names.add(GizConstants.KEY.LAST_INTERACTED_WITH);
            names.add(GizConstants.KEY.INACTIVE);
            names.add(GizConstants.KEY.LOST_TO_FOLLOW_UP);
            names.add(GizConstants.KEY.DOD);
            names.add(GizConstants.KEY.DATE_REMOVED);

            for (VaccineGroup vaccineGroup : vaccines) {
                populateAlertColumnNames(vaccineGroup.vaccines, names);
            }

            return names.toArray(new String[names.size()]);
        } else if (tableName.equals(DBConstantsUtils.WOMAN_TABLE_NAME)) {
            return new String[]{DBConstantsUtils.KeyUtils.BASE_ENTITY_ID, DBConstantsUtils.KeyUtils.FIRST_NAME, DBConstantsUtils.KeyUtils.LAST_NAME,
                    DBConstantsUtils.KeyUtils.LAST_INTERACTED_WITH, OpdDbConstants.KEY.REGISTER_ID, DBConstantsUtils.KeyUtils.DATE_REMOVED,
                    DBConstantsUtils.KeyUtils.NEXT_CONTACT_DATE, DBConstantsUtils.KeyUtils.CONTACT_STATUS, DBConstantsUtils.KeyUtils.EDD};
        } else if (tableName.equals(OpdDbConstants.Table.EC_CLIENT)) {
            return new String[]{OpdDbConstants.KEY.BASE_ENTITY_ID, OpdDbConstants.KEY.FIRST_NAME, OpdDbConstants.KEY.LAST_NAME,
                    OpdDbConstants.KEY.LAST_INTERACTED_WITH, OpdDbConstants.KEY.DATE_REMOVED};
        }

        return null;
    }

    private static void populateAlertColumnNames(List<Vaccine> vaccines, List<String> names) {

        for (Vaccine vaccine : vaccines)
            if (vaccine.getVaccineSeparator() != null && vaccine.getName().contains(vaccine.getVaccineSeparator().trim())) {
                String[] individualVaccines = vaccine.getName().split(vaccine.getVaccineSeparator().trim());

                List<Vaccine> vaccineList = new ArrayList<>();
                for (String individualVaccine : individualVaccines) {
                    Vaccine vaccineClone = new Vaccine();
                    vaccineClone.setName(individualVaccine.trim());
                    vaccineList.add(vaccineClone);

                }
                populateAlertColumnNames(vaccineList, names);


            } else {

                names.add("alerts." + VaccinateActionUtils.addHyphen(vaccine.getName()));
            }
    }


    private static void populateAlertScheduleMap(List<Vaccine> vaccines, Map<String, Pair<String, Boolean>> map) {

        for (Vaccine vaccine : vaccines)
            if (vaccine.getVaccineSeparator() != null && vaccine.getName().contains(vaccine.getVaccineSeparator().trim())) {
                String[] individualVaccines = vaccine.getName().split(vaccine.getVaccineSeparator().trim());

                List<Vaccine> vaccineList = new ArrayList<>();
                for (String individualVaccine : individualVaccines) {
                    Vaccine vaccineClone = new Vaccine();
                    vaccineClone.setName(individualVaccine.trim());
                    vaccineList.add(vaccineClone);

                }
                populateAlertScheduleMap(vaccineList, map);


            } else {

                map.put(vaccine.name, Pair.create(GizConstants.TABLE_NAME.CHILD, false));
            }
    }

    private static Map<String, Pair<String, Boolean>> getAlertScheduleMap(android.content.Context context) {
        List<VaccineGroup> vaccines = getVaccineGroups(context);

        Map<String, Pair<String, Boolean>> map = new HashMap<>();

        for (VaccineGroup vaccineGroup : vaccines) {
            populateAlertScheduleMap(vaccineGroup.vaccines, map);
        }
        return map;
    }

    public static synchronized GizMalawiApplication getInstance() {
        return (GizMalawiApplication) mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        context = Context.getInstance();

        String lang = GizUtils.getLanguage(getApplicationContext());
        Locale locale = new Locale(lang);
        Resources res = getApplicationContext().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = locale;
        res.updateConfiguration(conf, dm);

        context.updateApplicationContext(getApplicationContext());
        context.updateCommonFtsObject(createCommonFtsObject(context.applicationContext()));

        //Initialize Modules
        CoreLibrary.init(context, new GizMalawiSyncConfiguration(), BuildConfig.BUILD_TIMESTAMP);

        GrowthMonitoringConfig growthMonitoringConfig = new GrowthMonitoringConfig();
        GrowthMonitoringLibrary.init(context, getRepository(), BuildConfig.VERSION_CODE, BuildConfig.DATABASE_VERSION,
                growthMonitoringConfig);
        ImmunizationLibrary.init(context, getRepository(), createCommonFtsObject(context.applicationContext()), BuildConfig.VERSION_CODE,
                BuildConfig.DATABASE_VERSION);
        fixHardcodedVaccineConfiguration();

        ConfigurableViewsLibrary.init(context, getRepository());
        ChildLibrary.init(context, getRepository(), getMetadata(), BuildConfig.VERSION_CODE, BuildConfig.DATABASE_VERSION);

        ActivityConfiguration activityConfiguration = new ActivityConfiguration();
        activityConfiguration.setHomeRegisterActivityClass(AncRegisterActivity.class);
        AncLibrary.init(context, getRepository(), BuildConfig.DATABASE_VERSION, activityConfiguration);

        OpdMetadata opdMetadata = new OpdMetadata(OpdConstants.JSON_FORM_KEY.NAME, OpdDbConstants.KEY.TABLE,
                OpdConstants.EventType.OPD_REGISTRATION, OpdConstants.EventType.UPDATE_OPD_REGISTRATION,
                OpdConstants.CONFIG, OpdFormActivity.class, BaseOpdProfileActivity.class, true);

        OpdConfiguration opdConfiguration = new OpdConfiguration.Builder(OpdRegisterQueryProvider.class)
                .setOpdMetadata(opdMetadata)
                .setOpdRegisterRowOptions(GizOpdRegisterRowOptions.class)
                .setOpdRegisterSwitcher(GizOpdRegisterSwitcher.class)
                .build();

        OpdLibrary.init(context, getRepository(), opdConfiguration, BuildConfig.VERSION_CODE, BuildConfig.DATABASE_VERSION);

        Fabric.with(this, new Crashlytics.Builder().core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build());

        initRepositories();
        initOfflineSchedules();

        SyncStatusBroadcastReceiver.init(this);
        LocationHelper.init(GizUtils.ALLOWED_LEVELS, GizUtils.DEFAULT_LOCATION_LEVEL);
        jsonSpecHelper = new JsonSpecHelper(this);

        //init Job Manager
        JobManager.create(this).addJobCreator(new GizMalawiJobCreator());
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

    }

    private ChildMetadata getMetadata() {
        ChildMetadata metadata = new ChildMetadata(BaseChildFormActivity.class, ChildProfileActivity.class,
                ChildImmunizationActivity.class, true);
        metadata.updateChildRegister(GizConstants.JSON_FORM.CHILD_ENROLLMENT, GizConstants.TABLE_NAME.CHILD,
                GizConstants.TABLE_NAME.MOTHER_TABLE_NAME, GizConstants.EventType.CHILD_REGISTRATION,
                GizConstants.EventType.UPDATE_CHILD_REGISTRATION, GizConstants.EventType.OUT_OF_CATCHMENT, GizConstants.CONFIGURATION.CHILD_REGISTER,
                GizConstants.RELATIONSHIP.MOTHER, GizConstants.JSON_FORM.OUT_OF_CATCHMENT_SERVICE);
        return metadata;
    }

    private void initRepositories() {
        weightRepository();
        heightRepository();
        vaccineRepository();
        weightZScoreRepository();
        heightZScoreRepository();
    }

    private void initOfflineSchedules() {
        try {
            List<VaccineGroup> childVaccines = VaccinatorUtils.getSupportedVaccines(this);
            List<Vaccine> specialVaccines = VaccinatorUtils.getSpecialVaccines(this);
            VaccineSchedule.init(childVaccines, specialVaccines, GizConstants.KEY.CHILD);
            //  VaccineSchedule.vaccineSchedules.get(GizConstants.KEY.CHILD).remove("BCG 2");
        } catch (Exception e) {
            Timber.e(e, "GizMalawiApplication --> initOfflineSchedules");
        }
    }

    public WeightRepository weightRepository() {
        return GrowthMonitoringLibrary.getInstance().weightRepository();
    }

    public HeightRepository heightRepository() {
        return GrowthMonitoringLibrary.getInstance().heightRepository();
    }

    public VaccineRepository vaccineRepository() {
        return ImmunizationLibrary.getInstance().vaccineRepository();
    }

    public WeightZScoreRepository weightZScoreRepository() {
        return GrowthMonitoringLibrary.getInstance().weightZScoreRepository();
    }

    public HeightZScoreRepository heightZScoreRepository() {
        return GrowthMonitoringLibrary.getInstance().heightZScoreRepository();
    }

    @Override
    public void logoutCurrentUser() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        getApplicationContext().startActivity(intent);
        context.userService().logoutSession();
    }

    @Override
    public Repository getRepository() {
        try {
            if (repository == null) {
                repository = new GizMalawiRepository(getInstance().getApplicationContext(), context);
            }
        } catch (UnsatisfiedLinkError e) {
            Timber.e(e, "GizMalawiApplication --> getRepository");
        }
        return repository;
    }

    public String getPassword() {
        if (password == null) {
            String username = getContext().userService().getAllSharedPreferences().fetchRegisteredANM();
            password = getContext().userService().getGroupId(username);
        }
        return password;
    }

    public Context getContext() {
        return context;
    }

    @NotNull
    @Override
    public ClientProcessorForJava getClientProcessor() {
        return GizMalawiProcessorForJava.getInstance(this);
    }

    @Override
    public void onTerminate() {
        Timber.i("Application is terminating. Stopping sync scheduler and resetting isSyncInProgress setting.");
        cleanUpSyncState();
        TimeChangedBroadcastReceiver.destroy(this);
        SyncStatusBroadcastReceiver.destroy(this);
        super.onTerminate();
    }

    protected void cleanUpSyncState() {
        try {
            DrishtiSyncScheduler.stop(getApplicationContext());
            context.allSharedPreferences().saveIsSyncInProgress(false);
        } catch (Exception e) {
            Timber.e(e, "GizMalawiApplication --> cleanUpSyncState");
        }
    }

    @Override
    public void onTimeChanged() {
        context.userService().forceRemoteLogin();
        logoutCurrentUser();
    }

    @Override
    public void onTimeZoneChanged() {
        context.userService().forceRemoteLogin();
        logoutCurrentUser();
    }

    public Context context() {
        return context;
    }

    public EventClientRepository eventClientRepository() {
        if (eventClientRepository == null) {
            eventClientRepository = new EventClientRepository(getRepository());
        }
        return eventClientRepository;
    }

    public RecurringServiceTypeRepository recurringServiceTypeRepository() {
        return ImmunizationLibrary.getInstance().recurringServiceTypeRepository();
    }

    public RecurringServiceRecordRepository recurringServiceRecordRepository() {
        return ImmunizationLibrary.getInstance().recurringServiceRecordRepository();
    }

    public boolean isLastModified() {
        return lastModified;
    }

    public void setLastModified(boolean lastModified) {
        this.lastModified = lastModified;
    }

    public ECSyncHelper getEcSyncHelper() {
        if (ecSyncHelper == null) {
            ecSyncHelper = ECSyncHelper.getInstance(getApplicationContext());
        }
        return ecSyncHelper;
    }

    @VisibleForTesting
    protected void fixHardcodedVaccineConfiguration() {
        VaccineRepo.Vaccine[] vaccines = ImmunizationLibrary.getInstance().getVaccines();

        HashMap<String, VaccineDuplicate> replacementVaccines = new HashMap<>();
        replacementVaccines.put("MR 2", new VaccineDuplicate("MR 2", VaccineRepo.Vaccine.mr1, -1, 548, 183, "child"));
        replacementVaccines.put("BCG 2", new VaccineDuplicate("BCG 2", VaccineRepo.Vaccine.bcg, 1825, 0, 42, "child"));

        for (VaccineRepo.Vaccine vaccine : vaccines) {
            if (replacementVaccines.containsKey(vaccine.display())) {
                VaccineDuplicate vaccineDuplicate = replacementVaccines.get(vaccine.display());

                vaccine.setCategory(vaccineDuplicate.category());
                vaccine.setExpiryDays(vaccineDuplicate.expiryDays());
                vaccine.setMilestoneGapDays(vaccineDuplicate.milestoneGapDays());
                vaccine.setPrerequisite(vaccineDuplicate.prerequisite());
                vaccine.setPrerequisiteGapDays(vaccineDuplicate.prerequisiteGapDays());
            }
        }

        ImmunizationLibrary.getInstance().setVaccines(vaccines);
    }

    public static List<VaccineGroup> getVaccineGroups(android.content.Context context) {
        if (vaccineGroups == null) {

            vaccineGroups = VaccinatorUtils.getVaccineGroupsFromVaccineConfigFile(context, VaccinatorUtils.vaccines_file);
        }

        return vaccineGroups;
    }

    @VisibleForTesting
    public void setVaccineGroups(List<VaccineGroup> vaccines) {
        this.vaccineGroups = vaccines;
    }
}

