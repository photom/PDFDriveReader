package com.hitsuji.pdfdrivereader.domain.model

/**
 * Standardized result wrapper for Domain operations.
 */
sealed class DomainResult<out T> {
    /** Successful operation with result [data] */
    data class Success<out T>(val data: T) : DomainResult<T>()
    
    /** Failed operation with descriptive [message] and optional [error] */
    data class Error(val message: String, val error: Throwable? = null) : DomainResult<Nothing>()
    
    /** Ongoing operation */
    object Loading : DomainResult<Nothing>()
}
