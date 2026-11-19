package io.github.tombolinero.ordo.feature.ranking

import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers
import org.junit.rules.ExternalResource

/**
 * Loads pictures synchronously, so a screenshot never catches one half-loaded.
 * Use together with `LocalImageCrossfade provides false`.
 */
class ImmediateImages : ExternalResource() {
    override fun before() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(context)
                .coroutineContext(Dispatchers.Unconfined)
                .fetcherCoroutineContext(Dispatchers.Unconfined)
                .decoderCoroutineContext(Dispatchers.Unconfined)
                .interceptorCoroutineContext(Dispatchers.Unconfined)
                .build(),
        )
    }

    override fun after() = SingletonImageLoader.reset()
}
