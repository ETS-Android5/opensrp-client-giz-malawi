package org.smartregister.giz.presenter;

import androidx.annotation.Nullable;

import org.smartregister.giz.contract.ListContract;
import org.smartregister.giz.interactor.ListInteractor;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Callable;

public class ListPresenter<T extends ListContract.Identifiable> implements ListContract.Presenter<T> {

    @Nullable
    private WeakReference<ListContract.View<T>> weakReference;
    private ListContract.Interactor<T> interactor = new ListInteractor<>();
    private ListContract.Model<T> model;

    /**
     * Calling the fetch list method directly from the view may lead to memory leaks,
     * Use this method with caution when on the view
     *
     * @param callable
     */
    @Override
    public void fetchList(Callable<List<T>> callable) {
        ListContract.View<T> currentView = getView();
        if (currentView != null)
            currentView.setLoadingState(true);

        if (interactor != null) {
            interactor.runRequest(callable, this);
        }
    }

    @Override
    public void onItemsFetched(List<T> identifiables) {
        ListContract.View<T> currentView = getView();
        if (currentView == null) return;

        currentView.renderData(identifiables);
        currentView.refreshView();
        currentView.setLoadingState(false);
    }

    @Override
    public <V extends ListContract.View<T>> ListContract.Presenter<T> with(V view) {
        weakReference = new WeakReference<>(view);
        return this;
    }

    @Override
    public <I extends ListContract.Interactor<T>> ListContract.Presenter<T> using(I interactor) {
        this.interactor = interactor;
        return this;
    }

    @Override
    public <M extends ListContract.Model<T>> ListContract.Presenter<T> withModel(M model) {
        this.model = model;
        return this;
    }


    @Nullable
    @Override
    public ListContract.View<T> getView() {
        if (weakReference != null)
            return weakReference.get();

        return null;
    }

    @Override
    public ListContract.Model<T> getModel() {
        return model;
    }
}
