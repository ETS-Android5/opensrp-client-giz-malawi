package org.smartregister.giz.interactor;

import org.smartregister.child.util.AppExecutors;
import org.smartregister.giz.contract.FindReportContract;
import org.smartregister.giz.dao.ReportDao;

import java.util.Map;

import timber.log.Timber;

public class FindReportInteractor implements FindReportContract.Interactor {

    protected AppExecutors appExecutors;

    public FindReportInteractor() {
        appExecutors = new AppExecutors();
    }

    @Override
    public void processAvailableLocations(Map<String, String> locations, FindReportContract.Presenter presenter) {
        Runnable runnable = () -> {
            try {
                Map<String, String> hierarchy = ReportDao.extractRecordedLocations();

                appExecutors.mainThread().execute(() -> presenter.onReportHierarchyLoaded(hierarchy));
            } catch (Exception e) {
                Timber.e(e);
            }
        };
        appExecutors.diskIO().execute(runnable);
    }
}