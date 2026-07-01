package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.ReadVerseDao
import kotlinx.coroutines.flow.Flow

class SpiritualPathRepository(private val readVerseDao: ReadVerseDao) {

    val karmaYogaCount: Flow<Int> = readVerseDao.getKarmaYogaReadCountFlow()
    val bhaktiYogaCount: Flow<Int> = readVerseDao.getBhaktiYogaReadCountFlow()
    val jnanaYogaCount: Flow<Int> = readVerseDao.getJnanaYogaReadCountFlow()
    val dhyanaYogaCount: Flow<Int> = readVerseDao.getDhyanaYogaReadCountFlow()
    val rajaYogaCount: Flow<Int> = readVerseDao.getRajaYogaReadCountFlow()
}
