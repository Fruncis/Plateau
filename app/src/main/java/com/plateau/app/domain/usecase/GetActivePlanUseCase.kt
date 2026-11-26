package com.plateau.app.domain.usecase

import com.plateau.app.data.repository.PlanRepository
import com.plateau.app.domain.model.PlanDetail
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActivePlanUseCase @Inject constructor(
    private val planRepository: PlanRepository
) {
    operator fun invoke(): Flow<PlanDetail?> = planRepository.getActivePlanDetail()
}