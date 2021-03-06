package net.paulacr.fluffycat.ui.catdashboard

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LifecycleObserver
import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.paulacr.fluffycat.livedata.LiveDataWithValue
import net.paulacr.fluffycat.model.CatImage
import net.paulacr.fluffycat.model.Category

class CatDashboardViewModel(app: Application, private val repository: CatDashboardRepository) : AndroidViewModel(app),
    LifecycleObserver {

    val onDataReady = LiveDataWithValue<List<CatImage>>()
    private val compositeDisposable = CompositeDisposable()
    private var catsDisposable: Disposable? = null

    fun getCats() {

        catsDisposable?.dispose()
        catsDisposable =

            repository.getCachedCategories()
                .map { categoriesList: List<Category> ->
                    categoriesList.first()
                }.flatMap { category: Category ->
                    repository.getCatImages("100", category)
                }.flatMap { catsImageList: List<CatImage> ->
                    Observable.fromIterable(catsImageList)
                }.filter { catImage: CatImage ->
                    !catImage.categories.isNullOrEmpty()
                }.toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result: MutableList<CatImage> ->
                    if (result.isNotEmpty()) {
                        onDataReady.actionOccuredPost(result)
                        Log.i("Log Category ", "$result")
                    }
                }, { error ->
                    Log.e("Error", "error", error)
                })
        catsDisposable?.let {
            compositeDisposable.add(it)
        }
    }

    override fun onCleared() {
        compositeDisposable.clear()
        super.onCleared()
    }

    companion object {
        private const val LIMIT_ITEMS = "10"
    }
}