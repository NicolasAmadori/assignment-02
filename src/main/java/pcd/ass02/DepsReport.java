package pcd.ass02;

import io.reactivex.rxjava3.core.Observable;

public interface DepsReport<T> {

  Observable<T> getElements();

  String getName();
}
