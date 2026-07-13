package com.example.diabite.domain.usecase

import com.example.diabite.data.model.User
import com.example.diabite.domain.repository.AuthRepository
import com.example.diabite.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(email: String, password: String): Flow<Resource<User>> {
        return authRepository.login(email, password)
    }
}
