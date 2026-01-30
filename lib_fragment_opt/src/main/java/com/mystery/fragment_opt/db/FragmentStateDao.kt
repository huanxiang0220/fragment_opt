package com.mystery.fragment_opt.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FragmentStateDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: FragmentStateEntity)
    
    @Query("SELECT * FROM fragment_state_cache WHERE tag = :tag")
    suspend fun getState(tag: String): FragmentStateEntity?
    
    @Query("DELETE FROM fragment_state_cache WHERE tag = :tag")
    suspend fun clearState(tag: String)
    
    @Query("DELETE FROM fragment_state_cache")
    suspend fun clearAll()
    
    /**
     * 获取最久未使用的记录
     */
    @Query("SELECT * FROM fragment_state_cache ORDER BY lastActiveTime ASC LIMIT 1")
    suspend fun getOldestState(): FragmentStateEntity?
}
