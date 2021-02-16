package org.dedrakot

import org.dedrakot.api.BucketFactory
import org.dedrakot.impl.BucketFactoryImpl
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton

fun filesDI() = DI {
    val pathPrefix = System.getProperty("files.path", "tmp")
    bind<BucketFactory>() with singleton { BucketFactoryImpl(pathPrefix) }
}

