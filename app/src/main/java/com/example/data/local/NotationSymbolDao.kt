package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NotationSymbol
import kotlinx.coroutines.flow.Flow

@Dao
interface NotationSymbolDao {
    @Query("SELECT * FROM notation_symbols ORDER BY symbol ASC")
    fun getAllSymbols(): Flow<List<NotationSymbol>>

    @Query("SELECT * FROM notation_symbols WHERE id = :id LIMIT 1")
    suspend fun getSymbolById(id: String): NotationSymbol?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymbol(symbol: NotationSymbol)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymbols(symbols: List<NotationSymbol>)

    @Update
    suspend fun updateSymbol(symbol: NotationSymbol)

    @Query("DELETE FROM notation_symbols WHERE id = :id")
    suspend fun deleteSymbolById(id: String)

    @Query("SELECT COUNT(*) FROM notation_symbols")
    suspend fun getCount(): Int
}
