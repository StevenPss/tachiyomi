package eu.kanade.tachiyomi.ui.base.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import eu.kanade.tachiyomi.App;
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter;
import nucleus.factory.PresenterFactory;
import nucleus.factory.ReflectionPresenterFactory;
import nucleus.presenter.Presenter;
import nucleus.view.PresenterLifecycleDelegate;
import nucleus.view.ViewWithPresenter;

/**
 * This class is an example of how an activity could controls it's presenter.
 * You can inherit from this class or copy/paste this class's code to
 * create your own view implementation.
 *
 * @param <P> a type of presenter to return with {@link #getPresenter}.
 */
public abstract class BaseRxFragment<P extends Presenter> extends BaseFragment implements ViewWithPresenter<P> {

    private static final String PRESENTER_STATE_KEY = "presenter_state";
    private PresenterLifecycleDelegate<P> presenterDelegate =
            new PresenterLifecycleDelegate<>(ReflectionPresenterFactory.<P>fromViewClass(getClass()));

    /**
     * Returns a current presenter factory.
     */
    public PresenterFactory<P> getPresenterFactory() {
        return presenterDelegate.getPresenterFactory();
    }

    /**
     * Sets a presenter factory.
     * Call this method before onCreate/onFinishInflate to override default {@link ReflectionPresenterFactory} presenter factory.
     * Use this method for presenter dependency injection.
     */
    @Override
    public void setPresenterFactory(PresenterFactory<P> presenterFactory) {
        presenterDelegate.setPresenterFactory(presenterFactory);
    }

    /**
     * Returns a current attached presenter.
     * This method is guaranteed to return a non-null value between
     * onResume/onPause and onAttachedToWindow/onDetachedFromWindow calls
     * if the presenter factory returns a non-null value.
     *
     * @return a currently attached presenter or null.
     */
    public P getPresenter() {
        return presenterDelegate.getPresenter();
    }

    @Override
    public void onCreate(Bundle bundle) {
        final PresenterFactory<P> superFactory = getPresenterFactory();
        setPresenterFactory(new PresenterFactory<P>() {
            @Override
            public P createPresenter() {
                P presenter = superFactory.createPresenter();
                App app = (App) BaseRxFragment.this.getActivity().getApplication();
                app.getComponentReflection().inject(presenter);
                ((BasePresenter) presenter).setContext(app.getApplicationContext());
                return presenter;
            }
        });

        super.onCreate(bundle);
        if (bundle != null)
            presenterDelegate.onRestoreInstanceState(bundle.getBundle(PRESENTER_STATE_KEY));
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBundle(PRESENTER_STATE_KEY, presenterDelegate.onSaveInstanceState());
    }

    @Override
    public void onResume() {
        super.onResume();
        presenterDelegate.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        presenterDelegate.onPause(getActivity().isFinishing() || shouldDestroyPresenter(this));
    }

    private boolean shouldDestroyPresenter(Fragment fragment) {
        if (fragment == null) return false;
        else return fragment.isRemoving() || shouldDestroyPresenter(fragment.getParentFragment());
    }

}