package com.routecj.customer.domain.repository

import com.routecj.customer.domain.model.Customer

interface CustomerRepository {
    suspend fun getCustomer(customerId: String): Result<Customer>
    suspend fun createCustomer(customer: Customer): Result<Unit>
    suspend fun updateCustomer(customer: Customer): Result<Unit>
}
